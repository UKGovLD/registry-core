/******************************************************************
 * File:        StoreAPI.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import java.util.Calendar;
import java.util.List;

import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.ForwardingRecord;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.server.indexers.LuceneResult;
import com.hp.hpl.jena.rdf.model.Resource;


/**
 * Abstract interface onto the underlying versioned RDF storage and
 * indexing system. Should support non-RDF storage options as well as
 * straight triple stores.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface StoreAPI {

    /**
     * Lock a specific resource for updating. Will block until any existing lock is lifted.
     */
    public void lock(String uri);

    /**
     * Release the "forupdate" lock on the given URI (should be a Register or RegisterItem).
     * Throws an error if there is no such lock.
     */
    public void unlock(String uri);

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
     * @return Description containing a merge of the selected Version and the root VersionedThing
     */
    public Description getVersion(String uri);

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

    public RegisterItem getItemWithVersion(String uri, boolean withEntity);

    /**
     * Fetch the entity specified by the Register item and add it to the item's data structure.
     */
    public Resource getEntity(RegisterItem item);

    /**
     * Retrieve a set of RegisterItems
     * @param itemURIs the URIs of the items to retrieve
     * @param withEntity if true then the entity definition should be retrieved as well
     * @param withVersion if true then the version information should be retained instead of being flattend out.
     */
    public List<RegisterItem> fetchAll(List<String> itemURIs, boolean withEntity, boolean withVersion);

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
     */
    public void addToRegister(Register register, RegisterItem item);

    /**
     * Add a new registered item to a parent register.
     * Initializes the versioning of the new RegisterItem.
     * If the entity of the item is a Register then the versioning of the new sub-register will be initialized.
     * Otherwise a new entity graph will be created for the entity.
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


    // --- misc operations ---

    /**
     * Free text search over the registered items
     * @param query the text query, supports lucence search syntax
     * @param offset the number of results to skip (to find desired page)
     * @param maxresults the maximum number of results to return
     * @param fields alternating list of tagname/tagvalue pairs
     */
    public LuceneResult[] search(String query, int offset, int maxresults, String...fields);

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
}
