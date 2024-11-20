/******************************************************************
 * File:        TestSecurity.java
 * Created by:  Dave Reynolds
 * Created on:  4 Apr 2013
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDFS;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Test;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.vocab.RegistryVocab;

public class TestSecurity extends TomcatTestBase {
    @Override
    public String getWebappRoot() {
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

        String user = c.target(BASE_URL + "system/security/username").request().get(String.class);
        assertEquals("Alice", user);

        Response logoutResponse = c.target(BASE_URL + "system/security/logout").request().post(null);
        assertEquals(BASE_URL, logoutResponse.getLocation().toString());
        Response userResponse = c.target(BASE_URL + "system/security/username").request().get();
        assertTrue(userResponse.getStatus() >= 400);
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

    protected static Client makeClient() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        Client client = ClientBuilder.newClient(clientConfig);
        // TODO is the cookie policy set OK?
        return client;
    }

    protected static Model getModelResponse(Client c, String uri) {
        WebTarget r = c.target( uri );
        InputStream response = r.request("text/turtle").get(InputStream.class);
        Model result = ModelFactory.createDefaultModel();
        result.read(response, uri, "Turtle");
        return result;
    }


    protected static Client clientFor(String userid, String password) {
        Client c = makeClient();

        Form loginform = new Form();
        loginform.param("userid", userid);
        loginform.param("password", password);
        WebTarget r = c.target(BASE_URL + "system/security/apilogin");
        Response response = r.request().post( Entity.entity(loginform, MediaType.APPLICATION_FORM_URLENCODED_TYPE) );
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
        WebTarget r;
        Response response;

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
            response = c.target(url).request("text/turtle").get(Response.class);
            return this;
        }

        public TestBuilder post(String url) {
            File file = (sourceFile == null) ? null : new File(sourceFile);
            response = c.target(url).request().post( Entity.entity(file, mime) );
            return this;
        }

        public TestBuilder invoke(String method, String url) {
            File src = (sourceFile == null) ? null : new File(sourceFile);  
            WebTarget r = c.target(url);
            switch( method ) {
            case "GET" :
                response = r.request().get();
                return this;
            case "POST" :
                response = r.request().post( Entity.entity(src, mime) );
                return this;
            case "PUT" :
                response = r.request().put( Entity.entity(src, mime) );
                return this;
            case "DELETE":
                response = r.request().delete();
                return this;
            default:
                response = r.request().method(method, Entity.entity(src, mime) );
                return this;
            }
        }

        public Response getResponse() {
            assertNotNull(response);
            return response;
        }

        public int getStatus() {
            return getResponse().getStatus();
        }

        public Model getModel() {
            assertNotNull(response);
            InputStream stream = response.readEntity(InputStream.class);
            Model result = ModelFactory.createDefaultModel();
            result.read(stream, uri, "Turtle");
            return result;
        }

        public TestBuilder checkRejected() {
            if (getStatus() != 401) {
                System.out.println("Response was: " + response.readEntity(String.class) );
            }
            assertEquals(401, getStatus());
            return this;
        }

        public TestBuilder checkAccepted() {
            if (getStatus() >= 400) {
                System.out.println("Failure response: " + response.readEntity(String.class) );
            }
            assertTrue("Status was: " + getStatus(), getStatus() < 400);
            return this;
        }

    }
}
