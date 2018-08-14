package com.epimorphics.registry.store;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.*;
import com.epimorphics.registry.util.VersionUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Closure;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StoreWriter extends StoreReader implements StoreAPIx.WriteTransaction {
	private static final Logger log = LoggerFactory.getLogger(StoreWriter.class);

	private final StoreAPI store;
	private final Storex.WriteTransaction txn;

	public StoreWriter(StoreAPI store, Storex.WriteTransaction txn) {
		super(store, txn);
		this.store = store;
		this.txn = txn;
	}

	@Override
	public void storeGraph(String graphURI,Model entityModel) {
		// Avoids Store.updateGraph because we are already in a transaction
		Model graphModel = txn.getGraph(graphURI);
		if (graphModel == null) {
			// Probably an in-memory test store
			graphModel = ModelFactory.createDefaultModel();
			txn.insertGraph(graphURI, graphModel);
		} else {
			graphModel.removeAll();
		}
		graphModel.add(entityModel);
	}

	@Override
	public void addToRegister(Register register, RegisterItem item) {
		addToRegister(register, item, Calendar.getInstance());
	}

	@Override
	public void addToRegister(Register register, RegisterItem item, Calendar now) {
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
		return doUpdateRegister(register.getRoot(), timestamp).getURI();
	}

	@Override
	public String update(Register register) {
		return update(register, Calendar.getInstance());
	}

	protected Resource doUpdateRegister(Resource root, Calendar cal, Property... rigids) {
		root.removeAll(RegistryVocab.subregister);
		Model model = getDefaultModel();
		Resource current = model.getResource(root.getURI());
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
		root.removeAll(DCTerms.modified).addProperty(DCTerms.modified, model.createTypedLiteral(cal));
		return doUpdate(model, root, cal, rigids);
	}

	protected Resource doUpdate(Model st, Resource root, Calendar cal, Property... rigids) {
		Resource newVersion = VersionUtil.nextVersion(root, cal, rigids);
		root.inModel(st).removeAll(OWL.versionInfo).removeAll(Version.currentVersion);
		st.add(newVersion.getModel());
		return newVersion.inModel(st);
	}

	@Override
	public String update(RegisterItem item, boolean withEntity,
						 Calendar timestamp) {
		return doUpdateItem(item, withEntity, timestamp);
	}

	@Override
	public String update(RegisterItem item, boolean withEntity) {
		return update(item, withEntity, Calendar.getInstance());
	}

	private String doUpdateItem(RegisterItem item, boolean withEntity, Calendar now) {
		Model storeModel = getDefaultModel();
		Resource oldVersion = mod(item).getPropertyResourceValue(
				Version.currentVersion);

		Resource newVersion = doUpdate(storeModel, item.getRoot(), now, RegisterItem.RIGID_PROPS);

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
				storeGraph(graphURI, entityModel);

				if (!item.isGraph()) {
					txn.addAll(entityModel);
				}
				Resource graph = ResourceFactory.createResource(graphURI);
				entityRef.removeAll(RegistryVocab.sourceGraph);
				entityRef.addProperty(RegistryVocab.sourceGraph, graph);
			}
		}
		return newVersion.getURI();
	}

	private boolean removeGraphFor(Resource oldVersion) {
		Resource definition = mod(oldVersion).getPropertyResourceValue(RegistryVocab.definition);
		if (definition != null) {
			Resource graph = definition.getPropertyResourceValue(RegistryVocab.sourceGraph);
			if (graph != null) {
				txn.deleteGraph(graph.getURI());
				return true;
			}
		}
		return false;
	}

	@Override
	public void delete(String uri) {
		delete( asItem(uri) );
	}

	private void delete(RegisterItem item) {
		if ( item.isRegister() ) {
			Register register = item.getAsRegister(store);
			for (RegisterEntryInfo ei : listMembers(register )) {
				delete( getCurrentVersion(ei.getItemURI()).asRegisterItem() );
			}
		}

		Model toDelete = ModelFactory.createDefaultModel();
		Set<Resource> entities = new HashSet<>();
		Collection<Resource>graphs = scanAllVersions(item.getRoot(), toModel(toDelete), entities);
		txn.removeAll(toDelete);
		for (Resource graph : graphs) {
			txn.deleteGraph(graph.getURI());
		}

		// Now need to reference count the entities
		toDelete = ModelFactory.createDefaultModel();
		graphs = new HashSet<>();
		for (Iterator<Resource> ri = entities.iterator(); ri.hasNext();) {
			Resource entity = ri.next();
			ResIterator check = getDefaultModel().listSubjectsWithProperty(RegistryVocab.entity, entity);
			if ( ! check.hasNext()) {
				// No references so delete
				graphs.addAll( scanAllVersions(entity, toModel(toDelete), null) );
			} else {
				check.close();
			}
		}

		txn.removeAll(toDelete);
		for (Resource graph : graphs) {
			txn.deleteGraph(graph.getURI());
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

	@Override
	public void importTree(String uri, RDFSource source) {
		RegisterItem item = asItem(uri);
		if (item != null) {
			delete(item);
		}

		StreamRDF stream = new StreamRDF() {
			@Override public void start() {}
			@Override public void base(String base) {}
			@Override public void finish() {}

			@Override
			public void triple(Triple triple) {
				txn.insertTriple(triple);
			}

			@Override
			public void quad(Quad quad) {
				txn.insertQuad(quad);
			}

			@Override
			public void prefix(String prefix, String iri) {
				throw new UnsupportedOperationException();
			}
		};

		source.input(stream);
	}
}
