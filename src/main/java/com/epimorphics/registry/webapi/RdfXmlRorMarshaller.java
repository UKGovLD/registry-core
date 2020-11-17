package com.epimorphics.registry.webapi;

import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.ror.RorDescriptor;
import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.util.FileUtils;

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

    private final Registry registry;

    public RdfXmlRorMarshaller(Registry registry) {
        this.registry = registry;
    }

    public RdfXmlRorMarshaller() {
        this(Registry.get());
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Arrays.asList(type.getInterfaces()).contains(Model.class);
    }

    @Override
    public long getSize(Model t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(
        Model model,
        Class<?> type,
        Type genericType,
        Annotation[] annotations,
        MediaType mediaType,
        MultivaluedMap<String, Object> httpHeaders,
        OutputStream entityStream
    ) throws IOException, WebApplicationException {
        RorDescriptor descriptor = RorDescriptor.getInstance(model);

        StoreAPI store = registry.getStore();
        store.beginSafeRead();
        try {
            Model description = descriptor.describe(registry, store, model);
            RDFWriter writer = description.getWriter(FileUtils.langXML);
            writer.setProperty("prettyTypes", descriptor.rootTypes());
            writer.write(description, entityStream, null);
        } finally {
            store.endSafeRead();
        }
    }
}

