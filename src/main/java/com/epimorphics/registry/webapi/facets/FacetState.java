/******************************************************************
 * File:        FacetState.java
 * Created by:  Dave Reynolds
 * Created on:  13 Jun 2012
 *
 * (c) Copyright 2012, Epimorphics Limited
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

package com.epimorphics.registry.webapi.facets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.util.EpiException;
import org.apache.jena.rdf.model.RDFNode;

/**
 * Represents the state of facet selection in a way that can be serialized/deserialized in a URI.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class FacetState {

    public static final int SEARCH_LENGTH_LIMIT = 500;

    Map<String, FacetSpec> specs = new HashMap<String, FacetSpec>();
    List<FacetSpec> specList = new ArrayList<FacetSpec>();

    public FacetState(List<FacetSpec> specList) {
        for (FacetSpec spec : specList) {
            FacetSpec clone = new FacetSpec(spec.getName(), spec.getVarname(), spec.getPropertyPath());
            this.specList.add(clone);
            specs.put(spec.getVarname(), clone);
        }
    }

    /**
     * Expand the given base SPARQL query (which is assumed to bind a ?item var)
     * with additional OPTIONALs to retrieve facet values and FILTERs to filter them.
     */
    public String expandQuery(String baseQuery) {
        StringBuffer buff = new StringBuffer();
        buff.append( "SELECT * WHERE {");
        buff.append( baseQuery );
        buff.append("\n");
        for (FacetSpec spec : specs.values()) {
            buff.append( spec.query() );
        }
        buff.append(" } LIMIT " + SEARCH_LENGTH_LIMIT);
        return buff.toString();
    }

    /**
     * Return the list of facet specifications, ordered by name
     */
    public List<FacetSpec> getFacetSpecs() {
        return specList;
    }

    /**
     * Return the facet specification with the given name
     */
    public FacetSpec getFacetSpec(String name) {
        return specs.get(name);
    }

    /**
     * Serialize the state of facet selection.
     * Fragile assumes states don't have '=' or '|' characters.
     */
    public String serialize() {
        return serializeDelta(null, null, null);
    }

    /**
     * Seralize the state of facet selection with the addition of the given facet value
     */
    public String serializeWith(String add, RDFNode value) {
        return serializeDelta(null, add, value);
    }

    /**
     * Seralize the state of facet selection with the omission of
     * the named facet
     */
    public String serializeWithout(String omit) {
        return serializeDelta(omit, null, null);
    }

    // TODO improve
    private String serializeDelta(String omit, String add, RDFNode value) {
        StringBuffer buff = new StringBuffer();
        for (FacetSpec fs : specs.values()) {
            String fname = fs.getVarname();
            if ( ! fname.equals(omit) ) {
                RDFNode setval = fname.equals(add) ? value : fs.getValue();
                if (setval != null) {
                    if (buff.length() > 0) buff.append("|");
                    buff.append( fname + "=" + RDFUtil.serlialize( setval ) );
                }
            }
        }
        return buff.toString();
    }

    /**
     * Parse a serialize state of facet selection and apply it
     */
    public void setState(String serialize) {
        if (serialize == null || serialize.isEmpty()) return;
        for (String fstate: serialize.split("\\|")) {
            String[] fstatec = fstate.split("=");
            if (fstatec.length != 2) {
                throw new EpiException("Could not parse facet state: " + serialize);
            }
            FacetSpec fs = specs.get(fstatec[0]);
            if (fs == null) {
                throw new EpiException("Facet " + fstatec[0] + " not recognized in state: " + serialize);
            }
            fs.setValue( RDFUtil.deserialize( fstatec[1] ) );
        }
    }

}
