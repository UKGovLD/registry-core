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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jena.fuseki.server.DatasetRef;
import org.apache.jena.fuseki.server.DatasetRegistry;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.registry.store.Store;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.FileUtil;
import com.epimorphics.util.NameUtils;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.RDFS;

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
// This obsolete code, based on a poor store abstraction
// It's here to facilitate a first port to appbase
// but should be removed once we've switched to using SparqlStore
public class TDBStore  extends ComponentBase implements Store {
    static final Logger log = LoggerFactory.getLogger( TDBStore.class );

    public static final String MEMORY_LOCATION = "mem";
    
    protected String tdbDir;
    protected File backupDir;
    protected boolean isUnionDefault;

    public static final String ADD_ACTION = "ADD";
    public static final String UPDATE_ACTION = "UPDATE";
    public static final String DELETE_ACTION = "DELETE";

    protected Dataset dataset;
    protected String logDirectory;
    protected String qEndpoint;
    protected File textIndex;      
    protected String indexSpec = null;

    public void setEp(String endpoint) {
        qEndpoint = endpoint;
    }
    
    public void setLog(String dir) {
        logDirectory = expandFileLocation(dir);
        FileUtil.ensureDir(logDirectory);
    }
    
    /**
     * Set a directory from which the text index can be obtained
     */
    public void setIndex(String index) {
        textIndex = asFile(index);
    }

    /**
     * Configuration text indexing of the loaded data.
     * Value should be "default" (to index rdfs:label) or a comma-separated list of predicates to index. These can
     * use curies with the prefixes as defined in the application's prefix service.
     * Index will always include rdfs:label.
     */
    public void setTextIndex(String spec) {
        this.indexSpec = spec;
    }
        
    public void setLocation(String loc) {
        if ( ! loc.equals(MEMORY_LOCATION) ) {
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

        if (tdbDir != null) {
            log.info("Opening database at tdbDir");
            dataset = TDBFactory.createDataset( tdbDir );
        } else {
            log.warn("Opening in-memory database");
            dataset = TDBFactory.createDataset( );
        }

        if (isUnionDefault) {
            // Nasty global side effect, in normal implementation this is done per query
            TDB.getContext().set(TDB.symUnionDefaultGraph, true) ;
        }
        
        if (qEndpoint != null) {
            String base = AppConfig.getAppConfig().getContext().getContextPath();
            if ( ! base.endsWith("/")) {
                base += "/";
            }
            base += qEndpoint;
            DatasetRef ds = new DatasetRef();
            ds.name = qEndpoint;
            ds.query.endpoints.add("query" ); 
            ds.init();
            ds.dataset = dataset.asDatasetGraph();
            DatasetRegistry.get().put(base, ds);
            log.info("Installing SPARQL query endpoint at " + base + "/query");
        }
        if (textIndex != null || indexSpec != null) {
            try {
                Directory dir = null;
                if (textIndex == null) {
                    log.warn("Opening memory based text index, will not preserved across restarts");
                    dir = new RAMDirectory(); 
                } else {
                    dir = FSDirectory.open(textIndex);
                }
                EntityDefinition entDef = new EntityDefinition("uri", "text", RDFS.label.asNode()) ;
                if (indexSpec != null) {
                    for (String spec : indexSpec.split(",")) {
                        String uri = Prefixes.get().expandPrefix(spec.trim());
                        if ( ! uri.equals("default") ) {
                            Node predicate = NodeFactory.createURI(uri);
                            if (!predicate.equals(RDFS.label.asNode())) {
                                entDef.set("text", predicate);
                            }
                        }
                    }
                }
                dataset = TextDatasetFactory.createLucene(dataset, dir, entDef, new StandardAnalyzer(org.apache.jena.query.text.TextIndexLucene.VER)) ;            
            } catch (IOException e) {
                throw new EpiException("Failed to create jena-text lucence index area", e);
            }
        }        
    }

    @Override
    public Dataset asDataset() {
        return dataset;
    }

    @Override
    public Model getUnionModel() {
        return dataset.getNamedModel("urn:x-arq:UnionGraph");
    }

    @Override
    public void addGraph(String graphname, Model graph) {
        logAction(ADD_ACTION, graphname, graph);
        doAddGraph(graphname, graph);
    }

    @Override
    public void updateGraph(String graphname, Model graph) {
        logAction(UPDATE_ACTION, graphname, graph);
        doDeleteGraph(graphname);
        doAddGraph(graphname, graph);
    }

    @Override
    public void deleteGraph(String graphname) {
        logAction(DELETE_ACTION, graphname, null);
        doDeleteGraph(graphname);
    }

    @Override
    public void addGraph(String graphname, InputStream input, String mimeType) {
        doAddGraph(graphname, input, mimeType);
        logNamed(ADD_ACTION, graphname);
    }

    @Override
    public void updateGraph(String graphname, InputStream input, String mimeType) {
        doDeleteGraph(graphname);
        doAddGraph(graphname, input, mimeType);
        logNamed(UPDATE_ACTION, graphname);
    }

    /** Lock the dataset for reading */
    @Override
    public void lock() {
        asDataset().begin(ReadWrite.READ);
    }

    /** Lock the dataset for reading */
    @Override
    public void lockWrite() {
        asDataset().begin(ReadWrite.WRITE);
    }

    /** Abort the transaction. */
    @Override
    public void abort() {
        asDataset().abort();
    }
    
    /** Commit the transaction */
    @Override
    public void commit() {
        asDataset().commit();
    }
    
    /** End the transaction, if a write transaction and not commited then abort */
    @Override
    public void end() {
        asDataset().end();
    }

    protected
    void doAddGraph(String graphname, Model graph) {
        lockWrite();
        try {
            dataset.getNamedModel(graphname).add(graph);
            commit();
        } finally {
            end();
        }
    }

    protected
    void doDeleteGraph(String graphname) {
        lockWrite();
        try {
            Model store = dataset.getNamedModel(graphname);
            store.removeAll();
            commit();
        } finally {
            end();
        }
    }

    protected void doAddGraph(String graphname, InputStream input,
            String mimeType) {
        Lang lang = RDFLanguages.contentTypeToLang(mimeType);
        if (lang == null) {
            throw new EpiException("Cannot read MIME type: " + mimeType);
        }
        lockWrite();
        try {
            dataset.getNamedModel(graphname).read(input, graphname, lang.getName());
            commit();
            try { input.close(); } catch (IOException eio) {}
        } catch (Exception e) {
            abort();
            throw new EpiException(e);
        } finally {
            end();
        }
    }

    protected void logAction(String action, String graph, Model data) {
        if (logDirectory != null) {
            String dir = logDirectory + File.separator + NameUtils.encodeSafeName(graph);
            FileUtil.ensureDir(dir);
            String filename = String.format("on-%s-%s.ttl", System.currentTimeMillis(), action);
            File logFile = new File(dir, filename);
            try {
                if (data != null) {
                    OutputStream out = new FileOutputStream(logFile);
                    data.write(out, FileUtils.langTurtle);
                    out.close();
                } else {
                    logFile.createNewFile();
                }
            } catch (IOException e) {
                log.error("Failed to create log file: " + logFile);
            }
        }
    }

    protected void logNamed(String action, String graphname) {
        if (logDirectory != null) {
            lock();
            try {
                logAction(action, graphname, asDataset().getNamedModel(graphname) );
            } finally {
                end();
            }
        }
    }
}
