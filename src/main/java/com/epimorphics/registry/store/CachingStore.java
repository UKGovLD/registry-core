/******************************************************************
 * File:        CachingStore.java
 * Created by:  Dave Reynolds
 * Created on:  3 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import java.util.Calendar;
import java.util.List;

import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.util.DescriptionCache;
import com.epimorphics.server.indexers.LuceneResult;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Store wrapper that caches registers.
 * Could have a variant that caches individual descriptions though that seems less useful.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class CachingStore implements StoreAPI {

    protected StoreAPI store;
    protected DescriptionCache cache;

    public CachingStore(StoreAPI store, int cachesize) {
        this.store = store;
        cache = new DescriptionCache(cachesize);
    }

    @Override
    public void lock(String uri) {
        store.lock(uri);
    }

    @Override
    public void unlock(String uri) {
        store.unlock(uri);
    }

    @Override
    public Description getDescription(String uri) {
        return store.getDescription(uri);
    }

    @Override
    public Description getCurrentVersion(String uri) {
        Description d = cache.get(uri);
        if (d == null) {
            d = store.getCurrentVersion(uri);
            if (d instanceof Register) {
                cache.cache(d);
            }
        }
        return d;
    }

    @Override
    public Description getVersion(String uri) {
        return store.getVersion(uri);
    }

    @Override
    public Description getVersionAt(String uri, long time) {
        return store.getVersionAt(uri, time);
    }

    @Override
    public List<VersionInfo> listVersions(String uri) {
        return store.listVersions(uri);
    }

    @Override
    public RegisterItem getItem(String uri, boolean withEntity) {
        return store.getItem(uri, withEntity);
    }

    @Override
    public RegisterItem getItemWithVersion(String uri, boolean withEntity) {
        return store.getItemWithVersion(uri, withEntity);
    }

    @Override
    public Resource getEntity(RegisterItem item) {
        return store.getEntity(item);
    }

    @Override
    public List<RegisterEntryInfo> listMembers(Register register) {
        return store.listMembers(register);
    }

    @Override
    public void addToRegister(Register register, RegisterItem item) {
        cache.flush(register.getRoot().getURI());
        store.addToRegister(register, item);
    }

    @Override
    public void addToRegister(Register register, RegisterItem item,
            Calendar timestamp) {
        cache.flush(register.getRoot().getURI());
        store.addToRegister(register, item, timestamp);
    }

    @Override
    public String update(Register register) {
        cache.flush(register.getRoot().getURI());
        return store.update(register);
    }

    @Override
    public String update(Register register, Calendar timestamp) {
        cache.flush(register.getRoot().getURI());
        return store.update(register, timestamp);
    }

    @Override
    public String update(RegisterItem item, boolean withEntity) {
        cache.flush(item.getRegisterURI());
        return store.update(item, withEntity);
    }

    @Override
    public String update(RegisterItem item, boolean withEntity,
            Calendar timestamp) {
        cache.flush(item.getRegisterURI());
        return update(item, withEntity, timestamp);
    }

    @Override
    public boolean contains(Register register, String notation) {
        return store.contains(register, notation);
    }

    @Override
    public void loadBootstrap(String filename) {
        store.loadBootstrap(filename);
    }

    @Override
    public List<RegisterItem> fetchAll(List<String> itemURIs,
            boolean withEntity, boolean withVersion) {
        return store.fetchAll(itemURIs, withEntity, withVersion);
    }

    @Override
    public long versionStartedAt(String uri) {
        return store.versionStartedAt(uri);
    }

    @Override
    public List<EntityInfo> listEntityOccurences(String uri) {
        return store.listEntityOccurences(uri);
    }

    @Override
    public LuceneResult[] search(String query, int offset, int maxresults, String... fields) {
        return store.search(query, offset, maxresults, fields);
    }

}
