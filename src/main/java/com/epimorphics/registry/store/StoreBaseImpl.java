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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.DelegationRecord;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.ForwardingRecord;
import com.epimorphics.registry.core.ForwardingRecord.Type;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.util.DescriptionCache;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.util.VersionUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.server.core.Indexer;
import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.core.Store;
import com.epimorphics.server.indexers.LuceneIndex;
import com.epimorphics.server.indexers.LuceneResult;
import com.epimorphics.server.webapi.WebApiException;
import com.epimorphics.util.EpiException;
import com.epimorphics.vocabs.Time;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
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
    protected DescriptionCache cache;
    protected Map<String, Lock> locks = new HashMap<String, Lock>();

    @Override
    public void postInit() {
        store = getNamedService( getRequiredParam(STORE_PARAMETER), Store.class);
        String indexerName = config.get(INDEXER_PARAMETER);
        if (indexerName != null) {
            indexer = getNamedService(indexerName, Indexer.class);
        }
    }

    public synchronized void lock(String uri) {
        Lock lock = locks.get(uri);
        if (lock == null) {
            lock = new ReentrantLock();
            locks.put(uri, lock);
        }
        lock.lock();
        if (indexer != null) {
            indexer.startBatch();
        }
    }

    /**
     * Release the "forupdate" lock on the given URI (should be a Register or RegisterItem).
     * Throws an error if there is no such lock.
     */
    public synchronized void unlock(String uri) {
        Lock lock = locks.remove(uri);
        if (lock == null) {
            throw new EpiException("Internal error: tried to unlock a resource which was not locked for update");
        }
        if (storeWriteLocked) {
            store.unlock();
            storeWriteLocked = false;
        }
        lock.unlock();
        if (indexer != null) {
            indexer.endBatch();
        }
    }


    // Attempt to maintain store lock through a registry block operation
    boolean storeWriteLocked = false;

    protected synchronized void unlockStore() {
        if (!storeWriteLocked) {
            store.unlock();
        }
    }

    protected synchronized void lockStore() {
        if (!storeWriteLocked) {
            store.lock();
        }
    }

    protected synchronized void lockStoreWrite() {
        if (!storeWriteLocked) {
            store.lockWrite();
            storeWriteLocked = true;
        }
    }

    @Override
    public Description getDescription(String uri) {
        lockStore();
        try {
            Description d = asDescription( describe(uri, null) );
            return d;
        } finally {
            unlockStore();
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
        lockStore();
        try {
            Description d = asDescription( doGetCurrentVersion(uri, true, null) );
            return d;
        } finally {
            unlockStore();
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
        lockStore();
        try {
            return doGetVersion(uri, true);
        } finally {
            unlockStore();
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
        lockStore();
        try {
            RDFNode version = selectFirstVar("version", getDefaultModel(), VERSION_AT_QUERY, Prefixes.getDefault(),
                    "root", ResourceFactory.createResource(uri), "time", RDFUtil.fromDateTime(time) );
            if (version != null && version.isURIResource()) {
                return doGetVersion( version.asResource().getURI(), true );
            } else {
                return null;
            }
        } finally {
            unlockStore();
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
        lockStore();
        try {
            ResultSet rs = selectAll(getDefaultModel(), VERSION_LIST_QUERY, Prefixes.getDefault(),
                createBindings("root", ResourceFactory.createResource(uri)));
            List<VersionInfo> results = new ArrayList<VersionInfo>();
            while (rs.hasNext()) {
                QuerySolution soln = rs.next();
                VersionInfo vi = new VersionInfo(
                        soln.getResource("version"),
                        soln.getLiteral("info"),
                        soln.getLiteral("from"),
                        soln.getLiteral("to")      );
                Resource replaces = soln.getResource("replaces");
                if (replaces != null) {
                    vi.setReplaces(replaces.getURI());
                }
                results.add( vi );
            }
            return results;
        } finally {
            unlockStore();
        }
    }
    static String VERSION_LIST_QUERY =
            "SELECT ?version ?info ?from ?to ?replaces WHERE \n" +
            "{  \n" +
            "    ?version dct:isVersionOf ?root; \n" +
            "             owl:versionInfo ?info . \n" +
            "   OPTIONAL {?version version:interval [time:hasBeginning [time:inXSDDateTime ?from]].} \n" +
            "   OPTIONAL {?version version:interval [time:hasEnd [time:inXSDDateTime ?to]].} \n" +
            "   OPTIONAL {?version dct:replaces ?replaces.} \n" +
            "} ORDER BY ?info \n";

    @Override
    public long versionStartedAt(String uri) {
        lockStore();
        try {
            Resource root = getDefaultModel().getResource(uri);
            Resource interval = root.getPropertyResourceValue(Version.interval);
            if (interval == null) return -1;
            return RDFUtil.asTimestamp(
                    interval
                        .getPropertyResourceValue(Time.hasBeginning)
                        .getProperty(Time.inXSDDateTime)
                        .getObject() );
        } finally {
            unlockStore();
        }
    }

    @Override
    public List<EntityInfo> listEntityOccurences(String uri) {
        Resource entity = ResourceFactory.createResource(uri);
        lockStore();
        try {
            ResultSet matches = QueryUtil.selectAll(getDefaultModel(), ENTITY_FIND_QUERY, Prefixes.getDefault(), "entity", entity);
            List<EntityInfo> results = new ArrayList<EntityInfo>();
            while (matches.hasNext()) {
                QuerySolution soln = matches.next();
                results.add( new EntityInfo(entity, soln.getResource("item"), soln.getResource("register"), soln.getResource("status") ) );
            }
            return results;
        } finally {
            unlockStore();
        }
    }
    static String ENTITY_FIND_QUERY =
            "SELECT * WHERE { " +
                    "?item reg:register ?register; " +
                    "      version:currentVersion ?itemVer . " +
                    "?itemVer reg:status ?status; " +
                    "         reg:definition [reg:entity ?entity] . " +
            "}";


    @Override
    public RegisterItem getItem(String uri, boolean withEntity) {
        return doGetItem(uri, withEntity, true);
    }

    public RegisterItem getItemWithVersion(String uri, boolean withEntity) {
        return doGetItem(uri, withEntity, false);
    }

    private RegisterItem doGetItem(String uri, boolean withEntity, boolean flatten) {
        lockStore();
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
            unlockStore();
        }
    }

    protected Resource doGetEntity(RegisterItem item, boolean flatten, Model dest) {
        Resource root = item.getRoot();
        if (!flatten) {
            root = RDFUtil.getSubject(root.getModel(), DCTerms.isVersionOf, root);
            if (root == null) {
                log.error("Internal error finding non-flattened version of " + item.getRoot());
                return null;
            }
        }
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
        lockStore();
        try {
            return doGetEntity(item, true, null);
        } finally {
            unlockStore();
        }
    }

    @Override
    public List<RegisterItem> fetchAll(List<String> itemURIs, boolean withEntity, boolean withVersion) {
        lockStore();
        try {
            Model shared = ModelFactory.createDefaultModel();
            List<RegisterItem> results = new ArrayList<RegisterItem>(itemURIs.size());
            for ( String uri : itemURIs ) {
                Resource itemRoot = doGetCurrentVersion(uri, !withVersion, shared);
                RegisterItem item = new RegisterItem(itemRoot);
                if (withEntity) {
                    doGetEntity(item, !withVersion, shared);
                }
                results.add(item);
            }
            return results;
        } finally {
            unlockStore();
        }
    }


    public  List<RegisterItem>  doFetchMembers(Register register, boolean withEntity, boolean flatten) {
        lockStore();
        try {
            // A shared model would save having 2N models around but makes filtering painful
//            Model shared = ModelFactory.createDefaultModel();
            List<RDFNode> items = selectAllVar("ri", getDefaultModel(), REGISTER_ENUM_QUERY, Prefixes.getDefault(),
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
            unlockStore();
        }
    }
    static String REGISTER_ENUM_QUERY =
            "SELECT ?ri WHERE { ?ri reg:register ?register. }";

    @Override
    public List<RegisterEntryInfo> listMembers(Register register) {
        lockStore();
        try {
            ResultSet rs = selectAll(getDefaultModel(), REGISTER_LIST_QUERY, Prefixes.getDefault(),
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
            unlockStore();
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
        lockStore();
        try {
            String base = RDFUtil.getNamespace( register.getRoot() );
            Resource ri = ResourceFactory.createResource( base + "_" + notation);
            return getDefaultModel().contains(ri, RDF.type, RegistryVocab.RegisterItem);
        } finally {
            unlockStore();
        }
    }

    @Override
    public void loadBootstrap(String filename) {
        Model bootmodel = FileManager.get().loadModel(filename); // Load first in case of errors
        lock("/");
        lockStoreWrite();
        try {
            getDefaultModel().add( bootmodel );
        } finally {
            unlock("/");
        }
    }

    @Override
    public void addToRegister(Register register, RegisterItem item) {
        addToRegister(register, item, Calendar.getInstance());
    }

    @Override
    public void addToRegister(Register register, RegisterItem item, Calendar now) {
        lockStoreWrite();
        try {
            doUpdateItem(item, true, now);
            doUpdate(register.getRoot(), now);
            mod(item).addProperty(RegistryVocab.register, register.getRoot());
            Resource entity = item.getEntity();
            if (entity.hasProperty(RDF.type, RegistryVocab.Register)) {
                modCurrent(register).addProperty(RegistryVocab.subregister, entity);
            }
            modCurrent(register).removeAll(DCTerms.modified).addProperty(DCTerms.modified, getDefaultModel().createTypedLiteral(now));
        } finally {
            unlockStore();
        }
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
        lockStoreWrite();
        try {
            return doUpdateRegister(register.getRoot(), timestamp).getURI();
        } finally {
            unlockStore();
        }
    }

    @Override
    public String update(Register register) {
        return update(register, Calendar.getInstance());
    }

    protected Resource doUpdateRegister(Resource root, Calendar cal, Property... rigids) {
        root.removeAll(RegistryVocab.subregister);
        Resource current = getDefaultModel().getResource(root.getURI());
        Resource temp= current.getPropertyResourceValue(Version.currentVersion);
        if (temp != null) {
            current = temp;
        }
        RDFUtil.copyProperty( current, root, RegistryVocab.subregister );
        return doUpdate(root, cal, rigids);
    }


    protected Resource doUpdate(Resource root, Calendar cal, Property... rigids) {
        Resource newVersion = VersionUtil.nextVersion(root, cal, rigids);
        Model st = getDefaultModel();
        root.inModel(st).removeAll(OWL.versionInfo).removeAll(Version.currentVersion);
        st.add( newVersion.getModel() );
        return newVersion.inModel(st);
    }

    protected void doIndex(Resource root, String graph) {
        if (indexer != null) {
//            indexer.updateGraph(graph, root.getModel());
            indexer.addGraph(graph, root.getModel());
        }
    }

    @Override
    public String update(RegisterItem item, boolean withEntity, Calendar timestamp) {
        lockStoreWrite();
        try {
            return doUpdateItem(item, withEntity, timestamp);
        } finally {
            unlockStore();
        }
    }

    @Override
    public String update(RegisterItem item, boolean withEntity) {
        return update(item, withEntity, Calendar.getInstance());
    }

    private String doUpdateItem(RegisterItem item, boolean withEntity, Calendar now) {
        Model storeModel = getDefaultModel();
        Resource oldVersion = mod(item).getPropertyResourceValue(Version.currentVersion);

        Resource newVersion = doUpdate(item.getRoot(), now, RegisterItem.RIGID_PROPS);

        doIndex(item.getRoot(), newVersion.getURI());

        if (withEntity) {
            Resource entity = item.getEntity();
            Resource entityRef = newVersion.getPropertyResourceValue(RegistryVocab.definition);
            if (entityRef == null) {
                entityRef = storeModel.createResource();
                entityRef.addProperty(RegistryVocab.entity, entity);
                newVersion.addProperty(RegistryVocab.definition, entityRef);
            }
            if (entity.hasProperty(RDF.type, RegistryVocab.Register)) {
                doUpdateRegister( entity, now );
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
        return newVersion.getURI();
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

    @Override
    public LuceneResult[] search(String query, int offset, int maxresults, String... fields) {
        if (indexer != null) {
            Analyzer analyzer = new StandardAnalyzer(org.apache.lucene.util.Version.LUCENE_40);
            QueryParser parser = new QueryParser(org.apache.lucene.util.Version.LUCENE_40, LuceneIndex.FIELD_LABEL, analyzer);
            Query search;
            try {
                search = parser.parse( query );
            } catch (ParseException e) {
                throw new WebApiException(BAD_REQUEST, "Could not parse query: " + e.getMessage());
            }
            if (fields.length != 0) {
                BooleanQuery bq = new BooleanQuery();
                for (int i = 0; i < fields.length;) {
                    String key = SearchTagname.expandKey(fields[i++]);
                    if (i >= fields.length) {
                        throw new EpiException("Odd number of fields in search call");
                    }
                    String value = fields[i++];
                    Term t = new Term(key, value);
                    bq.add(new TermQuery( t ), BooleanClause.Occur.MUST);
                }
                bq.add(search, BooleanClause.Occur.MUST );
                search = bq;
            }

            return ((LuceneIndex)indexer).search(search, offset, maxresults);
        } else {
            return new LuceneResult[0];
        }
    }

    // Debug support only
    public void dump() {
        lockStore();
        try {
            getDefaultModel().write(System.out, "Turtle");
        } finally {
            unlockStore();
        }
    }

    @Override
    public List<ForwardingRecord> listDelegations() {
        List<ForwardingRecord> results = new ArrayList<>();
        lockStore();
        try {
            Model m = getDefaultModel();
            ResultSet rs = QueryUtil.selectAll(m, DELEGATION_LIST_QUERY, Prefixes.getDefault());
            while (rs.hasNext()) {
                QuerySolution soln = rs.nextSolution();
                Status status = Status.forResource( soln.getResource("status") );
                if (status.isAccepted()) {
                    Resource record = soln.getResource("record").inModel(m);
                    ForwardingRecord.Type type = Type.FORWARD;
                    if (record.hasProperty(RDF.type, RegistryVocab.FederatedRegister)) {
                        type = Type.FEDERATE;
                    } else if (record.hasProperty(RDF.type, RegistryVocab.DelegatedRegister)) {
                        type = Type.DELEGATE;
                    }
                    String target = soln.getResource("target").getURI();
                    ForwardingRecord fr = null;
                    if (type == Type.DELEGATE) {
                        DelegationRecord dr = new DelegationRecord(record.getURI(), target, type);
                        Resource s = soln.getResource("subject");
                        if (s != null) dr.setSubject(s);
                        Resource p = soln.getResource("predicate");
                        if (p != null) dr.setPredicate(p);
                        Resource o = soln.getResource("object");
                        if (o != null) dr.setObject(o);
                        fr = dr;
                    } else {
                        fr = new ForwardingRecord(record.getURI(), target, type);
                        Literal code = soln.getLiteral("code");
                        if (code != null) {
                            fr.setForwardingCode( code.getInt() );
                        }
                    }
                    results.add(fr);
                }
            }
            return results;
        } finally {
            unlockStore();
        }
    }
    static String DELEGATION_LIST_QUERY =
            "SELECT * WHERE {" +
            "   { " +                              // DelegatedRegister case, registers are managed, hence additional versioning
            "      ?record a reg:DelegatedRegister ; version:currentVersion ?ver ." +
            "      ?ver reg:delegationTarget ?target. " +
            "      ?item reg:status ?status; reg:definition [reg:entity ?record] . " +
            "      [] version:currentVersion ?item. " +
            "      OPTIONAL {?ver reg:forwardingCode ?code. } " +
            "      OPTIONAL {?ver reg:enumerationSubject ?subject. } " +
            "      OPTIONAL {?ver reg:enumerationPredicate ?predicate. } " +
            "      OPTIONAL {?ver reg:enumerationObject ?object. } " +
            "   } UNION {" +                        // Typical entity case
            "      ?record a reg:Delegated ; reg:delegationTarget ?target. " +
            "      ?item reg:status ?status; reg:definition [reg:entity ?record] . " +
            "      [] version:currentVersion ?item. " +
            "      OPTIONAL {?record reg:forwardingCode ?code. } " +
            "      OPTIONAL {?record reg:enumerationSubject ?subject. } " +
            "      OPTIONAL {?record reg:enumerationPredicate ?predicate. } " +
            "      OPTIONAL {?record reg:enumerationObject ?object. } " +
            "   } " +
            "}";


}
