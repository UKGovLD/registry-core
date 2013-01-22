/******************************************************************
 * File:        RequestProcessor.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.webapi;

import static com.epimorphics.webapi.marshalling.RDFXMLMarshaller.MIME_RDFXML;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.epimorphics.server.webapi.BaseEndpoint;
import com.epimorphics.uklregistry.core.Command;
import com.epimorphics.uklregistry.core.Command.Operation;
import com.epimorphics.uklregistry.core.CommandFactory;

/**
 * Filter all requests as possible register API requests.
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
@Path("{path: .*}")
public class RequestProcessor extends BaseEndpoint {

    @GET
    @Produces({MIME_TURTLE, MIME_RDFXML})
    public Response read() {
        Command command = makeCommand( Operation.Read );
        // TODO authorize
        return command.execute();
    }

    private Command makeCommand(Operation op) {
        return CommandFactory.get().make(op, uriInfo.getPath(), uriInfo.getQueryParameters());
    }

    @POST
    @Consumes({MIME_TURTLE, MIME_RDFXML})
    public Response register(@Context HttpHeaders hh, InputStream body) {
        MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
        Command command = null;
        if ( parameters.get(Parameters.VALIDATE) != null ) {
            command = makeCommand(Operation.Validate);
            command.setPayload( getBodyModel(hh, body) );
        } else if ( parameters.get(Parameters.STATUS_UPDATE) != null ) {
            command = makeCommand(Operation.StatusUpdate);
        } else {
            command = makeCommand(Operation.Register);
            command.setPayload( getBodyModel(hh, body) );
        }
        // TODO authorize
        return command.execute();
    }

    @PUT
    @Consumes({MIME_TURTLE, MIME_RDFXML})
    public Response update() {
        Command command = makeCommand( Operation.Update );
        // TODO authorize
        return command.execute();
    }

    @DELETE
    public Object delete() {
        Command command = makeCommand( Operation.Delete );
        // TODO authorize
        return command.execute();
    }

    @PATCH
    @Consumes({MIME_TURTLE, MIME_RDFXML})
    public Response updatePatch() {
        Command command = makeCommand( Operation.Update );  // Different op for patching?
        // TODO authorize
        return command.execute();
    }

}
