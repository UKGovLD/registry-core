/******************************************************************
 * File:        Entity.java
 * Created by:  Dave Reynolds
 * Created on:  23 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.util.Calendar;

import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.util.VersionUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.util.Closure;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;

/**
 * Provides core machinery for accessing descriptions of an RDF resource.
 * Allows the original version of a description to be kept in parallel
 * so that diffs can be calculated for some types of store update.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Description {

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
                Register reg = RegisterCache.getRegister(root);
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

    public Boolean isRegisterItem() {
        return false;
    }

    public Boolean isEntity() {
        return true;
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
            si.next();
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

}
