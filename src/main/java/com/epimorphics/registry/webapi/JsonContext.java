/******************************************************************
 * File:        JsonContext.java
 * Created by:  Dave Reynolds
 * Created on:  25 Feb 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.epimorphics.registry.util.JSONLDSupport;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

@Path(JsonContext.JSON_CONTEXT_PATH)
public class JsonContext {
    public static final String JSON_CONTEXT_PATH = "/system/json-context";
    
    @GET
    @Produces(JSONLDSupport.MIME_JSONLD)
    public Model serveDefaultJsonldContext() {
        return ModelFactory.createDefaultModel();
    }

}
