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
import com.epimorphics.registry.vocab.Prov;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.server.core.Store;
import com.epimorphics.util.TestUtil;
import com.epimorphics.vocabs.API;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.assembler.Mode;
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
import com.hp.hpl.jena.vocabulary.RDF;
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
        assertEquals(201, postFileStatus("test/red.ttl", REG1));

        doTaggingTest();
    }

    /**
     * Check that tagging a register works and the results can be retrieved
     */
    private void doTaggingTest() {
        // Create a clean register state
        assertEquals(201, postFileStatus("test/reg3.ttl", BASE_URL));
        assertEquals(201, postFileStatus("test/red.ttl", BASE_URL + "reg3"));
        assertEquals(201, postFileStatus("test/blue.ttl", BASE_URL + "reg3"));
        assertEquals(204, post(BASE_URL + "reg3/_red?update&status=stable").getStatus());
        assertEquals(204, post(BASE_URL + "reg3/_blue?update&status=stable").getStatus());
        
        // Tag it
        ClientResponse response = post(BASE_URL+"reg3?tag=tag1");
        assertEquals(201, response.getStatus());
        String uri = ROOT_REGISTER+"reg3?tag=tag1";
        assertEquals(uri, response.getLocation().toString());
        
        // Modify some entries
        response = invoke("PATCH", "test/red1b.ttl", BASE_URL + "reg3/red");
        assertEquals(204,  response.getStatus());
        assertEquals(201, postFileStatus("test/green.ttl", BASE_URL + "reg3"));
        
        Model model = getModelResponse(BASE_URL + "reg3?tag=tag1");
        Resource collection = model.getResource(uri);
        assertTrue(collection.hasProperty(RDF.type, Prov.Collection));
        assertTrue(collection.hasProperty(Prov.generatedAtTime));
        assertTrue(collection.hasProperty(RegistryVocab.tag, "tag1"));
        assertTrue(collection.hasProperty(Prov.wasDerivedFrom));
        List<RDFNode> members = model.listObjectsOfProperty(collection, Prov.hadMember).toList();
        assertTrue( members.contains( model.getResource(ROOT_REGISTER + "reg3/_red:2") ) );
        assertTrue( members.contains( model.getResource(ROOT_REGISTER + "reg3/_blue:2") ) );
        assertEquals(2, members.size());
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
