/******************************************************************
 * File:        MessagingService.java
 * Created by:  Dave Reynolds
 * Created on:  6 May 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.message;

/**
 * Signature of a service that delivers returns notification messages.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface MessagingService {

    public interface Process {
        /**
         * Process a received message
         */
        public void processMessage(Message message);
    }
    
    /**
     * Send a message to all channel subscribers
     */
    public void sendMessage(Message message);
    
    /**
     * Register a process callback to be invoked whenever a message is received
     */
    public void processMessages(Process process);
    
}
