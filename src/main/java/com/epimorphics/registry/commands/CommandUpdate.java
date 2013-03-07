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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.ValidationResponse;
import com.epimorphics.registry.util.PatchUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.server.webapi.WebApiException;
import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.NotFoundException;


public class CommandUpdate extends Command {

    protected boolean isPatch = false;

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
        return ValidationResponse.OK;
    }

    @Override
    public Response doExecute() {
        // TODO validation

        if (lastSegment.startsWith("_")) {
            Resource item = payload.getResource( target );
            return update(RegisterItem.fromRIRequest(item, parent, false), false);
        } else {
            return update(RegisterItem.fromEntityRequest(findSingletonRoot(), parent, false), true);
        }
    }

    protected Response update(RegisterItem newitem, boolean entityOnly) {
        String itemURI = newitem.getRoot().getURI();
        store.lock(itemURI);
        try {
            Resource entity = newitem.getEntity();
            boolean withEntity = entity != null;
            RegisterItem item = store.getItem(itemURI, isPatch && withEntity);
            if (item == null) {
                throw new NotFoundException();
            }

            boolean isRegister = item.isRegister();
            if (isRegister && entityOnly) {
                if (!parameters.containsKey(Parameters.COLLECTION_METADATA_ONLY)) {
                    throw new WebApiException(Status.BAD_REQUEST, "Can only PUT/PATCH register metadadata, use non-member-properties to signal this");
                }
            }
            if (withEntity) {
                if (isPatch){
                    if (isRegister) {
                        PatchUtil.patch(entity, item.getEntity(), RegistryVocab.subregister);
                    } else {
                        PatchUtil.patch(entity, item.getEntity());
                    }
                } else {
                    item.setEntity(entity);
                }
                item.updateForEntity(false, Calendar.getInstance());
            }

            if (!entityOnly) {
                if (isPatch) {
                    PatchUtil.patch(newitem.getRoot(), item.getRoot(), RegisterItem.RIGID_PROPS);
                } else {
                    PatchUtil.update(newitem.getRoot(), item.getRoot(), RegisterItem.RIGID_PROPS);
                }
            }
            String versionURI = store.update(item, withEntity);
            return Response.noContent().location(new URI(versionURI)).build();
        } catch (URISyntaxException e) {
            throw new WebApplicationException(e);
        } finally {
            store.unlock(itemURI);
        }
    }

}
