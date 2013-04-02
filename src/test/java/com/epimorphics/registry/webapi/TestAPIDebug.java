/******************************************************************
 * File:        TestAPIDebug.java
 * Created by:  Dave Reynolds
 * Created on:  6 Mar 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.server.core.Store;
import com.epimorphics.util.TestUtil;
import com.epimorphics.vocabs.API;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;
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
        // Set up some base data
        assertEquals(201, postFileStatus("test/reg1.ttl", BASE_URL));


        assertEquals(201, postFileStatus("test/bw-forward.ttl", REG1));
        assertEquals(404, getResponse(REG1 + "/eabw/ukc2102-03600").getStatus());
        assertEquals(204, post(REG1 + "/_eabw?update&status=stable").getStatus());
        assertEquals(200, getResponse(REG1 + "/eabw/ukc2102-03600").getStatus());
        Model m = getModelResponse(REG1 + "/eabw/ukc2102-03600");
        Resource bw = m.getResource("http://environment.data.gov.uk/id/bathing-water/ukc2102-03600");
        assertEquals("Spittal", RDFUtil.getStringValue(bw, SKOS.prefLabel));

        // convert forwarding to proxying, will only actually function if nginx is up and test doesn't require that
        assertEquals(204, invoke("PATCH", "test/bw-proxy-patch.ttl", REG1 + "/eabw").getStatus());
        String proxyConfig = FileManager.get().readWholeFileAsUTF8("/var/local/registry/proxy-registry.conf");
        assertTrue(proxyConfig.contains("location /reg1/eabw"));
        assertTrue(proxyConfig.contains("proxy_pass http://environment.data.gov.uk/doc/bathing-water/"));

        // Switch batch to forwarding mode to check switching off proxy works
        assertEquals(204, invoke("PATCH", "test/bw-forward-patch.ttl", REG1 + "/eabw").getStatus());
        proxyConfig = FileManager.get().readWholeFileAsUTF8("/var/local/registry/proxy-registry.conf");
        assertFalse(proxyConfig.contains("location /reg1/eabw"));
        assertEquals(200, getResponse(REG1 + "/eabw/ukc2102-03600").getStatus());
    }


    // Debugging utility only, should not be used while transactions are live
    public void printResourceState(String...uris) {
        Store storesvc = ServiceConfig.get().getServiceAs("basestore", Store.class);
        storesvc.lock();
        try {
            Dataset ds =  storesvc.asDataset();
            Model store = ds.getDefaultModel();
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
            for (NodeIterator ni = description.listObjectsOfProperty(RegistryVocab.sourceGraph); ni.hasNext(); ) {
                String graphname = ni.next().asResource().getURI();
                Model graph = ModelFactory.createDefaultModel().add( ds.getNamedModel(graphname) );
                System.out.println("Graph " + graphname);
                graph.setNsPrefixes(Prefixes.get());
                graph.write(System.out, "Turtle");
            }
        } finally {
            storesvc.unlock();
        }
    }

    public static void debugStatus(ClientResponse response) {
        if (response.getStatus() >= 400) {
            System.out.println("Response was: " + response.getEntity(String.class) + " (" + response.getStatus() + ")");
            assertTrue(false);
        }
    }
}
