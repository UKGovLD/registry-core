/******************************************************************
 * File:        DescriptionCache.java
 * Created by:  Dave Reynolds
 * Created on:  3 Feb 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import org.apache.commons.collections.map.LRUMap;

import com.epimorphics.registry.core.Description;

/**
 * Cache for Description objects which can optionally be injected into stores.
 * Especially useful for caching Register objects with their membership list.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class DescriptionCache  {
    static final int DEFAULT_SIZE = 100;
    
    LRUMap cache;
    
    public DescriptionCache() {
        this(DEFAULT_SIZE);
    }
    
    public DescriptionCache(int size) {
        cache = new LRUMap(size);
    }
    
    public void cache(Description d) {
        cache.put(d.getRoot().getURI(), d);
    }
    
    public Description get(String uri) {
        return (Description) cache.get(uri);
    }
    
    public void flush(String uri) {
        // TODO replace this with selective reset when we have event broadcast working
        Prefixes.resetCache();
        cache.remove(uri);
    }
    
    public void clear() {
        // TODO replace this with selective reset when we have event broadcast working
        Prefixes.resetCache();
        cache.clear();
    }
}
