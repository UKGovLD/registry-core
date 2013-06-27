/******************************************************************
 * File:        VersionInfo.java
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
    protected String replaces;
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

    public String getReplaces() {
        return replaces;
    }

    public void setReplaces(String replaces) {
        this.replaces = replaces;
    }

    @Override
    public String toString() {
        return String.format("%s (version: %s) [%d,", uri, version, fromTime) + (toTime == -1 ? " ...)" : (" " + toTime + "]"));
    }

}
