/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.ValidationResponse;
import com.epimorphics.registry.util.PatchUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;


public class CommandUpdate extends Command {

    protected boolean isPatch = false;
    protected boolean isEntityUpdate = true;
    protected RegisterItem newitem;
    protected RegisterItem currentItem;

    public CommandUpdate(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super(operation, target, parameters, registry);
    }

    public void setToPatch() {
        isPatch = true;
    }

    @Override
    public ValidationResponse validate() {
        // Payload must match target
        Resource item = payload.getResource( target );
        if ( ! item.listProperties().hasNext() ){
            // No properties, probably a URI mismatch
            return new ValidationResponse(Response.Status.BAD_REQUEST, "Payload URI does not match target");
        }

        // Only update one item this way, if there are multiple roots then reject
        List<Resource> roots = RDFUtil.findRoots(payload);
        if (roots.size() != 1 || !roots.get(0).getURI().equals(target)) {
            return new ValidationResponse(Response.Status.BAD_REQUEST, "Payload had multiple roots or root does not match target");
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
        if (currentItem.getStatus().isAccepted() && wouldChange(currentItem.getEntity(), newitem.getEntity(), RDF.type)) {
            typeOK = false;
        }
        if (!typeOK) {
            return new ValidationResponse(Response.Status.BAD_REQUEST, "The rdf:type of an entity cannot be changed once registered and accepted");
        }

        if (!isEntityUpdate && currentItem.getStatus().isAccepted()) {
            if (!currentItem.getEntitySpec().equals( newitem.getEntitySpec() )) {
                return new ValidationResponse(Status.BAD_REQUEST, "Request would change the URI of the registered entity, which is disallowed for items which have been accepted");
            }
        }

        // For registers can only update non-member properties this way
        if (isRegister && isEntityUpdate) {
            if (!parameters.containsKey(Parameters.COLLECTION_METADATA_ONLY)) {
                return new ValidationResponse(Status.BAD_REQUEST, "Can only PUT/PATCH register metadadata, use non-member-properties to signal this");
            }
        }

        // Santization
        for (Property p : RegisterItem.INTERNAL_PROPS) {
            root.removeAll(p);
        }

        return ValidationResponse.OK;
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
        String itemURI = newitem.getRoot().getURI();
        store.lock(itemURI);
        try {
            boolean isRegister = currentItem.isRegister();
            Resource entity = newitem.getEntity();
            boolean withEntity = entity != null;
            if (withEntity) {
                if (isPatch){
                    if (isRegister) {
                        PatchUtil.patch(entity, currentItem.getEntity(), RegistryVocab.subregister);
                    } else {
                        PatchUtil.patch(entity, currentItem.getEntity());
                    }
                } else {
                    currentItem.setEntity(entity);
                }
                currentItem.updateForEntity(false, Calendar.getInstance());
            }

            if (!isEntityUpdate) {
                if (isPatch) {
                    PatchUtil.patch(newitem.getRoot(), currentItem.getRoot(), RegisterItem.RIGID_PROPS);
                } else {
                    PatchUtil.update(newitem.getRoot(), currentItem.getRoot(), RegisterItem.RIGID_PROPS, RegisterItem.REQUIRED_PROPS);
                }
            }
            String versionURI = store.update(currentItem, withEntity);
            return Response.noContent().location(new URI(versionURI)).build();
        } catch (URISyntaxException e) {
            throw new WebApplicationException(e);
        } finally {
            store.unlock(itemURI);
        }
    }

}
