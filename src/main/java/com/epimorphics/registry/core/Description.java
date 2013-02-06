/******************************************************************
 * File:        Entity.java
 * Created by:  Dave Reynolds
 * Created on:  23 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.util.VersionUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Provides core machinery for accessing descriptions of an RDF resource.
 * Allows the original version of a description to be kept in parallel
 * so that diffs can be calculated for some types of store update.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Description {

    protected Resource root;

    // TODO the recording of changes is currently not used either remove or extend to entities so it can be used uniformly
    protected List<Statement> removals = new ArrayList<Statement>();
    protected List<Statement> additions = new ArrayList<Statement>();

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
     * Construct a description as a fresh bNode closure starting
     * from the given root.
     */
    public static Description descriptionOf(Resource root) {
        Model d = Closure.closure(root, false);
        root = root.inModel(d);
        return new Description(root);
    }

    /**
     * Return a typed description wrapper round a resource retrieved from a store
     */
    public static Description descriptionFrom(Resource root, StoreAPI store) {
        if (root.hasProperty(RDF.type)) {
            if (root.hasProperty( RDF.type, RegistryVocab.Register)) {
                Register reg = new Register(root);
                reg.setStore(store);
                return reg;
            } else if (root.hasProperty( RDF.type, RegistryVocab.RegisterItem)) {
                return new RegisterItem(root);
            } else {
                return new Description(root);
            }
        } else {
            return null;
        }
    }
    
    /**
     * Provide a new root resource and associated model to become the updated version
     * of the description.
     */
    public void setRoot(Resource root) {
        this.root = root;
    }

    /**
     * Return the root resource in a local model. Should be used only
     * for retrival, all modifications should be done via the other Description methods.
     */
    public Resource getRoot() {
        return root;
    }

    /**
     * Return the description as a Register
     */
    public Register asRegister() {
        return (Register)this;
    }

    /**
     * Return the description as a RegisterItem
     */
    public RegisterItem asRegisterItem() {
        return (RegisterItem)this;
    }
    /**
     * Flatten versioning information
     */
    public Description flatten() {
        doFlatten(root);
        return this;
    }

    protected void doFlatten(Resource r) {
        ResIterator it = r.getModel().listSubjectsWithProperty(DCTerms.isVersionOf, r);
        while (it.hasNext()) {
            VersionUtil.flatten(r, it.next());
        }
    }

    // TODO add hook for validation of changes before they occur?

    /**
     * Replace the current value(s), if any, of the given property by the supplied value
     */
    public Description setProperty(Property p, RDFNode value) {
        remove(p);
        root.addProperty(p, value);
        return this;
    }

    /**
     * Remove all current values of the given property
     */
    public Description remove(Property p) {
        for (StmtIterator si = root.listProperties(p); si.hasNext();) {
            removals.add(si.next());
            si.remove();
        }
        return this;
    }

    /**
     * Add a new value of the given property
     */
    public Description addProperty(Property p, RDFNode value) {
        Model m = root.getModel();
        Statement s = m.createStatement(root, p, value);
        m.add(s);
        additions.add(s);
        return this;
    }

    /**
     * Add a new value of the given property
     */
    public Description addProperty(Property p, String value) {
        return addProperty(p, ResourceFactory.createPlainLiteral(value));
    }

    /**
     * Add a new value of the given property
     */
    public Description addProperty(Property p, int i) {
        return addProperty(p, ResourceFactory.createTypedLiteral(i));
    }

    /**
     * Add a new value of the given property
     */
    public Description addProperty(Property p, String lex, RDFDatatype dt) {
        return addProperty(p, ResourceFactory.createTypedLiteral(lex, dt));
    }

    /**
     * Set the given property to be a stampstamp representing the time of the call
     */
    public Description timestamp(Property p) {
        return setProperty(p, root.getModel().createTypedLiteral(Calendar.getInstance()));
    }

    /**
     * Return the set of statements that have been added to this description since it was created.
     * For use by StoreAPI implementation.
     */
    public List<Statement> getAdditions() {
        return additions;
    }

    /**
     * Return the set of statements that have been removed from this description since it was created.
     * For use by StoreAPI implementation.
     */
    public List<Statement> getRemovals() {
        return removals;
    }
}
