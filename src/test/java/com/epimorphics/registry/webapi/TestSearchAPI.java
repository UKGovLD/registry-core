/******************************************************************
 * File:        TestAPI.java
 * Created by:  Dave Reynolds
 * Created on:  1 Feb 2013
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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.vocab.Ldbp_orig;
import com.epimorphics.util.TestUtil;
import com.epimorphics.vocabs.API;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDFS;


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

    public String getWebappRoot() {
        return "src/test/webapp-index";
    }

    @Test
    public void testSearch() {
        // Set up some examples
        assertEquals("Register a register", 201, postFile("test/reg1.ttl", BASE_URL, "text/turtle").getStatus());
        assertEquals(201, postFileStatus("test/red.ttl", REG1));
        assertEquals(201, postFileStatus("test/absolute-black.ttl", REG1));
        assertEquals(201, postFileStatus("test/blue.ttl", REG1));
        assertEquals(201, postFileStatus("test/green.ttl", REG1));

        assertEquals(204,  invoke("PUT", "test/red1.ttl", REG1 + "/red").getStatus());

        assertEquals(201, postFileStatus("test/bulk-skos-collection.ttl", BASE_URL + "?batch-managed"));

        Model m = getModelResponse(BASE_URL + "?query=blue");
        normalizeRoot(m, ROOT_REGISTER, "query=blue&firstPage");
        checkModelResponse(m, "test/expected/search-result-blue.ttl", API.items);
        
        m = getModelResponse(BASE_URL + "?query=blue&_view=with_metadata");
        m.write(System.out, "Turtle");
        normalizeRoot(m, ROOT_REGISTER, "query=blue&_view=with_metadata&firstPage");
        normalizeRoot(m, ROOT_REGISTER, "query=blue&_view=with_metadata");
        m.write(System.out, "Turtle");
        checkModelResponse(m, "test/expected/search-result-blue-with-metadata.ttl", API.items);

        checkSearch("query=blue", "blue");
        checkSearch("query=item", "item 1", "item 2", "item 3");
        checkSearch("query=collection", "A test collection");

        checkSearch("query=collection+item", "item 1", "item 2", "item 3", "A test collection");

        checkSearch("query=collection+item&rdf:type=http://www.w3.org/2004/02/skos/core%23Concept", "item 1", "item 2", "item 3");

        assertEquals(204, post(BASE_URL + "collection/_item2?update&status=stable").getStatus());
        checkSearch("query=collection+item&status=stable", "item 2");

//        m.write(System.out, "Turtle");
    }
    
    /**
     * The query argument order of the root resouce can vary depending on execution
     * environment, normalize it
     */
    protected void normalizeRoot(Model m, String base, String arg) {
        List<String> args = new ArrayList<>();
        for (String a : arg.split("&")) args.add(a);
        Set<String> perms = new HashSet<>();
        permute(args, 0, perms);
        for (String perm : perms) {
            Resource r = m.getResource(base + "?" + perm);
            if (r.listProperties().hasNext()) {
                ResourceUtils.renameResource(r, base + "?" + arg);
            }
        }
    }

    static void permute(List<String> arr, int k, Set<String> perms){
        for(int i = k; i < arr.size(); i++){
            Collections.swap(arr, i, k);
            permute(arr, k+1, perms);
            Collections.swap(arr, k, i);
        }
        if (k == arr.size() -1){
            StringBuffer merge = new StringBuffer();
            boolean started = false;
            for (String a : arr) {
                if (started) merge.append("&"); else started = true; 
                merge.append(a);
            }
            perms.add(merge.toString());
        }
    }
    
    protected Model checkSearch(String query, String...members) {
        Model m = getModelResponse(BASE_URL + "?" + query);
        List<RDFNode> roots = m.listObjectsOfProperty(Ldbp_orig.pageOf).toList();
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
