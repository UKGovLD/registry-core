/******************************************************************
 * File:        RegisterImpl.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.store;

import com.epimorphics.uklregistry.store.Register;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/*
 * Abstraction for access to a Register. The intention is that
 * implementations typically retreive a complete description from
 * the underlying store. Changes are made to a local in-memory model and
 * then updates are flushed out.
 */
public class Register extends Description {

    protected StoreAPI store;

    public Register(String uri, Model model, StoreAPI store) {
        super( model.createResource(uri) );
        this.store = store;
    }

    public Resource getRegister() {
        return getRoot();
    }

}
