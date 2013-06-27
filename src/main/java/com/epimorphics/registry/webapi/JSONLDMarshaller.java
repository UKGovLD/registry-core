/******************************************************************
 * File:        JSONLDMarshaller.java
 * Created by:  Dave Reynolds
 * Created on:  1 Dec 2012
 * 
 * (c) Copyright 2012, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.epimorphics.registry.util.JSONLDSupport;
import com.hp.hpl.jena.rdf.model.Model;

import com.github.jsonldjava.utils.JSONUtils;

@Provider
@Produces(JSONLDSupport.MIME_JSONLD)
public class JSONLDMarshaller implements MessageBodyWriter<Model> {

    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        Class<?>[] sigs = type.getInterfaces();
        for (Class<?> sig: sigs) {
            if (sig.equals(Model.class)) {
                return true;
            }
        }
        return false;
    }

    public long getSize(Model t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    public void writeTo(Model t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
//        Object jsonld = JSONLDSupport.toJSONLDNoContext(t);
        Object jsonld = JSONLDSupport.toJSONLD(t);
        Writer writer = new OutputStreamWriter(entityStream, Charset.forName("UTF-8"));
//        JSONUtils.write(writer, jsonld);
        JSONUtils.writePrettyPrint(writer, jsonld);
        writer.flush();
    }


}

