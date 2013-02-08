/******************************************************************
 * File:        Status.java
 * Created by:  Dave Reynolds
 * Created on:  26 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

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
        return this == Submitted || this == Invalid || this == NotAccepted;
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
            return this == Submitted || this == Invalid;
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
     * The target should be a concrete (leaf) status, other wise will return false.
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
        default:
            return false;
        }

    }

}
