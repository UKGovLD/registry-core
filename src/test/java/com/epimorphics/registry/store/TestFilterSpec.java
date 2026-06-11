/******************************************************************
 * File:        TestFilterSpec.java
 * Created by:  Dave Reynolds
 * Created on:  12 Jun 2017
 * 
 * (c) Copyright 2017, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestFilterSpec {

    @Test
    public void testBasics() {
        FilterSpec spec = new FilterSpec("rdfs:label", "foo");
        assertEquals("    ?entity <http://www.w3.org/2000/01/rdf-schema#label> ?var0 .\n    FILTER(?var0 = \"foo\")\n", spec.asQuery("entity", 0));

        spec = new FilterSpec("dct:date", "2017-06-12");
        assertEquals("    ?entity <http://purl.org/dc/terms/date> ?var1 .\n    FILTER(?var1 = \"2017-06-12\"^^<http://www.w3.org/2001/XMLSchema#date>)\n",
                 spec.asQuery("entity", 1));
        
        spec = new FilterSpec("rdf:type", "skos:Concept");
        assertEquals( "    ?entity <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?var2 .\n    FILTER(?var2 = <http://www.w3.org/2004/02/skos/core#Concept>)\n",
                spec.asQuery("entity", 2));
        
        assertEquals(
                "filter: <http://www.w3.org/2004/02/skos/core#notation> gte \"1\"^^<http://www.w3.org/2001/XMLSchema#integer>",
                 FilterSpec.filterFor("min-skos:notation", "1").toString() );
        
        assertEquals(
                "filter: <http://www.w3.org/2004/02/skos/core#notation> equal \"1\"^^<http://www.w3.org/2001/XMLSchema#integer>",
                 FilterSpec.filterFor("skos:notation", "1").toString() );
        
        assertEquals(
                "filter: <http://www.w3.org/2004/02/skos/core#notation> lt \"4\"^^<http://www.w3.org/2001/XMLSchema#integer>",
                 FilterSpec.filterFor("maxEx-skos:notation", "4").toString() );
    }
}
