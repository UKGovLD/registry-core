/******************************************************************
 * File:        DelegationRecord.java
 * Created by:  Dave Reynolds
 * Created on:  17 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class DelegationRecord extends ForwardingRecord {
    static final Logger log = LoggerFactory.getLogger( DelegationRecord.class );

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

    /**
     * Enumerate all members of the delegated register
     * @return
     */
    public List<Resource> listMembers() {
        log.debug("Fetch delegation members from " + getTarget());
        String query =
                subject == null ?
                        String.format("SELECT ?m WHERE {?m <%s> <%s>}", predicate.getURI(), object.getURI())
                      : String.format("SELECT ?m WHERE {<%s> <%s> ?m}", subject.getURI(), predicate.getURI());
        QueryExecution exec = QueryExecutionFactory.sparqlService(getTarget(), query + " ORDER BY ?m");
        try {
            List<Resource> members = new ArrayList<>();
            ResultSet results = exec.execSelect();
            while (results.hasNext()) {
                members.add( results.next().getResource("m") );
            }
            return members;
        } finally {
            exec.close();
        }
    }

    /**
     * Return a description of a single delegated member
     */
    public Model describeMember(Resource member) {
        QueryExecution exec = QueryExecutionFactory.sparqlService(getTarget(), "DESCRIBE <"+ member.getURI() + ">");
        try {
            return exec.execDescribe();
        } catch (Exception e) {
            // Assume this is a 404/500 from the service, need some way to check this and log if appropriate
            return null;
        } finally {
            exec.close();
        }
    }

    /**
     * Add a description of all of the list members to the given model.
     */
    public void fetchMembers(Model model, List<Resource> members) {
        StringBuffer query = new StringBuffer();
        query.append("DESCRIBE ");
        for (Resource member : members) {
            query.append(" <");
            query.append(member.getURI());
            query.append(">");
        }
        QueryExecution exec = QueryExecutionFactory.sparqlService(getTarget(), query.toString());
        try {
            exec.execDescribe(model);
        } finally {
            exec.close();
        }
    }

}
