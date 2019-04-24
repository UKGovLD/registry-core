/******************************************************************
 * File:        Status.java
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

package com.epimorphics.registry.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.vocabs.SKOS;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;

/**
 * The set of status values which a RegisterItem can have.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Status {
    static final Logger log = LoggerFactory.getLogger( Status.class );
    
    public static final String PRES_DEFAULT = "default";
    public static final String PRES_SUCCESS = "success";
    public static final String PRES_WARNING = "warning";
    public static final String PRES_DANGER  = "danger";
    
    public static Status Any          = new Status(RegistryVocab.statusAny,             "any");
    
    public static Status NotAccepted  = new Status(RegistryVocab.statusNotAccepted,  "notaccepted", PRES_DEFAULT, Any);
    public static Status Submitted    = new Status(RegistryVocab.statusSubmitted,    "submitted",   PRES_DEFAULT, NotAccepted);
    public static Status Reserved     = new Status(RegistryVocab.statusReserved,     "reserved",    PRES_DEFAULT, NotAccepted);
    public static Status Invalid      = new Status(RegistryVocab.statusInvalid,      "invalid",     PRES_DANGER,  NotAccepted);
    
    public static Status Accepted     = new Status(RegistryVocab.statusAccepted,     "accepted",    PRES_DEFAULT, Any);
    public static Status Valid        = new Status(RegistryVocab.statusValid,        "valid",       PRES_DEFAULT, Accepted);
    public static Status Deprecated   = new Status(RegistryVocab.statusDeprecated,   "deprecated",  PRES_DANGER,  Accepted);
    public static Status Superseded   = new Status(RegistryVocab.statusSuperseded,   "superseded",  PRES_DANGER,  Deprecated);
    public static Status Retired      = new Status(RegistryVocab.statusRetired,      "retired",     PRES_DANGER,  Deprecated);
    
    public static Status Stable       = new Status(RegistryVocab.statusStable,       "stable",       PRES_SUCCESS, Valid);
    public static Status Experimental = new Status(RegistryVocab.statusExperimental, "experimental", PRES_WARNING, Valid);

    public static final String LIFECYCLE_REGISTER = "/system/lifecycle";

    protected static Map<String, Status> statusIndex;
    protected static boolean needsLoad = true;
    
    protected Resource resource;
    protected String label;
    protected Set<Status> successors = new HashSet<>();
    protected Status parent = Any;
    protected String presentation;
    
    protected Status(Resource resource, String label) {
        this(resource, label, PRES_DEFAULT, null);
    }
    
    protected Status(Resource resource, String label, String presentation, Status parent, Status...successors) {
        this.resource = resource;
        this.label = label.toLowerCase();
        if (parent != null) this.parent = parent;
        this.presentation = presentation;
        for (Status successor : successors) {
            this.successors.add( successor );
        }
    }
    
    public void addSuccessor(Status successor) {
        successors.add(successor);
    }
    
    public String getPresentation() {
        return presentation;
    }

    public void setPresentation(String presentation) {
        this.presentation = presentation;
    }

    public String getLabel() {
        return label;
    }

    public Collection<Status> nextStates() {
        return successors;
    }

    public void setParent(Status parent) {
        this.parent = parent;
    }

    public Resource getResource() {
        return resource;
    }
    
    public void reset() {
        successors.clear();
        if (Invalid != null) {
            successors.add(Invalid);
        }
    }
    
    @Override
    public boolean equals(Object other) {
        return other instanceof Status && this.resource.equals( ((Status)other).resource );
    }
    
    @Override
    final public int hashCode() {
        return resource.hashCode();
    }

    @Override
    public String toString() {
        return label;
    }
    
    public static Status forResource(RDFNode r) {
        load();
        if (r.isLiteral()) {
            String key = r.asLiteral().getString();
            return statusIndex.get(key);
        } else if (r.isURIResource()) {
            for (Status s : statusIndex.values()) {
                if (s.getResource().equals(r.asResource())) {
                    return s;
                }
            }
        }

        return NotAccepted;
    }

    public static Status forString(String param, Status deflt) {
        load();
        Status s = statusIndex.get(param);
        return s == null ? deflt : s;
    }

    public boolean isNotAccepted() {
        return isA( NotAccepted );
    }

    public boolean isAccepted() {
        return ! isNotAccepted();
    }

    public boolean isDeprecated() {
        return this == Superseded || this == Retired;
    }

    /**
     * Return true if this status is an instance of the target status category
     */
    public boolean isA(Status target) {
        load();
        if (target == null) return true;
        if (this.equals(target)) {
            return true;
        } else {
            if (parent != null) {
                return parent.isA(target);
            } else {
                return false;
            }
        }
    }

    /**
     * Return true of the target status is a legal next state after this status.
     * The target should be a concrete (leaf) status, otherwise will return false.
     */
    public boolean legalNextState(Status target) {
        load();
        if (this.equals(target)) return true;
        for (Status n : successors) {
            if ( n.equals(target) ) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Force reload of the status register
     */
    public synchronized static void needsReload() {
        needsLoad = true;
    }
    
    /**
     * Update the status set based on the status register
     */
    public synchronized static void load() {
        if (needsLoad) {
            needsLoad = false;

            if (Registry.get() == null) { // Guard for test cases
                resetStatus();
                setDefaultLifecycle();
                return;
            }

            StoreAPI store = Registry.get().getStore();
            store.beginSafeRead();
            try {
                resetStatus();
    
                String registerURI = Registry.get().getBaseURI() + LIFECYCLE_REGISTER;
                Description d = store.getDescription(registerURI);
                if (d instanceof Register) {
                    Register register = (Register) d;
                    log.info("Loading custom status lifecycle");
            
                    Model view = ModelFactory.createDefaultModel();
                    List<Resource> members = new ArrayList<>();
                    register.constructView(view, false, null, 0, -1, -1, members);
                
                    for (Resource member : members) {
                        Status s = new Status(member, RDFUtil.getLabel(member));
                        addStatus(s);
                        Resource parent = RDFUtil.getResourceValue(member, SKOS.broader);
                        if (parent != null) {
                            s.setParent( forResource(parent) );
                        }
                        s.setPresentation( RDFUtil.getStringValue(member, RegistryVocab.presentation, PRES_DEFAULT) );
                        s.addSuccessor(Invalid);
                        for (Status suc : getStatusValues(member, RegistryVocab.nextState)) {
                            s.addSuccessor(suc);
                        }
                        for (Status p : getStatusValues(member, RegistryVocab.priorState)) {
                            p.addSuccessor(s);
                        }
                    }
                } else {
                    setDefaultLifecycle();
                }
            } finally {
                store.endSafeRead();
            }

            printLifecycle(Reserved, new HashSet<Status>());
        }
    }

    private static void setDefaultLifecycle() {
        log.info("Setting default status lifecycle");
        // No custom lifecycle found
        Submitted.addSuccessor(Stable);
        Submitted.addSuccessor(Experimental);
        
        Stable.addSuccessor(Retired);
        Stable.addSuccessor(Superseded);
        
        Experimental.addSuccessor(Stable);
        Experimental.addSuccessor(Retired);
        Experimental.addSuccessor(Superseded);
    }

    private static void resetStatus() {
        // Reset back to just builtins
        statusIndex = new HashMap<String, Status>();
        for (Status s : new Status[]{Any, NotAccepted, Submitted, Reserved, Invalid, Accepted, Valid, Deprecated, Superseded, Retired,Stable, Experimental}) {
            s.reset();
            addStatus(s);
        }
        Reserved.addSuccessor(Submitted);
    }
    
    // Log lifecycle summary, mostly for debugging
    private static void printLifecycle(Status start, Set<Status> seen) {
        if ( seen.add(start) ) {
            String message = "   " + start + " -> [";
            for (Status s : start.nextStates()) {
                message += s.toString() + " ";
            }
            message += "]";
            log.info(message);
            for (Status s : start.nextStates()) {
                printLifecycle(s, seen);
            }
        }
    }
    
    
    protected static List<Status> getStatusValues(Resource root, Property p) {
        List<Status> results = new ArrayList<>();
        for (StmtIterator i = root.listProperties(p); i.hasNext();) {
            RDFNode value = i.next().getObject();
            Status s = null;
            if (value.isLiteral()) {
                s = forString(value.asLiteral().getLexicalForm(), null);
            } else if (value.isURIResource()) {
                s = forResource(value.asResource());
            }
            if (s != null) {
                results.add( s );
            }
        }
        return results;
    }
    
    protected static void addStatus(Status s) {
        statusIndex.put(s.getLabel(), s);
    }
}
