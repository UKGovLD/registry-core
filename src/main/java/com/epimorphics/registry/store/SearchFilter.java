/******************************************************************
 * File:        SearchFilter.java
 * Created by:  Dave Reynolds
 * Created on:  24 Mar 2013
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

import org.apache.lucene.search.Query;

import com.epimorphics.server.indexers.LuceneIndex;
import com.epimorphics.server.indexers.LuceneResult;

/**
 * Support for compressing adjacent duplicates in text search results.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class SearchFilter {

    public static LuceneResult[] search(LuceneIndex index, Query query, int offset, int maxResults) {
        LuceneResult[] results = new LuceneResult[maxResults];
        int resultsFound = 0;
        int offsetSoFar = 0;
        String lastURI = null;
        while (resultsFound - offset < maxResults) {
            LuceneResult[] batch = index.search(query, offsetSoFar, maxResults);
            offsetSoFar += maxResults;
            if (batch.length == 0) break;
            for (int i = 0; i < batch.length; i++) {
                LuceneResult next = batch[i];
                if ( ! next.getURI().equals(lastURI) ) {
                    if (resultsFound >= offset) { 
                        results[resultsFound-offset] = next;
                    } 
                    resultsFound++;
                    lastURI = next.getURI();
                    if (resultsFound >= maxResults) break;
                }
            }
        }
        
        if (resultsFound < maxResults) {
            LuceneResult[] temp = new LuceneResult[resultsFound];
            for (int i = 0; i < resultsFound; i++) {
                temp[i] = results[i];
            }
            results = temp;
        }
        return results;
    }
}
