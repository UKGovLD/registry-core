/******************************************************************
 * File:        DescriptionCache.java
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
        cache.remove(uri);
    }
    
    public void clear() {
        cache.clear();
    }
}
