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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
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

        currentItem = store.getItem(newitem.getRoot().getURI(), isPatch && isEntityUpdate);
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
        if (!typeOK) {
            return new ValidationResponse(Response.Status.BAD_REQUEST, "The rdf:type of an entity cannot be changed once registered");
        }

        // For registers can only update non-member properties this way
        if (isRegister && isEntityUpdate) {
            if (!parameters.containsKey(Parameters.COLLECTION_METADATA_ONLY)) {
                return new ValidationResponse(Status.BAD_REQUEST, "Can only PUT/PATCH register metadadata, use non-member-properties to signal this");
            }
        }

        // Update will handle regid properties but protect against version info issues
        root.removeAll(OWL.versionInfo);

        return ValidationResponse.OK;
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
