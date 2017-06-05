/******************************************************************
 * File:        StoreAPI.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
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

import java.util.Calendar;
import java.util.List;

import org.apache.jena.riot.system.StreamRDF;

import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.ForwardingRecord;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;


/**
 * Abstract interface onto the underlying versioned RDF storage and
 * indexing system. 
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */

// TODO review this - it has accreted operations as they extensions have demanded them and is now
//                    rather unwieldy. Is there a useful set of core operations somewhere between 
//                    raw RDF storage and this application-centred API?

public interface StoreAPI {
    
    /**
     * Begin a read transaction. 
     */
    public void beginRead();
    
    /**
     * Begin a write transaction
     */
    public void beginWrite();

    /**
     * Commit a write transaction
     */
    public void commit();

    /**
     * Abort a write transaction
     */
    public void abort();
    
    /**
     * Finish a transaction. If this is a write transaction and commit has not been called
     * then the transaction is aborted.
     */
    public void end();
    
    /**
     * Begin a safe read block. If the store is already in a transaction this is a no-op, otherwise
     * it starts a read transaction.
     */
    public void beginSafeRead();
    
    /**
     * End a safe read block. If the transaction was created by the corresponding beginSafeRead then close the read transaction.
     */
    public void endSafeRead();

    // --- Methods for access versions of resource descriptions ---

    /**
     * Return the register/item/entity at the given address or null if there is none such.
     * No version processing.
     */
    public Description getDescription(String uri);

    /**
     * Return the current version of the resource.
     * @param uri the uri of the base VersionedThing
     * @return Description containing a merge of the selected Version and the root VersionedThing
     */
    public Description getCurrentVersion(String uri);

    /**
     * Return a specific version of a versioned resource .
     * @param uri the uri of the Version instance to be retrieved
     * @param withEntity if this is true and the resource is a RegisterItem then will also retrieve the corresponding entity
     * @return Description containing a merge of the selected Version and the root VersionedThing
     */
    public Description getVersion(String uri, boolean withEntity);

    /**
     * Return a specific version of a versioned resource .
     * @param uri the uri of the base VersionedThing
     * @param time the timestamp at which the desired version was valid
     * @return Description containing a merge of the selected Version and the root VersionedThing
     */
    public Description getVersionAt(String uri, long time);

    /**
     * Return the effective timestamp of a versioned resource
     */
    public long versionStartedAt(String uri);

    /**
     * List all known verisons of a VersionedThing
     * @param uri the uri of the base VersionedThing
     */
    public List<VersionInfo> listVersions(String uri);

    // --- Methods for accessing linked register resources ---

    /**
     * Return a RegisterItem, optionally along with the associated entity definition.
     * The RegisterItem root resource and current version will be merged. If the entity
     * is fetched and if the entity is itself a VersionedThing then its version information
     * will also be fetched and merged.
     * @param uri uri of the base (VersionedThing) RegisterItem to be fetched
     * @param withEntity if true then the entity definition should be retrieved as well
     * @return the item or null if the item is not a RegisterItem
     */
    public RegisterItem getItem(String uri, boolean withEntity);

    /**
     * Return a RegisterItem, optionally along with the associated entity definition.
     * No version flattening will be done.
     * <p>AT RISK: not sure we really want to expose the versioning model this way</p>
     * @param uri uri of the base (VersionedThing) RegisterItem to be fetched
     * @param withEntity if true then the entity definition should be retrieved as well
     */

//    public RegisterItem getItemWithVersion(String uri, boolean withEntity);

    /**
     * Fetch the entity specified by the Register item and add it to the item's data structure.
     */
    public Resource getEntity(RegisterItem item);

    /**
     * Retrieve a set of RegisterItems
     * @param itemURIs the URIs of the items to retrieve
     * @param withEntity if true then the entity definition should be retrieved as well
     */
    public List<RegisterItem> fetchAll(List<String> itemURIs, boolean withEntity);

    /**
     * List all members of a register. This gives a low cost way to enumerate the core information
     * on the members without fetching and merging version and entity descriptions.
     */
    public List<RegisterEntryInfo> listMembers(Register register);

    /**
     * Find all places where the given entity is registered and return the URIs for the coresponding
     * item and the register it is in.
     */
    public List<EntityInfo> listEntityOccurences(String uri);

    // --- Methods for updating information in the store ---

    /**
     * Add a new registered item to a parent register.
     * Initializes the versioning of the new RegisterItem.
     * If the entity of the item is a Register then the versioning of the new sub-register will be initialized.
     * Otherwise a new entity graph will be created for the entity.
     * Does not increment the version of the register itself, call update on the register to achieve that
     */
    public void addToRegister(Register register, RegisterItem item);

    /**
     * Add a new registered item to a parent register.
     * Initializes the versioning of the new RegisterItem.
     * If the entity of the item is a Register then the versioning of the new sub-register will be initialized.
     * Otherwise a new entity graph will be created for the entity.
     * Does not increment the version of the register itself, call update on the register to achieve that
     */
    public void addToRegister(Register register, RegisterItem item, Calendar timestamp);

    /**
     * Update the metadata for a register, managing the versioning information.
     * @return the URI of the new version of the item
     */
    public String update(Register register);

    /**
     * Update a Register item
     * @param withEntity if true then a new version of the entity will be saved, if false then
     * just the item metadata will be udpated.
     * @return the URI of the new version of the item
     */
    public String update(RegisterItem item, boolean withEntity);

    /**
     * Update the metadata for a register, managing the versioning information.
     * @return the URI of the new version of the item
     */
    public String update(Register register, Calendar timestamp);

    /**
     * Update a Register item
     * @param withEntity if true then a new version of the entity will be saved, if false then
     * just the item metadata will be udpated.
     * @return the URI of the new version of the item
     */
    public String update(RegisterItem item, boolean withEntity, Calendar timestamp);

    /**
     * Store a set of RDF data as a graph in the store, replacing any previous version for this graph
     */
    public void storeGraph(String graphURI, Model model);

    /**
     * Return a set of RDF from a named graph in the store
     */
    public Model getGraph(String graphURI);

    // --- misc operations ---

    /**
     * Free text search over the registered items
     * @param query the search parameters
     * @return list of URIs for the matched RegisterItems
     */
    public List<String> search(SearchRequest query);

    /**
     * Tests if the register contains an item with the given notation (relative URI)
     */
    public boolean contains(Register register, String notation);

    /**
     * Loads a bootstrap file which defines the initial state of the registry. Should at least
     * contain a root register but may also define vocabulary information or system registers.
     * Non-sparql store implementations may require non-trivial processing to separate out the
     * different system registers and may impose constraints on the file packaging supported.
     */
    public void loadBootstrap(String filename);

    /**
     * List all delegation/forwarding/federation records in the registry.
     */
    public List<ForwardingRecord> listDelegations();

    /**
     * Run a sparql query the store. It is guaranateed to be run
     * in an environment in which all annotation graphs area available as named graphs,
     * there are no guarantees on access to item versioning.
     */
    // This breaks the internal goal of hiding sparql, which in turn was to leave
    // open a path to simpler versioning implementations and maybe non-triple store implementations
    // The weasel words in the javadoc are intended to leave the possibility of just using
    // SPARQL for accessing the graph annotations (for which there's no alternative anyway)
    public ResultSet query(String query);
    
    /**
     * Delete all versions of an entry from the store.
     * For an item it deletes the associated entity (including it's graph).
     * For a register it recursively deletes all the register members as well.
     */
    public void delete(String uri);
    
    /**
     * Generate an export of the complete registry state for some tree of registers
     */
    public void exportTree(String uri, StreamRDF out);
    
    /**
     * Import a complete registry state, deletes the existing state if any
     */
    public StreamRDF importTree(String uri);
    
}
