/******************************************************************
 * File:        TestReplay.java
 * Created by:  Dave Reynolds
 * Created on:  24 Nov 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.junit.Test;

import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.message.GenericRequestLogger;

/**
 * Test capture and replay of update logs.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TestReplay extends TomcatTestBase  {

    static final String REG1 = BASE_URL + "reg1";
    static final String REG1_URI = ROOT_REGISTER + "reg1";
    static final String REG1_ITEM = ROOT_REGISTER + "_reg1";
    static final String REG1EXPT = REG1 + "?status=any";
    static final String REPLAY = BASE_URL + "system/replay";
    
    @Override
    public String getWebappRoot() {
        return "src/test/webapp";
    }

    protected File logDir;
    
    protected void configureLogDir() throws IOException {
        logDir = Files.createTempDirectory("replay").toFile();
    }
    
    protected void configureLogging() {
        Registry registry = Registry.get();
        GenericRequestLogger logger = new GenericRequestLogger();
        logger.setLogDir( logDir.getPath() );
        registry.setRequestLogger(logger);
    }
    
    @Test
    public void testLogAndReplay() throws Exception {
        configureLogDir();
        configureLogging();
        performUpdates();
        checkUpdates();
        containerStop();
        
        containerStart();
        configureLogging();
        checkNoUpdates();
        performReplay();
        checkUpdates();
    }

    protected void performUpdates() {
        assertEquals(201, postFileStatus("test/reg1.ttl", BASE_URL));
        assertEquals(201, postFileStatus("test/red.ttl", REG1));
        assertEquals(201, postFileStatus("test/blue.ttl", REG1));
        assertEquals(201, postFileStatus("test/green.ttl", REG1));
        assertEquals(204, post(REG1 + "/_blue?update&status=experimental").getStatus());
        assertEquals(204, post(REG1 + "/_red?update&status=experimental").getStatus());
        assertEquals(204, post(BASE_URL + "_reg1?update&status=experimental").getStatus());
        assertEquals(303, post(REG1 + "/_blue?real_delete").getStatus());
    }
    
    protected void checkNoUpdates() {
        assertEquals(404, getResponse(REG1EXPT).getStatus());
    }
    
    protected void checkUpdates() {
        checkModelResponse(REG1EXPT, "test/replication/expected-content.ttl", OWL.versionInfo, DCTerms.modified);
    }
    
    protected void performReplay() {
        List<String> logs = new ArrayList<>();
        for (String log :  logDir.list()) {
            logs.add(log);
        }
        Collections.sort(logs);
        for (String log : logs) {
            doReplay( new File(logDir, log).getPath() );
        }
    }
    
    protected void doReplay(String log) {
        int status = postFileStatus(log, REPLAY);
        assertTrue( status >= 200 && status < 400 );
    }
}
