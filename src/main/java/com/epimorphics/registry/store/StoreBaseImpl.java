/******************************************************************
 * File:        StoreBaseImpl.java
 * Created by:  Dave Reynolds
 * Created on:  27 Jan 2013
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

import static com.epimorphics.rdfutil.QueryUtil.createBindings;
import static com.epimorphics.rdfutil.QueryUtil.selectAll;
import static com.epimorphics.rdfutil.QueryUtil.selectFirstVar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.riot.system.StreamRDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.DelegationRecord;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.ForwardingRecord;
import com.epimorphics.registry.core.ForwardingRecord.Type;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.store.SearchRequest.KeyValuePair;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.util.VersionUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.NameUtils;
import com.epimorphics.vocabs.Time;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Implementation of the store API which uses a triple store for persistence and
 * indexes all registered items as they are registered. In this version all
 * current data is held in the default graph and named graphs are used for each
 * version of each defined entity.
 * <p>
 * This implementation uses use the ServiceConfig machinery to specify the store
 * to be used ("store" parameter) and the (optional) indexer ("indexer"
 * parameter). The configured store should <strong>not</strong> use
 * union-default.
 * </p>
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */

// TODO Replace by SPARQL-based version with underlying SPARQLSource

public class StoreBaseImpl extends ComponentBase implements StoreAPI {
    static final Logger log = LoggerFactory.getLogger(StoreBaseImpl.class);

    public static final String STORE_PARAMETER = "store";
    public static final String INDEXER_PARAMETER = "indexer";

    protected Store store;
    protected ThreadLocal<Boolean> writeLocked = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    
    protected AtomicInteger readLockCount = new AtomicInteger(0);

    public void setStore(Store store) {
        this.store = store;
    }

    /**
     * Provides access to underlying store implementation for management
     * purposes only.
     */
    public Store getStore() {
        return store;
    }

    // Implementation note: Assumes that the underlying Store, like TDB, has
    // per-thread transaction status so transaction state and corresponding
    // writeLocked local are on same thread.

    /**
     * Lock the store for an update transaction
     */
    public void lock() {
        store.lockWrite();
        writeLocked.set(true);
    }

    /**
     * Commit the update transaction
     */
    public void commit() {
        store.commit();
    }

    /**
     * Finish the update transaction, if commit has not been called then abort
     * the transaction
     */
    public void end() {
        store.end();
        writeLocked.set(false);
    }

    /**
     * Check that the store is locked for update
     */
    protected void checkWriteLocked() {
        if (!writeLocked.get()) {
            throw new EpiException("Failed to lock store before updating");
        }
    }

    /**
     * Lock the store for reading. If this thread is already in a write
     * transaction then do nothing. Otherwise maintain a count of read lock nesting level
     */
    @Override
    public synchronized void lockStoreRead() {
        if (!writeLocked.get()) {
            if (readLockCount.getAndIncrement() == 0) {
                store.lock();
            }
        }
    }

    /**
     * Unlock the store for reading. If this thread is in a write transaction
     * then do nothing
     */
    @Override
    public void unlockStoreRead() {
        if (!writeLocked.get()) {
            if (readLockCount.decrementAndGet() == 0) {
                store.end();
            }
        }
    }

    @Override
    public void storeGraph(String graphURI, Model model) {
        checkWriteLocked();
        storeLockedGraph(graphURI, model);
    }

    @Override
    public Model getGraph(String graphURI) {
        lockStoreRead();
        try {
            Model result = ModelFactory.createDefaultModel();
            result.add(store.asDataset().getNamedModel(graphURI));
            return result;
        } finally {
            unlockStoreRead();
        }
    }

    @Override
    public Description getDescription(String uri) {
        lockStoreRead();
        try {
            Description d = asDescription(describe(uri, null));
            return d;
        } finally {
            unlockStoreRead();
        }
    }

    // Assumes store is locked
    private Resource describe(String uri, Model dest) {
        Resource r = getDefaultModel().createResource(uri);
        if (dest == null) {
            dest = Closure.closure(r, false);
        } else {
            Closure.closure(r, false, dest);
        }
        return r.inModel(dest);
    }

    protected Model getDefaultModel() {
        return store.asDataset().getDefaultModel();
    }

    protected Description asDescription(Resource root) {
        return Description.descriptionFrom(root, this);
    }

    @Override
    public Description getCurrentVersion(String uri) {
        lockStoreRead();
        try {
            Description d = asDescription(doGetCurrentVersion(uri, null));
            return d;
        } finally {
            unlockStoreRead();
        }
    }

    protected Resource doGetCurrentVersion(String uri, Model dest) {
        Resource root = describe(uri, dest);
        Resource version = root
                .getPropertyResourceValue(Version.currentVersion);
        if (version != null) {
            Closure.closure(mod(version), false, root.getModel());
            VersionUtil.flatten(root, version);
        }
        return root;
    }

    @Override
    public Description getVersion(String uri, boolean withEntity) {
        lockStoreRead();
        try {
            Description d = doGetVersion(uri, true);
            if (d instanceof RegisterItem && withEntity) {
                doGetEntity((RegisterItem) d, null, false);
            }
            return d;
        } finally {
            unlockStoreRead();
        }
    }

    protected Description doGetVersion(String uri, boolean flatten) {
        Resource version = describe(uri, null);
        Resource root = version.getPropertyResourceValue(DCTerms.isVersionOf);
        if (root != null) {
            Closure.closure(mod(root), false, version.getModel());
            if (flatten)
                VersionUtil.flatten(root, version);
        } else {
            throw new EpiException(
                    "Version requested on resource with no isVersionOf root");
        }
        return asDescription(root);
    }

    @Override
    public Description getVersionAt(String uri, long time) {
        lockStoreRead();
        try {
            RDFNode version = selectFirstVar("version", getDefaultModel(),
                    VERSION_AT_QUERY, Prefixes.getDefault(), "root",
                    ResourceFactory.createResource(uri), "time",
                    RDFUtil.fromDateTime(time));
            if (version != null && version.isURIResource()) {
                return doGetVersion(version.asResource().getURI(), true);
            } else {
                return null;
            }
        } finally {
            unlockStoreRead();
        }
    }

    static String VERSION_AT_QUERY = "SELECT ?version WHERE \n"
            + "{  \n"
            + "    ?version dct:isVersionOf ?root; \n"
            + "             version:interval [ time:hasBeginning [time:inXSDDateTime ?start] ]. \n"
            + "    FILTER (?start <= ?time) \n"
            + "    FILTER NOT EXISTS { \n"
            + "        ?version version:interval [ time:hasEnd [time:inXSDDateTime ?end] ]. \n"
            + "        FILTER (?end <= ?time) \n" + "    } \n" + "} \n";

    @Override
    public List<VersionInfo> listVersions(String uri) {
        lockStoreRead();
        try {
            ResultSet rs = selectAll(getDefaultModel(), VERSION_LIST_QUERY,
                    Prefixes.getDefault(),
                    createBindings("root", ResourceFactory.createResource(uri)));
            List<VersionInfo> results = new ArrayList<VersionInfo>();
            while (rs.hasNext()) {
                QuerySolution soln = rs.next();
                VersionInfo vi = new VersionInfo(soln.getResource("version"),
                        soln.getLiteral("info"), soln.getLiteral("from"),
                        soln.getLiteral("to"));
                Resource replaces = soln.getResource("replaces");
                if (replaces != null) {
                    vi.setReplaces(replaces.getURI());
                }
                results.add(vi);
            }
            return results;
        } finally {
            unlockStoreRead();
        }
    }

    static String VERSION_LIST_QUERY = "SELECT ?version ?info ?from ?to ?replaces WHERE \n"
            + "{  \n"
            + "    ?version dct:isVersionOf ?root; \n"
            + "             owl:versionInfo ?info . \n"
            + "   OPTIONAL {?version version:interval [time:hasBeginning [time:inXSDDateTime ?from]].} \n"
            + "   OPTIONAL {?version version:interval [time:hasEnd [time:inXSDDateTime ?to]].} \n"
            + "   OPTIONAL {?version dct:replaces ?replaces.} \n"
            + "} ORDER BY ?info \n";

    @Override
    public long versionStartedAt(String uri) {
        lockStoreRead();
        try {
            Resource root = getDefaultModel().getResource(uri);
            Resource interval = root.getPropertyResourceValue(Version.interval);
            if (interval == null)
                return -1;
            return RDFUtil.asTimestamp(interval
                    .getPropertyResourceValue(Time.hasBeginning)
                    .getProperty(Time.inXSDDateTime).getObject());
        } finally {
            unlockStoreRead();
        }
    }

    @Override
    public List<EntityInfo> listEntityOccurences(String uri) {
        Resource entity = ResourceFactory.createResource(uri);
        lockStoreRead();
        try {
            ResultSet matches = QueryUtil.selectAll(getDefaultModel(),
                    ENTITY_FIND_QUERY, Prefixes.getDefault(), "entity", entity);
            List<EntityInfo> results = new ArrayList<EntityInfo>();
            while (matches.hasNext()) {
                QuerySolution soln = matches.next();
                results.add(new EntityInfo(entity, soln.getResource("item"),
                        soln.getResource("register"), soln
                                .getResource("status")));
            }
            return results;
        } finally {
            unlockStoreRead();
        }
    }

    static String ENTITY_FIND_QUERY = "SELECT * WHERE { "
            + "?item reg:register ?register; "
            + "      version:currentVersion ?itemVer . "
            + "?itemVer reg:status ?status; "
            + "         reg:definition [reg:entity ?entity] . " + "}";

    @Override
    public RegisterItem getItem(String uri, boolean withEntity) {
        lockStoreRead();
        try {
            Resource root = doGetCurrentVersion(uri, null);
            if (!root.hasProperty(RDF.type, RegistryVocab.RegisterItem)) {
                return null;
            }
            RegisterItem item = new RegisterItem(root);
            if (withEntity) {
                doGetEntity(item, null, true);
            }
            return item;
        } finally {
            unlockStoreRead();
        }
    }

    protected Resource doGetEntity(RegisterItem item, Model dest,
            boolean getCurrent) {
        Resource root = item.getRoot();
        if (dest == null) {
            dest = ModelFactory.createDefaultModel();
        }
        Resource entityRef = root
                .getPropertyResourceValue(RegistryVocab.definition);
        if (entityRef != null) {
            Resource entity = entityRef
                    .getPropertyResourceValue(RegistryVocab.entity);
            Resource srcGraph = entityRef
                    .getPropertyResourceValue(RegistryVocab.sourceGraph);
            if (srcGraph != null) {
                dest.add(store.asDataset().getNamedModel(srcGraph.getURI()));
                entity = entity.inModel(dest);
            } else {
                // Occurs for versioned things i.e. Registers
                if (getCurrent) {
                    entity = doGetCurrentVersion(entity.getURI(), dest);
                } else {
                    Resource regversion = entityRef
                            .getPropertyResourceValue(RegistryVocab.entityVersion);
                    if (regversion != null) {
                        entity = doGetVersion(regversion.getURI(), true)
                                .getRoot();
                    } else {
                        throw new EpiException(
                                "Illegal state of item for a Resgister, could not find entityVersion: "
                                        + root);
                    }
                }
            }
            item.setEntity(entity);
            return entity;
        } else {
            log.warn("Item requested had no entity reference: " + root);
            return null;
        }
    }

    @Override
    public Resource getEntity(RegisterItem item) {
        lockStoreRead();
        try {
            return doGetEntity(item, null, true);
        } finally {
            unlockStoreRead();
        }
    }

    @Override
    public List<RegisterItem> fetchAll(List<String> itemURIs, boolean withEntity) {
        lockStoreRead();
        try {
            Model shared = ModelFactory.createDefaultModel();
            List<RegisterItem> results = new ArrayList<RegisterItem>(
                    itemURIs.size());
            for (String uri : itemURIs) {
                Resource itemRoot = doGetCurrentVersion(uri, shared);
                RegisterItem item = new RegisterItem(itemRoot);
                if (withEntity) {
                    doGetEntity(item, shared, true);
                }
                results.add(item);
            }
            return results;
        } finally {
            unlockStoreRead();
        }
    }

    // public List<RegisterItem> doFetchMembers(Register register, boolean
    // withEntity) {
    // lockStore();
    // try {
    // // A shared model would save having 2N models around but makes filtering
    // painful
    // // Model shared = ModelFactory.createDefaultModel();
    // List<RDFNode> items = selectAllVar("ri", getDefaultModel(),
    // REGISTER_ENUM_QUERY, Prefixes.getDefault(),
    // "register", register.getRoot() );
    // List<RegisterItem> results = new ArrayList<RegisterItem>(items.size());
    // for ( RDFNode itemR : items) {
    // if (itemR.isURIResource()) {
    // Resource itemRoot = doGetCurrentVersion(itemR.asResource().getURI(),
    // null);
    // RegisterItem item = new RegisterItem(itemRoot);
    // doGetEntity(item, null);
    // results.add(item);
    // } else {
    // throw new EpiException("Found item which isn't a resource");
    // }
    // }
    // return results;
    // } finally {
    // unlockStore();
    // }
    // }
    // static String REGISTER_ENUM_QUERY =
    // "SELECT ?ri WHERE { ?ri reg:register ?register. }";

    @Override
    public List<RegisterEntryInfo> listMembers(Register register) {
        lockStoreRead();
        try {
            ResultSet rs = selectAll(getDefaultModel(), REGISTER_LIST_QUERY,
                    Prefixes.getDefault(),
                    createBindings("register", register.getRoot()));
            List<RegisterEntryInfo> results = new ArrayList<RegisterEntryInfo>();
            Resource priorItem = null;
            RegisterEntryInfo prior = null;
            while (rs.hasNext()) {
                QuerySolution soln = rs.next();
                Resource item = soln.getResource("item");
                if (item.equals(priorItem)) {
                    prior.addLabel(soln.getLiteral("label"));
                    prior.addType(soln.getResource("type"));
                } else {
                    prior = new RegisterEntryInfo(soln.getResource("status"),
                            item, soln.getResource("entity"),
                            soln.getLiteral("label"), soln.getResource("type"),
                            soln.getLiteral("notation"));
                    priorItem = item;
                    results.add(prior);
                }
            }
            return results;
        } finally {
            unlockStoreRead();
        }
    }

    static String REGISTER_LIST_QUERY = "SELECT * WHERE { "
            + "?item reg:register ?register; "
            + "      version:currentVersion ?itemVer; "
            + "      reg:notation ?notation; " + "      reg:itemClass ?type ."
            + "?itemVer reg:status ?status; "
            + "         reg:definition [reg:entity ?entity]; "
            + "         rdfs:label ?label . " + "} ORDER BY ?notation";

    @Override
    public boolean contains(Register register, String notation) {
        lockStoreRead();
        try {
            String base = RDFUtil.getNamespace(register.getRoot());
            Resource ri = ResourceFactory.createResource(base + "_" + notation);
            return getDefaultModel().contains(ri, RDF.type,
                    RegistryVocab.RegisterItem);
        } finally {
            unlockStoreRead();
        }
    }

    @Override
    public void loadBootstrap(String filename) {
        Model bootmodel = FileManager.get().loadModel(filename); // Load first
                                                                 // in case of
                                                                 // errors
        lock();
        try {
            getDefaultModel().add(bootmodel);
            commit();
        } finally {
            end();
        }
    }

    @Override
    public void addToRegister(Register register, RegisterItem item) {
        addToRegister(register, item, Calendar.getInstance());
    }

    @Override
    public void addToRegister(Register register, RegisterItem item, Calendar now) {
        checkWriteLocked();
        doUpdateItem(item, true, now);
        // Don't automatically update register, we want the option to do batch
        // updates
        // doUpdate(register.getRoot(), now);
        mod(item).addProperty(RegistryVocab.register, register.getRoot());
        Resource entity = item.getEntity();
        if (entity.hasProperty(RDF.type, RegistryVocab.Register)) {
            modCurrent(register).addProperty(RegistryVocab.subregister, entity);
        }
        // Don't automatically update register, we want the option to do batch
        // updates
        // modCurrent(register).removeAll(DCTerms.modified).addProperty(DCTerms.modified,
        // getDefaultModel().createTypedLiteral(now));
    }

    private Resource modCurrent(Description d) {
        Resource r = d.getRoot().inModel(getDefaultModel());
        Resource current = r.getPropertyResourceValue(Version.currentVersion);
        return current == null ? r : current;
    }

    private Resource mod(Description d) {
        return mod(d.getRoot());
    }

    private Resource mod(Resource r) {
        return r.inModel(getDefaultModel());
    }

    @Override
    public String update(Register register, Calendar timestamp) {
        checkWriteLocked();
        return doUpdateRegister(register.getRoot(), timestamp).getURI();
    }

    @Override
    public String update(Register register) {
        return update(register, Calendar.getInstance());
    }

    protected Resource doUpdateRegister(Resource root, Calendar cal,
            Property... rigids) {
        root.removeAll(RegistryVocab.subregister);
        Resource current = getDefaultModel().getResource(root.getURI());
        Resource temp = current
                .getPropertyResourceValue(Version.currentVersion);
        if (temp != null) {
            current = temp;
        }
        // doUpdate call will need current versionInfo to be able to allocate
        // next version correctly
        RDFUtil.copyProperty(current, root, OWL.versionInfo);
        // Preserve subregister - could this just be added to rigids
        RDFUtil.copyProperty(current, root, RegistryVocab.subregister);
        root.removeAll(DCTerms.modified).addProperty(DCTerms.modified,
                getDefaultModel().createTypedLiteral(cal));
        return doUpdate(root, cal, rigids);
    }

    protected Resource doUpdate(Resource root, Calendar cal, Property... rigids) {
        Resource newVersion = VersionUtil.nextVersion(root, cal, rigids);
        Model st = getDefaultModel();
        root.inModel(st).removeAll(OWL.versionInfo)
                .removeAll(Version.currentVersion);
        st.add(newVersion.getModel());
        return newVersion.inModel(st);
    }

    // protected void doIndex(Resource root, String graph) {
    // if (indexer != null) {
    // // If use updateGraph then get one entry per entity, which is much more
    // efficient
    // // However, then can't search on older names for entities which have been
    // updated
    // if (Registry.TEXT_INDEX_INCLUDES_HISTORY) {
    // indexer.addGraph(graph, root.getModel());
    // } else {
    // indexer.updateGraph(graph, root.getModel());
    // }
    // }
    // }

    @Override
    public String update(RegisterItem item, boolean withEntity,
            Calendar timestamp) {
        checkWriteLocked();
        return doUpdateItem(item, withEntity, timestamp);
    }

    @Override
    public String update(RegisterItem item, boolean withEntity) {
        return update(item, withEntity, Calendar.getInstance());
    }

    private String doUpdateItem(RegisterItem item, boolean withEntity,
            Calendar now) {
        Model storeModel = getDefaultModel();
        Resource oldVersion = mod(item).getPropertyResourceValue(
                Version.currentVersion);

        Resource newVersion = doUpdate(item.getRoot(), now,
                RegisterItem.RIGID_PROPS);

        if (withEntity) {
            Resource entity = item.getEntity();
            Resource entityRef = newVersion
                    .getPropertyResourceValue(RegistryVocab.definition);
            if (entityRef == null) {
                entityRef = storeModel.createResource();
                entityRef.addProperty(RegistryVocab.entity, entity);
                newVersion.addProperty(RegistryVocab.definition, entityRef);
            } else {
                Resource oldEntity = entityRef
                        .getPropertyResourceValue(RegistryVocab.entity);
                if (!entity.equals(oldEntity)) {
                    // Legality checks must be performed by API not by the store
                    // layer
                    entityRef.removeAll(RegistryVocab.entity);
                    entityRef.addProperty(RegistryVocab.entity, entity);
                }
            }
            if (entity.hasProperty(RDF.type, RegistryVocab.Register)) {
                doUpdateRegister(entity, now);
                Resource currentRegVersion = entity.inModel(storeModel)
                        .getPropertyResourceValue(Version.currentVersion);
                if (currentRegVersion != null) {
                    entityRef.removeAll(RegistryVocab.entityVersion);
                    entityRef.addProperty(RegistryVocab.entityVersion,
                            currentRegVersion);
                } else {
                    log.warn("Possible internal inconsistency. No version information available for register: "
                            + entity);
                }
            } else {
                if (oldVersion != null) {
                    if (!removeGraphFor(oldVersion)) {
                        log.error("Could not find graph for old version of an updated entity");
                    }
                }
                String graphURI = newVersion.getURI() + "#graph";
                Model entityModel = null;
                if (entity.getModel().contains(null, RDF.type,
                        RegistryVocab.RegisterItem)) {
                    // register item mixed in with entity graph, separate out
                    // for storage
                    log.debug("Mixed entity and item");
                    entityModel = Closure.closure(entity, false);
                } else {
                    entityModel = entity.getModel();
                }
                storeLockedGraph(graphURI, entityModel);

                if (!item.isGraph()) {
                    getDefaultModel().add(entityModel);
                }
                Resource graph = ResourceFactory.createResource(graphURI);
                entityRef.removeAll(RegistryVocab.sourceGraph);
                entityRef.addProperty(RegistryVocab.sourceGraph, graph);
            }
        }
        return newVersion.getURI();
    }

    private void storeLockedGraph(String graphURI, Model entityModel) {
        // Avoids Store.updateGraph because we are already in a transaction
        Model graphModel = store.asDataset().getNamedModel(graphURI);
        if (graphModel == null) {
            // Probably an in-memory test store
            graphModel = ModelFactory.createDefaultModel();
            store.asDataset().addNamedModel(graphURI, graphModel);
        } else {
            graphModel.removeAll();
        }
        graphModel.add(entityModel);
    }

    private boolean removeGraphFor(Resource oldVersion) {
        Model st = getDefaultModel();
        Resource definition = mod(oldVersion).getPropertyResourceValue(
                RegistryVocab.definition);
        if (definition != null) {
            Resource graph = definition
                    .getPropertyResourceValue(RegistryVocab.sourceGraph);
            if (graph != null) {
                st.remove(store.asDataset().getNamedModel(graph.getURI()));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> search(SearchRequest request) {
        try {
            // SelectBuilder doesn't support property paths so use brute force
            // string bashing
            StringBuffer query = new StringBuffer();
            query.append("PREFIX text: <http://jena.apache.org/text#>\n");
            query.append(String.format("PREFIX reg:     <%s>\n",
                    RegistryVocab.getURI()));
            query.append(String.format("PREFIX version: <%s>\n",
                    Version.getURI()));
            query.append(String.format("PREFIX dct:     <%s>\n",
                    DCTerms.getURI()));
            query.append(String.format("PREFIX rdfs:    <%s>\n", RDFS.getURI()));
            query.append("\nSELECT DISTINCT ?item WHERE {\n");
            query.append(String.format("    ?entity text:query '%s' . \n",
                    safeLiteral(request.getQuery())));

            if (request.isSearchVersions()) {
                query.append("    ?item ^dct:versionOf/reg:definition/reg:entity ?entity.\n");
            } else {
                query.append("    ?item version:currentVersion/reg:definition/reg:entity/version:currentVersion? ?entity.\n");
            }

            if (request.getStatus() != null) {
                String statusRef = "reg:status"
                        + StringUtils.capitalize(request.getStatus());
                query.append(String.format(
                        "   ?item version:currentVersion/reg:status %s.\n",
                        statusRef));
            }

            for (KeyValuePair filter : request.getFilters()) {
                String value = filter.value;
                if (value.startsWith("http://") || value.startsWith("https://")) {
                    query.append(String.format("    ?entity <%s> <%s>.\n",
                            filter.key, safeURI(value)));
                } else {
                    query.append(String.format("    ?entity <%s> '%s'.\n",
                            filter.key, safeLiteral(value)));
                }
            }
            query.append("} ");

            if (request.getLimit() != null) {
                query.append("LIMIT " + request.getLimit() + " ");
            }

            if (request.getOffset() != null) {
                query.append("OFFSET " + request.getOffset());
            }

            // log.debug("Search query = " + query.toString());

            lockStoreRead();
            try {
                // DEBUG
                QueryExecution exec = QueryExecutionFactory.create(
                        query.toString(), store.asDataset());
                try {
                    ResultSet results = exec.execSelect();
                    List<String> matches = new ArrayList<String>();
                    while (results.hasNext()) {
                        QuerySolution row = results.next();
                        matches.add(row.getResource("item").getURI());
                    }
                    return matches;
                } finally {
                    exec.close();
                }
            } finally {
                unlockStoreRead();
            }

        } catch (Exception e) {
            log.error("Search query failed, misconfigured store?", e);
            return Collections.emptyList();
        }
    }

    private String safeLiteral(String value) {
        return value.replace("'", "\\'");
    }

    private String safeURI(String value) {
        return value.replace("<", "&lt;");
    }

    // Debug support only
    public void dump() {
        lockStoreRead();
        try {
            getDefaultModel().write(System.out, "Turtle");
        } finally {
            unlockStoreRead();
        }
    }

    @Override
    public List<ForwardingRecord> listDelegations() {
        List<ForwardingRecord> results = new ArrayList<>();
        lockStoreRead();
        try {
            Model m = getDefaultModel();
            ResultSet rs = QueryUtil.selectAll(m, DELEGATION_LIST_QUERY,
                    Prefixes.getDefault());
            while (rs.hasNext()) {
                QuerySolution soln = rs.nextSolution();
                try {
                    Status status = Status.forResource(soln
                            .getResource("status"));
                    if (status.isAccepted()) {
                        Resource record = soln.getResource("record").inModel(m);
                        ForwardingRecord.Type type = Type.FORWARD;
                        if (record.hasProperty(RDF.type,
                                RegistryVocab.FederatedRegister)) {
                            type = Type.FEDERATE;
                        } else if (record.hasProperty(RDF.type,
                                RegistryVocab.DelegatedRegister)) {
                            type = Type.DELEGATE;
                        }
                        String target = soln.getResource("target").getURI();
                        ForwardingRecord fr = null;
                        if (type == Type.DELEGATE) {
                            DelegationRecord dr = new DelegationRecord(
                                    record.getURI(), target, type);
                            Resource s = soln.getResource("subject");
                            if (s != null)
                                dr.setSubject(s);
                            Resource p = soln.getResource("predicate");
                            if (p != null)
                                dr.setPredicate(p);
                            Resource o = soln.getResource("object");
                            if (o != null)
                                dr.setObject(o);
                            fr = dr;
                        } else {
                            fr = new ForwardingRecord(record.getURI(), target,
                                    type);
                            Literal code = soln.getLiteral("code");
                            if (code != null) {
                                fr.setForwardingCode(code.getInt());
                            }
                        }
                        results.add(fr);
                    }
                } catch (Exception e) {
                    log.error(
                            "Bad delegation record for " + soln.get("record"),
                            e);
                }
            }
            return results;
        } finally {
            unlockStoreRead();
        }
    }

    static String DELEGATION_LIST_QUERY = "SELECT * WHERE {"
            + "   { "
            + // DelegatedRegister case, registers are managed, hence additional
              // versioning
            "      ?record a reg:DelegatedRegister ; version:currentVersion ?ver ."
            + "      ?ver reg:delegationTarget ?target. "
            + "      ?item reg:status ?status; reg:definition [reg:entity ?record] . "
            + "      [] version:currentVersion ?item. "
            + "      OPTIONAL {?ver reg:forwardingCode ?code. } "
            + "      OPTIONAL {?ver reg:enumerationSubject ?subject. } "
            + "      OPTIONAL {?ver reg:enumerationPredicate ?predicate. } "
            + "      OPTIONAL {?ver reg:enumerationObject ?object. } "
            + "   } UNION {"
            + // Typical entity case
            "      ?record a reg:Delegated ; reg:delegationTarget ?target. "
            + "      ?item reg:status ?status; reg:definition [reg:entity ?record] . "
            + "      [] version:currentVersion ?item. "
            + "      OPTIONAL {?record reg:forwardingCode ?code. } "
            + "      OPTIONAL {?record reg:enumerationSubject ?subject. } "
            + "      OPTIONAL {?record reg:enumerationPredicate ?predicate. } "
            + "      OPTIONAL {?record reg:enumerationObject ?object. } "
            + "   } " + "}";

    @Override
    public ResultSet query(String query) {
        lockStoreRead();
        try {
            QueryExecution exec = QueryExecutionFactory.create(query,
                    store.asDataset());
            try {
                return ResultSetFactory.copyResults(exec.execSelect());
            } finally {
                exec.close();
            }
        } finally {
            unlockStoreRead();
        }
    }

    @Override
    public void delete(String uri) {
        delete( asItem(uri) );
    }
    
    private RegisterItem asItem(String uri) {
        String id = NameUtils.splitAfterLast(uri, "/");
        if ( ! id.startsWith("_") ) {
            // Ensure we start with an item URI
            uri = NameUtils.splitBeforeLast(uri, "/") + "/_" + id;
        }
        return getCurrentVersion(uri).asRegisterItem();
    }
    
    protected void delete(RegisterItem item) {
        checkWriteLocked();
        if ( item.isRegister() ) {
            Register register = item.getAsRegister(this);
            for (RegisterEntryInfo ei : listMembers(register )) {
                delete( getCurrentVersion(ei.getItemURI()).asRegisterItem() );
            }
        }
    
        Model toDelete = ModelFactory.createDefaultModel();
        Collection<Resource>graphs = scanAllVersions(item.getRoot(), toModel(toDelete));
        getDefaultModel().remove(toDelete);
        for (Resource graph : graphs) {
            store.asDataset().removeNamedModel( graph.getURI() );
        }
    }
    
    private StreamRDF toModel(final Model model) {
        return new StreamRDF() {
            @Override public void triple(Triple triple) { model.add( model.asStatement(triple) ); }
            @Override public void start() { }
            @Override public void quad(Quad quad) { throw new UnsupportedOperationException();  }
            @Override public void prefix(String prefix, String iri) {  model.setNsPrefix(prefix, iri); }
            @Override public void finish() {}
            @Override public void base(String base) { throw new UnsupportedOperationException(); }
        };
    }
    
    /**
     * Scan the tree streaming all the triples in the default graph to the given stream
     * and return a collection of the associated named graphs (entity definitions and annotations)
     */
    private Collection<Resource> scanAllVersions(Resource root, StreamRDF stream) {
        return scanAllVersions(root, stream, new HashSet<Resource>());
    }
    
    private Collection<Resource> scanAllVersions(Resource root, StreamRDF stream, Collection<Resource> sofar) {
        addRefClosure(stream, root);
        for (Resource version : getDefaultModel().listSubjectsWithProperty(DCTerms.isVersionOf, root).toList()) {
            addRefClosure(stream, version);
            addRefClosure(stream, getResourceValue(version, Version.interval));
            Resource definition = getResourceValue(version, RegistryVocab.definition);
            if (definition != null) {
                Resource graph = getResourceValue(definition, RegistryVocab.sourceGraph);
                if (graph != null) {
                    sofar.add(graph);
                }
                Resource entity = getResourceValue(definition, RegistryVocab.entity);
                if (entity != null) {
                    scanAllVersions(entity, stream, sofar);
                }
            }
            Resource graph = getResourceValue(version, RegistryVocab.annotation);
            if (graph != null) {
                sofar.add(graph);
            }
        }
        return sofar;
    }
    
    private void addRefClosure(StreamRDF accumulator, Resource root) {
        if (root == null) return;
        emitAll(accumulator, getDefaultModel().listStatements(null, null, root) );
        emitAll(accumulator, Closure.closure(root.inModel(getDefaultModel()), false).listStatements() );
    }
    
    private void emitAll(StreamRDF stream, StmtIterator si) {
        while (si.hasNext()) {
            stream.triple( si.nextStatement().asTriple() );
        }
    }
    
    private Resource getResourceValue( Resource root, Property prop) {
        NodeIterator ni = getDefaultModel().listObjectsOfProperty(root, prop);
        if (ni.hasNext()) {
            return ni.next().asResource();
        } else {
            return null;
        }
    }
    
    @Override
    public void exportTree(String uri, StreamRDF out) {
        RegisterItem item = asItem(uri);
        lockStoreRead();
        out.start();
        try {
            doExportTree(item, out);
        } finally {
            unlockStoreRead();
            out.finish();
        }
    }
    
    private void doExportTree(RegisterItem item, StreamRDF out) {
        if ( item.isRegister() ) {
            Register register = item.getAsRegister(this);
            for (RegisterEntryInfo ei : listMembers(register )) {
                doExportTree( getCurrentVersion(ei.getItemURI()).asRegisterItem(), out );
            }
        }
        
        Collection<Resource> graphs = scanAllVersions(item.getRoot(), out);
        for (Resource g : graphs) {
            Iterator<Quad> i = store.asDataset().asDatasetGraph().findNG(g.asNode(), Node.ANY, Node.ANY, Node.ANY);
            while (i.hasNext()){
                out.quad( i.next() );
            }
        }
    }
    
    @Override
    public void importTree(String uri, StreamRDF in) {
        checkWriteLocked();
        RegisterItem item = asItem(uri);
        delete(item);
        // TODO implement import!
    }
}
