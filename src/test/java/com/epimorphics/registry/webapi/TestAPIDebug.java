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
import com.epimorphics.registry.vocab.RegistryVocab;
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
        // Set up some base data
        assertEquals(201, postFileStatus("test/reg1.ttl", BASE_URL));

        // Testing update of item + entity - ISSUE-38
        ClientResponse response = postFile("test/jmt/number-eight-post-notation.ttl", REG1);
        assertEquals(201, response.getStatus());
        String itemURI = ROOT_REGISTER + "reg1/_eight";
        assertEquals(itemURI, response.getLocation().toASCIIString());

        Model m = getModelResponse(REG1+"/eight?_view=with_metadata");
        Resource item = m.getResource(itemURI);
        assertEquals("http://ukgovld-registry.dnsalias.net/def/numbers/eight",
                item.getPropertyResourceValue(RegistryVocab.definition)
                    .getPropertyResourceValue(RegistryVocab.entity)
                    .getURI());

        response = postFile("test/jmt/number-eightb-post-relative.ttl", REG1);
        assertEquals(201, response.getStatus());
        itemURI = ROOT_REGISTER + "reg1/_eightb";
        assertEquals(itemURI, response.getLocation().toASCIIString());
        m = getModelResponse(REG1+"/_eightb");
        item = m.getResource(itemURI);
        assertEquals("eightb", RDFUtil.getStringValue(item, RegistryVocab.notation));
        assertEquals("http://ukgovld-registry.dnsalias.net/def/numbers/eight",
                item.getPropertyResourceValue(RegistryVocab.definition)
                    .getPropertyResourceValue(RegistryVocab.entity)
                    .getURI());
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
