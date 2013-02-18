/******************************************************************
 * File:        ForwardingService.java
 * Created by:  Dave Reynolds
 * Created on:  18 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;


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

}
