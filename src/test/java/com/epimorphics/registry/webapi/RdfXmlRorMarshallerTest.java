package com.epimorphics.registry.webapi;

import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.FileUtils;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class RdfXmlRorMarshallerTest {
    private Registry registry = mock(Registry.class);
    private StoreAPI store = mock(StoreAPI.class);
    private RdfXmlRorMarshaller writer = new RdfXmlRorMarshaller(registry);

    @Before
    public void before() {
        Resource org = ModelFactory.createDefaultModel().createResource("http://example.org/org")
                .addProperty(RDF.type, FOAF.Agent)
                .addProperty(FOAF.name, "Example Org")
                .addProperty(FOAF.mbox, "mailto:example.org")
                .addProperty(FOAF.homepage, "http://example.org/org/home");

        when(registry.getStore()).thenReturn(store);
        when(store.getDescription("http://example.org/org")).thenReturn(new Description(org));
    }

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
    public void catalog_describesRegistry() throws FileNotFoundException {
        File ttl = new File("test/ror/catalog.ttl");
        FileReader reader = new FileReader(ttl);
        Model model = ModelFactory.createDefaultModel().read(reader, null, FileUtils.langTurtle);

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
                "    xmlns:dct=\"http://purl.org/dc/terms/\"\n" +
                "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "    xmlns:ex=\"http://example.org/\"\n" +
                "    xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\"\n" +
                "    xmlns:voaf=\"http://purl.org/vocommons/voaf#\"\n" +
                "    xmlns:dcat=\"http://www.w3.org/ns/dcat#\"\n" +
                "    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
                "    xmlns:foaf=\"http://xmlns.com/foaf/0.1/\">\n" +
                "  <dcat:Catalog rdf:about=\"http://example.org/catalog\">\n" +
                "    <dcat:dataset>\n" +
                "      <rdf:Description rdf:about=\"http://example.org/alpha\">\n" +
                "        <dcat:distribution rdf:parseType=\"Resource\">\n" +
                "          <dcat:downloadURL rdf:resource=\"http://example.org/alpha?_format=ror\"/>\n" +
                "          <dct:format rdf:resource=\"http://publications.europa.eu/resource/authority/file-type/RDF_XML\"/>\n" +
                "        </dcat:distribution>\n" +
                "      </rdf:Description>\n" +
                "    </dcat:dataset>\n" +
                "    <dcat:dataset>\n" +
                "      <rdf:Description rdf:about=\"http://example.org/yankee\">\n" +
                "        <dcat:distribution rdf:parseType=\"Resource\">\n" +
                "          <dcat:downloadURL rdf:resource=\"http://example.org/yankee?_format=ror\"/>\n" +
                "          <dct:format rdf:resource=\"http://publications.europa.eu/resource/authority/file-type/RDF_XML\"/>\n" +
                "        </dcat:distribution>\n" +
                "      </rdf:Description>\n" +
                "    </dcat:dataset>\n" +
                "    <dct:publisher>\n" +
                "      <foaf:Agent rdf:about=\"http://example.org/org\">\n" +
                "        <foaf:homepage>http://example.org/org/home</foaf:homepage>\n" +
                "        <foaf:name>Example Org</foaf:name>\n" +
                "        <foaf:mbox>mailto:example.org</foaf:mbox>\n" +
                "      </foaf:Agent>\n" +
                "    </dct:publisher>\n" +
                "    <dct:accrualPeriodicity rdf:resource=\"http://publications.europa.eu/resource/authority/frequency/DAILY\"/>\n" +
                "    <dct:description>An example</dct:description>\n" +
                "    <dct:title>Example registry</dct:title>\n" +
                "  </dcat:Catalog>\n" +
                "</rdf:RDF>\n";
        assertEquals(expected, result);
    }

    @Test
    public void conceptScheme_describesRegister() throws FileNotFoundException {
        File ttl = new File("test/ror/conceptScheme.ttl");
        FileReader reader = new FileReader(ttl);
        Model model = ModelFactory.createDefaultModel().read(reader, null, FileUtils.langTurtle);

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
                "    xmlns:dct=\"http://purl.org/dc/terms/\"\n" +
                "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "    xmlns:ex=\"http://example.org/\"\n" +
                "    xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\"\n" +
                "    xmlns:voaf=\"http://purl.org/vocommons/voaf#\"\n" +
                "    xmlns:dcat=\"http://www.w3.org/ns/dcat#\"\n" +
                "    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
                "    xmlns:foaf=\"http://xmlns.com/foaf/0.1/\">\n" +
                "  <skos:Concept rdf:about=\"http://example.org/zulu\">\n" +
                "    <foaf:name xml:lang=\"en\">Zulu</foaf:name>\n" +
                "    <skos:prefLabel>zulu</skos:prefLabel>\n" +
                "    <skos:inScheme>\n" +
                "      <skos:ConceptScheme rdf:about=\"http://example.org/yankee\"/>\n" +
                "    </skos:inScheme>\n" +
                "  </skos:Concept>\n" +
                "  <skos:Concept rdf:about=\"http://example.org/x-ray\">\n" +
                "    <skos:broader rdf:resource=\"http://example.org/zulu\"/>\n" +
                "    <skos:inScheme>\n" +
                "      <skos:ConceptScheme rdf:about=\"http://example.org/yankee\"/>\n" +
                "    </skos:inScheme>\n" +
                "  </skos:Concept>\n" +
                "  <skos:ConceptScheme rdf:about=\"http://example.org/yankee\">\n" +
                "    <voaf:reliesOn rdf:resource=\"http://example.org/alpha\"/>\n" +
                "    <dct:publisher>\n" +
                "      <foaf:Agent rdf:about=\"http://example.org/org\">\n" +
                "        <foaf:homepage>http://example.org/org/home</foaf:homepage>\n" +
                "        <foaf:name>Example Org</foaf:name>\n" +
                "        <foaf:mbox>mailto:example.org</foaf:mbox>\n" +
                "      </foaf:Agent>\n" +
                "    </dct:publisher>\n" +
                "    <dct:isPartOf>\n" +
                "      <dcat:Catalog rdf:about=\"http://example.org/catalog\"/>\n" +
                "    </dct:isPartOf>\n" +
                "    <dct:accrualPeriodicity rdf:resource=\"http://publications.europa.eu/resource/authority/frequency/DAILY\"/>\n" +
                "    <skos:definition>define y</skos:definition>\n" +
                "    <skos:prefLabel>yankee</skos:prefLabel>\n" +
                "  </skos:ConceptScheme>\n" +
                "</rdf:RDF>\n";
        assertEquals(expected, result);
    }
}