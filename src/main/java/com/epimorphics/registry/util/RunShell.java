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

package com.epimorphics.registry.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.TimerManager;

/**
 * Utility to run a shell script pass simple command line arguments.
 * Returns a Future to determine success of the script.
 * Any script output is logged.
 */
public class RunShell {
    static final Logger log = LoggerFactory.getLogger( RunShell.class );
    
    public static final String DEFAULT_SHELL = "/bin/bash";

    String scriptFile;
    
    public RunShell(String scriptFile) {
        this.scriptFile = scriptFile;
    }
    
    public Future<Boolean> run(String...args) {
        String[] argA = new String[2 + args.length];
        argA[0] = DEFAULT_SHELL;
        argA[1] = scriptFile;
        for (int i = 0; i < args.length; i++) {
            argA[2+i] = args[i];
        }
        ProcessBuilder scriptPB = new ProcessBuilder(argA);
        scriptPB.redirectErrorStream(true);
        
        try {
            Process scriptProcess = scriptPB.start();
            Future<Boolean> scriptStatus = TimerManager.get().submit( new TrackShell(scriptProcess) );
            return scriptStatus;
        } catch (IOException e) {
            log.error("Error invoking script: " + scriptFile, e);
            return ConcurrentUtils.constantFuture(false);
        }
    }
    
    public class TrackShell implements Callable<Boolean> {
        Process process;
        BufferedReader in;
        
        public TrackShell( Process process ) {
            in = new BufferedReader( new InputStreamReader(process.getInputStream()) );
        }
        
        @Override
        public Boolean call() throws Exception {
            try {
                String line = null;
                while( (line = in.readLine()) != null ) {
                    log.info(line);
                }
            } catch (IOException e) {
                log.error("Error in reading script output: " + e);
            }
            
            int status = process.waitFor();
            return status == 0;
        }
        
    }

}
