/******************************************************************
 * File:        StoreImpl.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.store.deflt;

import java.util.ArrayList;
import java.util.List;

import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.server.core.Store;
import com.epimorphics.uklregistry.store.Description;
import com.epimorphics.uklregistry.store.Register;
import com.epimorphics.uklregistry.store.RegisterItem;
import com.epimorphics.uklregistry.store.StoreAPI;
import com.epimorphics.uklregistry.vocab.Registry;
import com.epimorphics.util.PrefixUtils;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Default store interface for POC which uses direct access to a local TDB.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class StoreImpl implements StoreAPI {
    public static final String PREFIXES_FILE = "prefixes.ttl";
    
    Store store = ServiceConfig.get().getDefaultStore();
    Model prefixes = FileManager.get().loadModel(PREFIXES_FILE);

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
            Resource root = d.getRoot();
            if (root.hasProperty(RDF.type)) {
                if (root.hasProperty( RDF.type, Registry.Register)) {
                    return new Register(d);
                } else if (root.hasProperty( RDF.type, Registry.RegisterItem)) {
                    return new RegisterItem(root);
                } else {
                    return d;
                }
            } else {
                return null;
            }
        } finally {
            store.unlock();
        }
    }
    
    public List<Resource> fetchDescriptionsOf(String selectQuery, Model model) {
        String expandedQuery = PrefixUtils.expandQuery(selectQuery, prefixes);
        List<Resource> results = new ArrayList<Resource>();
        store.lock();
        Model src = store.getUnionModel();
        QueryExecution qexec = QueryExecutionFactory.create(expandedQuery, src);
        try {
            ResultSet matches = qexec.execSelect();
            while (matches.hasNext()) {
                Resource r = matches.nextSolution().getResource("item");
                results.add(r);
                Closure.closure(r.inModel(src), false, model);
            }
        } finally {
            qexec.close();
            store.unlock();
        }
        return results;
    }

}
