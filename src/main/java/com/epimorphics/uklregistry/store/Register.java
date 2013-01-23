/******************************************************************
 * File:        Register.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.store;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Abstraction for access to a Register. The intention is that
 * implementations typically retreive a complete description from
 * the underlying store. Changes are made to a local in-memory model and
 * then updates are flushed out.
 *  
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface Register {

    /**
     * Return the register resource within the local in-memory cached copy of the register.
     */
    public Resource getRegister();
    
}
