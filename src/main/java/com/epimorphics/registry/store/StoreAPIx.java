package com.epimorphics.registry.store;

import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.ForwardingRecord;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.system.StreamRDF;

import java.util.Calendar;
import java.util.List;

public interface StoreAPIx {

	<T> T read(ReadOperation<T> op);
	void write(WriteOperation op);

	interface ReadOperation<T> {
		T execute(ReadTransaction txn);
	}

	interface ReadTransaction {
		/**
		 * Return the register/item/entity at the given address or null if there is none such.
		 * No version processing.
		 */
		Description getDescription(String uri);

		/**
		 * Return the current version of the resource.
		 * @param uri the uri of the base VersionedThing
		 * @return Description containing a merge of the selected Version and the root VersionedThing
		 */
		Description getCurrentVersion(String uri);

		/**
		 * Return a specific version of a versioned resource .
		 * @param uri the uri of the Version instance to be retrieved
		 * @param withEntity if this is true and the resource is a RegisterItem then will also retrieve the corresponding entity
		 * @return Description containing a merge of the selected Version and the root VersionedThing
		 */
		Description getVersion(String uri, boolean withEntity);

		/**
		 * Return a specific version of a versioned resource .
		 * @param uri the uri of the base VersionedThing
		 * @param time the timestamp at which the desired version was valid
		 * @return Description containing a merge of the selected Version and the root VersionedThing
		 */
		Description getVersionAt(String uri, long time);

		/**
		 * Return the effective timestamp of a versioned resource
		 */
		long versionStartedAt(String uri);

		/**
		 * List all known verisons of a VersionedThing
		 * @param uri the uri of the base VersionedThing
		 */
		List<VersionInfo> listVersions(String uri);

		// --- Methods for accessing linked register resources ---

		/**
		 * Return a set of RDF from a named graph in the store
		 */
		Model getGraph(String graphURI);

		/**
		 * Return a RegisterItem, optionally along with the associated entity definition.
		 * The RegisterItem root resource and current version will be merged. If the entity
		 * is fetched and if the entity is itself a VersionedThing then its version information
		 * will also be fetched and merged.
		 * @param uri uri of the base (VersionedThing) RegisterItem to be fetched
		 * @param withEntity if true then the entity definition should be retrieved as well
		 * @return the item or null if the item is not a RegisterItem
		 */
		RegisterItem getItem(String uri, boolean withEntity);

		/**
		 * Return a RegisterItem, optionally along with the associated entity definition.
		 * No version flattening will be done.
		 * <p>AT RISK: not sure we really want to expose the versioning model this way</p>
		 * @param uri uri of the base (VersionedThing) RegisterItem to be fetched
		 * @param withEntity if true then the entity definition should be retrieved as well
		 */

		/**
		 * Fetch the entity specified by the Register item and add it to the item's data structure.
		 */
		Resource getEntity(RegisterItem item);

		/**
		 * Retrieve a set of RegisterItems
		 * @param itemURIs the URIs of the items to retrieve
		 * @param withEntity if true then the entity definition should be retrieved as well
		 */
		List<RegisterItem> fetchAll(List<String> itemURIs, boolean withEntity);

		/**
		 * List all members of a register. This gives a low cost way to enumerate the core information
		 * on the members without fetching and merging version and entity descriptions.
		 */
		List<RegisterEntryInfo> listMembers(Register register);

		/**
		 * List all members of a register which also pass the given SPARQL filter criteria
		 */
		List<RegisterEntryInfo> listMembers(Register register, List<FilterSpec> filters);

		/**
		 * Find all places where the given entity is registered and return the URIs for the coresponding
		 * item and the register it is in.
		 */
		List<EntityInfo> listEntityOccurences(String uri);

		/**
		 * Free text search over the registered items
		 * @param query the search parameters
		 * @return list of URIs for the matched RegisterItems
		 */
		List<String> search(SearchRequest query);

		/**
		 * Tests if the register contains an item with the given notation (relative URI)
		 */
		boolean contains(Register register, String notation);

		/**
		 * List all delegation/forwarding/federation records in the registry.
		 */
		List<ForwardingRecord> listDelegations();

		/**
		 * Run a sparql query the store. It is guaranateed to be run
		 * in an environment in which all annotation graphs area available as named graphs,
		 * there are no guarantees on access to item versioning.
		 */
		// This breaks the internal goal of hiding sparql, which in turn was to leave
		// open a path to simpler versioning implementations and maybe non-triple store implementations
		// The weasel words in the javadoc are intended to leave the possibility of just using
		// SPARQL for accessing the graph annotations (for which there's no alternative anyway)
		ResultSet query(String query);

		/**
		 * Generate an export of the complete registry state for some tree of registers
		 */
		void exportTree(String uri, StreamRDF out);
	}

	interface WriteOperation {
		void execute(WriteTransaction txn);
	}

	interface WriteTransaction {
		/**
		 * Add a new registered item to a parent register.
		 * Initializes the versioning of the new RegisterItem.
		 * If the entity of the item is a Register then the versioning of the new sub-register will be initialized.
		 * Otherwise a new entity graph will be created for the entity.
		 * Does not increment the version of the register itself, call update on the register to achieve that
		 */
		void addToRegister(Register register, RegisterItem item);

		/**
		 * Add a new registered item to a parent register.
		 * Initializes the versioning of the new RegisterItem.
		 * If the entity of the item is a Register then the versioning of the new sub-register will be initialized.
		 * Otherwise a new entity graph will be created for the entity.
		 * Does not increment the version of the register itself, call update on the register to achieve that
		 */
		void addToRegister(Register register, RegisterItem item, Calendar timestamp);

		/**
		 * Update the metadata for a register, managing the versioning information.
		 * @return the URI of the new version of the item
		 */
		String update(Register register);

		/**
		 * Update a Register item
		 * @param withEntity if true then a new version of the entity will be saved, if false then
		 * just the item metadata will be udpated.
		 * @return the URI of the new version of the item
		 */
		String update(RegisterItem item, boolean withEntity);

		/**
		 * Update the metadata for a register, managing the versioning information.
		 * @return the URI of the new version of the item
		 */
		String update(Register register, Calendar timestamp);

		/**
		 * Update a Register item
		 * @param withEntity if true then a new version of the entity will be saved, if false then
		 * just the item metadata will be udpated.
		 * @return the URI of the new version of the item
		 */
		String update(RegisterItem item, boolean withEntity, Calendar timestamp);

		/**
		 * Store a set of RDF data as a graph in the store, replacing any previous version for this graph
		 */
		void storeGraph(String graphURI, Model model);

		/**
		 * Delete all versions of an entry from the store.
		 * For an item it deletes the associated entity (including it's graph).
		 * For a register it recursively deletes all the register members as well.
		 */
		void delete(String uri);

		interface RDFSource {
			void input(StreamRDF input);
		}

		/**
		 * Import a complete registry state, deletes the existing state if any
		 */
		void importTree(String uri, RDFSource source);
	}
}
