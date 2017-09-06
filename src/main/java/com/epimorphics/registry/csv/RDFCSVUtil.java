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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.system.IRIResolver;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.XSD;

import com.epimorphics.util.EpiException;

public class RDFCSVUtil {
    public static final String STATUS_HEADER = "@status";
    public static final String NOTATION_HEADER = "@notation";
    public static final String SUCCESSOR_HEADER = "@successor";
    public static final String SUCCEEDS_HEADER = "@succeeds";
    public static final String MEDIA_TYPE = "text/csv";
    
    /**
     * Encode an RDF value as a cell value
     */
    public static String encode(RDFNode value, PrefixMapping prefixes) {
        return encode(value, prefixes, false, new HashSet<Resource>());
    }
    
    public static String encode(RDFNode value, PrefixMapping prefixes, boolean embedded, Set<Resource> seen) {
        if (value.isResource()) {
            Resource r = value.asResource();
            if (value.isURIResource()) {
                return asPrefixOrURI( r.getURI(), prefixes);
            } else {
                return serializeBnode(r, prefixes, seen);
            }
        } else {
            Literal l = value.asLiteral();
            String lex = l.getLexicalForm();
            if (l.getLanguage() == null || l.getLanguage().isEmpty()) {
                if (l.getDatatype() == null || l.getDatatypeURI().equals(XSD.xstring.getURI())) {
                    if (embedded) {
                        if (lex.contains("\n")) {
                            return "'''" + lex + "'''";
                        } else {
                            return "'" + lex.replace("'", "\\'") + "'";
                        }
                    } else {
                        if (TRAP_PATTERN.matcher(lex).matches() || lex.contains(VALUE_SEP)) {
                            // String that looks like a number or boolean so quote it
                            return "'" + lex.replace("'", "\\'") + "'";
                        } else {
                            return lex;
                        }
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
    
    /**
     * Decode a URI value from a csv
     */
    public static String asURI(String colValue, PrefixMapping prefixes, String baseURI) {
        colValue = colValue.trim();
        if (colValue.startsWith("<") && colValue.endsWith(">")) {
            String uri = colValue.substring(1, colValue.length()-1);
            return IRIResolver.resolve(uri, baseURI).toString();
        } else if (QNAME_PATTERN.matcher(colValue).matches()) {
            return prefixes.expandPrefix(colValue);
        } else {
            throw new EpiException("Not a legal URI encoding in CSV cell: " + colValue);
        }
    }
    
    /**
     * Package a value read back from a CSV into Turtle syntax for building up parsable description.
     * Most of the serialization is already Turtle
     */
    public static String toTurtle(String value) {
        if (value.startsWith("'")) {
            // Assume a property quoted string which may have lang tag
            return value;
        } else if (URI_PATTERN.matcher(value).matches()) {
            // URI or serialized bnode, let it through
            return value;
        } else if (TRAP_PATTERN.matcher(value).matches()) {
            // boolean or number, let through
            return value;
        }
        if (value.contains("\n")) {
            return "'''" + value + "'''";
        } else {
            return "'" + value.replace("'", "\\'") + "'";
        }
    }
    protected static final Pattern NUMBER_PATTERN = Pattern.compile("(-\\s*)?[0-9]+(\\.[0-9]+)?([eE][-+]?[0-9]+(\\.[0-9]+)?)?");
    protected static final Pattern TRAP_PATTERN = Pattern.compile("(-\\s*)?[0-9]+(\\.[0-9]+)?([eE][-+]?[0-9]+(\\.[0-9]+)?)?|true|TRUE|false|FALSE|[a-zA-Z_0-9\\-]+:\\S*");
    protected static final Pattern URI_PATTERN = Pattern.compile("<.*>|\\[.*\\]");
    protected static final Pattern QNAME_PATTERN = Pattern.compile("[a-zA-Z_0-9\\-]+:\\S*");
    
    private static String asPrefixOrURI(String uri, PrefixMapping prefixes) {
        String prefixed = prefixes.shortForm(uri);
        if (uri.equals(prefixed)) {
            return "<" + uri + ">";
        } else {
            return prefixed;
        }
    }
    
    private static final String[] NUMERIC_TYPES = new String[]{XSD.xint.getURI(), XSD.xlong.getURI(), 
        XSD.integer.getURI(), XSD.decimal.getURI(), XSD.xfloat.getURI(), XSD.xdouble.getURI(), XSD.xboolean.getURI()};

    /**
     * Takes a cell value (already parsed by a CSV reader) and unpacks it 
     * into possibly multiple values
     */
    public static List<String> unpackMultiValues(String value) {
        if (value.contains(VALUE_SEP)) {
            List<String> values = new ArrayList<>();
            // parse into | separated components
            // but if component starts with ' (after whitespace chomp) then ignore | until quote is closed
            // an embedded single ' is not quoting (sigh)
            // allow \ as an arbitrary escape for everything including itself
            int start = 0;
            while( start < value.length() ) {
                start = startOfNextToken(value, start);
                int index = start;
                boolean quoted = false;
                if ( value.charAt(start) == '\'' ) {
                    quoted = true;
                    index++;
                }
                for( ; index < value.length(); index++ ) {
                    char c = value.charAt(index);
                    if (c == '\\') {
                        // Escape next character
                        index++;
                        continue;
                    } else if (c == '\'') {
                        quoted = false;
                    } else if (c == '|' && !quoted) {
                        break;
                    }
                }
                values.add( value.substring(start, index).trim() );
                start = index+1;
            }            
            return values;
        } else {
            return Collections.singletonList(value.trim());
        }
    }
    
    private static int startOfNextToken(String value, int start) {
        int next = start;
        while (next < value.length()) {
            if ( ! Character.isWhitespace( value.charAt(next)) ) {
                break;
            }
            next++;
        }
        return next;
    }
    
    public static void main(String[] args) {
        String lex = "a|'name'@en|'b'|<a> | <b>|'foo | bar'|don't|do|'a \\'| string'";
        for (String value: unpackMultiValues(lex)) {
            System.out.println("> " + value);
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
            ser.append( encode(s.getPredicate(), prefixes, true, seen) );
            ser.append(" ");
            ser.append( encode(s.getObject(), prefixes, true, seen) );
            ser.append("; ");
        }
        ser.append("]");
        return ser.toString();
    }
}
