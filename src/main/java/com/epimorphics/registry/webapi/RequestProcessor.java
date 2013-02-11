/******************************************************************
 * File:        RequestProcessor.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

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
import javax.ws.rs.core.StreamingOutput;

import com.epimorphics.registry.commands.CommandUpdate;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.Command.Operation;
import com.epimorphics.registry.util.PATCH;
import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.server.templates.VelocityRender;
import com.epimorphics.server.webapi.BaseEndpoint;
import com.hp.hpl.jena.util.FileManager;
import com.sun.jersey.api.NotFoundException;

/**
 * Filter all requests as possible register API requests.
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
@Path("{path: .*}")
public class RequestProcessor extends BaseEndpoint {


    @GET
    @Produces("text/html")
    public Response htmlrender() {
        checkForPassThrough();
        MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
        if (parameters.containsKey(Parameters.FORMAT)) {
            Response response = read();
            if (parameters.getFirst(Parameters.FORMAT).equals("ttl")) {
                return Response.ok().type(MIME_TURTLE).entity(response.getEntity()).build();
            } else {
                return Response.ok().type(MIME_RDFXML).entity(response.getEntity()).build();
            }
        } else {
            VelocityRender velocity = ServiceConfig.get().getServiceAs(Registry.VELOCITY_SERVICE, VelocityRender.class);
            StreamingOutput out = velocity.render("main.vm", uriInfo.getPath(), context, parameters,
                        "registry", Registry.get(), "requestor", getRequestor());
            return Response.ok().type("text/html").entity(out).build();
        }
    }

    @GET
    @Produces({MIME_TURTLE, MIME_RDFXML})
    public Response read() {
        checkForPassThrough();
        Command command = null;
        if (uriInfo.getQueryParameters().containsKey(Parameters.QUERY)) {
            command = makeCommand( Operation.Search );
        } else {
            command = makeCommand( Operation.Read );
        }
        return command.execute();
    }

    private void checkForPassThrough() {
        String path = uriInfo.getPath();
        System.out.println("Path check on " + path);
        if (path.startsWith("ui") || path.startsWith("system/query") || path.equals("favicon.ico")) {
            // Pass through all ui requests to the generic velocity handler, which in turn falls through to file serving
            throw new NotFoundException();
        }
    }

    private Command makeCommand(Operation op) {
        Command c = Registry.get().make(op, uriInfo.getPath(), uriInfo.getQueryParameters());
        c.setRequestor(getRequestor());
        return c;
    }

    @POST
    @Consumes({MIME_TURTLE, MIME_RDFXML})
    public Response register(@Context HttpHeaders hh, InputStream body) {
        MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
        Command command = null;
        if ( parameters.get(Parameters.VALIDATE) != null ) {
            for (String uri : FileManager.get().readWholeFileAsUTF8(body).split("\\s")) {
                parameters.add(Parameters.VALIDATE, uri);
            }
            command = makeCommand(Operation.Validate);
        } else if ( parameters.get(Parameters.STATUS_UPDATE) != null ) {
            command = makeCommand(Operation.StatusUpdate);
        } else {
            command = makeCommand(Operation.Register);
            command.setPayload( getBodyModel(hh, body) );
        }
        return command.execute();
    }

    @PUT
    @Consumes({MIME_TURTLE, MIME_RDFXML})
    public Response update(@Context HttpHeaders hh, InputStream body) {
        Command command = makeCommand( Operation.Update );
        command.setPayload( getBodyModel(hh, body) );
        return command.execute();
    }

    @DELETE
    public Object delete() {
        Command command = makeCommand( Operation.Delete );
        return command.execute();
    }

    @PATCH
    @Consumes({MIME_TURTLE, MIME_RDFXML})
    public Response updatePatch(@Context HttpHeaders hh, InputStream body) {
        Command command = makeCommand( Operation.Update );
        ((CommandUpdate)command).setToPatch();
        command.setPayload( getBodyModel(hh, body) );
        return command.execute();
    }

}
