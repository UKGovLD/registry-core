/******************************************************************
 * File:        Entity.java
 * Created by:  Dave Reynolds
 * Created on:  23 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.store;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.util.Closure;

/**
 * Provides core machinery for accessing descriptions of an RDF resource.
 * Allows the original version of a description to be kept in parallel
 * so that diffs can be calculated for some types of store update.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Description {

    protected Model originalModel;
    protected Resource root;

    /**
     * Construct a description from a root resource. This is assumed to already
     * be in a local memory model which can be used as the base model.
     * This constructor should be used when the description has been retrieved
     * from the main store.
     */
    public Description(Resource root) {
        this.root = root;
    }

    /**
     * Construct an empty description. This can't be accessed until
     * a root has been added. This constructor should be used when the
     * description is being created prior to first storage.
     */
    public Description() {
    }

    /**
     * Provide a new root resource and associated model to become the updated version
     * of the description.
     */
    public void setRoot(Resource root) {
        this.root = root;
    }

    /**
     * Stash a copy of the original model, before any modification
     */
    public void keepCopy() {
        originalModel = ModelFactory.createDefaultModel();
        originalModel.add( root.getModel() );
    }

    /**
     * Return the root resource in a local model which can be modified.
     */
    public Resource getRoot() {
        return root;
    }

    /**
     * Return the original unmodified model for use in diff calculations.
     * May be null if the copy wasn't retained.
     */
    public Model getOriginalModel() {
        return originalModel;
    }

    /**
     * Construct a description as a fresh bNode closure starting
     * from the given root.
     */
    public static Description descriptionOf(Resource root) {
        Model d = Closure.closure(root, false);
        root = root.inModel(d);
        return new Description(root);
    }

}
