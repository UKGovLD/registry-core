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
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.server.webapi.WebApiException;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.api.NotFoundException;

/**
 * Command processor to handle registering a new entry.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class CommandRegister extends Command {
    static final Logger log = LoggerFactory.getLogger( CommandRegister.class );

    public CommandRegister(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super(operation, target, parameters, registry);
    }

    @Override
    public Response doExecute() {

        store.lock(target);
        try {
            Description d = store.getCurrentVersion(target);
            if (d == null) {
                throw new NotFoundException();
            }
            Register parent = d.asRegister();

            Resource location = null;
            if (payload.contains(null, RDF.type, RegistryVocab.RegisterItem)) {
                for (ResIterator ri = payload.listSubjectsWithProperty(RDF.type, RegistryVocab.RegisterItem); ri.hasNext();) {
                    Resource itemSpec = ri.next();
                    location = register(parent, itemSpec, true);
                }
            } else {
                List<Resource> roots = payload.listSubjectsWithProperty(RDF.type).toList();
                if (roots.size() != 1) {
                    throw new WebApiException(Response.Status.BAD_REQUEST, "Could not find unique entity root to register");
                }
                location = register(parent, findSingletonRoot(), false);
            }
            try {
                return Response.noContent().location(new URI(location.getURI())).build();
            } catch (URISyntaxException e) {
                throw new EpiException(e);
            }
        } finally {
            store.unlock(target);
        }
    }

    private Resource register(Register parent, Resource itemSpec, boolean withItemSpec) {
        String parentURI = parent.getRoot().getURI();
        RegisterItem ri = null;
        if ( withItemSpec ) {
            ri = RegisterItem.fromRIRequest(itemSpec, parentURI, true);
        } else {
            ri = RegisterItem.fromEntityRequest(itemSpec, parentURI, true);
        }

        if (store.getDescription(ri.getRoot().getURI()) != null) {
            // Item already exists
            throw new WebApiException(Response.Status.FORBIDDEN, "Item already registered at request location: " + ri.getRoot());
        }

        // TODO validate completeness of description

        Resource entity = ri.getEntity();
        if( entity.hasProperty(RDF.type, RegistryVocab.Register) ) {
            // TODO fill in void description
            // TODO fill in auto properties from parent register
            log.info("Created new sub-register: " + ri.getNotation());
        }
        store.addToRegister(parent, ri);
        return ri.getRoot();
    }

}
