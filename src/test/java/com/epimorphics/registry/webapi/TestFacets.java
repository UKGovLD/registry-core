/******************************************************************
 * File:        TestFacets.java
 * Created by:  Dave Reynolds
 * Created on:  20 May 2013
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

package com.epimorphics.registry.webapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;

import com.epimorphics.registry.webapi.facets.FacetResult;
import com.epimorphics.registry.webapi.facets.FacetResultEntry;
import com.epimorphics.registry.webapi.facets.FacetService;
import com.epimorphics.registry.webapi.facets.FacetSpec;
import org.apache.jena.rdf.model.Model;

public class TestFacets {

    @Test
    public void testBasicFacets() {
        FacetService service = new FacetService();
        service.setSpecFile("test/facets/dataset-facets.ttl");

        List<FacetSpec> facetSpecs = service.getSpecList();
        assertEquals(2, facetSpecs.size());
        FacetSpec spec = facetSpecs.get(0);
        assertEquals("Type", spec.getName());
        assertEquals("type", spec.getVarname());
        assertEquals("<http://purl.org/linked-data/registry#itemClass>", spec.getPropertyPath());

        Model testData = RDFDataMgr.loadModel("test/facets/facet-test.ttl");
        check(service, testData, "type=<http://example.com/test#type1>", null,2);
        check(service, testData, "type=<http://example.com/test#type2>", null,2);
        check(service, testData, "type=<http://example.com/test#type3>", null,1);
        check(service, testData, "category=<http://example.com/test#cat2>", null,2);
        check(service, testData, "type=<http://example.com/test#type1>|category=<http://example.com/test#cat2>", null,1);
    }

    @Test
    public void testMultilingualFacets() {
        FacetService service = new FacetService();
        service.setSpecFile("test/facets/dataset-facets-lang.ttl");

        List<FacetSpec> facetSpecs = service.getSpecList();
        assertEquals(2, facetSpecs.size());

        FacetSpec spec1 = facetSpecs.get(0);
        assertEquals("Type", spec1.getLabel("en"));
        assertEquals("Genre", spec1.getLabel("fr"));

        FacetSpec spec2 = facetSpecs.get(1);
        assertEquals("Category", spec2.getLabel("en"));
        assertEquals("Catégorie", spec2.getLabel("fr"));

        Model testData = RDFDataMgr.loadModel("test/facets/facet-test-lang.ttl");
        check(service, testData, "type=<http://example.com/test#type1>", "fr", 2);
        check(service, testData, "type=<http://example.com/test#type2>", "fr", 2);
        check(service, testData, "type=<http://example.com/test#type3>", "fr", 1);
        check(service, testData, "category=<http://example.com/test#cat2>", "fr", 2);
        check(service, testData, "type=<http://example.com/test#type1>|category=<http://example.com/test#cat2>", "fr", 1);
    }

    private void check(FacetService service, Model testData, String state, String lang, int expected) {
        FacetResult result = new FacetResult(service.getBaseQuery(), state, service.getSpecList(), lang, testData);
        List<FacetResultEntry> page = result.getResultsPage(0);
        assertEquals(expected, page.size());
    }
}
