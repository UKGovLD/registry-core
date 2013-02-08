/******************************************************************
 * File:        TestAPI.java
 * Created by:  Dave Reynolds
 * Created on:  1 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.vocab.Ldbp;
import com.epimorphics.util.TestUtil;
import com.epimorphics.vocabs.API;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;


/**
 * Test harness for testing access to the API - launches a registry using an
 * embedded tomcat and a memory-backed store then talks to the API using http.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TestSearchAPI extends TomcatTestBase {

    static final String EXT_BLACK = "http://example.com/colours/black";
    static final String REG1 = BASE_URL + "reg1";
    static final String REG1_URI = ROOT_REGISTER + "reg1";
    static final String REG1_ITEM = ROOT_REGISTER + "_reg1";
    static final String REGL_URL = BASE_URL + "regL";

    String getWebappRoot() {
        return "src/test/webapp-index";
    }

    @Test
    public void testSearch() {
        checkLive();

        // Set up some examples
        assertEquals("Register a register", 204, postFile("test/reg1.ttl", BASE_URL, "text/turtle").getStatus());
        assertEquals(204, postFileStatus("test/red.ttl", REG1));
        assertEquals(204, postFileStatus("test/absolute-black.ttl", REG1));
        assertEquals(204, postFileStatus("test/blue.ttl", REG1));
        assertEquals(204, postFileStatus("test/green.ttl", REG1));

        assertEquals(204,  invoke("PUT", "test/red1.ttl", REG1 + "/red").getStatus());

        assertEquals(204, postFileStatus("test/bulk-skos-collection.ttl", BASE_URL + "?batch-managed"));

        Model m = getModelResponse(BASE_URL + "?query=blue");
        checkModelResponse(m, "test/expected/search-result-blue.ttl", API.items);
        m = getModelResponse(BASE_URL + "?query=blue&_view=with_metadata");
        checkModelResponse(m, "test/expected/search-result-blue-with-metadata.ttl", API.items);

        checkSearch("query=blue", "blue");
        checkSearch("query=item", "item 1", "item 2", "item 3");
        checkSearch("query=collection", "A test collection");

        checkSearch("query=collection+item", "item 1", "item 2", "item 3", "A test collection");

        checkSearch("query=collection+item&type=http://www.w3.org/2004/02/skos/core%23Concept", "item 1", "item 2", "item 3");

        assertEquals(204, post(BASE_URL + "collection/_item2?update&status=stable").getStatus());
        checkSearch("query=collection+item&status=http://purl.org/linked-data/registry%23statusStable", "item 2");

//        m.write(System.out, "Turtle");
    }

    protected Model checkSearch(String query, String...members) {
        Model m = getModelResponse(BASE_URL + "?" + query);
        List<RDFNode> roots = m.listObjectsOfProperty(Ldbp.pageOf).toList();
        assertEquals(1, roots.size());
        Resource result = roots.get(0).asResource();
        List<String> actual = new ArrayList<String>();
        StmtIterator si = result.listProperties(RDFS.member);
        while (si.hasNext()) {
            Resource member = si.next().getObject().asResource();
            actual.add( RDFUtil.getStringValue(member, RDFS.label) );
        }
        TestUtil.testArray(actual, members);
        return m;
    }

}
