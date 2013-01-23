/******************************************************************
 * File:        RegisterImpl.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.store.deflt;

import com.epimorphics.uklregistry.store.Register;
import com.epimorphics.uklregistry.store.StoreAPI;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class RegisterImpl implements Register {

    protected StoreAPI store;
    protected Model model;
    protected Resource register;

    public RegisterImpl(String uri, Model model, StoreAPI store) {
        this.model = model;
        this.store = store;
        this.register = model.createResource(uri);
    }
    
    @Override
    public Resource getRegister() {
        return register;
    }

}
