/******************************************************************
 * File:        DelegationRecord.java
 * Created by:  Dave Reynolds
 * Created on:  17 Feb 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class DelegationRecord extends ForwardingRecord {

    protected Resource subject;
    protected Resource predicate; 
    protected Resource object;

    public DelegationRecord(String location, String target, Type type) {
        super(location, target, type);
    }

    public void setSubject(Resource subject) {
        this.subject = subject;
    }

    public void setPredicate(Resource predicate) {
        this.predicate = predicate;
    }

    public void setObject(Resource object) {
        this.object = object;
    }
    
    // TODO implement list method
    
    // TODO implement get method
}
