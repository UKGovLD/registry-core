/******************************************************************
 * File:        RegisterImpl.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import java.util.List;

import com.epimorphics.uklregistry.store.StoreAPI;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Abstraction for access to a Register.
 */
public class Register extends Description {
    protected List<RegisterItem> items;
    
    public Register(Resource root) {
        super( root );
    }

    public Register(Description d) {
        super( d.root );
    }

    public Resource getRegister() {
        return getRoot();
    }

    /**
     * Find all members of the register, fetch their entity definitions and add them to the description model
     */
    public void fetchMembers(StoreAPI store) {
        items = store.fetchMembers(this, true);
    }
}
