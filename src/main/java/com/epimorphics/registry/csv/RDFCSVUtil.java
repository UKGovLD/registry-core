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

import static com.epimorphics.registry.csv.CSVBaseWriter.QUOTE;
import static com.epimorphics.registry.csv.CSVBaseWriter.QUOTED_QUOTE;
import static com.epimorphics.registry.csv.CSVBaseWriter.QUOTED_VALUE_SEP;
import static com.epimorphics.registry.csv.CSVBaseWriter.QUOTE_CHAR;
import static com.epimorphics.registry.csv.CSVBaseWriter.VALUE_SEP;
import static com.epimorphics.registry.csv.CSVBaseWriter.VALUE_SEP_CHAR;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.out.NodeFmtLib;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.vocabulary.XSD;

public class RDFCSVUtil {
    
    /**
     * Encode an RDF value as a cell value
     */
    public static String encode(RDFNode value, PrefixMapping prefixes) {
        if (value.isResource()) {
            Resource r = value.asResource();
            if (value.isURIResource()) {
                String uri = r.getURI();
                String prefixed = prefixes.shortForm(uri);
                if (uri.equals(prefixed)) {
                    return "<" + uri + ">";
                } else {
                    return prefixed;
                }
            } else {
                return serializeBnode(r, prefixes);
            }
        } else {
            Literal l = value.asLiteral();
            if (l.getLanguage() == null || l.getLanguage().isEmpty()) {
                if (l.getDatatype() != null) {
                    String dt = l.getDatatypeURI();
                    for (String nt : NUMERIC_TYPES) {
                        if (dt.equals(nt)) {
                            return l.getLexicalForm();   // Treat numbers as plain, means round trip isn't safe TODO Review this 
                        }
                    }
                }
            }
            String fmt = NodeFmtLib.str( value.asNode() );
            // Use ' instead of " otherwise excel goes mad
            return fmt.replace("'", "\\'").replace('"', '\'');
        }
    }
    
    private static final String[] NUMERIC_TYPES = new String[]{XSD.xint.getURI(), XSD.xlong.getURI(), 
        XSD.integer.getURI(), XSD.decimal.getURI(), XSD.xfloat.getURI(), XSD.xdouble.getURI()};

    /**
     * Takes a cell value (already parsed by a CSV reader) and unpacks it 
     * into possibly multiple values
     */
    public static List<String> unpackageMultiValues(String value) {
        if (value.contains(VALUE_SEP)) {
            List<String> values = new ArrayList<>();
            String remainder = value;
            while ( ! remainder.isEmpty()) {
                int split = nextSpit(remainder);
                if (split == -1) {
                    values.add( unquote(remainder) );
                    break;
                } else {
                    values.add( unquote(remainder.substring(0, split)) );
                    remainder = remainder.substring(split+1);
                }
            }
            return values;
        } else {
            return Collections.singletonList(value);
        }
    }
    
    private static int nextSpit(String value) {
        int sofar = -1;
        while (true) {
            int split = value.indexOf(VALUE_SEP_CHAR, sofar);
            if (split <= 0) {
                return split;
            } else {
                if (value.charAt(split-1) != QUOTE_CHAR) {
                    return split;
                }
                sofar = split+1;
            }
        }
    }
    
    private static String unquote(String value) {
        return value.replace(QUOTED_VALUE_SEP, VALUE_SEP).replace(QUOTED_QUOTE, QUOTE);
    }
    
    private static String MARKER_URI = "http://donotuse/donotuse";
    private static String MARKER_SER = "<" + MARKER_URI + ">";
    private static Property MARKER_PROP = ResourceFactory.createProperty(MARKER_URI);
    
    private static String serializeBnode(Resource r, PrefixMapping prefixes) {
        Model closure = Closure.closure(r, false);
        closure.createResource(MARKER_URI).addProperty(MARKER_PROP, r);
        closure.setNsPrefixes(prefixes);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        closure.write(buffer, RDFLanguages.strLangTurtle);
        try {
            String serialization = buffer.toString(StandardCharsets.UTF_8.name());
            String[] lines= serialization.split("\n");
            int i = 0;
            StringBuffer result = new StringBuffer();
            while (i < lines.length) {
                String line = lines[i++].trim();
                if (line.isEmpty() || line.contains("@prefix")) {
                    continue;
                } else {
                    result.append(line.replace(MARKER_SER, ""));
                    break;
                }
            }
            while (i < lines.length) {
                result.append( " " + lines[i++].trim().replace(MARKER_SER, "") );
            }
            String ser = result.toString().trim();
            if (ser.endsWith(".")) {
                // This should always be the case.
                ser = ser.substring(0, ser.length()-1);
            }
            return ser;
        } catch (UnsupportedEncodingException e) {
            // Can't happen, UTF-8 always supported
            e.printStackTrace();
            return "";
        }
        
    }
}
