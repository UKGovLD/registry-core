/******************************************************************
 * File:        MessagingService.java
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
