/******************************************************************
 * File:        ForwardingService.java
 * Created by:  Dave Reynolds
 * Created on:  18 Feb 2013
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

package com.epimorphics.registry.core;

import java.util.List;


/**
 * Interface for a service that configures proxy/forwarding support for the register.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface ForwardingService {

    /**
     * Update the forwarding table to reflect a change in registration or status
     * of an item.
     * @param item The registered item, with associated entity information
     */
    public void update(RegisterItem item);

    /** Register a new forwarding spec at its target path */
    public void register(ForwardingRecord record);

    /** Remove an existing forwarding instructions for the given path */
    public void unregister(String path);

    /** Look up a URI to find any forwarding instructions that match it */
    public MatchResult match(String path);

    /** Call to finalize installation of registrations, e.g. by configuring an external proxy */
    public void updateConfig();

    /** Find all DelegatedRecords below the given path */
    public List<DelegationRecord> listDelegations(String path);
}
