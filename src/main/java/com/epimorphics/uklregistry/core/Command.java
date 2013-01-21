/******************************************************************
 * File:        Command.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Wraps up a registry request as a command object to modularize
 * processing such as authorization and audit trails.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Command {

    public enum Operation { Read, Register, Delete, Update, Validate };
    public enum TargetType { REGISTER, ITEM, ENTITY, STATUS };
    
    Operation operation;
    TargetType targetType;
    String target;   

    Map<String, String[]> parameters;  
    Model payload;
    
    /**
     * Constructor
     * @param operation   operation request, as determined by HTTP verb
     * @param targetType  type of thing to act on, may be amended or set later after more analysis
     * @param target      the URI to which the operation was targeted, omits the assumed base URI
     * @param parameters  the query parameters 
     */
    public Command(Operation operation, TargetType targetType, String target,  Map<String, String[]> parameters) {
        this.operation = operation;
        this.targetType = targetType;
        this.target = target;
        this.parameters = parameters;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
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

    public Map<String, String[]> getParameters() {
        return parameters;
    }
    
    @Override
    public String toString() {
        return String.format("Command: %s on %s (%s)", operation, target, targetType);
    }

}
