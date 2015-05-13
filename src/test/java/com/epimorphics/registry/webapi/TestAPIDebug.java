/******************************************************************
 * File:        TestAPIDebug.java
 * Created by:  Dave Reynolds
 * Created on:  6 Mar 2013
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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.junit.Test;

import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.registry.store.Store;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.util.Closure;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Place where new webapi tests can be developed to investigate
 * reported problems. Once running they get merged
 * in the main tests in TestAPI.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TestAPIDebug extends TomcatTestBase {

    static final String REG1 = BASE_URL + "reg1";
    static final String REG1_URI = ROOT_REGISTER + "reg1";

    String getWebappRoot() {
        return "src/test/webapp";
    }

    @Test
    public void testDebug() throws IOException {

    }
    

    // Debugging utility only, should not be used while transactions are live
    public static void printResourceState(String...uris) {
        Store storesvc = AppConfig.getApp().getComponentAs("basestore", Store.class);
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
            storesvc.end();
        }
    }


    // Debugging utility only, should not be used while transactions are live
    public static void printStore() {
        Store storesvc = AppConfig.getApp().getComponentAs("basestore", Store.class);
        storesvc.lock();
        try {
            Dataset ds =  storesvc.asDataset();
            Model store = ds.getDefaultModel();
            System.out.println("Default model:");
            store.write(System.out, "Turtle");
            for (Iterator<String> i = ds.listNames(); i.hasNext(); ) {
                String graphName = i.next();
                Model graph = ds.getNamedModel(graphName);
                System.out.println("Graph " + graphName + ":");
                graph.write(System.out, "Turtle");
            }
        } finally {
            storesvc.end();
        }
    }

    public static void debugStatus(ClientResponse response) {
        if (response.getStatus() >= 400) {
            System.out.println("Response was: " + response.getEntity(String.class) + " (" + response.getStatus() + ")");
            assertTrue(false);
        }
    }
}
