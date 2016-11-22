/******************************************************************
 * File:        TestRequestLogging.java
 * Created by:  Dave Reynolds
 * Created on:  21 Nov 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.jena.ext.com.google.common.io.Files;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Before;
import org.junit.Test;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Command.Operation;
import com.epimorphics.registry.core.Registry;
import com.sun.jersey.api.uri.UriComponent;

public class TestRequestLogging {
    Registry registry = new Registry();
    
    @Before
    public void setUp() {
        registry.setBaseURI("http://environment.data.gov.uk/registry");
        File tdir = Files.createTempDir();
        GenericRequestLogger logger = new GenericRequestLogger();
        logger.setLogDir( tdir.getPath() );
        logger.setNotificationScript( "test/replication/testScript.sh" );
        registry.setRequestLogger( logger );
    }

    @Test
    public void testRequestLoggingSupport() throws IOException {
        doTest("Delete", "root/test", makeParams("force=true&foo=3&foo=4"), null);
        Model testPayload = RDFDataMgr.loadModel("test/colours.ttl");
        doTest("Update", "reg1/colours", makeParams(""), testPayload);
    }
    
    private void doTest(String command, String target, MultivaluedMap<String, String> parameters, Model payload) throws IOException {
        Command result = roundTrip(command, target, parameters, payload);
        assertEquals(command, result.getOperation().name());
        assertEquals(registry.getBaseURI() + "/" + target, result.getTarget());
        assertEquals(parameters, result.getParameters());
        if (payload != null) {
            assertTrue( payload.isIsomorphicWith( result.getPayload() ));
        }
    }
    
    private Command roundTrip(String op, String target, MultivaluedMap<String, String> parameters, Model payload) throws IOException {
        Command command = registry.make(Operation.valueOf(op), target, parameters);
        if (payload != null) {
            command.setPayload(payload);
        }
        
        String file = registry.getRequestLogger().writeLog(command);
        
        // Check the notification has fired
        assertTrue( new File( file + ".log" ).exists() );
        
        InputStream in = new FileInputStream(file);
        return registry.getRequestLogger().getLog(in);
    }
    
    private MultivaluedMap<String, String> makeParams(String params) {
        return UriComponent.decodeQuery(params, true);
    }
}
