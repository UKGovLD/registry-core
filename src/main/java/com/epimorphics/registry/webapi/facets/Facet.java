/******************************************************************
 * File:        Facet.java
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epimorphics.rdfutil.RDFUtil;
import org.apache.jena.rdf.model.RDFNode;

/**
 * Represents an individual facet option. Corresponds to some
 * property and a set of possible values.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Facet implements Comparable<Facet> {

    Map<RDFNode, FacetValue> values = new HashMap<>();
    RDFNode fixedValue;
    FacetSpec spec;
    String valueLex;
    String lang;


    public Facet(FacetSpec spec, String lang) {
        this.spec = spec;
        this.lang = lang;
    }

    /**
     * Record that this facet has been forced to a single value by the facet state
     */
    public void setFixedValue(RDFNode value) {
        fixedValue = value;
        valueLex = getLabel(value);
    }

    /**
     * Return true if this facet is set of a value by the facet state
     */
    public boolean isSet() {
        return fixedValue != null;
    }

    /**
     * Return the value, if any, which this facet has been assigned by state
     */
    public RDFNode getValue() {
        return fixedValue;
    }

    public String getValueLexicalForm() {
        return valueLex;
    }

    public void inc(RDFNode item) {
        FacetValue value = values.get(item);
        if (value == null) {
            value = new FacetValue(item, lang);
            values.put(item, value);
        } else {
            value.inc();
        }
    }

    /**
     * Return true if this facet is not empty (either has been selected or has a non-empty set of value options
     */
    public boolean notEmpty() {
        return fixedValue != null || values.size() > 0;
    }

    /**
     * Return the name of this facet
     */
    public String getName() {
        return spec.getLabel(lang);
    }

    /**
     * Return the variable name used for this facet in the query
     */
    public String getVarname() {
        return spec.getVarname();
    }

    /**
     * Return an ordered list of value objects for this facet
     */
    public List<FacetValue> getValues() {
        List<FacetValue> results = new ArrayList<>(values.values().size());
        results.addAll( values.values() );
        Collections.sort( results );
        return results;
    }

    @Override
    public int compareTo(Facet arg0) {
        return spec.name.compareTo( arg0.spec.name );
    }

    private String getLabel(RDFNode node) {
        if (node.isResource()) {
            return RDFUtil.getLabel(node.asResource(), lang);
        } else {
            return RDFUtil.getLabel(node);
        }
    }
}
