/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.Command.Operation;
import com.epimorphics.registry.store.StoreAPI;
import com.sun.jersey.api.NotFoundException;


public class CommandRead extends Command {

    public CommandRead(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super(operation, target, parameters, registry);
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
