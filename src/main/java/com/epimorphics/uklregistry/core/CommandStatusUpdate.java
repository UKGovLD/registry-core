/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.uklregistry.store.Description;
import com.epimorphics.uklregistry.store.RegisterItem;
import com.epimorphics.uklregistry.store.StoreAPI;
import com.epimorphics.uklregistry.vocab.Registry;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.sun.jersey.api.NotFoundException;


public class CommandStatusUpdate extends Command {
    public static final String STATUS_PARAM = "status";

    public CommandStatusUpdate(Operation operation, String target,
            MultivaluedMap<String, String> parameters, StoreAPI store) {
        super(operation, target, parameters, store);
    }

    @Override
    public Response execute() {
        Description description = store.getDescription(target);
        if (description instanceof RegisterItem) {
            // TODO auth
            // TODO lifecyle checks
            // TODO handle verification for accepted
            // TODO handle deletion for Invalid
            RegisterItem ri = (RegisterItem) description;
            String requestedStatus = parameters.getFirst(STATUS_PARAM);
            Resource status = ri.setStatus(requestedStatus);
            if (status == null) {
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            if (status.equals(Registry.statusExperimental) || status.equals(Registry.statusStable)) {
                RDFUtil.timestamp(ri.getRoot(), DCTerms.dateAccepted);
            }
            store.storeDescription(ri);
            return Response.noContent().build();
        } else if (description == null) {
            throw new NotFoundException();
        } else {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

}
