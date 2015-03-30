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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import com.epimorphics.appbase.core.AppConfig;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class TestSearch {

    @Test
    public void testSearchMachinery() {
        AppConfig.startApp("app", "src/test/webapp-index/WEB-INF/app.conf");
        
        Store store = AppConfig.getApp().getComponentAs("basestore", Store.class);
        
        store.addGraph("test", RDFDataMgr.loadModel("test/expected/red.ttl"));
        store.addGraph("testb", RDFDataMgr.loadModel("test/expected/red_item_version.ttl"));
        
        store.lock();
        String query = "PREFIX text: <http://jena.apache.org/text#> "
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
                + "PREFIX reg: <http://purl.org/linked-data/registry#> "
                + "PREFIX version: <http://purl.org/linked-data/version#> "
                + "SELECT * WHERE { "
                + "   ?entity text:query 'red'. "
                + "   ?item version:currentVersion/reg:definition/reg:entity ?entity. "
                + "}";
        QueryExecution qexec = QueryExecutionFactory.create(query, store.asDataset());
        ResultSet results = qexec.execSelect();
        assertTrue( results.hasNext() );
        QuerySolution row = results.next();
        assertEquals( "http://location.data.gov.uk/reg1/red", row.getResource("entity").getURI() );
        assertEquals( "http://location.data.gov.uk/reg1/_red", row.getResource("item").getURI() );
        store.unlock();
    }
}
