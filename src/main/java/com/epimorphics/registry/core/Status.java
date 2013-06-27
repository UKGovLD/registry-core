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
import java.util.List;

import com.epimorphics.registry.vocab.RegistryVocab;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * The set of status values which a RegisterItem can have.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public enum Status {
    NotAccepted(RegistryVocab.statusNotAccepted),
    Submitted(RegistryVocab.statusSubmitted),
    Reserved(RegistryVocab.statusReserved),
    Invalid(RegistryVocab.statusInvalid),
    Accepted(RegistryVocab.statusAccepted),
    Valid(RegistryVocab.statusValid),
    Experimental(RegistryVocab.statusExperimental),
    Stable(RegistryVocab.statusStable),
    Deprecated(RegistryVocab.statusDeprecated),
    Superseded(RegistryVocab.statusSuperseded),
    Retired(RegistryVocab.statusRetired),
    Any(RegistryVocab.Status)
    ;

    Resource resource;

    Status(Resource resource) {
        this.resource = resource;
    }

    public static Status forResource(Resource r) {
        return r == null ? NotAccepted : Status.valueOf(r.getLocalName().substring(6));
    }

    public static Status forString(String param, Status deflt) {
        if (param == null) {
            return deflt;
        }
        for (Status s : Status.values()) {
            if (s.name().equalsIgnoreCase(param)) {
                return s;
            }
        }
        return deflt;
    }

    public Resource getResource() {
        return resource;
    }

    public boolean isNotAccepted() {
        return this == Submitted || this == Invalid || this == NotAccepted || this == Reserved ;
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
        if (target == null || target == this) return true;
        switch (target) {
        case NotAccepted:
            return this == Submitted || this == Invalid || this == Reserved;
        case Accepted:
            return isA(Valid) || isA(Deprecated);
        case Valid:
            return this == Experimental || this == Stable;
        case Deprecated:
            return this == Superseded || this == Retired;
        case Any:
            return true;
        default:
            return false;
        }
    }

    /**
     * Return true of the target status is a legal next state after this status.
     * The target should be a concrete (leaf) status, otherwise will return false.
     */
    public boolean legalNextState(Status target) {
        if (target == null) return false;
        if (target == this) return true;
        switch (target) {
        case Experimental:
            return this == Submitted;
        case Stable:
            return this == Submitted || this == Experimental;
        case Retired:
        case Superseded:
            return this == Experimental || this == Stable || this == Accepted || this == Valid;
        case Invalid:
            return true;
        case Submitted:
            return this == Reserved;
        default:
            return false;
        }
    }
    
    /**
     * Return a list of all status values which are legal successors to this one
     */
    public List<Status> nextStates() {
        List<Status> results = new ArrayList<Status>();
        for (Status s : Status.values()) {
            if (legalNextState(s)) {
                results.add(s);
            }
        }
        return results;
    }

}
