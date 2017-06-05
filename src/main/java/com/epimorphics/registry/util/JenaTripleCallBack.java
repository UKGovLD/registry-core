/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// 
package com.epimorphics.registry.util;

// Largely a direct copy of the Jena JsonLDReader but with the details exposed for our use case

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.lang.LabelToNode;
import org.apache.jena.riot.system.SyntaxLabels;

import com.github.jsonldjava.core.JsonLdTripleCallback;
import com.github.jsonldjava.core.RDFDataset;

public class JenaTripleCallBack implements JsonLdTripleCallback {

    private LabelToNode  labels     = SyntaxLabels.createLabelToNode() ;

    public static String LITERAL    = "literal" ;
    public static String BLANK_NODE = "blank node" ;
    public static String IRI        = "IRI" ;
    public static final String xsdString = XSDDatatype.XSDstring.getURI() ;

    @Override
    public Object call(RDFDataset dataset) {
        Model model = ModelFactory.createDefaultModel();
        
        // Copy across namespaces
        for (Entry<String, String> namespace : dataset.getNamespaces().entrySet()) {
            model.setNsPrefix(namespace.getKey(), namespace.getValue());
        }
        
        // Copy triples and quads
        for ( String gn : dataset.keySet() ) {
            Object x = dataset.get(gn) ;
            if ( "@default".equals(gn) ) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> triples = (List<Map<String, Object>>)x ;
                for ( Map<String, Object> t : triples ) {
                    Node s = createNode(t, "subject") ;
                    Node p = createNode(t, "predicate") ;
                    Node o = createNode(t, "object") ;
                    model.getGraph().add( new Triple(s,p,o) );
                }
            } 
        }
        return model ;
    }

    private Node createNode(Map<String, Object> tripleMap, String key) {
        @SuppressWarnings("unchecked")
        Map<String, Object> x = (Map<String, Object>)(tripleMap.get(key)) ;
        return createNode(x) ;
    }

    
    private Node createNode(Map<String, Object> map) {
        String type = (String)map.get("type") ;
        String lex = (String)map.get("value") ;
        if ( type.equals(IRI) )
            return NodeFactory.createURI(lex);
        else if ( type.equals(BLANK_NODE) )
            return labels.get(null, lex) ;  //??
        else if ( type.equals(LITERAL) ) {
            String lang = (String)map.get("language") ;
            String datatype = (String)map.get("datatype") ;
            if ( Objects.equals(xsdString, datatype) )
                // In RDF 1.1, simple literals and xsd:string are the same.
                // During migration, we prefer simple literals to xsd:strings. 
                datatype = null ;
            if ( lang == null && datatype == null )
                return NodeFactory.createLiteral(lex);
            if ( lang != null )
                return NodeFactory.createLiteral(lex, lang);
            RDFDatatype dt = NodeFactory.getType(datatype) ;
            return NodeFactory.createLiteral(lex, dt);
        } else
            throw new InternalErrorException("Node is not a IRI, bNode or a literal: " + type) ;
    }

}
