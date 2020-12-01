/******************************************************************
 * File:        CachingStore.java
 * Created by:  Dave Reynolds
 * Created on:  3 Feb 2013
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
import java.util.Collection;
import java.util.List;

import org.apache.jena.riot.system.StreamRDF;

import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.ForwardingRecord;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.util.DescriptionCache;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

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
    public void beginRead() {
        store.beginRead();
    }
    
    @Override
    public void beginWrite() {
        store.beginWrite();
    }

    @Override
    public void commit() {
        store.commit();
    }

    @Override
    public void abort() {
        store.abort();
    }
    
    @Override
    public void end() {
        store.end();
    }
    
    @Override
    public synchronized void beginSafeRead() {
        store.beginSafeRead();
    }
    
    @Override
    public synchronized void endSafeRead() {
        store.endSafeRead();
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
    public Description getVersion(String uri, boolean withEntity) {
        return store.getVersion(uri, withEntity);
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

//    @Override
//    public RegisterItem getItemWithVersion(String uri, boolean withEntity) {
//        return store.getItemWithVersion(uri, withEntity);
//    }

    @Override
    public Resource getEntity(RegisterItem item) {
        return store.getEntity(item);
    }

    @Override
    public List<RegisterEntryInfo> listMembers(Register register) {
        return store.listMembers(register);
    }
    
    @Override
    public List<RegisterEntryInfo> listMembers(Register register, List<FilterSpec> filters) {
        return store.listMembers(register, filters);
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
        if (withEntity && item.isRegister()) {
            cache.flush(item.getEntity().getURI());
        }
        return store.update(item, withEntity);
    }

    @Override
    public String update(RegisterItem item, boolean withEntity,
            Calendar timestamp) {
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
            boolean withEntity) {
        return store.fetchAll(itemURIs, withEntity);
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
    public List<String> search(SearchRequest request) {
        return store.search(request);
    }

    @Override
    public List<ForwardingRecord> listDelegations() {
        return store.listDelegations();
    }

    @Override
    public void storeGraph(String graphURI, Model model) {
        store.storeGraph(graphURI, model);
    }

    @Override
    public Model getGraph(String graphURI) {
        return store.getGraph(graphURI);
    }

    @Override
    public ResultSet query(String query) {
        return store.query(query);
    }

    @Override
    public List<String> delete(String uri) {
        return store.delete(uri);
    }

    @Override
    public void exportTree(String uri, StreamRDF out) {
        store.exportTree(uri, out);
    }

    @Override
    public StreamRDF importTree(String uri) {
        return store.importTree(uri);
    }

    @Override
    public Model findSimilar(Collection<RegisterItem> items, Boolean withEdits, Double similarity) { return store.findSimilar(items, withEdits, similarity); }
}
