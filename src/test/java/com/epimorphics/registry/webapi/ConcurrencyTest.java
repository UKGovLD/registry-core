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

import javax.ws.rs.core.Response;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Test;

import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.util.NameUtils;
import com.epimorphics.vocabs.SKOS;

/**
 * Test for concurrency issues by hammering a test instance with parallel reads and interspersed writes.
 * Not run as part of routine unit tests - used for soak testing.
 */
public class ConcurrencyTest extends TomcatTestBase {
    static final String REG1 = BASE_URL + "reg1";
    static final String REG1_URI = ROOT_REGISTER + "reg1";
    static final String REG1_ITEM = ROOT_REGISTER + "_reg1";
    
    @Override
    public String getWebappRoot() {
        return "src/test/webapp";
    }
    
    @Test
    public void testConcurrency() {
        try {
            createTester(10).run(100, 20, 5);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public static void main(String[] args) throws Exception {
        int nthreads = 10;
        int nreads = 10000;
        int nwrites = 200;
        int delay = 10;
        
        ConcurrencyTest test = new ConcurrencyTest();
        test.containerStart();
        try {
            long start = System.currentTimeMillis();
            test.createTester(nthreads).run(nreads, nwrites, delay);
            long duration = System.currentTimeMillis() - start;
            System.out.println( String.format("Completed %d reads in %s", nreads, NameUtils.formatDuration(duration)) );
        } finally {        
            test.containerStop();
        }
    }
    
    public Tester createTester(int nthreads) {
        return new Tester(nthreads);
    }
    
    public class Tester {

        protected Random rand = new Random();
        protected Thread[] readThreads;
        protected Thread  writeThread;
        protected int nthreads;
        
        public Tester(int nthreads) {
            this.nthreads = nthreads;
            readThreads = new Thread[nthreads];
        }
        

        public void run(final int nreads, final int nwrites, final int delay) throws InterruptedException {
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

        }
    
        protected void initRegister() {
            Response response = postFile("test/reg1.ttl", BASE_URL, "text/turtle");
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
                    assertTrue( registerList() > 0 );
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
}
