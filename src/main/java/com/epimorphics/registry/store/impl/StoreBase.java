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
import com.epimorphics.util.EpiException;
import com.epimorphics.util.FileUtil;
import com.epimorphics.util.NameUtils;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Base implementation of a generic store. 
 * Supports optional logging of all requests to a nominated file system.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public abstract class StoreBase extends ComponentBase implements Store {
    static Logger log = LoggerFactory.getLogger(StoreBase.class);

    public static final String ADD_ACTION = "ADD";
    public static final String UPDATE_ACTION = "UPDATE";
    public static final String DELETE_ACTION = "DELETE";

    protected Dataset dataset;
    protected String logDirectory;
    protected boolean inWrite = false;
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
    
    @Override
    public void startup(App app) {
        super.startup(app);
        
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
                        String uri = getApp().getPrefixes().expandPrefix(spec.trim());
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
    
    protected abstract void doAddGraph(String graphname, Model graph);
    protected abstract void doAddGraph(String graphname, InputStream input, String mimeType);
    protected abstract void doDeleteGraph(String graphname);

    @Override
    abstract public Dataset asDataset();

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
    public synchronized void lock() {
        Dataset dataset = asDataset();
        if (dataset.supportsTransactions()) {
            dataset.begin(ReadWrite.READ);
        } else {
            dataset.asDatasetGraph().getLock().enterCriticalSection(Lock.READ);
        }
    }

    /** Lock the dataset for write */
    public synchronized void lockWrite() {
        Dataset dataset = asDataset();
        if (dataset.supportsTransactions()) {
            dataset.begin(ReadWrite.WRITE);
            inWrite = true;
        } else {
            dataset.asDatasetGraph().getLock().enterCriticalSection(Lock.WRITE);
        }
    }

    /** Unlock the dataset */
    public synchronized void unlock() {
        Dataset dataset = asDataset();
        if (dataset.supportsTransactions()) {
            if (inWrite) {
                dataset.commit();
                inWrite = false;
            }
            dataset.end();
        } else {
            dataset.asDatasetGraph().getLock().leaveCriticalSection();
        }
    }

    /** Unlock the dataset, aborting the transaction. Only useful if the dataset is transactional */
    public synchronized void abort() {
        Dataset dataset = asDataset();
        if (dataset.supportsTransactions()) {
            if (inWrite) {
                dataset.abort();
                inWrite = false;
            }
            dataset.end();
        } else {
            dataset.asDatasetGraph().getLock().leaveCriticalSection();
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
            lockWrite();
            try {
                logAction(action, graphname, asDataset().getNamedModel(graphname) );
            } finally {
                unlock();
            }
        }
    }

}
