/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.server.webapi.BaseEndpoint;
import com.epimorphics.uklregistry.store.Description;
import com.epimorphics.uklregistry.store.Register;
import com.epimorphics.uklregistry.store.StoreAPI;
import com.epimorphics.uklregistry.vocab.Registry;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.DCTerms;
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

        if (payload.contains(null, RDF.type, Registry.Register)) {
            Register parent = store.getRegister(target);
            if (parent == null) {
                throw new NotFoundException();
            }

            for (ResIterator ri = payload.listSubjectsWithProperty(RDF.type, Registry.Register); ri.hasNext();) {
                Resource subreg = ri.next();

                // TODO validate completeness of description
                // TODO fill in void description
                // TODO timestamp and version the parent register

                String location = determineRegistrationLocation(subreg);
                String subregURI = parent.getRoot().getURI() + "/" + location;
                ResourceUtils.renameResource(subreg, subregURI);

                Description subregDescription = new Description();
                subregDescription.setRoot( payload.getResource(subregURI) );
                RDFUtil.timestamp(subreg, DCTerms.modified);
                payload.add(parent.getRoot(), Registry.subregister, subreg);
                store.storeDescription( subregDescription );
                log.info("Created new sub-register: " + subregURI);
            }

        } else {
//            String registerURI = Configuration.getBaseURI() + "/" + target;
            // TODO check its a register
            // TODO make sure there's a root register
//            String notation = determineRegistrationLocation();
        }
        // TODO implement
        return Response.noContent().build();
    }

    private String determineRegistrationLocation(Resource root) {
        String location = null;
        if (root.isURIResource()) {
            String uri = root.getURI();
            if (uri.equals(BaseEndpoint.DUMMY_BASE_URI)) {
                // Empty relative URI specified
                if (root.hasProperty(SKOS.notation)) {
                    location = RDFUtil.getStringValue(root, SKOS.notation);
                }
            } else if (uri.startsWith(BaseEndpoint.DUMMY_BASE_URI)) {
                // relative URI
                location = uri.substring(BaseEndpoint.DUMMY_BASE_URI.length() + 1);
            } else if (uri.startsWith(target)) {
                location = uri.substring(target.length() + 1);
            }
        } else {
            if (root.hasProperty(SKOS.notation)) {
                location = RDFUtil.getStringValue(root, SKOS.notation);
            }
        }
        if (location == null) {
            location = UUID.randomUUID().toString();
        } else if (location.contains("/")) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        return location;
    }

}
