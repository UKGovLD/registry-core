/******************************************************************
 * File:        Command.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.epimorphics.registry.store.StoreAPI;
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
    protected String parent;
    protected String lastSegment;
    protected MultivaluedMap<String, String> parameters;
    protected Model payload;
    protected Registry registry;
    protected StoreAPI store;

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
        this.store = registry.getStore();
        Matcher segmatch = LAST_SEGMENT.matcher(target);
        if (segmatch.matches()) {
            this.lastSegment = segmatch.group(2);
            this.parent = segmatch.group(1);
        }
    }
    static final Pattern LAST_SEGMENT = Pattern.compile("^.*/([^/]+)$");

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

    protected boolean hasParamValue(String param, String value) {
        return parameters.get(param).contains(value);
    }
}
