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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
    public Response register() {
        MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
        Command command = null;
        if ( parameters.get(Parameters.VALIDATE) != null ) {
            command = makeCommand(Operation.Validate);
        } else if ( parameters.get(Parameters.STATUS_UPDATE) != null ) {
            command = makeCommand(Operation.StatusUpdate);
        } else {
            command = makeCommand(Operation.Register);
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

/*
    public static Command determineCommand(HttpServletRequest request) {
        String target = request.getRequestURI();
        String method = request.getMethod();
        Map<String, String[]> parameters = request.getParameterMap();
        CommandFactoryI cf = CommandFactory.get();

        if (method.equalsIgnoreCase("GET")) {
            return cf.make(Operation.Read, target, parameters);
        } else if (method.equalsIgnoreCase("PUT") || (method.equalsIgnoreCase("PATCH"))) {
            return cf.make(Operation.Update, target, parameters);
        } else if (method.equalsIgnoreCase("DELETE")) {
            return cf.make(Operation.Delete, target, parameters);
        } else if (method.equalsIgnoreCase("POST")) {
            if (request.getParameter(Parameters.VALIDATE) != null) {
                return cf.make(Operation.Validate, target, parameters);
            } else if (request.getParameter(Parameters.STATUS_UPDATE) != null) {
                return cf.make(Operation.StatusUpdate, target, parameters);
            } else {
                return cf.make(Operation.Register, target, parameters);
            }
        }
        throw new NotFoundException();      // Leave body blank so Jersey filter can forward on down chain
    }
*/

}
