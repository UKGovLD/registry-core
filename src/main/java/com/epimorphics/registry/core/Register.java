/******************************************************************
 * File:        RegisterImpl.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.util.List;

import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.vocab.Ldbp;
import com.epimorphics.registry.vocab.RegistryVocab;
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
     * Fetch all the members of the register and construct an RDF view
     * according the given flags. The constructed view side is stored
     * in the description model and thus side-effects future accesses.
     * 
     * @param withVersion  if true then versioning information is included, if false the Version/VersionedThing pairs are merged
     * @param withMetadata if true then both RegisterItems and the entities are included, if false then just entities are shown
     * @return
     */
    public Model constructView(StoreAPI store, boolean withVersion, boolean withMetadata) {
        
        // TODO optimize the common case of false/false with a StoreAPI extension that puts everying in one model
        
        List<RegisterItem> members = withVersion ? store.fetchMembersWithVersion(this, true) : store.fetchMembers(this, true);
        
        Resource predicateR = null;
        boolean isInverse = false;
        predicateR = root.getPropertyResourceValue(Ldbp.membershipPredicate);
        if (predicateR == null) {
            predicateR = root.getPropertyResourceValue(RegistryVocab.inverseMembershipPredicate);
            if (predicateR != null) {
                isInverse = true;
            } else {
                predicateR = RDFS.member;
            }
        }
        Property predicate = ResourceFactory.createProperty( predicateR.getURI() );

        Model m = root.getModel();
        for (RegisterItem item : members) {
            m.add( item.getEntity().getModel() );
            if (withMetadata) {
                m.add( item.getRoot().getModel() );
            }
            if (isInverse) {
                item.getEntity().addProperty(predicate, root);
            } else {
                root.addProperty(predicate, item.getEntity());
            }
        }
        
        return m;
    }

}
