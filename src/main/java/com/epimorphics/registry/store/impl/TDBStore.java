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

package com.epimorphics.registry.store.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.FileUtil;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;

/**
 * Store implementation using TDB.
 *
 * <p>Set "location" parameter to define where the
 * TDB store sites, use ${webapp} in the parameter value to reference the directory
 * where the webapp is installed.</p>
 *
 * <p>Set "union=true" to set the (currently GLOBAL) flag to make
 * sparql queries see the default graph as the union of all the named graphs.</p>
 *
 * <p>Set "ep={ds}" to register a fuseki query endpoint /ds/query.</p>
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TDBStore extends StoreBase {
    static final Logger log = LoggerFactory.getLogger( TDBStore.class );

    public static final String MEMORY_LOCATION = "mem";
    
    protected String tdbDir;
    protected File backupDir;
    protected boolean isUnionDefault;
    
    public void setLocation(String loc) {
        if ( loc.equals(MEMORY_LOCATION) ) {
            tdbDir = MEMORY_LOCATION;
        } else {
            tdbDir = expandFileLocation(loc);
            FileUtil.ensureDir(tdbDir);
        }
    }
    
    public void setBackupDir(String loc) {
        FileUtil.ensureDir(loc);
        backupDir = asFile(loc);
    }
        
    public void setUnionDefault(boolean flag) {
        isUnionDefault = flag;
    }
    
    @Override
    public void startup(App app) {
        super.startup(app);
        
        dataset = tdbDir.equals(MEMORY_LOCATION) ? TDBFactory.createDataset() :  TDBFactory.createDataset( tdbDir );

        if (isUnionDefault) {
            // Nasty global side effect, in normal implementation this is done per query
            TDB.getContext().set(TDB.symUnionDefaultGraph, true) ;
        }
    }

    @Override
    protected
    void doAddGraph(String graphname, Model graph) {
        lockWrite();
        try {
            dataset.getNamedModel(graphname).add(graph);
        } finally {
            unlock();
        }
    }


    @Override
    protected
    void doDeleteGraph(String graphname) {
        lockWrite();
        try {
            Model store = dataset.getNamedModel(graphname);
            store.removeAll();
        } finally {
            unlock();
        }
    }

    @Override
    public Dataset asDataset() {
        return dataset;
    }

    @Override
    protected void doAddGraph(String graphname, InputStream input,
            String mimeType) {
        Lang lang = RDFLanguages.contentTypeToLang(mimeType);
        if (lang == null) {
            throw new EpiException("Cannot read MIME type: " + mimeType);
        }
        lockWrite();
        try {
            dataset.getNamedModel(graphname).read(input, graphname, lang.getName());
            try { input.close(); } catch (IOException eio) {}
        } catch (Exception e) {
            abort();
            throw new EpiException(e);
        } finally {
            unlock();
        }
    }

    @Override
    public Model getUnionModel() {
        return dataset.getNamedModel("urn:x-arq:UnionGraph");
    }

}
