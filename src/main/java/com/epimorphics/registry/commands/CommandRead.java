/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import static com.epimorphics.registry.webapi.Parameters.COLLECTION_METADATA_ONLY;
import static com.epimorphics.registry.webapi.Parameters.FIRST_PAGE;
import static com.epimorphics.registry.webapi.Parameters.PAGE_NUMBER;
import static com.epimorphics.registry.webapi.Parameters.STATUS;
import static com.epimorphics.registry.webapi.Parameters.VERSION_AT;
import static com.epimorphics.registry.webapi.Parameters.VIEW;
import static com.epimorphics.registry.webapi.Parameters.WITH_METADATA;
import static com.epimorphics.registry.webapi.Parameters.WITH_VERSION;

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
import com.epimorphics.registry.vocab.Ldbp;
import com.epimorphics.server.webapi.WebApiException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.api.NotFoundException;


public class CommandRead extends Command {

    boolean withVersion;
    boolean withMetadata;
    boolean paged;
    boolean versioned;
    boolean timestamped;
    int length = -1;
    int pagenum = 0;

    public CommandRead(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super(operation, target, parameters, registry);
        withVersion = hasParamValue(VIEW, WITH_VERSION);
        withMetadata = hasParamValue(VIEW, WITH_METADATA);
        if (parameters.containsKey(FIRST_PAGE)) {
            paged = true;
            length = registry.getPageSize();
        } else if (parameters.containsKey(PAGE_NUMBER)) {
            paged = true;
            length = registry.getPageSize();
            try {
                pagenum = Integer.parseInt( parameters.getFirst(PAGE_NUMBER) );
            } catch (NumberFormatException e) {
                throw new WebApiException(javax.ws.rs.core.Response.Status.BAD_REQUEST, "Illegal page number");
            }
        }
        versioned = lastSegment.contains(":");
        timestamped = parameters.containsKey(VERSION_AT);
    }

    @Override
    public Response doExecute() {
        Description d = null;
        boolean entityWithMetadata = false;
        if (lastSegment.startsWith("_")) {
            // An item
            if (versioned) {
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
            if (versioned) {
                // This case only arises for registers
                d = store.getVersion(target);
            } else  if ( withMetadata ) {
                // Entity with metadata
                entityWithMetadata = true;
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
        }
        if (entityWithMetadata) {
            d = Description.descriptionFrom(d.asRegisterItem().getEntity(), store);
        }
        if (d instanceof Register) {
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
            // Status filter option
            Status status = Status.forString( parameters.getFirst(STATUS), Status.Accepted );
            
            // Select verison of view
            long timestamp = -1;
            if (versioned) {
                timestamp = store.versionStartedAt(target);
            } else {
//                String ts = parameters.getFirst(VERSION_AT);
                // TODO parse timestamp argument
            }
            Model view = ModelFactory.createDefaultModel();
            boolean complete = register.constructView(view, withVersion, withMetadata, status, pagenum * length, length, timestamp);
            
            // Paging parameters
            if (paged) {
                injectPagingInformation(view, register.getRoot(), !complete);
            }
            
            return view;
        }
    }
    
    private void injectPagingInformation(Model m, Resource regroot, boolean more) {
        String url = target + "?" + makeParamString(parameters);
        Resource page = m.createResource(url)
            .addProperty(RDF.type, Ldbp.Page)
            .addProperty(Ldbp.pageOf, regroot);
        if (more) {
            String pageParams = "?" + PAGE_NUMBER + "=" + (pagenum+1);
            String otherParams = makeParamString(parameters, FIRST_PAGE, PAGE_NUMBER);
            if (!otherParams.isEmpty()) {
                pageParams += "&" + otherParams;
            }
            page.addProperty(Ldbp.nextPage, m.createResource( target + pageParams ));
        }
    }

}
