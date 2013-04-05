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

import java.io.InputStream;

import javax.ws.rs.core.MediaType;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
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
        // TODO
    }

    protected void shoudReject(ClientResponse response) {
        assertTrue(response.getStatus() == 401);
    }

    protected void shoudAccept(ClientResponse response) {
        assertTrue(response.getStatus() < 400);
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
            TestBuilder t = new TestBuilder(c);
            t.sourceFile = filename;
            return t;
        }

        public TestBuilder mime(String mime) {
            assertNull(response);
            TestBuilder t = new TestBuilder(c);
            t.mime = mime;
            return t;
        }

        // get
        // post
        // invoke(method)
        // getResponse
        // getStatus
        // getModel
        // checkRejected
        // checkAccepted

    }
}
