package com.epimorphics.registry.webapi;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileUtils;
import org.apache.jena.vocabulary.SKOS;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

@Provider
@Produces(RdfXmlRorMarshaller.MIME_TYPE)
public class RdfXmlRorMarshaller implements MessageBodyWriter<Model> {

    public static final String MIME_TYPE = "application/x-ror-rdf+xml";

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Arrays.asList(type.getInterfaces()).contains(Model.class);
    }

    @Override
    public long getSize(Model t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    private final Resource[] rootTypes = new Resource[] { SKOS.Concept, SKOS.ConceptScheme };

    @Override
    public void writeTo(
            Model t,
            Class<?> type,
            Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream
    ) throws IOException, WebApplicationException {
        RDFWriter writer = t.getWriter(FileUtils.langXMLAbbrev);
        writer.setProperty("prettyTypes", rootTypes);
        writer.write(t, entityStream, null);
    }
}

