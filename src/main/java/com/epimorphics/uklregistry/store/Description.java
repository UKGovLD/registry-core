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

/**
 * Provides core machinery for accessing descriptions of an RDF resource.
 * This encapsulate a model plus a root resource plus a
 * set of updates to that model.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Description {

    protected Model baseModel;
    protected Model updatedModel;
    protected Resource root;

    /**
     * Construct a description from a root resource. This is assumed to already
     * be in a local memory model which can be used as the base model.
     * This constructor should be used when the description has been retrieved
     * from the main store.
     */
    protected Description(Resource root) {
        baseModel = root.getModel();
        updatedModel = ModelFactory.createDefaultModel();  // TODO make this lazy?
        updatedModel.add( baseModel );
        this.root = root.inModel(updatedModel);
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
        this.updatedModel = root.getModel();
    }

    public Resource getRoot() {
        return root;
    }
}
