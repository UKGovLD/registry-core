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

import com.epimorphics.uklregistry.vocab.Registry;
import com.hp.hpl.jena.vocabulary.RDF;


public class CommandRegister extends Command {

    public CommandRegister(Operation operation, String target,
            MultivaluedMap<String, String> parameters) {
        super(operation, target, parameters);
    }

    @Override
    public Response execute() {

        if (payload.contains(null, RDF.type, Registry.Register)) {
            // TODO implement creation of sub-registers
        } else {
            String registerURI = Configuration.getBaseURI() + "/" + target;
            // TODO check its a register
            // TODO make sure there's a root register
            String notation = determineRegistrationLocation();
        }
        // TODO implement
        System.out.println("Execute on " + this);
        return Response.noContent().build();
    }

    private String determineRegistrationLocation() {
        // TODO implement - based on relative URI in request, or notation on explicit register item or auto-allocation
        return null;
    }

}
