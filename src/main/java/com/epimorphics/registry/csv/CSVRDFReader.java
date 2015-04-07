/******************************************************************
 * (c) Copyright 2015, Epimorphics Limited
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
package com.epimorphics.registry.csv;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;

/**
 * Support for reading back a CSV exported by CSVRDFWriter.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class CSVRDFReader {

    protected CSVReader reader;
    protected InputStream ins;
    protected PrefixMapping prefixes;
    protected Map<String, Integer> headerIndex = new HashMap<String, Integer>();
    protected String[] currentRow;
    protected String[] headers;
    protected String baseURI;
    
    public CSVRDFReader(InputStream ins, PrefixMapping prefixes) {
        reader = new CSVReader(
                    new InputStreamReader(ins, StandardCharsets.UTF_8), 
                    ',', '"', CSVParser.NULL_CHARACTER );
        this.prefixes = prefixes;
        try {
            headers = reader.readNext();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i], i);
            }
        } catch (IOException e) {
            throw new EpiException("Problem reading CSV", e);
        }
    }

    /**
     * Provide an optional baseURI for resolving any relative URIs
     */
    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }    
    
    /**
     * Fetch the next row, decode it as a resource, store the resource in the given model
     */
    public Resource nextResource(Model model) {
        try {
            currentRow = reader.readNext();
            if (currentRow == null) {
                return null;
            }
            // Unsubtle approach but performance is not really the issue here
            StringBuffer src = new StringBuffer();
            if (baseURI != null) {
                src.append( String.format("@base <%s> .\n", baseURI) );
            }
            for (String prefix : prefixes.getNsPrefixMap().keySet()) {
                src.append( String.format("@prefix %s: <%s> .\n", prefix, prefixes.getNsPrefixURI(prefix)) );
            }
            src.append( getColumnValue(CSVRDFWriter.ID_COL)  + "\n");
            for (int i = 0; i < headers.length; i++) {
                String prop = headers[i];
                if (! prop.startsWith("@") ) {
                    src.append("   ");
                    src.append(prop);
                    src.append(" ");
                    boolean started = false;
                    for (String value : RDFCSVUtil.unpackMultiValues( currentRow[i] ) ) {
                        if (started) {
                            src.append(", ");
                        } else {
                            started = true;
                        }
                        src.append(value);
                    }
                    src.append(";\n");
                }
            }
            src.append(".\n");
            
            InputStream isrc = new ByteArrayInputStream( src.toString().getBytes() );
            RDFDataMgr.read(model, isrc, RDFLanguages.TURTLE);
            return RDFUtil.findRoot(model);
        } catch (IOException e) {
            throw new EpiException("Problem reading CSV", e);
        }
    }
    
    /**
     * Fetch the next row, decode it as a resource
     */
    public Resource nextResource() {
        return nextResource( ModelFactory.createDefaultModel() );
    }
    
    /**
     * Fetch the value of the given column from the current row.
     * This is useful for retrieving additional annotations outside of the RDF serialization.
     */
    public String getColumnValue(String columnName) {
        if (currentRow == null) {
            throw new EpiException("No current row to retrieve");
        }
        Integer index = headerIndex.get(columnName);
        if (index == null) {
            throw new EpiException("Unknown column: " + columnName);
        }
        return currentRow[ index ];
    }
    
    public void close() throws IOException {
        reader.close();
    }
    
}
