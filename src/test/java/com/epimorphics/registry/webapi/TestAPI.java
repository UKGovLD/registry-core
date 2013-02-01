/******************************************************************
 * File:        TestAPI.java
 * Created by:  Dave Reynolds
 * Created on:  1 Feb 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;


/**
 * Test harness for testing access to the API - launches a registry using an 
 * embedded tomcat and a memory-backed store then talks to the API using http.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TestAPI extends TomcatTestBase {
    Client c = Client.create();

    @Test
    public void testBasics() {
        assertEquals(204, postFile("test/reg1.ttl", BASE_URL, "text/turtle"));
    }
    
    protected int postFile(String file, String uri, String mime) {
        WebResource r = c.resource(uri);
        File src = new File(file);
        ClientResponse response = r.type(mime).post(ClientResponse.class, src);
        return response.getStatus();
    }
    
}
