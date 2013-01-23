/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.uklregistry.store.Register;
import com.epimorphics.uklregistry.store.RegisterItem;
import com.epimorphics.uklregistry.store.StoreAPI;
import com.epimorphics.uklregistry.vocab.Registry;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.api.NotFoundException;


public class CommandRegister extends Command {
    static final Logger log = LoggerFactory.getLogger( CommandRegister.class );

    public CommandRegister(Operation operation, String target,
            MultivaluedMap<String, String> parameters, StoreAPI store) {
        super(operation, target, parameters, store);
    }

    @Override
    public Response execute() {

        Register parent = store.getRegister(target);
        if (parent == null) {
            throw new NotFoundException();
        }

        if (payload.contains(null, RDF.type, Registry.RegisterItem)) {
            for (ResIterator ri = payload.listSubjectsWithProperty(RDF.type, Registry.RegisterItem); ri.hasNext();) {
                Resource itemSpec = ri.next();
                register(parent, itemSpec);
            }
        } else {
            List<Resource> roots = payload.listSubjectsWithProperty(RDF.type).toList();
            if (roots.size() != 1) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
            register(parent, roots.get(0));
        }
        return Response.noContent().build();
    }

    private void register(Register parent, Resource itemSpec) {
        String parentURI = parent.getRoot().getURI();
        RegisterItem ri = null;
        if ( itemSpec.hasProperty(RDF.type, Registry.RegisterItem) ) {
            ri = RegisterItem.fromRIRequest(itemSpec, parentURI);
        } else {
            ri = RegisterItem.fromEntityRequest(itemSpec, parentURI);
        }

        // TODO check if item already exists,
        // TODO validate completeness of description
        // TODO timestamp and version the parent register

        Resource entity = ri.getEntity();
        if( entity.hasProperty(RDF.type, Registry.Register) ) {
            // TODO fill in void description
            // TODO fill in auto properties from parent register
            entity.getModel().add(parent.getRoot(), Registry.subregister, entity);
            log.info("Created new sub-register: " + ri.getNotation());
        }
        ri.getRoot().addProperty(Registry.register, parent.getRoot());
        store.storeDescription(ri);
    }

}
