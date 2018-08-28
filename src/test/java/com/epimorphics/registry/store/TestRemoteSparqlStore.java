/******************************************************************
 * File:        TestStoreImpl.java
 * Created by:  Dave Reynolds
 * Created on:  30 Jan 2013
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

package com.epimorphics.registry.store;

import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.registry.store.impl.RemoteSparqlStore;
import com.epimorphics.registry.store.impl.TDBStore;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDFS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


public class TestRemoteSparqlStore {

    private Store store;

    @Before
    public void setup() {
        store = new RemoteSparqlStore();
        ((RemoteSparqlStore) store).setEndpoint("http://localhost:3030/ds/query");
        ((RemoteSparqlStore) store).setUpdateEndpoint("http://localhost:3030/ds/update");
        ((RemoteSparqlStore) store).setGraphEndpoint("http://localhost:3030/ds/data");
//        store = new TDBStore();
//        ((ComponentBase) store).startup(null);
    }

    @After
    public void tearDown() throws InterruptedException {
        emptyRemoteFusekiStore();
        store = null;
        Thread.sleep(1000);
    }

    private void emptyRemoteFusekiStore() {
        UpdateRequest request = new UpdateRequest();
        request.add("DROP ALL");

        UpdateExecutionFactory.createRemote(request, "http://localhost:3030/ds/update").execute();
    }


    @Test
    public void testInsertAndRemoveTriple() {
        Node subject = NodeFactory.createURI("http://subject.com");
        Node predicate = NodeFactory.createURI("http://predicate.com");
        Node objekt = NodeFactory.createLiteral("Big Object");

        Triple triple = new Triple(subject, predicate, objekt);

        store.insertTriple(triple);

        assertEquals(1, store.getDefaultModel().listStatements().toList().size());
        assertEquals(triple, store.getDefaultModel().listStatements().nextStatement().asTriple());

        store.removeTriple(triple);

        assertEquals(0, store.getDefaultModel().listStatements().toList().size());
    }

    @Test
    public void testInsertAndRemoveQuad() {
        Node graph = NodeFactory.createURI("http://graph.com");
        Node subject = NodeFactory.createURI("http://subject.com");
        Node predicate = NodeFactory.createURI("http://predicate.com");
        Node objekt = NodeFactory.createLiteral("Big Object");

        Quad quad = new Quad(graph, subject, predicate, objekt);

        store.insertQuad(quad);

        assertEquals(1, store.getGraph(graph.getURI()).listStatements().toList().size());
        assertEquals(quad.asTriple(), store.getGraph(graph.getURI()).listStatements().nextStatement().asTriple());

        store.removeQuad(quad);

        assertNull(store.getGraph(graph.getURI()));
    }

    @Test
    public void testGraphManipulation() {
        Node graph = NodeFactory.createURI("http://graph.com");

        Statement statement = new StatementImpl(
                ResourceFactory.createResource("http://entry.com"),
                RDFS.label,
                ResourceFactory.createStringLiteral("Some Label")
        );

        Model graphModel = ModelFactory.createDefaultModel().add(statement);

        store.insertGraph(graph.getURI(), graphModel);

        assertTrue(store.getGraph(graph.getURI()).contains(statement));
        assertEquals(1, store.getGraph(graph.getURI()).size());

        Statement newStatement = new StatementImpl(
                ResourceFactory.createResource("http://new-entry.com"),
                RDFS.label,
                ResourceFactory.createStringLiteral("Another Label")
        );

        Model newGraphModel = ModelFactory.createDefaultModel().add(newStatement);

        store.updateGraph(graph.getURI(), newGraphModel);

        assertFalse(store.getGraph(graph.getURI()).contains(statement));
        assertTrue(store.getGraph(graph.getURI()).contains(newStatement));
        assertEquals(1, store.getGraph(graph.getURI()).size());

        store.deleteGraph(graph.getURI());

        assertNull(store.getGraph(graph.getURI()));
    }

    @Test
    public void testAddAndRemoveAll() {
        Statement statement0 = new StatementImpl(
                ResourceFactory.createResource("http://entry.com"),
                RDFS.label,
                ResourceFactory.createStringLiteral("Label")
        );
        Statement statement1 = new StatementImpl(
                ResourceFactory.createResource("http://entry.com"),
                RDFS.comment,
                ResourceFactory.createStringLiteral("Very important comment")
        );
        Statement statement2 = new StatementImpl(
                ResourceFactory.createResource("http://entry.com"),
                RDFS.seeAlso,
                ResourceFactory.createResource("http://entry.com/seeAlso")
        );
        Model baseModel = ModelFactory.createDefaultModel()
                .add(statement0)
                .add(statement1)
                .add(statement2);

        store.addAll(baseModel);

        assertEquals(3, store.getDefaultModel().size());
        assertTrue(store.getDefaultModel().contains(statement0));
        assertTrue(store.getDefaultModel().contains(statement1));
        assertTrue(store.getDefaultModel().contains(statement2));

        Statement statement3 = new StatementImpl(
                statement0.getSubject(),
                statement0.getPredicate(),
                ResourceFactory.createStringLiteral("New Label")
        );

        baseModel.remove(statement0);
        baseModel.add(statement3);

        store.addAll(baseModel);

        assertEquals(4, store.getDefaultModel().size());
        assertTrue(store.getDefaultModel().contains(statement0));
        assertTrue(store.getDefaultModel().contains(statement1));
        assertTrue(store.getDefaultModel().contains(statement2));
        assertTrue(store.getDefaultModel().contains(statement3));

        store.removeAll(baseModel);

        assertEquals(1, store.getDefaultModel().size());
        assertTrue(store.getDefaultModel().contains(statement0));
    }

    @Test
    public void testResourceManipulation() {
        Resource resource = ResourceFactory.createResource("http://resource.com");
        resource = resource.inModel(ModelFactory.createDefaultModel());
        resource.addProperty(RDFS.label, "New Resource");
        resource.addProperty(RDFS.comment, "The best resource ever");

        store.addResource(resource);

        assertEquals(2, store.getDefaultModel().size());
        resource.listProperties().forEachRemaining( stm -> assertTrue(store.getDefaultModel().contains(stm)));


        store.addResource(resource);

        assertEquals(2, store.getDefaultModel().size());
        resource.listProperties().forEachRemaining( stm -> assertTrue(store.getDefaultModel().contains(stm)));
    }


}
