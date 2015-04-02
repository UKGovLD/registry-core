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

import java.io.InputStream;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;


/**
 * RDF store abstraction. Obsolete - predates jena support for transactions
 * and designed for now=deprecated component model. Retained here temporarily
 * as part of porting.
 */
// TODO replace with SparqSource
public interface Store {

    public void addGraph(String graphname, Model graph);
    public void addGraph(String graphname, InputStream input, String mimeType);

    public void updateGraph(String graphname, Model graph);
    public void updateGraph(String graphname, InputStream input, String mimeType);

    public void deleteGraph(String graphname);

    public Dataset asDataset();

    public Model getUnionModel();

    public void lock();
    public void lockWrite();
    public void abort();
    public void commit();
    public void end();
}
