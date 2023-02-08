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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;

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
                 "a|b|c,f|g",
                 "\"foo, bar\",\"\"\" and \\ and | are special\"" });
        Files.delete(path);
    }
    
    @Test
    public void testUnpack() {
        String source = "first|'foo \\ bar | baz'|third|'hello'@en";
        List<String> values = RDFCSVUtil.unpackMultiValues(source);
        TestUtil.testArray(values, new String[]{"first", "'foo \\ bar | baz'", "third", "'hello'@en"});
        source = "a|'name'@en|'b'|<a> | <b>|'foo | bar'|don't|do|'a \\'| string'";
        values = RDFCSVUtil.unpackMultiValues(source);
        TestUtil.testArray(values, new String[]{"a", "'name'@en", "'b'", "<a>", "<b>", "'foo | bar'", "don't", "do", "'a \\'| string'"});
    }
    
    @Test
    public void testRDFSerialize() throws IOException {
        Model model = RDFDataMgr.loadModel("test/csv/testResource.ttl");
        Path path = writeCSV(model);        
        String csv = FileManager.get().readWholeFileAsUTF8( path.toString() );
        String expected = FileManager.get().readWholeFileAsUTF8( "test/csv/testResource.csv" );
        assertEquals(expected, csv);
        Files.delete(path);
    }
    
    private Path writeCSV(Model model) throws IOException {
        String baseURI = "http://localhost/test/";
        Resource r = model.getResource(baseURI + "r");
        
        Path path = Files.createTempFile("rdfTest", ".csv");
        FileOutputStream out = new FileOutputStream( path.toFile() );
        CSVRDFWriter writer = new CSVRDFWriter(out, model);
        
        writer.setBaseURI(baseURI);
        writer.addHeader("@dummy");
        writer.addHeader(r);
        writer.write(r);
        writer.finishRow();
        writer.close();

        return path;
    }
    
    @Test
    public void testRDFRoundTrip() throws IOException {
        testRoundTrip("test/csv/testResource.ttl" );
        testRoundTrip("test/csv/testResource2.ttl" );
    }
    
    private void testRoundTrip(String file) throws IOException {
        Model model = RDFDataMgr.loadModel(file);
        String baseURI = "http://localhost/test/";
        Path path = writeCSV(model);
        
        FileInputStream in = new FileInputStream( path.toFile() );
        CSVRDFReader reader = new CSVRDFReader(in, model);
        reader.setBaseURI(baseURI);
        
        Resource r = reader.nextResource();
        assertNotNull(r);
        assertTrue( model.isIsomorphicWith( r.getModel() ));
        
        Files.delete(path);
    }
    
    @Test
    public void testRDFRead() throws IOException {
        doTestRDFRead("test/csv/read-test.csv", "test/csv/read-test-expected.ttl");
        
    }
    
    @Test
    public void testBOMRead() throws IOException {
        doTestRDFRead("test/csv/read-bom.csv", "test/csv/read-bom-expected.ttl");
    }
    
    private void doTestRDFRead(String source, String expected) throws IOException {
        Model prefixes = ModelFactory.createDefaultModel();
        prefixes.setNsPrefix("eg", "http://localhost/def/");
        prefixes.setNsPrefix("eg-alt", "http://localhost/alt/");
       
        String baseURI = "http://localhost/test/";
        FileInputStream in = new FileInputStream( source );
        CSVRDFReader reader = new CSVRDFReader(in, prefixes);
        reader.setBaseURI(baseURI);
        Model payload = ModelFactory.createDefaultModel();
        while (reader.nextResource(payload) != null) {
            // Nothing, it's in the payload
        }
//        payload.setNsPrefixes(prefixes);
//        payload.write(System.out, "Turtle");
        Model expectedM = RDFDataMgr.loadModel(expected);
        assertTrue( expectedM.isIsomorphicWith(payload) );
    }
    
}
