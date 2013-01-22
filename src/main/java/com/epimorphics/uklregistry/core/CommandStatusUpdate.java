/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;


public class CommandStatusUpdate extends Command {

    public CommandStatusUpdate(Operation operation, String target,
            MultivaluedMap<String, String> parameters) {
        super(operation, target, parameters);
    }

    @Override
    public Response execute() {
        // TODO implement
        System.out.println("Execute on " + this);
        return Response.ok().build();
    }

}
