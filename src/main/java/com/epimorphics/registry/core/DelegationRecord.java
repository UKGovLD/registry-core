/******************************************************************
 * File:        DelegationRecord.java
 * Created by:  Dave Reynolds
 * Created on:  17 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

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
     */
    public List<Resource> listMembers() {
        log.debug("Fetch delegation members from {}", getTarget());
        String query =
                subject == null ?
                        String.format("SELECT ?m WHERE {?m <%s> <%s>}", predicate.getURI(), object.getURI())
                      : String.format("SELECT ?m WHERE {<%s> <%s> ?m}", subject.getURI(), predicate.getURI());
        try (QueryExecution exec = QueryExecution.service(getTarget()).query(query + " ORDER BY ?m").build()) {
            List<Resource> members = new ArrayList<>();
            ResultSet results = exec.execSelect();
            while (results.hasNext()) {
                members.add(results.next().getResource("m"));
            }
            return members;
        }
    }

    /**
     * Return a description of a single delegated member
     */
    public Model describeMember(Resource member) {
        try (QueryExecution exec = QueryExecution.service(getTarget()).query("DESCRIBE <" + member.getURI() + ">").build()) {
            return exec.execDescribe();
        } catch (Exception e) {
            // Assume this is a 404/500 from the service, need some way to check this and log if appropriate
            return null;
        }
    }

    /**
     * Add a description of all of the list members to the given model.
     */
    public void fetchMembers(Model model, List<Resource> members) {
        StringBuilder query = new StringBuilder();
        query.append("DESCRIBE ");
        for (Resource member : members) {
            query.append(" <");
            query.append(member.getURI());
            query.append(">");
        }

        try (QueryExecution exec = QueryExecution.service(getTarget()).query(query.toString()).build()) {
            exec.execDescribe(model);
        }
    }

}
