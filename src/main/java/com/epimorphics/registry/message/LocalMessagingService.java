/******************************************************************
 * File:        LocalMessagingService.java
 * Created by:  Dave Reynolds
 * Created on:  6 May 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;

/**
 * Trivial, in-process version of a messaging service.
 * Processing is done asychronously on a separate thread.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class LocalMessagingService extends ServiceBase implements Service, MessagingService {
    protected Executor executor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
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
    public void sendMessage(Message message) {
        for (Process p : processors) {
            executor.execute( new Task(message, p) );
        }
    } 

    @Override
    public void processMessages(Process process) {
        processors.add(process);
    }

}
