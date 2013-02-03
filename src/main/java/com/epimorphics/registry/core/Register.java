/******************************************************************
 * File:        RegisterImpl.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.util.ArrayList;
import java.util.List;

import com.epimorphics.registry.store.RegisterEntryInfo;
import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.vocab.Ldbp;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Abstraction for access to a Register.
 */
public class Register extends Description {
    List<RegisterEntryInfo> members;
    StoreAPI store;

    public Register(Resource root) {
        super( root );
    }

    public Register(Description d) {
        super( d.root );
    }

    public Resource getRegister() {
        return getRoot();
    }
    
    public void setStore(StoreAPI store) {
        this.store = store;
    }
    
    public List<RegisterEntryInfo> getMembers() {
        if (members == null) {
            members = store.listMembers(this);
        }
        return members;
    }

    /**
     * Fetch all the members of the register and construct an RDF view
     * according the given flags. 
     *
     * @param withVersion  if true then versioning information is included, if false the Version/VersionedThing pairs are merged
     * @param withMetadata if true then both RegisterItems and the entities are included, if false then just entities are shown
     * @param status only return members which are specializations of this status, use null as a wildcard
     * @param offset offset in the list to start the return window
     * @param length then maximum number of members to return, -1 for no limit
     * @return
     */
    public Model constructView(StoreAPI store, boolean withVersion, boolean withMetadata, Status status, int offset, int length) {
        List<String> itemURIs = new ArrayList<String>( length == -1 ? 50 : length );
        List<String> entityURIs = new ArrayList<String>( length == -1 ? 50 : length );
        
        int count = 0;
        int limit = length == -1 ? Integer.MAX_VALUE : offset + length;
        for (RegisterEntryInfo info : getMembers()) {
            if (info.getStatus().isA(status)) {
                if (count >= offset) {
                    itemURIs.add( info.getItemURI() );
                    entityURIs.add( info.getEntityURI() );
                }
                count++;
                if (count >= limit) break;
            }
        }
        
        Model result = null;
        if (withMetadata && !itemURIs.isEmpty()) {
            List<RegisterItem> items = store.fetchAll(itemURIs, true, withVersion);
            result = items.get(0).getRoot().getModel();
        } else {
            result = ModelFactory.createDefaultModel();
            for (String uri : entityURIs) {
                result.add( store.getDescription(uri).getRoot().getModel() );
            }
        }
        
        result.add( root.getModel() );

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

        Resource reg = result.getResource(root.getURI());
        for (String uri : entityURIs) {
            Resource entity = result.getResource(uri);
            if (isInverse) {
                entity.addProperty(predicate, reg);
            } else {
                reg.addProperty(predicate, entity);
            }
        }

        return result;
    }

}
