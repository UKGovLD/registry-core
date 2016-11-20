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

package com.epimorphics.registry.util;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.out.NodeToLabel;
import org.apache.jena.riot.system.SyntaxLabels;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.core.RDFParser;

public class JenaJSONLDParser implements RDFParser {
    
    NodeToLabel labels = SyntaxLabels.createNodeToLabel() ;
    
    @Override
    public RDFDataset parse(Object input) throws JsonLdError {
        RDFDataset dataset = new RDFDataset();
        Model m = (Model)input;
        for (StmtIterator i = m.listStatements(); i.hasNext(); ) {
            Statement stmt = i.next();
            String s = idForResource( stmt.getSubject() );
            String p = idForResource( stmt.getPredicate() );
            RDFNode o = stmt.getObject();
            if( o.isLiteral() ) {
                Literal l = o.asLiteral();
                String lang = l.getLanguage();
                if (lang != null && lang.length() == 0) {
                    lang = null ;
                }
                String dt = l.getDatatypeURI();
                if (dt == null ) {
                    dt = XSDDatatype.XSDstring.getURI() ;
                }
                dataset.addTriple(s, p, l.getLexicalForm(), dt, lang);
            } else {
                dataset.addTriple(s, p, idForResource( o.asResource() ));
            }
        }
        return dataset;
    }
    
    private String idForResource(Resource r) {
        if (r.isAnon()) {
            return labels.get(null, r.asNode());
        } else {
            return r.getURI();
        }
    }
}
