/******************************************************************
 * File:        StoreBaseImpl.java
 * Created by:  Dave Reynolds
 * Created on:  27 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import static com.epimorphics.rdfutil.QueryUtil.createBindings;
import static com.epimorphics.rdfutil.QueryUtil.selectAll;
import static com.epimorphics.rdfutil.QueryUtil.selectAllVar;
import static com.epimorphics.rdfutil.QueryUtil.selectFirstVar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.util.VersionUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.server.core.Indexer;
import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.core.Store;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Implementation of the store API which uses a triple store for persistence
 * and indexes all registered items as they are registered. In this version
 * all current data is held in the default graph and named graphs are used for
 * each version of each defined entity.
 * <p>
 * This implementation uses use the ServiceConfig machinery to specify the
 * store to be used ("store" parameter) and the (optional) indexer ("indexer" parameter).
 * The configured store should <strong>not</strong> use union-default.
 * </p>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class StoreBaseImpl extends ServiceBase implements StoreAPI, Service {
    static final Logger log = LoggerFactory.getLogger( StoreBaseImpl.class );

    public static final String STORE_PARAMETER = "store";
    public static final String INDEXER_PARAMETER = "indexer";

    protected Store store;
    protected Indexer indexer;
    protected Map<String, Lock> locks = new WeakHashMap<String, Lock>();

    @Override
    public void postInit() {
        store = getNamedService( getRequiredParam(STORE_PARAMETER), Store.class);
        String indexerName = config.get(INDEXER_PARAMETER);
        if (indexerName != null) {
            indexer = getNamedService(indexerName, Indexer.class);
        }
    }

    @Override
    public Description getDescription(String uri) {
        store.lock();
        try {
            Description d = asDescription( describe(uri, null) );
            return d;
        } finally {
            store.unlock();
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
        if (root.hasProperty(RDF.type)) {
            if (root.hasProperty( RDF.type, RegistryVocab.Register)) {
                return new Register(root);
            } else if (root.hasProperty( RDF.type, RegistryVocab.RegisterItem)) {
                return new RegisterItem(root);
            } else {
                return new Description(root);
            }
        } else {
            return null;
        }

    }

    public synchronized void lock(String uri) {
        Lock lock = locks.get(uri);
        if (lock == null) {
            lock = new ReentrantLock();
            locks.put(uri, lock);
        }
        lock.lock();
    }

    /**
     * Release the "forupdate" lock on the given URI (should be a Register or RegisterItem).
     * Throws an error if there is no such lock.
     */
    public synchronized void unlock(String uri) {
        Lock lock = locks.get(uri);
        if (lock == null) {
            throw new EpiException("Internal error: tried to unlock a resource which was not locked for update");
        }
        lock.unlock();
    }

    @Override
    public Description getCurrentVersion(String uri) {
        store.lock();
        try {
            Description d = asDescription( doGetCurrentVersion(uri, true, null) );
            return d;
        } finally {
            store.unlock();
        }
    }

    protected Resource doGetCurrentVersion(String uri, boolean flatten, Model dest) {
        Resource root = describe(uri, dest);
        Resource version = root.getPropertyResourceValue(Version.currentVersion);
        if (version != null) {
            Closure.closure(mod(version), false, root.getModel());
            if (flatten) VersionUtil.flatten(root, version);
        }
        return root;
    }

    @Override
    public Description getVersion(String uri) {
        store.lock();
        try {
            return doGetVersion(uri, true);
        } finally {
            store.unlock();
        }
    }

    protected Description doGetVersion(String uri, boolean flatten) {
        Resource version = describe(uri, null);
        Resource root = version.getPropertyResourceValue(DCTerms.isVersionOf);
        if (root != null) {
            Closure.closure(mod(root), false, version.getModel());
            if (flatten) VersionUtil.flatten(root, version);
        } else {
            throw new EpiException("Version requested on resource with no isVersionOf root");
        }
        return asDescription(root);
    }

    @Override
    public Description getVersionAt(String uri, long time) {
        store.lock();
        try {
            RDFNode version = selectFirstVar("version", getDefaultModel(), VERSION_AT_QUERY, Prefixes.get(),
                    "root", ResourceFactory.createResource(uri), "time", RDFUtil.fromDateTime(time) );
            if (version != null && version.isURIResource()) {
                return doGetVersion( version.asResource().getURI(), true );
            } else {
                return null;
            }
        } finally {
            store.unlock();
        }
    }
    static String VERSION_AT_QUERY =
                    "SELECT ?version WHERE \n" +
                    "{  \n" +
                    "    ?version dct:isVersionOf ?root; \n" +
                    "             version:interval [ time:hasBeginning [time:inXSDDateTime ?start] ]. \n" +
                    "    FILTER (?start <= ?time) \n" +
                    "    FILTER NOT EXISTS { \n" +
                    "        ?version version:interval [ time:hasEnd [time:inXSDDateTime ?end] ]. \n" +
                    "        FILTER (?end <= ?time) \n" +
                    "    } \n" +
                    "} \n";


    @Override
    public List<VersionInfo> listVersions(String uri) {
        store.lock();
        try {
            ResultSet rs = selectAll(getDefaultModel(), VERSION_LIST_QUERY, Prefixes.get(),
                createBindings("root", ResourceFactory.createResource(uri)));
            List<VersionInfo> results = new ArrayList<VersionInfo>();
            while (rs.hasNext()) {
                QuerySolution soln = rs.next();
                results.add(
                        new VersionInfo(
                                soln.getResource("version"),
                                soln.getLiteral("info"),
                                soln.getLiteral("from"),
                                soln.getLiteral("to")      ));
            }
            return results;
        } finally {
            store.unlock();
        }
    }
    static String VERSION_LIST_QUERY =
            "SELECT ?version ?info ?from ?to WHERE \n" +
            "{  \n" +
            "    ?version dct:isVersionOf ?root; \n" +
            "             owl:versionInfo ?info . \n" +
            "   OPTIONAL {?version version:interval [time:hasBeginning [time:inXSDDateTime ?from]].} \n" +
            "   OPTIONAL {?version version:interval [time:hasEnd [time:inXSDDateTime ?to]].} \n" +
            "} ORDER BY ?info \n";

    @Override
    public RegisterItem getItem(String uri, boolean withEntity) {
        return doGetItem(uri, withEntity, true);
    }

    public RegisterItem getItemWithVersion(String uri, boolean withEntity) {
        return doGetItem(uri, withEntity, false);
    }

    private RegisterItem doGetItem(String uri, boolean withEntity, boolean flatten) {
        store.lock();
        try {
            Resource root = doGetCurrentVersion(uri, flatten, null);
            if (! root.hasProperty(RDF.type, RegistryVocab.RegisterItem)) {
                return null;
            }
            RegisterItem item = new RegisterItem(root);
            if (withEntity) {
                doGetEntity(item, flatten, null);
            }
            return item;
        } finally {
            store.unlock();
        }
    }
    protected Resource doGetEntity(RegisterItem item, boolean flatten, Model dest) {
        Resource root = item.getRoot();
        if (dest == null) {
            dest = ModelFactory.createDefaultModel();
        }
        Resource entityRef = root.getPropertyResourceValue(RegistryVocab.definition);
        if (entityRef != null) {
            Resource entity = entityRef.getPropertyResourceValue(RegistryVocab.entity);
            Resource srcGraph = entityRef.getPropertyResourceValue(RegistryVocab.sourceGraph);
            if (srcGraph != null) {
                dest.add( store.asDataset().getNamedModel(srcGraph.getURI()) );
                entity = entity.inModel(dest);
            } else {
                entity = doGetCurrentVersion( entity.getURI(), flatten, dest );
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
        store.lock();
        try {
            return doGetEntity(item, true, null);
        } finally {
            store.unlock();
        }
    }

    @Override
    public  List<RegisterItem>  fetchMembers(Register register, boolean withEntity) {
        return doFetchMembers(register, withEntity, true);
    }

    @Override
    public  List<RegisterItem>  fetchMembersWithVersion(Register register, boolean withEntity) {
        return doFetchMembers(register, withEntity, false);
    }

    public  List<RegisterItem>  doFetchMembers(Register register, boolean withEntity, boolean flatten) {
        store.lock();
        try {
            // A shared model would save having 2N models around but makes filtering painful
//            Model shared = ModelFactory.createDefaultModel();
            List<RDFNode> items = selectAllVar("ri", getDefaultModel(), REGISTER_ENUM_QUERY, Prefixes.get(),
                    "register", register.getRoot() );
            List<RegisterItem> results = new ArrayList<RegisterItem>(items.size());
            for ( RDFNode itemR : items) {
                if (itemR.isURIResource()) {
                    Resource itemRoot = doGetCurrentVersion(itemR.asResource().getURI(), flatten, null);
                    RegisterItem item = new RegisterItem(itemRoot);
                    doGetEntity(item, flatten, null);
                    results.add(item);
                } else {
                    throw new EpiException("Found item which isn't a resource");
                }
            }
            return results;
        } finally {
            store.unlock();
        }
    }
    static String REGISTER_ENUM_QUERY =
            "SELECT ?ri WHERE { ?ri reg:register ?register. }";

    @Override
    public List<RegisterEntryInfo> listMembers(Register register) {
        store.lock();
        try {
            ResultSet rs = selectAll(getDefaultModel(), REGISTER_LIST_QUERY, Prefixes.get(),
                                        createBindings("register", register.getRoot()) );
            List<RegisterEntryInfo> results = new ArrayList<RegisterEntryInfo>();
            Resource priorItem = null;
            RegisterEntryInfo prior = null;
            while (rs.hasNext()) {
                QuerySolution soln = rs.next();
                Resource item = soln.getResource("item");
                if (item.equals(priorItem)) {
                    prior.addLabel( soln.getLiteral("label") );
                    prior.addType( soln.getResource("type") );
                } else {
                    prior = new RegisterEntryInfo(
                            soln.getResource("status"),
                            item,
                            soln.getResource("entity"),
                            soln.getLiteral("label"),
                            soln.getResource("type"),
                            soln.getLiteral("notation") );
                    priorItem = item;
                    results.add( prior );
                }
            }
            return results;
        } finally {
            store.unlock();
        }
    }
    static String REGISTER_LIST_QUERY =
            "SELECT * WHERE { " +
                    "?item reg:register ?register; " +
                    "      version:currentVersion ?itemVer; " +
                    "      reg:notation ?notation; " +
                    "      reg:itemClass ?type ." +
                    "?itemVer reg:status ?status; " +
                    "         reg:definition [reg:entity ?entity]; " +
                    "         rdfs:label ?label . " +
            "} ORDER BY ?item";


    @Override
    public boolean contains(Register register, String notation) {
        store.lock();
        try {
            String base = RDFUtil.getNamespace( register.getRoot() );
            Resource ri = ResourceFactory.createResource( base + "_" + notation);
            return getDefaultModel().contains(ri, RDF.type, RegistryVocab.RegisterItem);
        } finally {
            store.unlock();
        }
    }

    @Override
    public void loadBootstrap(String filename) {
        Model bootmodel = FileManager.get().loadModel(filename); // Load first in case of errors
        store.lockWrite();
        try {
            getDefaultModel().add( bootmodel );
        } finally {
            store.unlock();
        }
    }

    @Override
    public void addToRegister(Register register, RegisterItem item) {
        addToRegister(register, item, Calendar.getInstance());
    }

    @Override
    public void addToRegister(Register register, RegisterItem item, Calendar now) {
        store.lockWrite();
        try {
            doUpdateItem(item, true, now);
            doUpdate(register.getRoot(), now);
            mod(item).addProperty(RegistryVocab.register, register.getRoot());
            Resource entity = item.getEntity();
            if (entity.getPropertyResourceValue(RDF.type).equals(RegistryVocab.Register)) {
                mod(register).addProperty(RegistryVocab.subregister, entity);
            }
        } finally {
//            unlock(register.getRoot().getURI());
            store.unlock();
        }
    }

    private Resource mod(Description d) {
        return mod(d.getRoot());
    }

    private Resource mod(Resource r) {
        return r.inModel(getDefaultModel());
    }

    @Override
    public void update(Register register, Calendar timestamp) {
        store.lockWrite();
        try {
            doUpdate(register.getRoot(), timestamp);
        } finally {
            store.unlock();
        }
    }

    @Override
    public void update(Register register) {
        update(register, Calendar.getInstance());
    }

    protected Resource doUpdate(Resource root, Calendar cal, Property... rigids) {
        Resource newVersion = VersionUtil.nextVersion(root, cal, rigids);
        Model st = getDefaultModel();
        root.inModel(st).removeAll(OWL.versionInfo).removeAll(Version.currentVersion);
        st.add( newVersion.getModel() );
        return newVersion.inModel(st);
    }

    @Override
    public void update(RegisterItem item, boolean withEntity, Calendar timestamp) {
        store.lockWrite();
        try {
            doUpdateItem(item, withEntity, timestamp);
        } finally {
            store.unlock();
        }
    }

    @Override
    public void update(RegisterItem item, boolean withEntity) {
        update(item, withEntity, Calendar.getInstance());
    }

    private void doUpdateItem(RegisterItem item, boolean withEntity, Calendar now) {
        Model storeModel = getDefaultModel();
        Resource oldVersion = mod(item).getPropertyResourceValue(Version.currentVersion);

        Resource newVersion = doUpdate(item.getRoot(), now, RegisterItem.RIGID_PROPS);
        if (withEntity) {
            Resource entity = item.getEntity();
            Resource entityRef = newVersion.getPropertyResourceValue(RegistryVocab.definition);
            if (entityRef == null) {
                entityRef = storeModel.createResource();
                entityRef.addProperty(RegistryVocab.entity, entity);
                newVersion.addProperty(RegistryVocab.definition, entityRef);
            }
            if (entity.hasProperty(RDF.type, RegistryVocab.Register)) {
                doUpdate( entity, now );
            } else {
                if (oldVersion != null) {
                    if (!removeGraphFor(oldVersion)) {
                        log.error("Could not find graph for old version of an updated entity");
                    }
                }
                String graphURI = newVersion.getURI() + "#graph";
                Model entityModel = null;
                if (entity.getModel().contains(null, RDF.type, RegistryVocab.RegisterItem)) {
                    // register item mixed in with entity graph, separate out for storage
                    log.debug("Mixed entity and item");
                    entityModel = Closure.closure(entity, false);
                } else {
                    entityModel = entity.getModel();
                }
                storeGraph(graphURI, entityModel);

                getDefaultModel().add( entityModel );
                Resource graph = ResourceFactory.createResource( graphURI );
                entityRef.removeAll(RegistryVocab.sourceGraph);
                entityRef.addProperty(RegistryVocab.sourceGraph, graph);
            }
        }
    }

    private void storeGraph(String graphURI, Model entityModel) {
        // Avoids Store.updateGraph because we are already in a transaction
        Model graphModel = store.asDataset().getNamedModel(graphURI);
        if (graphModel == null) {
            // Probably an in-memory test store
            graphModel = ModelFactory.createDefaultModel();
            store.asDataset().addNamedModel(graphURI, graphModel);
        } else {
            graphModel.removeAll();
        }
        graphModel.add( entityModel );
    }

    private boolean removeGraphFor(Resource oldVersion) {
        Model st = getDefaultModel();
        Resource definition = mod(oldVersion).getPropertyResourceValue(RegistryVocab.definition);
        if (definition != null) {
            Resource graph = definition.getPropertyResourceValue(RegistryVocab.sourceGraph);
            if (graph != null) {
                st.remove( store.asDataset().getNamedModel(graph.getURI()));
                return true;
            }
        }
        return false;
    }

}
