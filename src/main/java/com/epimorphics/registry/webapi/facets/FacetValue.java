/******************************************************************
 * File:        FacetValue.java
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

import com.epimorphics.rdfutil.RDFUtil;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * Represents a possible value for a Facet together with the filter count for it.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class FacetValue implements Comparable<FacetValue> {

    RDFNode value;
    String lex;
    int count;

    public FacetValue(RDFNode value) {
        this.value = value;
        count = 1;
        lex = RDFUtil.getLabel(value);
    }

    public void inc() {
        count++;
    }

    public RDFNode getValue() {
        return value;
    }

    public int getCount() {
        return count;
    }

    public String getLexicalForm() {
        return lex;
    }

    @Override
    public int compareTo(FacetValue o) {
        return lex.compareTo( o.lex );
    }

}
