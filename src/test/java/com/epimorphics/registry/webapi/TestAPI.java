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

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.util.JSONLDSupport;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.Ldbp;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.util.TestUtil;
import com.epimorphics.vocabs.API;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
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
    public void testBasics() throws IOException {
        // Create reg1
        doRegisterRegistrationTests();

        // Puts red in reg1/red
        doItemRegistrationTests();

        // Adds external resource reg1/black
        doExternalRegistrationTests();

        // Updates red to be called red1
        String versionSuffix = doUpdateTest();

        // Patches reg to be called red1b
        Calendar checkpoint = Calendar.getInstance();
        doPatchTest();

        // Check we can still get back the old version of reg from before the last patch
        checkModelResponse(REG1 + "/_red" + versionSuffix, ROOT_REGISTER + "reg1/red", "test/expected/red1.ttl");

        // Adds reg1/blue and tests views with different status setting and metadata
        doRegisterListingTest();

        // Update tests on register itself, ISSUE-27, changes register name so so after listing test
        doRegUpdateTest();

        // Check for case of invalid patch requests
        doInvalidUpdateTest();

        // Create a large register regL and checks paged views and retrieving early version of register
        doPagingTest();
        doRegisterVersionRetrievalTest();

        // Register timestamp view - predates changing red1 to red1b and adding blue
        checkRegisterList( getModelResponse(REG1 + "?status=any&_versionAt=" + checkpoint.getTimeInMillis()), REG1_URI, "red1", "black");

        // List versions
        doListVersionsTest();

        // Check some status transitions
        doStatusTransitionsTest();

        // Checking of legal relative URIs in registration payload
        assertEquals(400, postFileStatus("test/bad-green.ttl", REG1));

        // Bulk registration - creates a collection and a scheme register from skos collection and concept scheme
        doBulkRegistrationTest();

        // Check patching of register metadata - tests on /collection created in prior step
        doRegisterMetadataPatchTest();

        // Test deletions using the /collection register
        doDeletionTest();

        // List a register - was a bug here
        Model m = getModelResponse(BASE_URL + "?status=any");
        checkRegisterList(m, RDFS.member, ROOT_REGISTER, "system register", "register 1", "A test collection", "Long register", "A nice test collection", "A test concept scheme") ;

        // Delegation tests
        doForwardingTest();
        doDelegationTest();

        // Check prefix system register
        doPrefixTests();

        // Check we can register and patch using jsonld syntax payloads
        doJsonldTests();

        // Check registration of entity and item information together
        doRegisterWithMetadataTest();

        // Check patching metadata annotations
        doMetadataPatchTest();

//        System.out.println("Store dump");
//        ServiceConfig.get().getServiceAs("basestore", Store.class).asDataset().getDefaultModel().write(System.out, "Turtle");
    }

    private void doInvalidUpdateTest() {
        assertEquals(400, invoke("PATCH", "test/blue-patch.ttl", BASE_URL).getStatus());
        assertEquals(400, invoke("PATCH", "test/blue-patch.ttl", REG1).getStatus());
    }

    private void doRegisterRegistrationTests() {
        // Leaves register reg1 set up
        ClientResponse response = postFile("test/reg1.ttl", BASE_URL, "text/turtle");
        assertEquals("Register a register", 201, response.getStatus());
        assertEquals(REG1_ITEM, response.getLocation().toString());
        assertEquals("Register in non-existant location", 404, postFileStatus("test/reg1.ttl", BASE_URL+"foo"));
        assertEquals("Register the same again", 403, postFileStatus("test/reg1.ttl", BASE_URL));
    }

    // Assumes reg1 has been created
    // Adds an item (reg1/red) and checks it all looks OK
    // Leaves reg1/red in plce
    private void doItemRegistrationTests() {
        assertEquals(201, postFileStatus("test/red.ttl", REG1));

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

    }

    // Assumes reg1 has been created
    // Adds an external entity and check it can be retrieved
    private void doExternalRegistrationTests() {
        // External (not managed) entities
        assertEquals(201, postFileStatus("test/absolute-black.ttl", REG1));
        Model m = checkModelResponse(REG1 + "/_black", EXT_BLACK, "test/expected/absolute-black.ttl");
        checkEntity(m, ROOT_REGISTER + "reg1/_black", EXT_BLACK);

        // Entity retrieval
        checkModelResponse(REG1 + "?entity=" + EXT_BLACK, EXT_BLACK, "test/expected/absolute-black.ttl");
        checkModelResponse(BASE_URL + "?entity=" + EXT_BLACK, EXT_BLACK, "test/expected/absolute-black.ttl");
        m = getModelResponse(BASE_URL, "entity", EXT_BLACK, "_view", "with_metadata");
        checkModelResponse(m, EXT_BLACK, "test/expected/absolute-black.ttl");
        checkModelResponse(m, ROOT_REGISTER + "reg1/_black", "test/expected/absolute-black.ttl");
    }

    // Assumes reg1 and reg1/red exists
    // Updates name of red to red1
    // Returns the version number of the new version
    private String doUpdateTest() {
        // Updates, change properties on red
        ClientResponse response = invoke("PUT", "test/red1.ttl", REG1 + "/red");
        assertEquals(204,  response.getStatus());
        String red1Version = response.getLocation().toString();
        assertTrue(red1Version.startsWith(ROOT_REGISTER + "reg1/_red:"));
        String versionSuffix = red1Version.substring( red1Version.length()-2 );
        checkModelResponse(REG1 + "/red", ROOT_REGISTER + "reg1/red", "test/expected/red1.ttl");
        return versionSuffix;
    }

    private void doRegUpdateTest() {
        String reg1meta = REG1 + "?non-member-properties";
        Model m = getModelResponse(reg1meta);
        Resource reg1 = m.getResource(REG1_URI);
        long version = RDFUtil.getLongValue(reg1, OWL.versionInfo);

        ClientResponse response = invoke("PUT", "test/reg1-put.ttl", reg1meta);
        assertEquals(204, response.getStatus());

        m = getModelResponse(reg1meta);
        reg1 = m.getResource(REG1_URI);
        long newversion = RDFUtil.getLongValue(reg1, OWL.versionInfo);
        assertEquals(version + 1, newversion);
        assertEquals("Example register 1 - put update", RDFUtil.getStringValue(reg1, DCTerms.description));

        response = invoke("PATCH", "test/reg1-patch.ttl", reg1meta);
        assertEquals(204, response.getStatus());
        m = getModelResponse(reg1meta);
        reg1 = m.getResource(REG1_URI);
        newversion = RDFUtil.getLongValue(reg1, OWL.versionInfo);
        assertEquals(version + 2, newversion);
        assertEquals("Example register 1 - patch update", RDFUtil.getStringValue(reg1, DCTerms.description));

        // Verify access to earlier version of the RegisterItem containing the register - ISSUE-26
        Resource current = getModelResponse(BASE_URL + "_reg1").getResource(REG1_URI);
        assertEquals(version + 2, RDFUtil.getLongValue(current, OWL.versionInfo).longValue());
        assertEquals("Example register 1 - patch update", RDFUtil.getStringValue(current, DCTerms.description));
        
        Resource orig =  getModelResponse(BASE_URL + "_reg1:1").getResource(REG1_URI);
        assertEquals(1, RDFUtil.getLongValue(orig, OWL.versionInfo).longValue());
        assertEquals("Example register 1", RDFUtil.getStringValue(orig, DCTerms.description));
        
        // Safe against versioninfo being in payload
        response = invoke("PUT", "test/reg1-put2.ttl", reg1meta);
        m = getModelResponse(reg1meta);
        reg1 = m.getResource(REG1_URI);
        newversion = RDFUtil.getLongValue(reg1, OWL.versionInfo);
        assertEquals(version + 3, newversion);
        assertEquals("Example register 1 - put update2", RDFUtil.getStringValue(reg1, DCTerms.description));

        // Try to change RDF type
//        assertEquals(400, invoke("PUT", "test/reg1-bad-put1.ttl", reg1meta).getStatus());

        // Multiple roots in change file
        assertEquals(400, invoke("PUT", "test/reg1-bad-put2.ttl", reg1meta).getStatus());

    }

    // Assumes reg1/red exists
    // Patch its name to red1b
    private void doPatchTest() {
        ClientResponse response = invoke("PATCH", "test/red1b.ttl", REG1 + "/red");
        assertEquals(204,  response.getStatus());
        checkModelResponse(REG1 + "/red", ROOT_REGISTER + "reg1/red", "test/expected/red1b.ttl");
    }

    // Assumes reg1 exists and already has red (now called red1b) and black in it
    // Tests listing different status filters and basic metadata access
    // Leaves register with blue as well, red as "stable" and black as "experimental"
    private void doRegisterListingTest() {
        // Register read
        checkModelResponse(REG1, ROOT_REGISTER + "reg1", "test/expected/reg1-empty.ttl");
        assertEquals(204, post(REG1 + "/_red?update&status=stable").getStatus());
        assertEquals(204, post(REG1 + "/_black?update&status=experimental").getStatus());
        checkModelResponse(REG1, ROOT_REGISTER + "reg1", "test/expected/reg1_red_black.ttl");

        // Register listing
        assertEquals(201, postFileStatus("test/blue.ttl", REG1));
        Model m = getModelResponse(REG1 + "?non-member-properties");

        checkModelResponse(m, REG1_URI, "test/expected/reg1-empty.ttl");
        checkRegisterList( getModelResponse(REG1 + "?status=stable"), REG1_URI, "red1b");
        checkRegisterList( getModelResponse(REG1 + "?status=experimental"), REG1_URI, "black");
        checkRegisterList( getModelResponse(REG1 + "?status=valid"), REG1_URI, "red1b", "black");
        checkRegisterList( getModelResponse(REG1 + "?status=accepted"), REG1_URI, "red1b", "black");
        checkRegisterList( getModelResponse(REG1 + "?status=notaccepted"), REG1_URI, "blue");
        checkRegisterList( getModelResponse(REG1 + "?status=any"), REG1_URI, "red1b", "black", "blue");

        // Register metadata view
        m = getModelResponse(REG1 + "?non-member-properties&_view=with_metadata");
        checkModelResponse(m, REG1_URI, "test/expected/reg1_nmp_metadata.ttl");
        checkModelResponse(m, REG1_ITEM, "test/expected/reg1_nmp_metadata.ttl");
        m = getModelResponse(REG1 + "?_view=with_metadata");
        checkModelResponse(m, REG1_URI, "test/expected/reg1_red_black_metadata.ttl");
        checkModelResponse(m, REG1_URI+"/red", "test/expected/reg1_red_black_metadata.ttl");
        checkModelResponse(m, REG1_URI+"/_red", "test/expected/reg1_red_black_metadata.ttl");
        checkModelResponse(m, EXT_BLACK, "test/expected/reg1_red_black_metadata.ttl");
        checkModelResponse(m, REG1_URI+"/_black", "test/expected/reg1_red_black_metadata.ttl");
        checkModelResponse(m, REG1_ITEM, "test/expected/reg1_nmp_metadata.ttl");
    }

    private void doPagingTest() {
        // Register paging
        makeRegister(60);
        checkPageResponse(getModelResponse(REGL_URL + "?firstPage&status=notaccepted"), "1", 50);
        checkPageResponse(getModelResponse(REGL_URL + "?_page=1&status=notaccepted"), null, 10);
    }

    private void doRegisterVersionRetrievalTest() {
        // Register version view
        Model m = getModelResponse(REGL_URL + ":3?status=notaccepted");
        checkModelResponse(m, ROOT_REGISTER + "regL", "test/expected/regL-two-entries.ttl");
        checkModelResponse(m, ROOT_REGISTER + "regL/item0", "test/expected/regL-two-entries.ttl");
        checkModelResponse(m, ROOT_REGISTER + "regL/item1", "test/expected/regL-two-entries.ttl");
    }

    // Leaves new registers /collection and /scheme
    private void doBulkRegistrationTest() {
        assertEquals("Not a bulk type", 400, postFileStatus("test/blue.ttl", BASE_URL + "?batch-managed"));

        assertEquals(201, postFileStatus("test/bulk-skos-collection.ttl", BASE_URL + "?batch-managed"));
        Model m = getModelResponse(BASE_URL + "collection?status=any");
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
    }

    // Assumes /collection exists from bulk registration test
    private void doRegisterMetadataPatchTest() {
        // Updating register metadata
        assertEquals(400, invoke("PATCH", "test/register-update.ttl", BASE_URL + "collection").getStatus());
        assertEquals(204, invoke("PATCH", "test/register-update.ttl", BASE_URL + "collection?non-member-properties").getStatus());
        Model m = getModelResponse(BASE_URL + "collection?non-member-properties");
        checkModelResponse(m, ROOT_REGISTER + "collection", "test/expected/updated-collection-register.ttl");
    }

    // Assumes /colleciton exists from bulk registration tests
    // Test deletion of items from register
    private void doDeletionTest() {
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
    }

    // Assumes reg1/red exists and has go through update (to red1) and patch (to red1b) and status change
    private void doListVersionsTest() {
        Model m = getModelResponse(BASE_URL + "reg1/_red?_view=version_list");
        assertTrue( m.listSubjectsWithProperty(DCTerms.isVersionOf, m.getResource(ROOT_REGISTER + "reg1/_red")).toList().size() >= 4);
        assertTrue( m.contains(m.getResource(ROOT_REGISTER + "reg1/_red:3"), DCTerms.replaces, m.getResource(ROOT_REGISTER + "reg1/_red:2")) );
        assertTrue( m.contains(m.getResource(ROOT_REGISTER + "reg1/_red"), Version.currentVersion, m.getResource(ROOT_REGISTER + "reg1/_red:4")) );
    }

    // Asumes reg1/blue exists, leaves it in "invalid" status
    private void doStatusTransitionsTest() {
        assertEquals(403, post(REG1 + "/_blue?update&status=retired").getStatus());
        assertEquals(403, post(REG1 + "/_blue?update&status=superseded").getStatus());
        assertEquals(204, post(REG1 + "/_blue?update&status=experimental").getStatus());
        assertEquals(204, post(REG1 + "/_blue?update&status=stable").getStatus());
        assertEquals(403, post(REG1 + "/_blue?update&status=experimental").getStatus());
        assertEquals(403, post(REG1 + "/_blue?update&status=submitted").getStatus());
        assertEquals(204, post(REG1 + "/_blue?update&status=superseded").getStatus());
        assertEquals(204, post(REG1 + "/_blue?update&status=invalid").getStatus());
    }

    // Set up a namespace forward to EA data and checks can access it
    // Relies on the EA service being up :)
    private void doForwardingTest() {
        assertEquals(201, postFileStatus("test/bw-forward.ttl", REG1));
        assertEquals(404, getResponse(REG1 + "/eabw/ukc2102-03600").getStatus());
        assertEquals(204, post(REG1 + "/_eabw?update&status=stable").getStatus());
        assertEquals(200, getResponse(REG1 + "/eabw/ukc2102-03600").getStatus());
        Model m = getModelResponse(REG1 + "/eabw/ukc2102-03600");
        Resource bw = m.getResource("http://environment.data.gov.uk/id/bathing-water/ukc2102-03600");
        assertEquals("Spittal", RDFUtil.getStringValue(bw, SKOS.prefLabel));
    }

    // Set up a delegated register for the EA bathingwaters URI set and checks register listing
    // Relies on the EA service being up :)
    private void doDelegationTest() {
        // Check read on delegated register
        assertEquals(201, postFileStatus("test/bw-delegated.ttl", REG1));
        assertEquals(204, post(REG1 + "/_bathingWaters?update&status=stable").getStatus());
        Model m = getModelResponse(REG1 + "/bathingWaters?firstPage");
        Resource register = m.getResource(REG1_URI + "/bathingWaters");
        List<Resource> members = RDFUtil.allResourceValues(register, RDFS.member);
        assertTrue(members.size() > 20);
        for (Resource member : members) {
            assertTrue(member.hasProperty(RDFS.label));
            assertTrue(member.hasProperty(RDF.type));
        }
        m = getModelResponse(REG1 + "?entity=http://environment.data.gov.uk/id/bathing-water/ukc2102-03600");
        Resource bw = m.getResource("http://environment.data.gov.uk/id/bathing-water/ukc2102-03600");
        assertEquals("Spittal", RDFUtil.getStringValue(bw, SKOS.prefLabel));
    }

    private void doPrefixTests() {
        Map<String, String> pm = Prefixes.get().getNsPrefixMap();
        assertTrue(pm.containsKey("rdf"));
        assertEquals(RDF.getURI(), pm.get("rdf"));
        assertTrue(pm.containsKey("reg"));
        assertEquals(RegistryVocab.getURI(), pm.get("reg"));

        assertEquals(200, getResponse(BASE_URL + "system").getStatus());
        assertEquals(200, getResponse(BASE_URL + "system/prefixes").getStatus());
        assertEquals(200, getResponse(BASE_URL + "system/prefixes").getStatus());

        // Prefix update
        assertEquals(201, postFileStatus("test/prefix-test-xyz.ttl", BASE_URL + "system/prefixes"));
        pm = Prefixes.get().getNsPrefixMap();
        assertTrue(pm.containsKey("xyz"));
        assertEquals("http://example.com/xyz", pm.get("xyz"));
    }

    // Assumes /reg1 exists all earlier tests run (so has at least blue in it)
    // Leaves it with /reg1/purple and with reg1 dct:description changed
    private void doJsonldTests() throws IOException {
        // Assumes reg1 set up
        assertEquals(201, postFileStatus("test/purple-testcase.jsonld", REG1, JSONLDSupport.MIME_JSONLD));
        Model m = getModelResponse(BASE_URL + "reg1/purple");
        Resource r = m.getResource(ROOT_REGISTER + "reg1/purple");
        assertEquals("purple", RDFUtil.getStringValue(r, RDFS.label));
        assertEquals("I am purple but described using JSON-LD, good luck with that", RDFUtil.getStringValue(r, DCTerms.description));

        ClientResponse response = getResponse(BASE_URL + "reg1/blue", JSONLDSupport.MIME_JSONLD);
        assertEquals(200, response.getStatus());
        InputStream is = response.getEntityInputStream();
        m = JSONLDSupport.readModel(RequestProcessor.DUMMY_BASE_URI, is);
        is.close();
        r = m.getResource(ROOT_REGISTER + "reg1/blue");
        assertEquals("blue", RDFUtil.getStringValue(r, RDFS.label));

        // JSON-LD patch
        assertEquals(204, invoke("PATCH", "test/reg1-patch.jsonld", REG1 + "?non-member-properties", "application/ld+json").getStatus());
        m = getModelResponse(REG1 + "?non-member-properties");
        assertEquals("Updated register 1", RDFUtil.getStringValue(m.getResource(REG1_URI), DCTerms.description));

    }

    // Register two entries for "eight", both with explicit RegsiterItems as well as the associated entity
    // First version has no URI and gives notation, second uses relative URI to infer notation
    // ISSUE-38
    private void doRegisterWithMetadataTest() {

        // Testing update of item + entity - ISSUE-38
        ClientResponse response = postFile("test/jmt/number-eight-post-notation.ttl", REG1);
        assertEquals(201, response.getStatus());
        String itemURI = ROOT_REGISTER + "reg1/_eight";
        assertEquals(itemURI, response.getLocation().toASCIIString());
        checkRegisteredMetadata( getModelResponse(REG1+"/eight?_view=with_metadata"), "eight" );

        response = postFile("test/jmt/number-eightb-post-relative.ttl", REG1);
        assertEquals(201, response.getStatus());
        itemURI = ROOT_REGISTER + "reg1/_eightb";
        assertEquals(itemURI, response.getLocation().toASCIIString());
        checkRegisteredMetadata( getModelResponse(REG1+"/_eightb"), "eightb" );
    }

    private void checkRegisteredMetadata(Model m, String notation) {
        String itemURI = ROOT_REGISTER + "reg1/_" + notation;
        Resource item = m.getResource(itemURI);
        assertEquals(notation, RDFUtil.getStringValue(item, RegistryVocab.notation));
        assertEquals("http://ukgovld-registry.dnsalias.net/def/numbers/eight",
                item.getPropertyResourceValue(RegistryVocab.definition)
                    .getPropertyResourceValue(RegistryVocab.entity)
                    .getURI());
        assertEquals(RegistryVocab.statusStable, item.getPropertyResourceValue(RegistryVocab.status));

        Property attributedTo = m.getProperty("http://www.w3.org/ns/prov#wasAttributedTo");
        assertEquals("http://jeremytandy.me.uk/self#id", item.getPropertyResourceValue(attributedTo).getURI());
    }

    // Test patching a RegisterItem with external metadata
    // ISSUE-39
    private void doMetadataPatchTest() {
        assertEquals(201, postFileStatus("test/jmt/number-nine-post.ttl", REG1));

        Property attributedTo = ResourceFactory.createProperty("http://www.w3.org/ns/prov#wasAttributedTo");
        Resource item = getModelResponse(REG1 + "/nine?_view=with_metadata").getResource(REG1_URI + "/_nine");
        assertFalse(item.hasProperty(attributedTo));

        assertEquals(204, invoke("PATCH", "test/jmt/number-nine-patch.ttl", REG1 + "/_nine").getStatus() );

        item = getModelResponse(REG1 + "/nine?_view=with_metadata").getResource(REG1_URI + "/_nine");
        assertEquals("http://jeremytandy.me.uk/self#id", item.getPropertyResourceValue(attributedTo).getURI());

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
        List<RDFNode> items = page.getPropertyResourceValue(API.items).as(RDFList.class).asJavaList();
        assertEquals(length, items.size());
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
