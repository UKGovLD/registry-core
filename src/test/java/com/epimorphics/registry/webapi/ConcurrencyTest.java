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

import com.epimorphics.util.NameUtils;

/**
 * Test for concurrency issues by hammering a test instance with parallel reads and interspersed writes.
 * Not run as part of routine unit tests - used for soak testing.
 */
public class ConcurrencyTest extends TomcatTestBase {
    protected int nthreads = 1;
    
    @Override
    String getWebappRoot() {
        return "src/test/webapp";
    }

    public ConcurrencyTest(int nthreads) {
        this.nthreads = nthreads;
    }
    
    public void run(int niterations) {
        try {
            long start = System.currentTimeMillis();
            containerStart();
            for (int iteration = 0; iteration < niterations; iteration++) {
                
            }
            containerStop();
            
            long duration = System.currentTimeMillis() - start;
            System.out.println( String.format("Completed %d iterations in %s", niterations, NameUtils.formatDuration(duration)) );
            
        } catch (Exception e) {
            System.err.println("Exception: " + e);
        }
    }
    
    public static void main(String[] args) {
        ConcurrencyTest test = new ConcurrencyTest(10);
        test.run(1);
    }
}
