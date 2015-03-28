/******************************************************************
 * File:        LocalMessagingService.java
 * Created by:  Dave Reynolds
 * Created on:  6 May 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.registry.message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.core.Shutdown;

/**
 * Trivial, in-process version of a messaging service.
 * Processing is done asychronously on a separate thread.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class LocalMessagingService extends ComponentBase implements MessagingService, Shutdown {
    protected ExecutorService executor = Executors.newSingleThreadExecutor();
//                                 new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    List<Process> processors = new ArrayList<>();

    static class Task implements Runnable {
        Message message;
        Process processor;

        public Task(Message message, Process processor) {
            this.message = message;
            this.processor = processor;
        }

        @Override
        public void run() {
            processor.processMessage(message);
        }
    }

    @Override
    public synchronized void sendMessage(Message message) {
        for (Process p : processors) {
            executor.execute( new Task(message, p) );
        }
    }

    @Override
    public synchronized void processMessages(Process process) {
        processors.add(process);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }


}
