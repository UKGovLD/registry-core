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

package com.epimorphics.registry.csv;

import static com.epimorphics.registry.csv.CSVBaseWriter.VALUE_SEP;
import static com.epimorphics.registry.csv.CSVBaseWriter.VALUE_SEP_CHAR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.vocabulary.XSD;

public class RDFCSVUtil {
    
    /**
     * Encode an RDF value as a cell value
     */
    public static String encode(RDFNode value, PrefixMapping prefixes) {
        return encode(value, prefixes, new HashSet<Resource>());
    }
    
    public static String encode(RDFNode value, PrefixMapping prefixes, Set<Resource> seen) {
        if (value.isResource()) {
            Resource r = value.asResource();
            if (value.isURIResource()) {
                return asPrefixOrURI( r.getURI(), prefixes);
            } else {
                return serializeBnode(r, prefixes, seen);
            }
        } else {
            Literal l = value.asLiteral();
            String lex = l.getLexicalForm().replace("'", "\\'");
            if (l.getLanguage() == null || l.getLanguage().isEmpty()) {
                if (l.getDatatype() == null) {
                    if (lex.contains("\n")) {
                        return "'''" + lex + "'''";
                    } else {
                        return "'" + lex + "'";
                    }
                } else {
                    String dt = l.getDatatypeURI();
                    for (String nt : NUMERIC_TYPES) {
                        if (dt.equals(nt)) {
                            return l.getLexicalForm();   // Treat numbers as plain, means round trip isn't safe TODO Review this 
                        }
                    }
                    return String.format("'%s'^^%s", lex, asPrefixOrURI(dt, prefixes));
                }
            } else {
                return String.format("'%s'@%s", lex, l.getLanguage());
            }
        }
    }
    
    
    private static String asPrefixOrURI(String uri, PrefixMapping prefixes) {
        String prefixed = prefixes.shortForm(uri);
        if (uri.equals(prefixed)) {
            return "<" + uri + ">";
        } else {
            return prefixed;
        }
    }
    
    private static final String[] NUMERIC_TYPES = new String[]{XSD.xint.getURI(), XSD.xlong.getURI(), 
        XSD.integer.getURI(), XSD.decimal.getURI(), XSD.xfloat.getURI(), XSD.xdouble.getURI()};

    /**
     * Takes a cell value (already parsed by a CSV reader) and unpacks it 
     * into possibly multiple values
     */
    public static List<String> unpackMultiValues(String value) {
        if (value.contains(VALUE_SEP)) {
            List<String> values = new ArrayList<>();
            String remainder = value;
            String nextValue = "";
            while ( ! remainder.isEmpty()) {
                int sofar = -1;
                while (true) {
                    int split = remainder.indexOf(VALUE_SEP_CHAR, sofar);
                    if (split < 0) {
                        nextValue = remainder;
                        remainder = "";
                        break;
                    } else if (split == remainder.length() - 1) {
                        nextValue = remainder.substring(0, remainder.length() - 1);
                        remainder = "";
                        break;
                    } else if (remainder.charAt(split+1) == VALUE_SEP_CHAR) {
                        remainder = remainder.substring(0, split) + remainder.substring(split + 1);
                        sofar = split + 1;
                    } else {
                        nextValue = remainder.substring(0, split);
                        remainder = remainder.substring(split + 1);
                        break;
                    }
                }
                values.add( nextValue );
            }
            return values;
        } else {
            return Collections.singletonList(value);
        }
    }
    
    // Direct implementation rather than use turtle serializer 
    // because we have to force use of ' rather than " to avoid upsetting poor dumb little excel
    private static String serializeBnode(Resource r, PrefixMapping prefixes, Set<Resource> seen) {
        if (seen.contains(r)) {
            throw new EpiException("Circular reference, can only serialize tree-shaped bNodes");
        }
        seen.add(r);
        
        StringBuffer ser = new StringBuffer();
        ser.append("[" );
        for (StmtIterator si = r.listProperties(); si.hasNext(); ) {
            Statement s = si.next();
            ser.append( encode(s.getPredicate(), prefixes, seen) );
            ser.append(" ");
            ser.append( encode(s.getObject(), prefixes, seen) );
            ser.append("; ");
        }
        ser.append("]");
        return ser.toString();
    }
}
