/******************************************************************
 * (c) Copyright 2015, Epimorphics Limited
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

import java.util.ArrayList;
import java.util.List;

/**
 * A specification for a free-text search.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
// Would be nice to put all the SPARQL query logic in here but that
// would couple us too strongly to SPARQL and indeed jena-text.
public class SearchRequest {
 
    protected String query;
    protected Integer offset;
    protected Integer limit;
    protected boolean searchVersions;
    protected String status;
    protected List<FilterSpec> filters = new ArrayList<>();
    
    public SearchRequest(String query) {
        this.query = query;
    }
    
    public String getQuery() {
        return query;
    }
    
    /**
     * Set the free text query, supports Lucene query syntax
     */
    public SearchRequest setQuery(String query) {
        this.query = query;
        return this;
    }
    
    public Integer getOffset() {
        return offset;
    }

    /**
     * Sets the number of search results to skip before returning matches
     */
    public SearchRequest setOffset(Integer offset) {
        this.offset = offset;
        return this;
    }
    
    public Integer getLimit() {
        return limit;
    }

    /**
     * Sets an upper bound on the number of results to return
     */
    public SearchRequest setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }
    
    public boolean isSearchVersions() {
        return searchVersions;
    }
    
    /**
     * Set to true to include different versions of an item in the search.
     * By default only matches on the current version.
     * Placeholder - may not be implemented.
     */
    public SearchRequest setSearchVersions(boolean searchVersions) {
        this.searchVersions = searchVersions;
        return this;
    }
    
    public String getStatus() {
        return status;
    }

    /**
     * Set the query to limit the matches those items with the given status
     * Placeholder - may not be implemented.
     */
    public SearchRequest setStatus(String status) {
        this.status = status;
        return this;
    }
    
    public List<FilterSpec> getFilters() {
        return filters;
    }

    /**
     * Add a filter on the value of a property of the entity.
     * @param key   The URI of the property to filter (called should expand any curies)
     * @param value The value the match, this is either the lexical form of a literal or (if it begins with http[s]) a resource URI
     */
    public SearchRequest addFilter(String key, String value) {
        filters.add( FilterSpec.filterFor(key, value) );
        return this;
    }
    
    
}
