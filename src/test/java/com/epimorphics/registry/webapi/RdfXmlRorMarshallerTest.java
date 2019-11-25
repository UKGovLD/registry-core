package com.epimorphics.registry.webapi;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileUtils;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RdfXmlRorMarshallerTest {
    private MessageBodyWriter<Model> writer = new RdfXmlRorMarshaller();

    @Test
    public void producesRdfXml() {
        String ttl =
                "@prefix ex: <http://example.org/> ." +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." +
                "ex:x rdfs:label \"x-ray\" .";
        InputStream input = new ByteArrayInputStream(ttl.getBytes());
        Model model = ModelFactory.createDefaultModel().read(input, null, FileUtils.langTurtle);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            writer.writeTo(model, Model.class, null, null, MediaType.valueOf("application/x-ror-rdf+xml"), null, output);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }

        byte[] bytes = output.toByteArray();
        String result = new String(bytes);
        String expected =
                "<rdf:RDF\n" +
                "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "    xmlns:ex=\"http://example.org/\"\n" +
                "    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n" +
                "  <rdf:Description rdf:about=\"http://example.org/x\">\n" +
                "    <rdfs:label>x-ray</rdfs:label>\n" +
                "  </rdf:Description>\n" +
                "</rdf:RDF>\n";
        assertEquals(expected, result);
    }

    @Test
    public void putsRootTypesAtTopLevel() {
        String ttl =
                "@prefix ex: <http://example.org/> ." +
                "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ." +
                "@prefix skos: <http://www.w3.org/2004/02/skos/core#> ." +
                "ex:x a skos:Concept; skos:inScheme ex:y; skos:broader ex:z ." +
                "ex:y a skos:ConceptScheme; rdfs:label \"yankee\" ." +
                "ex:z a skos:Concept; rdfs:label \"zulu\".";
        InputStream input = new ByteArrayInputStream(ttl.getBytes());
        Model model = ModelFactory.createDefaultModel().read(input, null, FileUtils.langTurtle);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            writer.writeTo(model, Model.class, null, null, MediaType.valueOf("application/x-ror-rdf+xml"), null, output);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }

        byte[] bytes = output.toByteArray();
        String result = new String(bytes);
        String expected =
                "<rdf:RDF\n" +
                "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "    xmlns:ex=\"http://example.org/\"\n" +
                "    xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\"\n" +
                "    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n" +
                "  <skos:Concept rdf:about=\"http://example.org/x\">\n" +
                "    <skos:broader>\n" +
                "      <skos:Concept rdf:about=\"http://example.org/z\"/>\n" +
                "    </skos:broader>\n" +
                "    <skos:inScheme>\n" +
                "      <skos:ConceptScheme rdf:about=\"http://example.org/y\"/>\n" +
                "    </skos:inScheme>\n" +
                "  </skos:Concept>\n" +
                "  <skos:Concept rdf:about=\"http://example.org/z\">\n" +
                "    <rdfs:label>zulu</rdfs:label>\n" +
                "  </skos:Concept>\n" +
                "  <skos:ConceptScheme rdf:about=\"http://example.org/y\">\n" +
                "    <rdfs:label>yankee</rdfs:label>\n" +
                "  </skos:ConceptScheme>\n" +
                "</rdf:RDF>\n";
        assertEquals(expected, result);
    }
}