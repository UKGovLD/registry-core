/******************************************************************
 * File:        TomcatTestContainerFactory.java
 * Created by:  Dave Reynolds
 * Created on:  30 Nov 2012
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

package com.epimorphics.registry.webapi;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.apache.catalina.startup.Tomcat;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.util.NameUtils;
import com.epimorphics.util.TestUtil;
import com.epimorphics.vocabs.SKOS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TomcatTestBase {

    static final String BASE_URL = "http://localhost:8070/";
    static final String ROOT_REGISTER =  "http://location.data.gov.uk/";

    Tomcat tomcat ;
    Client c;

    public abstract String getWebappRoot() ;

    public String getWebappContext() {
        return "";
    }
    
    public String getBaseURL() {
        String base = BASE_URL;
        if ( ! getWebappContext().equals("/") ) {
            base += getWebappContext();
        }
        base = NameUtils.ensureLastSlash(base);
        return base;
    }

    @BeforeEach
    public void containerStart() throws Exception {
        String root = getWebappRoot();
        tomcat = new Tomcat();
        tomcat.setPort(8070);
        tomcat.getConnector();

        tomcat.setBaseDir(".");

        String contextPath = getWebappContext();;

        File rootF = new File(root);
        if (!rootF.exists()) {
            rootF = new File(".");
        }
        if (!rootF.exists()) {
            System.err.println("Can't find root app: " + root);
            System.exit(1);
        }

        tomcat.addWebapp(contextPath,  rootF.getAbsolutePath());
        tomcat.start();

        // Allow arbitrary HTTP methods so we can use PATCH
        c = ClientBuilder.newClient();
        c.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);

        checkLive(200);
    }

    @AfterEach
    public void containerStop() throws Exception {
        tomcat.stop();
        tomcat.destroy();
        try {
            checkLive(503);
        } catch (Throwable e) {
            // Can get net connection exceptions talking to dead tomcat, that's OK
        }
    }

    protected int postFileStatus(String file, String uri) {
        return postFileStatus(file, uri, "text/turtle");
    }

    protected int postFileStatus(String file, String uri, String mime) {
        return postFile(file, uri, mime).getStatus();
    }

    protected Response postFile(String file, String uri) {
        return postFile(file, uri, "text/turtle");
    }

    protected Response postFile(String file, String uri, String mime) {
        return postFile(file, uri, mime, 10);
    }

    protected Response postFile(String file, String uri, String mime, Integer retries) {
        WebTarget r = c.target(uri);
        File src = new File(file);
        Response response = r.request().post( Entity.entity(src, mime) );
        if (response.getStatus() == 503) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {

            }
            if (retries > 0) {
                return postFile(file, uri, mime, retries - 1);
            } else {
                return response;
            }
        } else {
            return response;
        }
    }

    protected Response postModel(Model m, String uri) {
        WebTarget r = c.target(uri);
        StringWriter sw = new StringWriter();
        m.write(sw, "Turtle");
        Response response = r.request().post(Entity.entity(sw.getBuffer().toString(), "text/turtle"));
        return response;
    }

    protected Response invoke(String method, String file, String uri, String mime) {
        File src = (file == null) ? null : new File(file);  
        WebTarget r = c.target(uri);
        switch( method ) {
        case "GET" :
            return r.request().get();
        case "POST" :
            return r.request().post( Entity.entity(src, mime) );
        case "PUT" :
            return r.request().put( Entity.entity(src, mime) );
        case "DELETE":
            return r.request().delete();
        default:
            return r.request().method(method, Entity.entity(src, mime) );
        }
    }

    protected Response post(String uri, String...paramvals) {
        WebTarget r = c.target(uri);
        r.property(ClientProperties.FOLLOW_REDIRECTS, false);
        for (int i = 0; i < paramvals.length; ) {
            String param = paramvals[i++];
            String value = paramvals[i++];
            r = r.queryParam(param, value);
        }
        Response response = r.request().post(null);
        return response;
    }

    protected Response invoke(String method, String file, String uri) {
        return invoke(method, file, uri, "text/turtle");
    }

    protected Model getModelResponse(String uri, String...paramvals) {
        WebTarget r = c.target( uri );
        for (int i = 0; i < paramvals.length; ) {
            String param = paramvals[i++];
            String value = paramvals[i++];
            r = r.queryParam(param, value);
        }
        InputStream response = r.request("text/turtle").get(InputStream.class);
        Model result = ModelFactory.createDefaultModel();
        result.read(response, uri, "Turtle");
        return result;
    }

    protected Response getResponse(String uri) {
        return getResponse(uri, "text/turtle");
    }

    protected Response getResponse(String uri, String mime) {
        WebTarget r = c.target( uri );
        return r.request(mime).get(Response.class);
    }

    protected Model checkModelResponse(String fetch, String rooturi, String file, Property...omit) {
        Model m = getModelResponse(fetch);
        Resource actual = m.getResource(rooturi);
        Resource expected = RDFDataMgr.loadModel(file).getResource(rooturi);
        assertTrue(expected.listProperties().hasNext());  // guard against wrong rooturi in config
        TestUtil.testResourcesMatch(expected, actual, omit);
        return m;
    }

    protected Model checkModelResponse(String fetch, String file, Property...omit) {
        return checkModelResponse(getModelResponse(fetch), file, omit);
    }

    protected Model checkModelResponse(Model m, String rooturi, String file, Property...omit) {
        Resource actual = m.getResource(rooturi);
        Resource expected = RDFDataMgr.loadModel(file).getResource(rooturi);
        assertTrue(expected.listProperties().hasNext());  // guard against wrong rooturi in config
        TestUtil.testResourcesMatch(expected, actual, omit);
        return m;
    }

    protected Model checkModelResponse(Model m, String file, Property...omit) {
        Model expected = RDFDataMgr.loadModel(file);
        for (Resource root : expected.listSubjects().toList()) {
            if (root.isURIResource()) {
                TestUtil.testResourcesMatch(root, m.getResource(root.getURI()), omit);
            }
        }
        return m;
    }

    protected void checkEntity(Model m, String itemURI, String entityURI) {
        Resource entity = m.getResource(itemURI).getPropertyResourceValue(RegistryVocab.definition).getPropertyResourceValue(RegistryVocab.entity);
        assertEquals(entityURI, entity.getURI());
    }

    protected void checkRegisterList(Model m, String rooturi, String...entries) {
        checkRegisterList(m, SKOS.member, rooturi, entries);
    }

    protected void checkRegisterList(Model m, Property link, String rooturi, String...entries) {
        Resource register = m.getResource(rooturi);
        assertTrue( register.hasProperty(RDF.type, RegistryVocab.Register));
        TestUtil.testArray(getRegisterList(m, link, rooturi), entries);
    }

    protected List<String> getRegisterList(Model m, Property link, String rooturi) {
        List<String> actualEntries = new ArrayList<String>();
        ResultSet rs = QueryUtil.selectAll(m, "SELECT ?label WHERE {?register ?link[rdfs:label ?label]}", Prefixes.getDefault(), "register", rooturi, "link", link);
        while (rs.hasNext()) {
            actualEntries.add( rs.next().getLiteral("label").getLexicalForm() );
        }
        return actualEntries;
    }

    protected void printStatus(Response response) {
        String msg = "Response: " + response.getStatus();
        if (response.hasEntity() && response.getStatus() != 204) {
            msg += " (" + response.readEntity(String.class) + ")";
        }
        System.out.println(msg);
    }


    protected void checkLive(int targetStatus) {
        boolean tomcatLive = false;
        int count = 0;
        while (!tomcatLive) {            
            int status = getResponse( getBaseURL() ).getStatus();
            if (status != targetStatus) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
                if (count++ > 10) {
                    fail("Too many tries");
                }
            } else {
                tomcatLive = true;
            }
        }
    }

}
