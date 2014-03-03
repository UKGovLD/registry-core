/******************************************************************
 * File:        RegisterImpl.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
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

import java.util.ArrayList;
import java.util.List;

import com.epimorphics.registry.store.RegisterEntryInfo;
import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.util.Util;
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
    List<Resource> delegatedMembers;

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
     * @param model model in which to store the results
     * @param withVersion  if true then versioning information is included, if false the Version/VersionedThing pairs are merged
     * @param withMetadata if true then both RegisterItems and the entities are included, if false then just entities are shown
     * @param status only return members which are specializations of this status, use null as a wildcard
     * @param offset offset in the list to start the return window
     * @param length then maximum number of members to return, -1 for no limit
     * @param timestamp the time at which the values should be valid, -1 for current value
     * @param items an array in which to return an ordered list of the items, if null if not required
     * @return whether the view is complete
     */
    public boolean constructView(Model model, boolean withMetadata, Status status, int offset, int length, long timestamp, List<Resource> results) {
        getMembers();

        List<String> itemURIs = new ArrayList<String>( length == -1 ? 50 : length );
        List<String> entityURIs = new ArrayList<String>( length == -1 ? 50 : length );

        int count = 0;
        boolean incomplete = false;
        int limit = length == -1 ? Integer.MAX_VALUE : offset + length;
        for (RegisterEntryInfo info : members) {
            boolean valid = (timestamp == -1) ? info.getStatus().isA(status) : true;
            if (valid) {
                if (timestamp != -1) {
                    Description d = store.getVersionAt(info.getItemURI(), timestamp);
                    if (d != null) {
                        RegisterItem ri = d.asRegisterItem();
                        if (ri.getStatus().isA(status)) {
                            model.add( store.getEntity(ri).getModel() );
                        } else {
                            valid = false;
                        }
                    } else {
                        valid = false;
                    }
                }
            }
            if (valid) {
                if (count >= offset && count < limit) {
                    itemURIs.add( info.getItemURI() );
                    entityURIs.add( info.getEntityURI() );
                }
                count++;
                if (count == limit) {
                    incomplete = true;
                }
                if (count > limit) break;
            }
        }
        
        if (timestamp != -1) {
            // already fetched while checking for valid entries
        } else if (withMetadata && !itemURIs.isEmpty()) {
            List<RegisterItem> items = store.fetchAll(itemURIs, true);
            model.add( items.get(0).getRoot().getModel() );
        } else {
            for (String uri : entityURIs) {
                Model d =  store.getCurrentVersion(uri).getRoot().getModel();
                model.add( d );
            }
        }

        model.add( root.getModel() ); 
        
        List<Resource> entities = results;
        if (entities == null) {
            entities = new ArrayList<Resource>();
        }

        for (String uri : entityURIs) {
            entities.add( model.getResource(uri) );
        }

        addMembership(model, entities);

        return !incomplete;
    }
    
    public List<Resource> getAllEntities() {
        Model model = ModelFactory.createDefaultModel();
        List<Resource> entities = new ArrayList<>();
        constructView(model, false, Status.Accepted, 0, -1, -1, entities);
        return entities;
    }

    protected void addMembership(Model model, List<Resource> entities) {
        // Membership relations
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

        Resource reg = model.getResource(root.getURI());
        for (Resource entity : entities) {
            if (isInverse) {
                entity.inModel(model).addProperty(predicate, reg);
            } else {
                reg.addProperty(predicate, entity);
            }
        }
    }


    /**
     * Fetch all the members of a delegated register and construct an RDF view
     * according the given flags.
     *
     * @param model model in which to store the results
     * @param delegation description of the source delegated to
     * @param offset offset in the list to start the return window
     * @param length then maximum number of members to return, -1 for no limit
     * @param items an array in which to return an ordered list of the items, if null if not required
     */

    public void constructDelegatedView(Model view, DelegationRecord delegation,
            int offset, int length, List<Resource> items) {

        if (delegatedMembers == null) {
            delegatedMembers = delegation.listMembers();
        }

        List<Resource> windowedMembers = Util.listWindow(delegatedMembers, offset, length);
        delegation.fetchMembers(view,  windowedMembers);

       view.add( root.getModel() );

       addMembership(view, windowedMembers);

       if (items != null) {
           for (Resource m : windowedMembers) {
               items.add( m.inModel(view) );
           }
       }
    }


}
