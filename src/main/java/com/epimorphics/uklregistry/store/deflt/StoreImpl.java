/******************************************************************
 * File:        StoreImpl.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.store.deflt;

import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.server.core.Store;
import com.epimorphics.uklregistry.store.Description;
import com.epimorphics.uklregistry.store.Register;
import com.epimorphics.uklregistry.store.StoreAPI;
import com.epimorphics.uklregistry.vocab.Registry;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Default store interface for POC which uses direct access to a local TDB.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class StoreImpl implements StoreAPI {

    Store store = ServiceConfig.get().getDefaultStore();

    @Override
    public Register getRegister(String uri) {
        Description register = getDescription(uri);
        if (register instanceof Register) {
            return (Register) register;
        } else {
            return null;
        }
    }

    @Override
    public void storeDescription(Description d) {
            Resource root = d.getRoot();
            store.updateGraph(root.getURI(), root.getModel());
    }

    @Override
    public Description getDescription(String uri) {
        store.lock();
        Model src = store.getUnionModel();
        try {
            Description d = Description.descriptionOf( src.getResource(uri) );
            if (d.getRoot().hasProperty( RDF.type, Registry.Register)) {
                return new Register(d);
            } else {
                // TODO entity and item cases
                return null;
            }
        } finally {
            store.unlock();
        }
    }

}
