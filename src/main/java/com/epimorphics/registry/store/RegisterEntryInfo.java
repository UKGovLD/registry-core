/******************************************************************
 * File:        RegisterEntryInfo.java
 * Created by:  Dave Reynolds
 * Created on:  26 Jan 2013
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

package com.epimorphics.registry.store;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.epimorphics.registry.core.Status;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Struct which provides a summary description of an entry in a register.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegisterEntryInfo {

    protected Status status;
    protected String itemURI;
    protected String entityURI;
    protected Set<Literal> labels = new HashSet<Literal>();
    protected Set<Resource> types = new HashSet<Resource>();
    protected String notation;

    public RegisterEntryInfo(Resource status, Resource item, Resource entity, Literal label, Resource type, Literal notation) {
        this.status = Status.forResource(status);
        this.itemURI = item.getURI();
        this.entityURI = entity.getURI();
        this.labels.add( label );
        this.types.add( type );
        this.notation = notation == null ? null : notation.getLexicalForm();
    }

    public RegisterEntryInfo(Resource entity, Status status) {
        this.status = status;
        this.entityURI = entity.getURI();
    }

    public void addLabel(Literal label) {
        labels.add(label);
    }

    public void addType(Resource type) {
        types.add(type);
    }

    public Status getStatus() {
        return status;
    }
    public String getItemURI() {
        return itemURI;
    }
    public String getEntityURI() {
        return entityURI;
    }
    public Collection<Literal> getLabels() {
        return labels;
    }
    public Collection<Resource> getTypes() {
        return types;
    }
    public String getNotation() {
        return notation;
    }
    
    public boolean hasLabel(String lexical) {
        for (Literal label : labels) {
            if (label.getLexicalForm().equals(lexical)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("Entry %s (%s) - %s", labels.iterator().next(), entityURI, status);
    }

}
