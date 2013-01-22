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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MultivaluedMap;

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
    public Object read() {
        String target = uriInfo.getPath();
        MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
        Command command = CommandFactory.get().make(Operation.Read, target, parameters);
        // TODO authorize
        return command.execute();
    }

    @POST
    @Consumes({MIME_TURTLE, MIME_RDFXML})
    public Object register() {
        String target = uriInfo.getPath();
        MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
        Command command = null;
        if ( parameters.get(Parameters.VALIDATE) != null ) {
            command = CommandFactory.get().make(Operation.Validate, target, parameters);
        } else if ( parameters.get(Parameters.STATUS_UPDATE) != null ) {
            command = CommandFactory.get().make(Operation.StatusUpdate, target, parameters);
        } else {
            command = CommandFactory.get().make(Operation.Register, target, parameters);
        }
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
