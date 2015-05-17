/******************************************************************
 * File:        CommandRead.java
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

package com.epimorphics.registry.commands;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.core.ValidationResponse;
import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.security.RegAction;
import com.epimorphics.registry.security.RegPermission;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;


public class CommandUpdate extends Command {

    protected boolean isPatch = false;
    protected boolean isEntityUpdate = true;
    protected RegisterItem newitem;
    protected RegisterItem currentItem;
    boolean needStatusPermission = false;
    boolean needStatusForce = false;

    public void setToPatch() {
        isPatch = true;
    }

    @Override
    public ValidationResponse validate() {
        // Payload must match target
        Resource item = payload.getResource( target );
        if ( ! item.listProperties().hasNext() ){
            // No properties, probably a URI mismatch
            return new ValidationResponse(BAD_REQUEST, "Payload URI does not match target");
        }

        // Only update one item this way, if there are multiple roots then reject
        List<Resource> roots = RDFUtil.findRoots(payload);
        if (roots.size() != 1 || !roots.get(0).getURI().equals(target)) {
            return new ValidationResponse(BAD_REQUEST, "Payload had multiple roots or root does not match target");
        }
        Resource root = roots.get(0);

        if (lastSegment.startsWith("_")) {
            newitem = RegisterItem.fromRIRequest(root, parent, false);
            isEntityUpdate = false;
        } else {
            newitem = RegisterItem.fromEntityRequest(root, parent, false);
        }

        currentItem = store.getItem(newitem.getRoot().getURI(), isEntityUpdate);
        if (currentItem == null) {
            return new ValidationResponse(Response.Status.NOT_FOUND, "Item to update does not exist");
        }

        // Validate RDF type invariants
        boolean isRegister = currentItem.isRegister();
        boolean typeOK = true;
        if (isEntityUpdate) {
            if ( isRegister ) {
                if ( !isPatch || root.hasProperty(RDF.type) ) {
                    typeOK = root.hasProperty(RDF.type, RegistryVocab.Register);
                }
            }
        } else {
            if ( !isPatch || root.hasProperty(RDF.type) ) {
                typeOK = root.hasProperty(RDF.type, RegistryVocab.RegisterItem);
            }
        }
        Resource newEntity = newitem.getEntity();
        if (!isPatch && !newEntity.hasProperty(RDF.type)) {
            typeOK = false;
        } else if (currentItem.getStatus().isAccepted() && wouldChange(currentItem.getEntity(), newEntity, RDF.type)) {
            typeOK = false;
        }
        if (!typeOK) {
            return new ValidationResponse(BAD_REQUEST, "The rdf:type of an entity cannot be changed once registered and accepted");
        }
        
        if (!isPatch && !newEntity.hasProperty(RDFS.label)) {
            return new ValidationResponse(BAD_REQUEST, "Cannot remove the rdfs:label of an entity");
        }

        if (!isEntityUpdate && currentItem.getStatus().isAccepted()) {
            boolean changesEntity = !currentItem.getEntitySpec().equals( newitem.getEntitySpec() );
            if (isPatch && newitem.getEntitySpec() == null) changesEntity = false;
            if (changesEntity) {
                return new ValidationResponse(BAD_REQUEST, "Request would change the URI of the registered entity, which is disallowed for items which have been accepted");
            }
        }

        // For registers can only update non-member properties this way
        if (isRegister && isEntityUpdate) {
            if (!parameters.containsKey(Parameters.COLLECTION_METADATA_ONLY)) {
                return new ValidationResponse(BAD_REQUEST, "Can only PUT/PATCH register metadadata, use non-member-properties to signal this");
            }
        }

        // Will this change the status?
        if (!isEntityUpdate) {
            if (newitem.getRoot().hasProperty(RegistryVocab.status)) {
                Status oldstatus = currentItem.getStatus();
                Status newstatus = newitem.getStatus();
                if (!newstatus.equals(oldstatus)) {
                    needStatusPermission = true;
                    if (!oldstatus.legalNextState(newstatus)) {
                        needStatusForce = true;
                    }
                    if (newstatus.equals(Status.Superseded)) {
                        if (! newitem.getRoot().hasProperty(RegistryVocab.successor)) {
                            return new ValidationResponse(BAD_REQUEST,  "Must specify successor when superseding an item");
                        }
                    }
                }
            }

        }

        // Santization
        for (Property p : RegisterItem.INTERNAL_PROPS) {
            root.removeAll(p);
        }

        return ValidationResponse.OK;
    }

    @Override
    public RegPermission permissionRequired() {
        RegPermission required = super.permissionRequired();
        if (needStatusPermission) {
            required.addAction(RegAction.StatusUpdate);
            if (needStatusForce) {
                required.addAction( RegAction.Force );
            }
        }
        return required;
    }

    /**
     * Test if the (resource) values of a property will be changed by the replacement or patch.
     * Relies on currentItem, newItem, isPatch being already set correctly.
     * @param r the current resource
     * @param p the property to test
     */
    private boolean wouldChange(Resource r, Resource newR, Property p) {
        if (r == null) return false;

        if (isPatch && !newR.hasProperty(p)) return false;

        List<Resource>  newValues = RDFUtil.allResourceValues(newR, p);
        List<Resource>  currentValues = RDFUtil.allResourceValues(r, p);
        if (newValues.size() != currentValues.size()) return true;
        return ! newValues.containsAll(currentValues);
    }

    @Override
    public Response doExecute() {
        // Current and newitem will have been set and checked by validation step, so just process them
        store.lock();
        try {
            String versionURI = applyUpdate(currentItem, newitem, isPatch, !isEntityUpdate);
            store.commit();
            
            notify( new Message(this, newitem) );
            return Response.noContent().location(new URI(versionURI)).build();
        } catch (URISyntaxException e) {
            throw new WebApplicationException(e);
        } finally {
            store.end();
        }
    }

}
