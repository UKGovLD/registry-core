/******************************************************************
 * File:        TestSecurity.java
 * Created by:  Dave Reynolds
 * Created on:  4 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;

import org.junit.Test;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.ApacheHttpClientConfig;

public class TestSecurity extends TomcatTestBase {
    @Override
    String getWebappRoot() {
        return "src/test/webapp-security";
    }

    @Test
    public void runtests() {
        checkLoginLogout();
        checkBootstrappedRegisters();
        checkRegistration();
        checkUpdate();
        checkStatusUpdate();
        checkOpen();
    }

    /**
     * Test that a sample user can login and be recognized.
     */
    protected void checkLoginLogout() {
        Client c = clientFor("http://example.com/alice", "testing");

        String user = c.resource(BASE_URL + "system/security/username").get(String.class);
        assertEquals("Alice", user);

        c.resource(BASE_URL + "system/security/logout").post(ClientResponse.class);
        ClientResponse response = c.resource(BASE_URL + "system/security/username").get(ClientResponse.class);
        assertTrue(response.getStatus() >= 400);
    }

    /**
     * Check that the bootstrap registers have been loaded,
     * otherwise a lot of later tests won't make sense.
     */
    protected void checkBootstrappedRegisters() {
        Client c = makeClient();
        Model m = getModelResponse(c, BASE_URL + "open/colours/blue");
        Resource blue = m.getResource(ROOT_REGISTER + "open/colours/blue");
        assertTrue(blue.hasProperty(RDFS.label, "blue"));
    }

    /**
     * Check the ability to register in various places
     */
    protected void checkRegistration() {
        testFor("bob").source("test/absolute-black.ttl").post(BASE_URL + "secure/reg2/colours").checkRejected();
        testFor("bob").source("test/reg1.ttl").post(BASE_URL + "secure/reg2").checkRejected();
        testFor("bob").source("test/absolute-black.ttl").post(BASE_URL + "secure/reg1/colours").checkAccepted();
        Model m = testFor("bob").get(BASE_URL + "secure/reg1/colours/_black").checkAccepted().getModel();
        Resource item = m.getResource(ROOT_REGISTER + "secure/reg1/colours/_black");
        Resource submitter = item.getPropertyResourceValue(RegistryVocab.submitter);
        assertNotNull(submitter);
        assertEquals("Bob", RDFUtil.getStringValue(submitter, FOAF.name));

        testFor("bob").source("test/reg1.ttl").post(BASE_URL + "secure").checkRejected();
        testFor("bob").source("test/reg1.ttl").post(BASE_URL + "secure/reg1").checkAccepted();

        testFor("alice").source("test/reg1.ttl").post(BASE_URL).checkRejected();
        testFor("alice").source("test/reg1.ttl").post(BASE_URL + "secure/reg2").checkAccepted();


        testFor("admin").source("test/reg1.ttl").post(BASE_URL).checkAccepted();
    }

    /**
     * Check the ability to update items and entities
     */
    protected void checkUpdate() {
        testFor("colin").source("test/red1.ttl").invoke("PUT", BASE_URL + "secure/reg2/colours/red").checkRejected();
        testFor("alice").source("test/red1.ttl").invoke("PUT", BASE_URL + "secure/reg2/colours/red").checkAccepted();

        testFor("david").source("test/red1.ttl").invoke("PUT", BASE_URL + "secure/reg1/colours/red").checkRejected();
        testFor("colin").source("test/red1.ttl").invoke("PUT", BASE_URL + "secure/reg1/colours/red").checkAccepted();

        testFor("colin").source("test/green-item-update.ttl").invoke("PATCH", BASE_URL + "secure/reg1/colours/_green").checkRejected();
        testFor("david").source("test/green-item-update.ttl").invoke("PATCH", BASE_URL + "secure/reg1/colours/_green").checkAccepted();
        testFor("alice").source("test/green-item-update.ttl").invoke("PATCH", BASE_URL + "secure/reg2/colours/_green").checkAccepted();

        testFor("colin").source("test/reg1-patch.ttl").invoke("PATCH", BASE_URL + "secure/reg1?non-member-properties").checkRejected();
        testFor("bob").source("test/reg1-patch.ttl").invoke("PATCH", BASE_URL + "secure/reg1?non-member-properties").checkAccepted();
    }

    /**
     * Check direct and indirect ability to update status information
     */
    protected void checkStatusUpdate() {
        // Explicit status change, lifecycle compliant
        testFor("bob").post(BASE_URL + "secure/reg2/colours/_red?update&status=retired").checkRejected();
        testFor("colin").post(BASE_URL + "secure/reg1/colours/_red?update&status=retired").checkRejected();
        testFor("bob").post(BASE_URL + "secure/reg1/colours/_red?update&status=retired").checkAccepted();

        // Explicit status, breaks lifecycle
        testFor("bob").post(BASE_URL + "secure/reg1/colours/_red?update&force&status=submitted").checkRejected();
        testFor("admin").post(BASE_URL + "secure/reg1/colours/_red?update&force&status=submitted").checkAccepted();

        // Implicit status, lifecycle compliant
        testFor("david").source("test/green-item-update-withstatus.ttl").invoke("PATCH", BASE_URL + "secure/reg1/colours/_green").checkRejected();
        testFor("bob").source("test/green-item-update-withstatus.ttl").invoke("PATCH", BASE_URL + "secure/reg1/colours/_green").checkAccepted();

        // Implicit status, lifecycle violated
        testFor("alice").source("test/green-item-update-withbadstatus.ttl").invoke("PATCH", BASE_URL + "secure/reg2/colours/_green").checkRejected();
        testFor("admin").source("test/green-item-update-withbadstatus.ttl").invoke("PATCH", BASE_URL + "secure/reg2/colours/_green").checkAccepted();

    }

    /**
     * Check the ability to set some areas as open to anyone
     */
    protected void checkOpen() {
        // register
        testFor("david").source("test/absolute-black.ttl").post(BASE_URL + "open/colours").checkAccepted();
        testFor("david").source("test/reg1.ttl").post(BASE_URL + "open").checkAccepted();
        // Update
        testFor("david").source("test/red1.ttl").invoke("PUT", BASE_URL + "open/colours/red").checkAccepted();
        // Status update
        testFor("david").post(BASE_URL + "open/colours/_red?update&status=retired").checkAccepted();
        // But not force
        testFor("david").source("test/green-item-update-withbadstatus.ttl").invoke("PATCH", BASE_URL + "open/colours/_green").checkRejected();
    }

    protected static ApacheHttpClient makeClient() {
        ApacheHttpClient c = ApacheHttpClient.create();
        c.getProperties().put(ApacheHttpClientConfig.PROPERTY_HANDLE_COOKIES, true);
        return c;
    }

    protected static Model getModelResponse(Client c, String uri) {
        WebResource r = c.resource( uri );
        InputStream response = r.accept("text/turtle").get(InputStream.class);
        Model result = ModelFactory.createDefaultModel();
        result.read(response, uri, "Turtle");
        return result;
    }


    protected static ApacheHttpClient clientFor(String userid, String password) {
        ApacheHttpClient c = makeClient();

        Form loginform = new Form();
        loginform.add("userid", userid);
        loginform.add("password", password);
        WebResource r = c.resource(BASE_URL + "system/security/apilogin");
        ClientResponse response = r.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, loginform);
        assertTrue("Login failed for " + userid, response.getStatus() < 400);
        return c;
    }

    public static TestBuilder testFor(String user) {
        return new TestBuilder( clientFor("http://example.com/" + user, "testing"));
    }

    static class TestBuilder {
        Client c;
        String sourceFile;
        String mime = "text/turtle";
        String uri;
        WebResource r;
        ClientResponse response;

        public TestBuilder(Client c) {
            this.c = c;
        }

        public TestBuilder source(String filename) {
            assertNull(response);
            sourceFile = filename;
            return this;
        }

        public TestBuilder mime(String mime) {
            assertNull(response);
            this.mime = mime;
            return this;
        }

        public TestBuilder get(String url) {
            response = c.resource(url).accept("text/turtle").get(ClientResponse.class);
            return this;
        }

        public TestBuilder post(String url) {
            Builder b = c.resource(url).type(mime);
            if (sourceFile != null) {
                File file = new File(sourceFile);
                response = b.post(ClientResponse.class, file);
            } else {
                response = b.post(ClientResponse.class);
            }
            return this;
        }

        public TestBuilder invoke(String method, String url) {
            Builder b = c.resource(url).type(mime);
            if (sourceFile != null) {
                File file = new File(sourceFile);
                response = b.header("X-HTTP-Method-Override", method).post(ClientResponse.class, file);
            } else {
                response = b.header("X-HTTP-Method-Override", method).post(ClientResponse.class);
            }
            return this;
        }

        public ClientResponse getResponse() {
            assertNotNull(response);
            return response;
        }

        public int getStatus() {
            return getResponse().getStatus();
        }

        public Model getModel() {
            assertNotNull(response);
            InputStream stream = response.getEntity(InputStream.class);
            Model result = ModelFactory.createDefaultModel();
            result.read(stream, uri, "Turtle");
            return result;
        }

        public TestBuilder checkRejected() {
            if (getStatus() != 401) {
                System.out.println("Response was: " + response.getEntity(String.class) );
            }
            assertEquals(401, getStatus());
            return this;
        }

        public TestBuilder checkAccepted() {
            if (getStatus() >= 400) {
                System.out.println("Failure response: " + response.getEntity(String.class) );
            }
            assertTrue("Status was: " + getStatus(), getStatus() < 400);
            return this;
        }

    }
}
