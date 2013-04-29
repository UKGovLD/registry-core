/******************************************************************
 * File:        CommandGraphRegister.java
 * Created by:  Dave Reynolds
 * Created on:  28 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.ValidationResponse;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Support registration of a complete graph as a managed entity.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class CommandGraphRegister extends CommandRegister {

    Resource root;

    public CommandGraphRegister(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super(operation, target, parameters, registry);
    }

    @Override
    public ValidationResponse validate() {
        root = payload.getResource(target);

        // Check the parent register exists
        Description d = store.getCurrentVersion(parent);
        if (d == null) {
            return new ValidationResponse(NOT_FOUND, "No such register");
        }
        if (!(d instanceof Register)) {
            return new ValidationResponse(BAD_REQUEST, "Can only register items in a register");
        }
        parentRegister= d.asRegister();

        // Must register via entity
        if (lastSegment.startsWith("_")) {
            return new ValidationResponse(Status.BAD_REQUEST, "Can only store graphs against a managed entity, not the item metadata");
        }

        // Verify that the graph payload does at least contain a minimum description of the target
        if (!root.hasProperty(RDFS.label)) {
            return new ValidationResponse(Status.BAD_REQUEST, "Entity description must include an rdfs:label");
        }
        if (!root.hasProperty(RDF.type)) {
            return new ValidationResponse(Status.BAD_REQUEST, "Entity description must include a type");
        }

        return ValidationResponse.OK;
    }

    @Override
    public Response doExecute() {
        store.lock(parent);
        try {
            // Check if this entity is already registered
            RegisterItem item = store.getItem(parent +"/_" + lastSegment, false);

            if (item != null) {
                // This is actually an update
                item.setEntity(root);
                item.setAsGraph(true);
                item.updateForEntity(false, Calendar.getInstance());
                String versionURI = store.update(item, true);
                return Response.noContent().location(new URI(versionURI)).build();

            } else {
                Resource location = register(parentRegister, root, false, true);
                return Response.created(new URI(location.getURI())).build();

            }
        } catch (URISyntaxException e) {
            throw new WebApplicationException(e);
        } finally {
            store.unlock(parent);
        }
    }

}
