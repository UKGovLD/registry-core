/******************************************************************
 * File:        Command.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Wraps up a registry request as a command object to modularize
 * processing such as authorization and audit trails.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public abstract class Command {

    public enum Operation { Read, Register, Delete, Update, StatusUpdate, Validate };

    protected Operation operation;
    protected String target;
    protected MultivaluedMap<String, String> parameters;
    protected Model payload;
    protected Registry registry;

    /**
     * Constructor
     * @param operation   operation request, as determined by HTTP verb
     * @param targetType  type of thing to act on, may be amended or set later after more analysis
     * @param target      the URI to which the operation was targeted, omits the assumed base URI
     * @param parameters  the query parameters
     */
    public Command(Operation operation, String target,  MultivaluedMap<String, String> parameters, Registry registry) {
        this.operation = operation;
        this.target = registry.getBaseURI() + (target.isEmpty() ? "" : "/" + target);
        this.parameters = parameters;
        this.registry = registry;
    }

    public Model getPayload() {
        return payload;
    }

    public void setPayload(Model payload) {
        this.payload = payload;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getTarget() {
        return target;
    }

    public MultivaluedMap<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return String.format("Command: %s on %s", operation, target);
    }

    public abstract Response execute() ;
}
