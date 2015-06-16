/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epimorphics.registry.webapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;

import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.util.NameUtils;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Test for concurrency issues by hammering a test instance with parallel reads and interspersed writes.
 * Not run as part of routine unit tests - used for soak testing.
 */
public class ConcurrencyTest extends TomcatTestBase {
    static final String REG1 = BASE_URL + "reg1";
    static final String REG1_URI = ROOT_REGISTER + "reg1";
    static final String REG1_ITEM = ROOT_REGISTER + "_reg1";

    protected Random rand = new Random();
    protected Thread[] readThreads;
    protected Thread  writeThread;
    protected int nthreads;
    
    @Override
    String getWebappRoot() {
        return "src/test/webapp";
    }

    public ConcurrencyTest(int nthreads) {
        this.nthreads = nthreads;
        readThreads = new Thread[nthreads];
    }
    
    public static void main(String[] args) {
        ConcurrencyTest test = new ConcurrencyTest(2);
        test.run(2, 2, 200);
    }
    
    public void run(final int nreads, final int nwrites, final int delay) {
        try {
            long start = System.currentTimeMillis();
            containerStart();
            initRegister();
            registerItem(nwrites);

            // set up threads
            for (int t = 0; t < nthreads; t++) {
                readThreads[t] = new Thread( new ReadWorker(nreads, delay) );
            }
            int wdelay = nreads * delay / nwrites;
            writeThread = new Thread( new WriteWorker(nwrites - 1, wdelay) );
            
            // Start running
            for (int t = 0; t < nthreads; t++) {
                readThreads[t].start();
            }
            writeThread.start();
            
            // Wait for completion
            writeThread.join();
            for (int t = 0; t < nthreads; t++) {
                readThreads[t].join();
            }
            
            // Check
            assertEquals( nwrites, registerList() );
            
            long duration = System.currentTimeMillis() - start;
            System.out.println( String.format("Completed %d reads in %s", nreads, NameUtils.formatDuration(duration)) );
            
        } catch (Exception e) {
            System.err.println("Exception: " + e);
        } finally {
            try {
                containerStop();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    protected void initRegister() {
        ClientResponse response = postFile("test/reg1.ttl", BASE_URL, "text/turtle");
        assertEquals("Register a register", 201, response.getStatus());
        assertEquals(REG1_ITEM, response.getLocation().toString());
    }
    
    protected void registerItem(int itemNumber) {
        Model m = ModelFactory.createDefaultModel();
        m.createResource("i" + itemNumber)
            .addProperty(RDF.type, m.createResource("http://localhost/test/Item"))
            .addProperty(RDFS.label, "Item " + itemNumber);
        assertEquals(201, postModel(m, REG1).getStatus());
        System.out.println("Registered item " + itemNumber);
    }
    
    protected int registerList() {
//        System.out.println("Checking register list");
        Model m = getModelResponse(REG1 + "?status=any");
        Resource register = m.getResource(REG1_URI);
        assertTrue( m.contains(register, RDF.type, RegistryVocab.Register) );
        List<RDFNode> members = m.listObjectsOfProperty(SKOS.member).toList();
        return members.size();
    }
    
    protected boolean skewedDelay(int averageDelay) {
        long delay = averageDelay + ((rand.nextInt(100) - 50)/100);
        try {
            Thread.sleep(delay);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
    
    class ReadWorker implements Runnable {
        int iterations;
        int delay;
        
        public ReadWorker(int iterations, int delay) {
            this.iterations = iterations;
            this.delay = delay;
        }
        
        @Override
        public void run() {
            for (int i = 0; i < iterations; i++) {
                if ( !skewedDelay(delay) ) return;
                try {
                    assertTrue( registerList() > 0 );
                } catch (Exception e) {
                    System.err.println("Error during read: " + e);
                }
            }
        }
        
    }
    
    class WriteWorker implements Runnable {
        int iterations;
        int delay;
        
        public WriteWorker(int iterations, int delay) {
            this.iterations = iterations;
            this.delay = delay;
        }
        
        @Override
        public void run() {
            for (int i = 0; i < iterations; i++) {
                if ( !skewedDelay(delay) ) return;
                registerItem(i);
            }
        }
        
    }
}
