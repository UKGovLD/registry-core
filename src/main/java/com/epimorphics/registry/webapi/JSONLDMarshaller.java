/******************************************************************
 * File:        JSONLDMarshaller.java
 * Created by:  Dave Reynolds
 * Created on:  1 Dec 2012
 * 
 * (c) Copyright 2012, Epimorphics Limited
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

import org.apache.jena.rdf.model.Model;

import com.epimorphics.registry.util.JSONLDSupport;
import com.github.jsonldjava.utils.JsonUtils;

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
        JsonUtils.writePrettyPrint(writer, jsonld);
        writer.flush();
    }


}

