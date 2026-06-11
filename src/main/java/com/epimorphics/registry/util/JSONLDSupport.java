/******************************************************************
 * File:        JSONLDSupport.java
 * Created by:  Dave Reynolds
 * Created on:  30 Nov 2012
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

package com.epimorphics.registry.util;

import java.io.InputStream;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;

import org.apache.jena.rdf.model.Model;

import com.epimorphics.util.EpiException;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * Utilities for assisting with JSON-LD parsing.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class JSONLDSupport {

    public static final String MIME_JSONLD = "application/ld+json";
    public static final String FULL_MIME_JSONLD = "application/ld+json; charset=UTF-8";
    public static final MediaType MT_JSONLD = new MediaType("application", "ld+json");
    public static final String CONTEXT_KEY = "@context";

    public static Model readModel(String baseURI, InputStream inputStream) {
        try {
            Object json = JsonUtils.fromInputStream(inputStream);
            inputStream.close();
            return parseModel(baseURI, json );
        } catch (Exception e) {
            throw new EpiException(e);
        }
    }

    public static Model parseModel(String baseURI, Object jsonObject) {
        try {
            // Identify system context
            if (jsonObject instanceof Map<?,?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> top = (Map<String,Object>)jsonObject;
                Object context = top.get(CONTEXT_KEY);
                if (context != null && context instanceof String && context.equals( Prefixes.getJsonContextURI() )) {
                    top.put(CONTEXT_KEY, Prefixes.getJsonldContext());
                }
            }
            JenaTripleCallBack callback = new JenaTripleCallBack();
            //            Model m = (Model) JSONLD.toRDF(jsonObject, callback, new Options(baseURI));
            return (Model) JsonLdProcessor.toRDF(jsonObject, callback, new JsonLdOptions(baseURI));
        } catch (Exception e) {
            throw new EpiException(e);
        }
    }

    public static Object toJSONLD(Model m) {
        try {
            Object json = JsonLdProcessor.fromRDF(m, new JenaJSONLDParser());
            json = JsonLdProcessor.compact(json, Prefixes.getJsonldContext(), new JsonLdOptions());
            return json;
        } catch (Exception e) {
            throw new EpiException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static Object toJSONLDNoContext(Model m) {
        Object json = toJSONLD(m);
        if (json instanceof Map<?,?>) {
            Map<String, Object> rawJson = (Map<String, Object>)json;
            rawJson.remove("@context");
            return rawJson;
        }
        return json;
    }
    

}
