/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.epimorphics.registry.store;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Quad;


/**
 * RDF store abstraction. Obsolete - predates jena support for transactions
 * and designed for now-deprecated component model. Retained here temporarily
 * as part of porting.
 */
// TODO replace with SparqSource
public interface Store {
    /* read */

    Dataset asDataset();
    Model getGraph(String name);
    Model getDefaultModel();
    ResultSet query(String sparql);

    void insertTriple(Triple t); // default graph
    void insertQuad(Quad q);

    void removeTriple(Triple t);
    void removeQuad(Quad q);

    void addPropertyToResource(Resource resource, Property property, Resource object);
    void addResource(Resource resource); // default graph

    void addAll(Model model); // default graph
    void removeAll(Model model); // default graph

    /* write */

    void insertGraph(String name, Model graph);
    void updateGraph(String name, Model graph);
    void deleteGraph(String name);

    void lock();
    void lockWrite();
    void abort();
    void commit();
    void end();
}
