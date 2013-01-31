/******************************************************************
 * File:        VersionInfo.java
 * Created by:  Dave Reynolds
 * Created on:  26 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import com.epimorphics.rdfutil.RDFUtil;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Struct used for reporting the available versions of a resource.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class VersionInfo {

    protected String uri;
    protected String version;
    protected long fromTime = -1;
    protected long toTime = -1;

    public VersionInfo(Resource resource, Literal version, Literal from, Literal to) {
        this.uri = resource.getURI();
        this.version = version.getLexicalForm();
        if (from != null) {
            this.fromTime = RDFUtil.asTimestamp(from);
        }
        if (to != null) {
            this.toTime = RDFUtil.asTimestamp(to);
        }
    }

    public String getUri() {
        return uri;
    }

    public String getVersion() {
        return version;
    }

    public long getFromTime() {
        return fromTime;
    }

    public long getToTime() {
        return toTime;
    }

    @Override
    public String toString() {
        return String.format("%s (version: %s) [%d,", uri, version, fromTime) + (toTime == -1 ? " ...)" : (" " + toTime + "]"));
    }

}
