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

    /**
     * Return the register/item/entity at the given address or null if there is none such
     */
    public Description getDescription(String uri);

    // TODO fetch item/entity

    // TODO List register entities

    // TODO general search support

    // TODO decide whether to provide a batched set of updates or update on the fly

    /**
     * Save a description to the default graph
     */
    public void storeDescription(Description d);

}
