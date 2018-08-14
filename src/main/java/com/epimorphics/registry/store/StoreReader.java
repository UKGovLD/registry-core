package com.epimorphics.registry.store;

import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.*;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.util.VersionUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.NameUtils;
import com.epimorphics.vocabs.Time;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Closure;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.epimorphics.rdfutil.QueryUtil.createBindings;
import static com.epimorphics.rdfutil.QueryUtil.selectAll;
import static com.epimorphics.rdfutil.QueryUtil.selectFirstVar;

public class StoreReader implements StoreAPIx.ReadTransaction {
	private static final Logger log = LoggerFactory.getLogger(StoreReader.class);

	private final StoreAPI store;
	private final Storex.ReadTransaction txn;

	public StoreReader(StoreAPI store, Storex.ReadTransaction txn) {
		this.store = store;
		this.txn = txn;
	}

	@Override
	public Model getGraph(String graphURI) {
		Model result = ModelFactory.createDefaultModel();
		result.add(txn.getGraph(graphURI));
		return result;
	}

	@Override
	public Description getDescription(String uri) {
		Description d = asDescription(describe(uri, null));
		return d;
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
		return txn.getDefaultModel();
	}

	private Description asDescription(Resource root) {
		return Description.descriptionFrom(root, store);
	}

	@Override
	public Description getCurrentVersion(String uri) {
		Description d = asDescription(doGetCurrentVersion(uri, null));
		return d;
	}

	private Resource doGetCurrentVersion(String uri, Model dest) {
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
		Description d = doGetVersion(uri, true);
		if (d instanceof RegisterItem && withEntity) {
			doGetEntity((RegisterItem) d, null, false);
		}
		return d;
	}

	private Description doGetVersion(String uri, boolean flatten) {
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
		RDFNode version = selectFirstVar("version", getDefaultModel(),
				VERSION_AT_QUERY, Prefixes.getDefault(), "root",
				ResourceFactory.createResource(uri), "time",
				RDFUtil.fromDateTime(time));
		if (version != null && version.isURIResource()) {
			return doGetVersion(version.asResource().getURI(), true);
		} else {
			return null;
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
		Resource root = getDefaultModel().getResource(uri);
		Resource interval = root.getPropertyResourceValue(Version.interval);
		if (interval == null)
			return -1;
		return RDFUtil.asTimestamp(interval
				.getPropertyResourceValue(Time.hasBeginning)
				.getProperty(Time.inXSDDateTime).getObject());
	}

	@Override
	public List<EntityInfo> listEntityOccurences(String uri) {
		Resource entity = ResourceFactory.createResource(uri);
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
	}

	static String ENTITY_FIND_QUERY = "SELECT * WHERE { "
			+ "?entity ^reg:entity/^reg:definition ?itemVer . "
			+ "?itemVer reg:status ?status . "
			+ "?itemVer ^version:currentVersion ?item ."
			+ "?item reg:register ?register . "
			+ "}";

	@Override
	public RegisterItem getItem(String uri, boolean withEntity) {
		Resource root = doGetCurrentVersion(uri, null);
		if (!root.hasProperty(RDF.type, RegistryVocab.RegisterItem)) {
			return null;
		}
		RegisterItem item = new RegisterItem(root);
		if (withEntity) {
			doGetEntity(item, null, true);
		}
		return item;
	}

	private Resource doGetEntity(RegisterItem item, Model dest, boolean getCurrent) {
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
				dest.add(txn.getGraph(srcGraph.getURI()));
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
		return doGetEntity(item, null, true);
	}

	@Override
	public List<RegisterItem> fetchAll(List<String> itemURIs, boolean withEntity) {
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
	}

	@Override
	public List<RegisterEntryInfo> listMembers(Register register) {
		return listMembers(register, null);
	}

	@Override
	public List<RegisterEntryInfo> listMembers(Register register, List<FilterSpec> filters) {
		String query = REGISTER_LIST_QUERY;
		if (filters != null) {
			query = query.replace("#filtertag", FilterSpec.asQuery(filters, "entity"));
		}
		ResultSet rs = selectAll(getDefaultModel(), query,
				Prefixes.getDefault(),
				createBindings("register", register.getRoot()));
		List<RegisterEntryInfo> results = new ArrayList<RegisterEntryInfo>();
		Resource priorItem = null;
		RegisterEntryInfo prior = null;
		while (rs.hasNext()) {
			QuerySolution soln = rs.next();
			try {
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
			} catch (ClassCastException e) {
				log.warn("Skipping ill-formed resource: " + soln.get("item"));
				// Skip ill-formed resources
				// Though these should be blocked on registration
			}
		}
		return results;
	}

	static String REGISTER_LIST_QUERY = "SELECT * WHERE { "
			+ "?item reg:register ?register; "
			+ "      version:currentVersion ?itemVer; "
			+ "      reg:notation ?notation; reg:itemClass ?type ."
			+ "?itemVer reg:status ?status; "
			+ "         reg:definition [reg:entity ?entity]; "
			+ "         rdfs:label ?label . \n"
			+ " #filtertag\n" +
			"} ORDER BY ?notation";


	@Override
	public boolean contains(Register register, String notation) {
		String base = RDFUtil.getNamespace(register.getRoot());
		Resource ri = ResourceFactory.createResource(base + "_" + notation);
		return getDefaultModel().contains(ri, RDF.type,
				RegistryVocab.RegisterItem);
	}

	private Resource mod(Resource r) {
		return r.inModel(getDefaultModel());
	}

	@Override
	public List<String> search(SearchRequest request) {
		try {
			// SelectBuilder doesn't support property paths so use brute force
			// string bashing
			String query = request.isSearchVersions() ? QUERY_VERSION_TEMPLATE : QUERY_TEMPLATE;
			query = query.replace("?text", safeLiteral(request.getQuery()) );
			String filterStr = "";

			if (request.getStatus() != null) {
				String statusRef = "reg:status"
						+ StringUtils.capitalize(request.getStatus());
				filterStr = String.format("   ?item version:currentVersion/reg:status %s.\n", statusRef);
			}

			filterStr += FilterSpec.asQuery(request.getFilters(), "entity");
			query = query.replace("#$FILTER$", filterStr);

			if (request.getLimit() != null) {
				query += "LIMIT " + request.getLimit() + " ";
			}

			if (request.getOffset() != null) {
				query +=  "OFFSET " + request.getOffset();
			}

			log.debug("Search query = " + query);

			ResultSet results = txn.query(query);
			List<String> matches = new ArrayList<String>();
			while (results.hasNext()) {
				QuerySolution row = results.next();
				matches.add(row.getResource("item").getURI());
			}
			return matches;

		} catch (Exception e) {
			log.error("Search query failed, misconfigured store?", e);
			return Collections.emptyList();
		}
	}

	private static final String QUERY_TEMPLATE=
			"PREFIX text:    <http://jena.apache.org/text#>\n"
					+ "PREFIX reg:     <http://purl.org/linked-data/registry#>\n"
					+ "PREFIX version: <http://purl.org/linked-data/version#>\n"
					+ "PREFIX dct:     <http://purl.org/dc/terms/>\n"
					+ "PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "SELECT DISTINCT ?item WHERE {\n"
					+ "    {\n"
					+ "        ?entity text:query '?text' .\n"
					+ "        ?item version:currentVersion/reg:definition/reg:entity ?entity.\n"
					+ "    } UNION {\n"
					+ "        ?entity text:query '?text' .\n"
					+ "        ?item version:currentVersion/reg:definition/reg:entity/version:currentVersion ?entity.\n"
					+ "    }\n"
					+ "    #$FILTER$\n"
					+ "}";

	private static final String QUERY_VERSION_TEMPLATE=
			"PREFIX text:    <http://jena.apache.org/text#>\n"
					+ "PREFIX reg:     <http://purl.org/linked-data/registry#>\n"
					+ "PREFIX version: <http://purl.org/linked-data/version#>\n"
					+ "PREFIX dct:     <http://purl.org/dc/terms/>\n"
					+ "PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "SELECT DISTINCT ?item WHERE {\n"
					+ "    ?entity text:query '?text' .\n"
					+ "    ?item ^dct:versionOf/reg:definition/reg:entity ?entity.\n"
					+ "    #$FILTER$\n"
					+ "}";

	private String safeLiteral(String value) {
		return value.replace("'", "\\'");
	}

	@Override
	public List<ForwardingRecord> listDelegations() {
		List<ForwardingRecord> results = new ArrayList<>();
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
					ForwardingRecord.Type type = ForwardingRecord.Type.FORWARD;
					if (record.hasProperty(RDF.type,
							RegistryVocab.FederatedRegister)) {
						type = ForwardingRecord.Type.FEDERATE;
					} else if (record.hasProperty(RDF.type,
							RegistryVocab.DelegatedRegister)) {
						type = ForwardingRecord.Type.DELEGATE;
					}
					String target = soln.getResource("target").getURI();
					ForwardingRecord fr;
					if (type == ForwardingRecord.Type.DELEGATE) {
						DelegationRecord dr = new DelegationRecord(
								record.getURI(), target, type);
						Resource s = soln.getResource("subject");
						if (s != null)
							dr.setSubject(s);
						Resource p = soln.getResource("predicate");
						if (p != null)
							dr.setPredicate(p);
						RDFNode on = soln.get("object");
						Resource o = null;
						if (on.isResource()) {
							o = on.asResource();
						} else if (on.isLiteral()) {
							String olit = on.asLiteral().getLexicalForm();
							if (NameUtils.isURI(olit)) {
								o = ResourceFactory.createResource(olit);
							}
						}
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
		log.debug("Query: " + query);
		ResultSet results = txn.query(query);

		return ResultSetFactory.copyResults(results);
	}

	protected RegisterItem asItem(String uri) {
		String id = NameUtils.splitAfterLast(uri, "/");
		if ( ! id.startsWith("_") ) {
			// Ensure we start with an item URI
			uri = NameUtils.splitBeforeLast(uri, "/") + "/_" + id;
		}
		Description d = getCurrentVersion(uri);
		if (d != null) {
			return d.asRegisterItem();
		} else {
			return null;
		}
	}

	/**
	 * Scan the tree streaming all the triples in the default graph to the given stream
	 * and return a collection of the associated named graphs (entity definitions and annotations)
	 * If the entities argument is null then all entities are included in the scan, otherwise only the
	 * reference to the entity is included and the entity is returned in the entities collection for
	 * a post-scan cleanup. This is needed to allow for multiple references to entities.
	 */
	protected Collection<Resource> scanAllVersions(Resource root, StreamRDF stream, Collection<Resource> entities) {
		return scanAllVersions(root, stream, new HashSet<Resource>(), entities);
	}

	private Collection<Resource> scanAllVersions(Resource root, StreamRDF stream, Collection<Resource> sofar, Collection<Resource> entities) {
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
					if ( entities == null ) {
						scanAllVersions(entity, stream, sofar, entities);
					} else {
						// Defer deleting entities because we have to reference count them
						stream.triple( new Triple(definition.asNode(), RegistryVocab.entity.asNode(), entity.asNode()) );
						entities.add( entity );
					}
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
		out.start();
		try {
			doExportTree(item, out);
		} finally {
			out.finish();
		}
	}

	private void doExportTree(RegisterItem item, StreamRDF out) {
		if ( item.isRegister() ) {
			Register register = item.getAsRegister(store);
			for (RegisterEntryInfo ei : listMembers(register )) {
				doExportTree( getCurrentVersion(ei.getItemURI()).asRegisterItem(), out );
			}
		}

		Collection<Resource> graphs = scanAllVersions(item.getRoot(), out, null);
		for (Resource g : graphs) {
			StmtIterator stIt = store.getGraph(g.getURI()).listStatements();
			while (stIt.hasNext()) {
				Triple triple = stIt.next().asTriple();
				out.quad(new Quad(g.asNode(), triple));
			}
		}
	}
}
