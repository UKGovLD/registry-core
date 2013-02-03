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
    ;

    Resource resource;

    Status(Resource resource) {
        this.resource = resource;
    }

    public static Status forResource(Resource r) {
        return r == null ? NotAccepted : Status.valueOf(r.getLocalName().substring(6));
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
        default:
            return false;
        }
    }

}
