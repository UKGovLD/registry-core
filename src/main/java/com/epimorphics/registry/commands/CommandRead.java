/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import static com.epimorphics.registry.webapi.Parameters.*;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.server.webapi.WebApiException;
import com.hp.hpl.jena.rdf.model.Model;
import com.sun.jersey.api.NotFoundException;


public class CommandRead extends Command {

    boolean withVersion;
    boolean withMetadata;

    public CommandRead(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super(operation, target, parameters, registry);
        withVersion = hasParamValue(VIEW, WITH_VERSION);
        withMetadata = hasParamValue(VIEW, WITH_METADATA);
    }

    @Override
    public Response doExecute() {
        Description d = null;

        if (lastSegment.startsWith("_")) {
            // An item
            if (lastSegment.contains(":")) {
                // An explicit item version
                d = store.getVersion(target);
                if (d != null) {
                    store.getEntity(d.asRegisterItem());
                }
            } else {
                //  plain item
                if (withVersion) {
                    d = store.getItemWithVersion(target, true);
                } else {
                    d = store.getItem(target, true) ;
                }
            }
        } else {
            // An entity
            if ( withMetadata ) {
                // Entity with metadata
                d = store.getItem(parent +"/_" + lastSegment, true);
            } else {
                // plain entity
                d = store.getCurrentVersion(target);
            }
        }

        if (d == null) {
            throw new NotFoundException();
        }

        Model m = d.getRoot().getModel();
        // Include any entity in the response
        if (d instanceof RegisterItem) {
            RegisterItem ri = d.asRegisterItem();
            if (ri.getEntity() != null) {
                m.add( ri.getEntity().getModel() );
            }
        } else if (d instanceof Register) {
            m = registerRead(d.asRegister());
        }

        URI uri;
        try {
            uri = new URI( d.getRoot().getURI() );
        } catch (URISyntaxException e) {
            throw new WebApplicationException(e);
        }
        return Response.ok().location(uri).entity( m ).build();
    }
    
    Model registerRead(Register register) {
        if (parameters.containsKey(COLLECTION_METADATA_ONLY)) {
            return register.getRoot().getModel();
        } else {
            Status status = Status.forString( parameters.getFirst(STATUS), Status.Accepted );
            int offset = 0;
            int length = -1;
            if (parameters.containsKey(FIRST_PAGE)) {
                length = registry.getPageSize();
            } else if (parameters.containsKey(PAGE_NUMBER)) {
                length = registry.getPageSize();
                try {
                    offset = length * Integer.parseInt( parameters.getFirst(PAGE_NUMBER) );
                } catch (NumberFormatException e) {
                    throw new WebApiException(javax.ws.rs.core.Response.Status.BAD_REQUEST, "Illegal page number");
                }
            }
            Model view = register.constructView(store, withVersion, withMetadata, status, offset, length);
            // TODO << paging information
            return view;
        }
    }

}
