/******************************************************************
 * File:        ForwardingRecord.java
 * Created by:  Dave Reynolds
 * Created on:  17 Feb 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

/**
 * Base class to record any forward/federation/delgation spec.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ForwardingRecord {
    
    public enum Type {
        FORWARD, FEDERATE, DELEGATE
    }

    protected String location;
    protected String delegationTarget;
    protected Type type;
    
    protected int forwardingCode = DEFAULT_CODE;
    
    public static int DEFAULT_CODE = 307;
    
    public ForwardingRecord(String location, String target, Type type) {
        this.location = location;
        delegationTarget = target;
        this.type = type;
    }

    public int getForwardingCode() {
        return forwardingCode;
    }

    public void setForwardingCode(int forwardingCode) {
        this.forwardingCode = forwardingCode;
    }

    public Type getType() {
        return type;
    }
    
    public String getLocation() {
        return location;
    }
    
    public String getTarget() {
        return delegationTarget;
    }
        
}
