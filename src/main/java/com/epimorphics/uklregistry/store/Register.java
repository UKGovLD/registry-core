/******************************************************************
 * File:        RegisterImpl.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.store;

import java.util.List;

import com.epimorphics.uklregistry.vocab.Ldbp;
import com.epimorphics.uklregistry.vocab.Registry;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Abstraction for access to a Register.
 */
public class Register extends Description {

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
    // TODO version of this for paged result sets
    public void fetchMembers(StoreAPI store) {
        Model m = root.getModel();
        Resource predicateR = null;
        boolean isInverse = false;
        predicateR = root.getPropertyResourceValue(Ldbp.membershipPredicate);
        if (predicateR == null) {
            predicateR = root.getPropertyResourceValue(Registry.inverseMembershipPredicate);
            if (predicateR != null) {
                isInverse = true;
            } else {
                predicateR = RDFS.member;
            }
        }
        Property predicate = ResourceFactory.createProperty( predicateR.getURI() );
        List<Resource> entities = store.fetchDescriptionsOf(String.format("SELECT ?item WHERE { ?ri reg:register <%s>; reg:definition [reg:entity ?item] . }", root.getURI()), m);
        for (Resource entity : entities) {
            if (isInverse) {
                entity.addProperty(predicate, root);
            } else {
                root.addProperty(predicate, entity);
            }
        }
    }
}
