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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epimorphics.util.EpiException;

/**
 * Support for outputing CSV format files.
 * This base class just provides headers and string output, no specialist RDF or Registry knowledge.
 */
public class CSVBaseWriter {
    
    protected static final String LINE_END = "\r\n" ;
    protected static final String SEP = "," ;
    protected static final String VALUE_SEP = "|" ;
    protected static final char   VALUE_SEP_CHAR = '|' ;
    protected static final String QUOTED_VALUE_SEP = "\\|" ;
    protected static final String QUOTE = "\\" ;
    protected static final char   QUOTE_CHAR = '\\';
    protected static final String QUOTED_QUOTE = "\\\\" ;
    protected static final Charset ENC = StandardCharsets.UTF_8;
    
    protected List<String> headers;
    protected Map<String, Integer> headerIndex;
    protected OutputStream out;
    protected String[] currentRow;

    public CSVBaseWriter(OutputStream out) {
        this.out = out;
    }
    
    /**
     * Set the headers for this CSV, causes the header row to be written out.
     * Must be called before any write calls.
     */
    public void setHeaders(List<String>headers) {
        this.headers = headers;
        headerIndex = new HashMap<String, Integer>();
        int index = 0;
        for (String header : headers) {
            headerIndex.put(header, index++);
        }
        try {
            writeHeaders();
        } catch (IOException e) {
            throw new EpiException("Failed to write CSV", e);
        }
    }

    /**
     * Start a new output row
     */
    public void startRow() {
        currentRow = new String[ headers.size() ];
        for (int i = 0; i < headers.size(); i++) {
            currentRow[i] = "";
        }
    }
    
    /**
     * Finish the current output row, writes it out
     */
    public void finishRow() {
        try {
            StringBuffer buf = new StringBuffer();
            boolean started = false;
            for (String cell : currentRow) {
                if (started) { buf.append(SEP); } else { started = true; }
                buf.append( safeString(cell) );
            }
            buf.append(LINE_END);
            out.write( buf.toString().getBytes(ENC) );
        } catch (IOException e) {
            throw new EpiException("Failed to write CSV", e);
        }
        currentRow = null;
    }
    
    /**
     * Add a value to the row being assembled. Multiple values for
     * the same column will be separated by "|".
     * Implicitly starts a new row the last row was finished.
     * @throws EpiException if the header is not registered
     */
    public void write(String header, String value) {
        if (currentRow == null)  startRow(); 
        Integer index = headerIndex.get(header);
        if (index == null) {
            throw new EpiException("Not a registered header");
        }
        String current = currentRow[ index ];
        if (current.isEmpty()) {
            currentRow[ index ] = quoteValueSep(value);
        } else {
            currentRow[ index ] = current + VALUE_SEP + quoteValueSep(value);
        }
    }

    /**
     * Finish and close the stream.
     */
    public void close() {
        if (currentRow != null) {
            finishRow();
        }
        try {
            out.close();
        } catch (IOException e) {
            throw new EpiException("Failed to write CSV", e);
        }
    }
    
    protected String quoteValueSep(String value) {
        return value.replace(QUOTE, QUOTED_QUOTE).replace(VALUE_SEP, QUOTED_VALUE_SEP);
    }
    
    protected void writeHeaders() throws IOException {
        StringBuffer buf = new StringBuffer();
        boolean started = false;
        for (String header : headers) {
            if (started) { buf.append(SEP); } else { started = true; }
            buf.append( safeString(header) );
        }
        buf.append(LINE_END);
        out.write( buf.toString().getBytes(ENC) );
    }
    
    protected String safeString(String str) {
        if (str.contains("\"") || str.contains(",") || str.contains("\r") || str.contains("\n") ) {
            str = "\"" + str.replace("\"", "\"\"") + "\"";
        } else if ( str.isEmpty() ) {
            // Return the quoted empty string. 
            str = "\"\"" ;
        } 
        return str;
    }
}
