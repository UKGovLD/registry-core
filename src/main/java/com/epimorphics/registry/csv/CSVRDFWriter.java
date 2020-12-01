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

import java.io.OutputStream;
import java.util.*;

import com.epimorphics.registry.webapi.LibReg;
import com.epimorphics.util.EpiException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.PrefixMapping;

import javax.ws.rs.core.MultivaluedHashMap;

/**
 * Support for writing RDF resources encoded into CSV files.
 * In typical use should set baseURI, any prefixes and then any headers before starting writing.
 */
public class CSVRDFWriter extends CSVBaseWriter {
    public static final String ID_COL = "@id";

    protected PrefixMapping prefixes;
    protected String baseURI;
    protected Set<String> headers = new HashSet<>();
    protected boolean started = false;
    
    public CSVRDFWriter(OutputStream out, PrefixMapping mapping) {
        super(out);
        this.prefixes = mapping;
    }
    
    /**
     * Provide an optional baseURI to which all resource URIs will be relativised.
     */
    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }
    
    /**
     * Register an additional header to be included in the CSV.
     * Non-RDF headers should start with "@".
     */
    public void addHeader(String header) {
        headers.add(header);
    }
    
    /**
     * Register a set of additional header to be included in the CSV.
     * Non-RDF headers should start with "@".
     */
    public void addHeader(String[] headers) {
        for (String header: headers) addHeader(header);
    }
    
    /**
     * Register headers needed to serialize the given Resource  
     */
    public void addHeader(Resource r) {
        addHeader( Collections.singletonList(r) );
    }
    
    /**
     * Register headers needed to serialize the given set of Resources  
     */
    public void addHeader(List<Resource> resources) {
        Set<Property> props = new HashSet<>();
        for (Resource r : resources) {
            for (StmtIterator i = r.listProperties(); i.hasNext();) {
                props.add( i.next().getPredicate() );
            }
        }
        for (Property prop : props) {
            addHeader( RDFCSVUtil.encode(prop, prefixes) );
        }
    }
    
    protected void checkStarted() {
        if (!started) {
            headers.add( ID_COL );
            List<String> headerList = new ArrayList<>( headers );
            Collections.sort(headerList);
            setHeaders(headerList);
            started = true;
        }
    }
    
    /**
     * Write a resource to the current row.
     * Any property which does not match a registered header is silently ignored.
     * Does not finish the row, so other cells may be added.
     */
    public void write(Resource r) {
        checkStarted();
        String uri = r.getURI();
        if (uri == null) {
            uri = "";
        } else {
            if (baseURI != null && uri.startsWith(baseURI)) {
                uri = uri.substring(baseURI.length());
            }
        }
        write(ID_COL, "<" + uri + ">");
        MultivaluedHashMap<Property, RDFNode> valuesByProp = new MultivaluedHashMap<>();
        r.listProperties().forEachRemaining(stmt -> {
            valuesByProp.add(stmt.getPredicate(), stmt.getObject());
        });
        valuesByProp.forEach((prop, nodes) -> {
            nodes.sort(Comparator.comparing((node) -> {
                if (node.isLiteral()) {
                    String lang = node.asLiteral().getLanguage();
                    return lang == null ? null
                        : lang.isEmpty() ? null
                        : lang.equals("en") ? "" // first
                        : lang;
                } else {
                    return null;
                }
            }, Comparator.nullsLast(String::compareTo)));
            String header = RDFCSVUtil.encode(prop, prefixes);
            nodes.forEach(node -> {
                String value = RDFCSVUtil.encode(node, prefixes);
                try {
                    write(header, value);
                } catch (EpiException e) {
                    // Ignore
                }
            });
        });
    }
}
