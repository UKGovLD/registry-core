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

import com.epimorphics.uklregistry.store.Description;
import com.epimorphics.uklregistry.store.Register;
import com.epimorphics.uklregistry.store.StoreAPI;
import com.sun.jersey.api.NotFoundException;


public class CommandRead extends Command {

    public CommandRead(Operation operation, String target,
            MultivaluedMap<String, String> parameters, StoreAPI store) {
        super(operation, target, parameters, store);
    }

    @Override
    public Response execute() {
        Description description = store.getDescription(target);
        if (description == null) {
            throw new NotFoundException();
        }
        if (description instanceof Register) {
            Register register = (Register)description;
            register.fetchMembers(store);
        }
        return Response.ok().entity(description.getRoot().getModel()).build();
    }

}
