/******************************************************************
 * File:        JSONLDSupport.java
 * Created by:  Dave Reynolds
 * Created on:  30 Nov 2012
 *
 * (c) Copyright 2012, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import java.io.InputStream;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import de.dfki.km.json.JSONUtils;
import de.dfki.km.json.jsonld.JSONLD;
import de.dfki.km.json.jsonld.JSONLDProcessor;
import de.dfki.km.json.jsonld.JSONLDProcessor.Options;
import de.dfki.km.json.jsonld.impl.JenaJSONLDSerializer;
import de.dfki.km.json.jsonld.impl.JenaTripleCallback;

/**
 * Utilities for assisting with JSON-LD parsing.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class JSONLDSupport {

    public static final String MIME_JSONLD = "application/ld+json";
    public static final MediaType MT_JSONLD = new MediaType("application", "ld+json");
    public static final String CONTEXT_KEY = "@context";

    static Options readOptions;

//    static {
//        readOptions = new Options(BaseEndpoint.DUMMY_BASE_URI + "/");
//    }

    public static Model readModel(String baseURI, InputStream inputStream) {
        try {
            Object json = JSONUtils.fromInputStream(inputStream);
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
            JenaTripleCallback callback = new JenaTripleCallback();
            Model m = ModelFactory.createDefaultModel();
            callback.setJenaModel(m);
            JSONLD.toRDF(jsonObject, new Options(baseURI), callback);
            return m;
        } catch (Exception e) {
            throw new EpiException(e);
        }
    }

    public static Object toJSONLD(Model m) {
        try {
            JenaJSONLDSerializer serializer = new JenaJSONLDSerializer();
            Object json = JSONLD.fromRDF(m, serializer);
            json = JSONLD.compact(json, Prefixes.getJsonldContext(), new JSONLDProcessor.Options());
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
