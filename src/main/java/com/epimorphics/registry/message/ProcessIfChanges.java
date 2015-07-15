/******************************************************************
 * File:        ProcessIfChanges.java
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
        if (message.getTarget().startsWith(target) || message.getTarget().replace("/_", "/").startsWith(target)) {
            log.debug("Processing message " + message.getOperation() + " on " + message.getTarget());
            process.processMessage(message);
        }
    }
}
