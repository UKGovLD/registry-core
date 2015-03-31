/******************************************************************
 * File:        FacetSpec.java
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

import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * Specification for a single facet.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class FacetSpec implements Comparable<FacetSpec> {

    String name;
    String varname;
    String propertyPath;
    RDFNode value;

    /**
     * Create a facet specification
     * @param name the label of the facet, will be displayed
     * @param varname short name for the facet which will be used in state serialization
     * @param propertyPath sparql property path which links an item to the facet value
     */
    public FacetSpec(String name, String varname, String propertyPath) {
        this.name = name;
        this.varname = varname;
        this.propertyPath = propertyPath;
    }

    /**
     * Generate a SPARQL query fragment which returns the value for this facet, filtering if necessary
     */
    public String query() {
        return queryCheck() + (value != null ? queryFilter(value) : "");
    }

    private String queryCheck() {
        return "OPTIONAL { ?item " + propertyPath + " ?" + varname + " .}\n";
    }

    /**
     * Generate a SPARQL query fragment which filters for a specific value for this facet
     */
    private String queryFilter(RDFNode value) {
        String valueEnc = value.isURIResource() ? "<" + value.asResource().getURI() + ">" : value.toString();
        return "FILTER(?" + varname + " = " + valueEnc + ")\n";
    }

    @Override
    public int compareTo(FacetSpec arg0) {
        return name.compareTo(arg0.name);
    }

    public boolean isSet() {
        return value != null;
    }

    public void setValue(RDFNode value) {
        this.value = value;
    }


    public String getName() {
        return name;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public RDFNode getValue() {
        return value;
    }

    public String getVarname() {
        return varname;
    }
}
