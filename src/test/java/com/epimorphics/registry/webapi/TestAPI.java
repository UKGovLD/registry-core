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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.Ldbp;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.util.TestUtil;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.ibm.icu.util.Calendar;
import com.sun.jersey.api.client.ClientResponse;


/**
 * Test harness for testing access to the API - launches a registry using an
 * embedded tomcat and a memory-backed store then talks to the API using http.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TestAPI extends TomcatTestBase {

    static final String EXT_BLACK = "http://example.com/colours/black";
    static final String REG1 = BASE_URL + "reg1";
    static final String REG1_URI = ROOT_REGISTER + "reg1";
    static final String REG1_ITEM = ROOT_REGISTER + "_reg1";
    static final String REGL_URL = BASE_URL + "regL";

    String getWebappRoot() {
        return "src/test/webapp";
    }

    @Test
    public void testBasics() {
        // Registration
        ClientResponse response = postFile("test/reg1.ttl", BASE_URL, "text/turtle");
        assertEquals("Register a register", 201, response.getStatus());
        assertEquals(REG1_ITEM, response.getLocation().toString());
        assertEquals("Register in non-existant location", 404, postFileStatus("test/reg1.ttl", BASE_URL+"foo"));
        assertEquals("Register the same again", 403, postFileStatus("test/reg1.ttl", BASE_URL));
        assertEquals(201, postFileStatus("test/red.ttl", REG1));

        // Entity and item access
        checkModelResponse(REG1 + "/red", ROOT_REGISTER + "reg1/red", "test/expected/red.ttl");
        Model m = getModelResponse(REG1 + "/red?_view=with_metadata");
        checkModelResponse(m, ROOT_REGISTER + "reg1/_red", "test/expected/red_item.ttl");
        checkModelResponse(m, ROOT_REGISTER + "reg1/red", "test/expected/red.ttl");
        checkEntity(m, ROOT_REGISTER + "reg1/_red",  ROOT_REGISTER + "reg1/red");
        assertEquals(404, getResponse(REG1 + "/notred").getStatus());
        m = getModelResponse(REG1 + "/_red");
        checkModelResponse(m, ROOT_REGISTER + "reg1/_red", "test/expected/red_item.ttl");
        checkModelResponse(m, ROOT_REGISTER + "reg1/red", "test/expected/red.ttl");
        checkEntity(m, ROOT_REGISTER + "reg1/_red",  ROOT_REGISTER + "reg1/red");

        m = getModelResponse(REG1 + "/_red", "_view", "version");
        checkModelResponse(m, ROOT_REGISTER + "reg1/red",  "test/expected/red.ttl");
        checkModelResponse(m, ROOT_REGISTER + "reg1/_red",  "test/expected/red_item_version.ttl");
        checkModelResponse(m, ROOT_REGISTER + "reg1/_red:1",  "test/expected/red_item_version.ttl");

        // External (not managed) entities
        assertEquals(201, postFileStatus("test/absolute-black.ttl", REG1));
        m = checkModelResponse(REG1 + "/_black", EXT_BLACK, "test/expected/absolute-black.ttl");
        checkEntity(m, ROOT_REGISTER + "reg1/_black", EXT_BLACK);

        // Entity retrieval
        checkModelResponse(REG1 + "?entity=" + EXT_BLACK, EXT_BLACK, "test/expected/absolute-black.ttl");
        checkModelResponse(BASE_URL + "?entity=" + EXT_BLACK, EXT_BLACK, "test/expected/absolute-black.ttl");
        m = getModelResponse(BASE_URL, "entity", EXT_BLACK, "_view", "with_metadata");
        checkModelResponse(m, EXT_BLACK, "test/expected/absolute-black.ttl");
        checkModelResponse(m, ROOT_REGISTER + "reg1/_black", "test/expected/absolute-black.ttl");

        // Updates, change properties on red
        response = invoke("PUT", "test/red1.ttl", REG1 + "/red");
        assertEquals(204,  response.getStatus());
        String red1Version = response.getLocation().toString();
        assertTrue(red1Version.startsWith(ROOT_REGISTER + "reg1/_red:"));
        String versionSuffix = red1Version.substring( red1Version.length()-2 );
        checkModelResponse(REG1 + "/red", ROOT_REGISTER + "reg1/red", "test/expected/red1.ttl");

        Calendar checkpoint = Calendar.getInstance();

        response = invoke("PATCH", "test/red1b.ttl", REG1 + "/red");
        assertEquals(204,  response.getStatus());
        checkModelResponse(REG1 + "/red", ROOT_REGISTER + "reg1/red", "test/expected/red1b.ttl");

        checkModelResponse(REG1 + "/_red" + versionSuffix, ROOT_REGISTER + "reg1/red", "test/expected/red1.ttl");

        // Register read
        checkModelResponse(REG1, ROOT_REGISTER + "reg1", "test/expected/reg1-empty.ttl");
        assertEquals(204, post(REG1 + "/_red?update&status=stable").getStatus());
        assertEquals(204, post(REG1 + "/_black?update&status=experimental").getStatus());
        checkModelResponse(REG1, ROOT_REGISTER + "reg1", "test/expected/reg1_red_black.ttl");

        // Basic version view
        m = getModelResponse(REG1 + "/_red?_view=version");
        String uri = QueryUtil.selectFirstVar("entity", m, "SELECT ?entity WHERE { ?item version:currentVersion [ reg:definition [ reg:entity ?entity]].}",
                                                    Prefixes.get(), "item", ROOT_REGISTER + "reg1/_red").asResource().getURI();
        assertEquals(ROOT_REGISTER + "reg1/red", uri);

        // Register listing
        assertEquals(201, postFileStatus("test/blue.ttl", REG1));
        checkModelResponse(REG1 + "?non-member-properties", REG1_URI, "test/expected/reg1-empty.ttl");
        checkRegisterList( getModelResponse(REG1 + "?status=stable"), REG1_URI, "red1b");
        checkRegisterList( getModelResponse(REG1 + "?status=experimental"), REG1_URI, "black");
        checkRegisterList( getModelResponse(REG1 + "?status=valid"), REG1_URI, "red1b", "black");
        checkRegisterList( getModelResponse(REG1 + "?status=accepted"), REG1_URI, "red1b", "black");
        checkRegisterList( getModelResponse(REG1 + "?status=notaccepted"), REG1_URI, "blue");
        checkRegisterList( getModelResponse(REG1 + "?status=any"), REG1_URI, "red1b", "black", "blue");

        // Register metadata view
        checkModelResponse(REG1 + "?non-member-properties&_view=with_metadata", REG1_URI, "test/expected/reg1_nmp_metadata.ttl");
        m = getModelResponse(REG1 + "?_view=with_metadata");
        checkModelResponse(m, REG1_URI, "test/expected/reg1_red_black_metadata.ttl");
        checkModelResponse(m, REG1_URI+"/red", "test/expected/reg1_red_black_metadata.ttl");
        checkModelResponse(m, REG1_URI+"/_red", "test/expected/reg1_red_black_metadata.ttl");
        checkModelResponse(m, EXT_BLACK, "test/expected/reg1_red_black_metadata.ttl");
        checkModelResponse(m, REG1_URI+"/_black", "test/expected/reg1_red_black_metadata.ttl");

        // Register paging
        makeRegister(60);
        checkPageResponse(getModelResponse(REGL_URL + "?firstPage&status=notaccepted"), "1", 50);
        checkPageResponse(getModelResponse(REGL_URL + "?_page=1&status=notaccepted"), null, 10);

        // Register version view
        m = getModelResponse(REGL_URL + ":3?status=notaccepted");
        checkModelResponse(m, ROOT_REGISTER + "regL", "test/expected/regL-two-entries.ttl");
        checkModelResponse(m, ROOT_REGISTER + "regL/item0", "test/expected/regL-two-entries.ttl");
        checkModelResponse(m, ROOT_REGISTER + "regL/item1", "test/expected/regL-two-entries.ttl");

        // Register timestamp view - predates changing red1 to red1b and adding blue
        checkRegisterList( getModelResponse(REG1 + "?status=any&_versionAt=" + checkpoint.getTimeInMillis()), REG1_URI, "red1", "black");

        // Checking of legal relative URIs in registration payload
        assertEquals(400, postFileStatus("test/bad-green.ttl", REG1));

        // Bulk registration
        assertEquals("Not a bulk type", 400, postFileStatus("test/blue.ttl", BASE_URL + "?batch-managed"));

        assertEquals(201, postFileStatus("test/bulk-skos-collection.ttl", BASE_URL + "?batch-managed"));
        m = getModelResponse(BASE_URL + "collection?status=any");
        checkRegisterList( m, ROOT_REGISTER + "collection", "item 1", "item 2", "item 3");

        assertEquals(201, postFileStatus("test/bulk-skos-scheme.ttl", BASE_URL + "?batch-managed"));
        m = getModelResponse(BASE_URL + "scheme?status=any");
        TestUtil.testArray(
                m.listSubjectsWithProperty(SKOS.inScheme, m.getResource(ROOT_REGISTER + "scheme")).toList(),
                new Resource[]{
                    m.createResource(ROOT_REGISTER + "scheme/item1"),
                    m.createResource(ROOT_REGISTER + "scheme/item2"),
                    m.createResource(ROOT_REGISTER + "scheme/item3")
                }
                );

        assertEquals(201, postFileStatus("test/bulk-skos-collection.ttl", BASE_URL + "collection?batch-managed&status=stable"));
        m = getModelResponse(BASE_URL + "collection/collection?status=stable");
        checkRegisterList( m, ROOT_REGISTER + "collection/collection", "item 1", "item 2", "item 3");

        assertEquals(201, postFileStatus("test/bulk-skos-collection-of-scheme.ttl", BASE_URL + "?batch-referenced"));
        m = getModelResponse(BASE_URL + "scheme-collection?status=any");
        checkRegisterList( m, ROOT_REGISTER + "scheme-collection", "item 1", "item 2");

        // Updating register metadata
        assertEquals(400, invoke("PATCH", "test/register-update.ttl", BASE_URL + "collection").getStatus());
        assertEquals(204, invoke("PATCH", "test/register-update.ttl", BASE_URL + "collection?non-member-properties").getStatus());
        m = getModelResponse(BASE_URL + "collection?non-member-properties");
        checkModelResponse(m, ROOT_REGISTER + "collection", "test/expected/updated-collection-register.ttl");

        // Deletion
        long timestamp = System.currentTimeMillis();
        assertEquals(204, invoke("DELETE", null, BASE_URL + "collection/collection/item1").getStatus());
        checkRegisterList(
                getModelResponse(BASE_URL + "collection/collection?status=stable"), ROOT_REGISTER + "collection/collection", "item 2", "item 3");

        assertEquals(204, invoke("DELETE", null, BASE_URL + "collection/collection/_item2").getStatus());
        checkRegisterList(
                getModelResponse(BASE_URL + "collection/collection?status=stable"), ROOT_REGISTER + "collection/collection", "item 3");

        // Reconstruct register pre-delete
        checkRegisterList(
                getModelResponse(BASE_URL + "collection/collection?status=stable&_versionAt=" + timestamp), ROOT_REGISTER + "collection/collection", "item 1", "item 2", "item 3");

        Resource collection = ResourceFactory.createResource("http://location.data.gov.uk/collection");
        Resource collectionCollection = ResourceFactory.createResource("http://location.data.gov.uk/collection/collection");
        assertTrue( getModelResponse(BASE_URL + "collection?status=stable").contains(collection, SKOS.member, collectionCollection));
        assertEquals(204, invoke("DELETE", null, BASE_URL + "collection/collection").getStatus());
        assertFalse( getModelResponse(BASE_URL + "collection?status=stable").contains(collection, SKOS.member, collectionCollection));
        checkRegisterList(
                getModelResponse(BASE_URL + "collection/collection?status=stable"), ROOT_REGISTER + "collection/collection");

        assertEquals(400, postFileStatus("test/validation-request1.txt", BASE_URL + "?validate" ));
        assertEquals(204, post(BASE_URL + "collection?update&status=stable&force").getStatus());
        assertEquals(200, postFileStatus("test/validation-request1.txt", BASE_URL + "?validate" ));
        assertEquals(400, postFileStatus("test/validation-request2.txt", BASE_URL + "?validate" ));
        assertEquals(200, post(BASE_URL + "?validate=http://location.data.gov.uk/collection/item1").getStatus());
        assertEquals(400, post(BASE_URL + "?validate=http://location.data.gov.uk/collection/item1&validate=http://location.data.gov.uk/collection/item8").getStatus());

        // List a register - was a bug here
        m = getModelResponse(BASE_URL + "?status=any");
        checkRegisterList(m, RDFS.member, ROOT_REGISTER, "register 1", "A test collection", "Long register", "A nice test collection", "A test concept scheme") ;

        // List versions
        m = getModelResponse(BASE_URL + "reg1/_red?_view=version_list");
        assertTrue( m.listSubjectsWithProperty(DCTerms.isVersionOf, m.getResource(ROOT_REGISTER + "reg1/_red")).toList().size() >= 4);
        assertTrue( m.contains(m.getResource(ROOT_REGISTER + "reg1/_red:3"), DCTerms.replaces, m.getResource(ROOT_REGISTER + "reg1/_red:2")) );

        // Check some status transitions
        assertEquals(403, post(REG1 + "/_blue?update&status=retired").getStatus());
        assertEquals(403, post(REG1 + "/_blue?update&status=superseded").getStatus());
        assertEquals(204, post(REG1 + "/_blue?update&status=experimental").getStatus());
        assertEquals(204, post(REG1 + "/_blue?update&status=stable").getStatus());
        assertEquals(403, post(REG1 + "/_blue?update&status=experimental").getStatus());
        assertEquals(403, post(REG1 + "/_blue?update&status=submitted").getStatus());
        assertEquals(204, post(REG1 + "/_blue?update&status=superseded").getStatus());
        assertEquals(204, post(REG1 + "/_blue?update&status=invalid").getStatus());

//        m.write(System.out, "Turtle");
    }

    private void checkPageResponse(Model m, String nextpage, int length) {
        ResIterator ri = m.listSubjectsWithProperty(RDF.type, Ldbp.Page);
        assertTrue(ri.hasNext());
        Resource page = ri.next();
        assertFalse(ri.hasNext());
        if (nextpage != null) {
            Resource next = page.getPropertyResourceValue(Ldbp.nextPage);
            assertNotNull(next);
            assertTrue(next.getURI().contains("_page=" + nextpage));
        } else {
            assertFalse(page.hasProperty(Ldbp.nextPage));
        }
        Resource reg = page.getPropertyResourceValue(Ldbp.pageOf);
        int count = 0;
        for (StmtIterator si = reg.listProperties(RDFS.member); si.hasNext();) {
            si.next();
            count++;
        }
        assertEquals(length, count);
    }

    private void makeRegister(int length) {
        Model m = ModelFactory.createDefaultModel();
        m.createResource("regL")
                .addProperty(RDF.type, RegistryVocab.Register)
                .addProperty(RDFS.label, "Long register");
        assertEquals(201, postModel(m, BASE_URL).getStatus());
        for (int i = 0; i < length; i++) {
            m = ModelFactory.createDefaultModel();
            m.createResource("item" +i)
                    .addProperty(RDFS.label, "item" + i)
                    .addProperty(RDF.type, SKOS.Concept);
            assertEquals(201, postModel(m, REGL_URL).getStatus());
        }
    }

}
