/******************************************************************
 * File:        StoreAPI.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.store;

/**
 * Abstract interface onto the underlying versioned RDF storage and
 * indexing system. Should support non-RDF storage options as well as
 * straight triple stores.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface StoreAPI {

    /**
     * Return the register metadata for the given register or null
     * if there is no such register recorded.
     */
    public Register getRegister(String uri);
}
