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

import com.epimorphics.uklregistry.store.StoreAPI;


public class CommandUpdate extends Command {

    public CommandUpdate(Operation operation, String target,
            MultivaluedMap<String, String> parameters, StoreAPI store) {
        super(operation, target, parameters, store);
    }

    @Override
    public Response execute() {
        // TODO implement
        System.out.println("Execute on " + this);
        return Response.ok().build();
    }

}
