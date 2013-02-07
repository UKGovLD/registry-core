/******************************************************************
 * File:        TomcatTestContainerFactory.java
 * Created by:  Dave Reynolds
 * Created on:  30 Nov 2012
 *
 * (c) Copyright 2012, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.catalina.startup.Tomcat;
import org.junit.After;
import org.junit.Before;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;

public class TomcatTestBase {

    static final String BASE_URL = "http://localhost:8070/";
    static final String ROOT_REGISTER = "http://location.data.gov.uk/";
    
    Tomcat tomcat ;
    Client c;
    

    @Before
    public void containerStart() throws Exception {
        String root = "src/test/webapp";
        tomcat = new Tomcat();
        tomcat.setPort(8070);

        tomcat.setBaseDir(".");

        String contextPath = "/";

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
        DefaultClientConfig config = new DefaultClientConfig();
        config.getProperties().put(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true);
        c = Client.create(config);
    }

    @After
    public void containerStop() throws Exception {
        tomcat.stop();
        tomcat.destroy();
    }
    
    protected int postFileStatus(String file, String uri) {
        return postFileStatus(file, uri, "text/turtle");
    }
    
    protected int postFileStatus(String file, String uri, String mime) {
        return postFile(file, uri, mime).getStatus();
    }
    
    protected ClientResponse postFile(String file, String uri) {
        return postFile(file, uri, "text/turtle");
    }
    
    protected ClientResponse postFile(String file, String uri, String mime) {
        WebResource r = c.resource(uri);
        File src = new File(file);
        ClientResponse response = r.type(mime).post(ClientResponse.class, src);
        return response;
    }
    
    protected ClientResponse postModel(Model m, String uri) {
        WebResource r = c.resource(uri);
        StringWriter sw = new StringWriter();
        m.write(sw, "Turtle");
        ClientResponse response = r.type("text/turtle").post(ClientResponse.class, sw.getBuffer().toString());
        return response;
    }
    
    protected ClientResponse invoke(String method, String file, String uri, String mime) {
        WebResource r = c.resource(uri);
        ClientResponse response = null;
        if (file == null) {
            response = r.type(mime).header("X-HTTP-Method-Override", method).post(ClientResponse.class);
        } else {
            File src = new File(file);
            response = r.type(mime).header("X-HTTP-Method-Override", method).post(ClientResponse.class, src);
        }
        return response;
    }
    
    protected ClientResponse post(String uri, String...paramvals) {
        WebResource r = c.resource(uri);
        for (int i = 0; i < paramvals.length; ) {
            String param = paramvals[i++];
            String value = paramvals[i++];
            r = r.queryParam(param, value);
        }
        ClientResponse response = r.post(ClientResponse.class);
        return response;
    }

    protected ClientResponse invoke(String method, String file, String uri) {
        return invoke(method, file, uri, "text/turtle");
    }

    protected Model getModelResponse(String uri, String...paramvals) {
        WebResource r = c.resource( uri );
        for (int i = 0; i < paramvals.length; ) {
            String param = paramvals[i++];
            String value = paramvals[i++];
            r = r.queryParam(param, value);
        }
        InputStream response = r.accept("text/turtle").get(InputStream.class);
        Model result = ModelFactory.createDefaultModel();
        result.read(response, uri, "Turtle");
        return result;
    }

    protected ClientResponse getResponse(String uri) {
        WebResource r = c.resource( uri );
        return r.accept("text/turtle").get(ClientResponse.class);
    }
}
