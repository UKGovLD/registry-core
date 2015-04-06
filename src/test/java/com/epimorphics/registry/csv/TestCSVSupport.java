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

import static org.junit.Assert.assertEquals;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import com.epimorphics.util.TestUtil;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

public class TestCSVSupport {

    @Test
    public void testBaseWriter() throws IOException {
        Path path = Files.createTempFile("baseWriter", ".csv");
        
        FileOutputStream out = new FileOutputStream( path.toFile() );
        CSVBaseWriter writer = new CSVBaseWriter(out);
        
        List<String> headers = new ArrayList<>();
        headers.add("first");
        headers.add("second");
        writer.setHeaders(headers);
        
        writer.write("first", "1");
        writer.write("second", "2");
        writer.finishRow();
        
        writer.write("first", "a");
        writer.write("first", "b");
        writer.write("first", "c");
        writer.write("second", "f|g");
        writer.finishRow();
        
        writer.write("first", "foo, bar");
        writer.write("second", "\" and \\ and | are special");
        writer.finishRow();
        writer.close();
        
        List<String> actual   = Files.readAllLines(path, StandardCharsets.UTF_8);
        TestUtil.testArray(actual, new String[]{
                 "first,second",
                 "1,2",
                 "a|b|c,f\\|g",
                 "\"foo, bar\",\"\"\" and \\\\ and \\| are special\"" });
        Files.delete(path);
    }
    
    @Test
    public void testUnpack() {
        String source = "first|foo \\\\ bar \\| baz|third";
        List<String> values = RDFCSVUtil.unpackageMultiValues(source);
        TestUtil.testArray(values, new String[]{"first", "foo \\ bar | baz", "third"});
    }
    
    @Test
    public void testRDFSerialize() throws IOException {
        Model model = RDFDataMgr.loadModel("test/csv/testResource.ttl");
        String baseURI = "http://localhost/test#";
        Resource r = model.getResource(baseURI + "r");
        
        Path path = Files.createTempFile("rdfTest", ".csv");
        System.out.println(path.toString());
        FileOutputStream out = new FileOutputStream( path.toFile() );
        CSVRDFWriter writer = new CSVRDFWriter(out);
        
        writer.setBaseURI(baseURI);
        writer.setPrefixes( model );
        writer.addHeader("@dummy");
        writer.addHeader( r );
        writer.write(r);
        writer.finishRow();
        writer.close();
        
        String csv = FileManager.get().readWholeFileAsUTF8( path.toString() );
        String expected = FileManager.get().readWholeFileAsUTF8( "test/csv/testResource.csv" );
        assertEquals(expected, csv);
        Files.delete(path);
    }
}
