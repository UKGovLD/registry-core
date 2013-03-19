/******************************************************************
 * File:        TestAPIDebug.java
 * Created by:  Dave Reynolds
 * Created on:  6 Mar 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.server.core.Store;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Place where new webapi tests can be developed to investigate
 * reported problems. Once running they get merged
 * in the main tests in TestAPI.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TestAPIDebug extends TomcatTestBase {

    static final String EXT_BLACK = "http://example.com/colours/black";
    static final String REG1 = BASE_URL + "reg1";
    static final String REG1_URI = ROOT_REGISTER + "reg1";

    String getWebappRoot() {
        return "src/test/webapp";
    }

    @Test
    public void testDebug() throws IOException {
//        Model m = null;

        // Set up some base data
        assertEquals(201, postFileStatus("test/reg1.ttl", BASE_URL));
        assertEquals(201, postFileStatus("test/red.ttl", REG1));
        assertEquals(201, postFileStatus("test/blue.ttl", REG1));

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

    }

    // Debugging utility only, should not be used while transactions are live
    public void printResourceState(String...uris) {
        Model store = ServiceConfig.get().getServiceAs("basestore", Store.class).asDataset().getDefaultModel();
        Model description = ModelFactory.createDefaultModel();
        for (String uri: uris) {
            Resource r = store.getResource(uri);
            Closure.closure(r, false, description);
            if (r.hasProperty(Version.currentVersion)) {
                r = r.getPropertyResourceValue(Version.currentVersion);
                Closure.closure(r, false, description);
            }
        }
        description.setNsPrefixes( Prefixes.get() );
        description.write(System.out, "Turtle");
    }

}
