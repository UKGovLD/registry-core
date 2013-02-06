/******************************************************************
 * File:        Command.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.server.webapi.WebApiException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

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
        this.target = registry.getBaseURI() + (target.isEmpty() ? "/" : "/" + target);
        this.parameters = parameters;
        this.registry = registry;
        this.store = registry.getStore();
        Matcher segmatch = LAST_SEGMENT.matcher(this.target);
        if (segmatch.matches()) {
            this.lastSegment = segmatch.group(2);
            this.parent = segmatch.group(1);
        } else {
            // Root register
            this.lastSegment = "";
            this.parent = target;
        }
    }
    static final Pattern LAST_SEGMENT = Pattern.compile("(^.*)/([^/]+)$");

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

    public abstract Response doExecute() ;
    
    public Response execute()  {
        // TODO - authorization
        Response response = doExecute();
        // TODO catch and rethrow exceptions to capture 404 and other status responses
        // TODO - logging and notification
        return response;
    }

    protected boolean hasParamValue(String param, String value) {
        List<String> values = parameters.get(param);
        if (values != null) {
            return values.contains(value);
        }
        return false;
    }

    protected String notation() {
        if (lastSegment.startsWith("_")) {
            return lastSegment.substring(0, lastSegment.length() - 1);
        } else {
            return lastSegment;
        }
    }

    protected String entityURI() {
        if (lastSegment.startsWith("_")) {
            return parent + "/" + notation();
        } else {
            return target;
        }
    }

    protected String itemURI() {
        if (lastSegment.startsWith("_")) {
            return target;
        } else {
            return parent + "/_" + lastSegment;
        }
    }

    protected Resource findSingletonRoot() {
        List<Resource> roots = payload.listSubjectsWithProperty(RDF.type).toList();
        if (roots.size() != 1) {
            throw new WebApiException(Response.Status.BAD_REQUEST, "Could not find unique entity root to register");
        }
        return roots.get(0);
    }
    
    protected String makeParamString(MultivaluedMap<String, String> parameters, String...omit) {
        StringBuffer params = new StringBuffer();
        boolean startedParams = false;
        for (String p : parameters.keySet()) {
            boolean skip = false;
            for (String o : omit) {
                if (p.equals(o)) {
                    skip = true;
                    break;
                }
            }
            if (skip) continue;
            if (startedParams) {
                params.append("&");
            } else {
                startedParams = true;
            }
            List<String> values = parameters.get(p);
            params.append(p);
            if (values == null || values.isEmpty()) continue;
            if (values.size() == 1 && values.get(0) == null) continue;
            params.append("=");
            boolean started = false;
            for (String value: values) {
                if (started) {
                    params.append(",");
                } else {
                    started = true;
                }
                params.append(value);
            }
        }
        return params.toString();
    }
}
