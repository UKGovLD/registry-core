/******************************************************************
 * File:        ProcessIfChanges.java
 * Created by:  Dave Reynolds
 * Created on:  6 May 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for a message process that will only trigger the wrapped
 * process if the event changes (directly or indirectly) some target register.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ProcessIfChanges implements MessagingService.Process {
    static final Logger log = LoggerFactory.getLogger( ProcessIfChanges.class );
    
    String target;
    MessagingService.Process process;
    
    public ProcessIfChanges(MessagingService.Process process, String target) {
        this.target = target;
        this.process = process;
    }

    @Override
    public void processMessage(Message message) {
        log.debug("Received " + message.getOperation() + " on " + message.getTarget());
        if (message.getTarget().startsWith(target)) {
            process.processMessage(message);
        }
    }
}
