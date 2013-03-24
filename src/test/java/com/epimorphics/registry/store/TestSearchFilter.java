/******************************************************************
 * File:        TestSearchFilter.java
 * Created by:  Dave Reynolds
 * Created on:  24 Mar 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.junit.Test;
import static org.junit.Assert.*;

import com.epimorphics.server.indexers.LuceneIndex;
import com.epimorphics.server.indexers.LuceneResult;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class TestSearchFilter {

    @Test
    public void testSearchFilter() throws ParseException {
        LuceneIndex index = new LuceneIndex();
        Map<String, String> config = new HashMap<String, String>();
        config.put("config", "src/main/webapp/WEB-INF/index-config.ttl");
        index.init(config, null);
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 4; v++) {
                index.addGraph(BASE_URI + "graph/" + u, graphFor("u"+u, "label" + (u%4)));
            }
        }
        LuceneResult[] results = index.search(queryFor("label1"), 0, 100);
        assertEquals(16, results.length);
        
        results = SearchFilter.search(index, queryFor("label1"), 0, 100);
        assertEquals(4, results.length);
        
        results = SearchFilter.search(index, queryFor("label1"), 0, 2);
        assertEquals(2, results.length);
        assertEquals("http://example.com/test/u1", results[0].getURI());
        assertEquals("http://example.com/test/u5", results[1].getURI());
        
        results = SearchFilter.search(index, queryFor("label1"), 1, 2);
        assertEquals(2, results.length);
        assertEquals("http://example.com/test/u5", results[0].getURI());
        assertEquals("http://example.com/test/u9", results[1].getURI());
        
        results = SearchFilter.search(index, queryFor("label1"), 2, 2);
        assertEquals(2, results.length);
        assertEquals("http://example.com/test/u9", results[0].getURI());
        assertEquals("http://example.com/test/u13", results[1].getURI());
    }
    
    private static final String BASE_URI = "http://example.com/test/";
    
    private Model graphFor(String uri, String label) {
        Model m =  ModelFactory.createDefaultModel();
         m.createResource(BASE_URI + uri)
                .addProperty(RDFS.label, label)
                .addProperty(RDF.type, SKOS.Concept);
        return m;
    }
    
    private Query queryFor(String search) throws ParseException {
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
        QueryParser parser = new QueryParser(Version.LUCENE_40, LuceneIndex.FIELD_LABEL, analyzer);
        return parser.parse(search);
    }
    
}
