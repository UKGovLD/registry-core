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

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.util.PatchUtil;
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
    public Response execute() {
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

            if (withEntity) {
                if (isPatch){
                    PatchUtil.patch(entity, item.getEntity());
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
