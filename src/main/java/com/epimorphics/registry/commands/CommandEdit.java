/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epimorphics.registry.commands;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.ValidationResponse;
import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.security.RegAction;
import com.epimorphics.registry.security.RegPermission;
import com.epimorphics.registry.util.PatchUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.util.NameUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Import provides a bulk update/patch capability.
 * Payload expected to contain a set of register items for a register.
 * For each item if it
 * already exists in the register then it is patched to match the request,
 * otherwise it is added to the register.
 */
public class CommandEdit extends Command {
    
    Register parentRegister;
    List<RegisterItem> requestItems = new ArrayList<>();

    @Override
    public ValidationResponse validate() {
        Description d = store.getCurrentVersion(target);
        if (d == null) {
            return new ValidationResponse(NOT_FOUND, "No such register");
        }
        if (!(d instanceof Register)) {
            return new ValidationResponse(BAD_REQUEST, "Can only bulk edit a register");
        }
        parentRegister = d.asRegister();
        String parentURI = NameUtils.stripLastSlash( parentRegister.getRoot().getURI() );
        // String stripLastSlash needed to cope with the out-of-pattern URI for the root register
        
        for (ResIterator ri = payload.listSubjectsWithProperty(RDF.type, RegistryVocab.RegisterItem); ri.hasNext();) {
            Resource itemSpec = ri.next();
            StmtIterator i = itemSpec.listProperties(RegistryVocab.status);
            while (i.hasNext()) {
                RDFNode status = i.next().getObject();
                if (!status.isResource()) {
                    return new ValidationResponse(BAD_REQUEST, "reg:status value which is not a resource " + status);
                }
            }

            RegisterItem item = RegisterItem.fromRIRequest(itemSpec, parentURI, true);
            ValidationResponse entityValid = validateEntity(parentRegister, item.getEntity() );
            if (!entityValid.isOk()) {
                return entityValid;
            }
            requestItems.add( item );
        }
        if (requestItems.isEmpty()) {
            // Check for direct registration of entities
            for (Resource entity : findEntities()) {
                RegisterItem item = RegisterItem.fromEntityRequest(entity, parentURI, true); 
                ValidationResponse entityValid = validateEntity(parentRegister, entity );
                if (!entityValid.isOk()) {
                    return entityValid;
                }
                requestItems.add( item );
            }
        }
        if (requestItems.isEmpty()) {
            return new ValidationResponse(BAD_REQUEST, "No items found in request");
        }
        
        return ValidationResponse.OK;
    }
    
    /**
     * Returns the permissions that will be required to authorize this
     * operation or null if no permissions are needed.
     */
    public RegPermission permissionRequired() {
        RegPermission permission = new RegPermission(RegAction.Update, "/" + path);
        permission.addAction( RegAction.Register );
        // TODO this isn't always required could be less restrictive depending on request specifics
        permission.addAction( RegAction.StatusUpdate );
        return permission;
    }


    
    @Override
    public Response doExecute() {
        // Extract the current members as a batch (performance/scaling tradeoff)
        Model view = ModelFactory.createDefaultModel();
        List<Resource> members = new ArrayList<>();
        parentRegister.constructView(view, true, null, 0, -1, -1, members);
        
        // Remove any materialized membership links
        Resource reg = parentRegister.getRoot();
        Resource im = RDFUtil.getResourceValue(reg, RegistryVocab.inverseMembershipPredicate);
        if (im != null) {
            Property imp = view.createProperty(im.getURI());
            view.remove( view.listStatements(null, imp, reg).toList() );
        }
        
        store.lock();
        try {
            for (RegisterItem importItem : requestItems) {
                Resource itemR = importItem.getRoot();
                if (view.contains(importItem.getRoot(), RDF.type)) {
                    // An existing item
                    RegisterItem currentItem = new RegisterItem( itemR.inModel(view) );
                    currentItem.setEntity( importItem.getEntity().inModel(view) );
                    
                    if ( PatchUtil.willChange(itemR, currentItem.getRoot(), RIGID_ITEM_PROPS)
                      || PatchUtil.willChange(importItem.getEntity(), currentItem.getEntity(), RegistryVocab.subregister) ) {
                        applyUpdate(currentItem, importItem, true, true);
                    }
                } else {
                    addToRegister(parentRegister, importItem);
                }
            }
            store.commit();
            
            for (RegisterItem item : requestItems) {
                notify( new Message(this, item) );
            }
            
            return Response.noContent().location(new URI(path)).build();
        } catch (URISyntaxException e) {
            return Response.noContent().build();
        } finally {
            store.end();
        }
    }
    
    private static final Property[] RIGID_ITEM_PROPS = new Property[] {
        RegistryVocab.register, RegistryVocab.notation,
        RegistryVocab.itemClass, RegistryVocab.predecessor,
        RegistryVocab.submitter, RDF.type,
        DCTerms.dateSubmitted, OWL.versionInfo,
        RegistryVocab.definition};
}
