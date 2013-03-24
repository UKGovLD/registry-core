/******************************************************************
 * File:        SearchFilter.java
 * Created by:  Dave Reynolds
 * Created on:  24 Mar 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
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
