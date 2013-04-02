/******************************************************************
 * File:        JSONLDCheck.java
 * Created by:  Dave Reynolds
 * Created on:  25 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.hp.hpl.jena.rdf.model.Model;

import de.dfki.km.json.JSONUtils;
import de.dfki.km.json.jsonld.JSONLD;
import de.dfki.km.json.jsonld.JSONLDProcessingError;
import de.dfki.km.json.jsonld.JSONLDProcessor.Options;
import de.dfki.km.json.jsonld.impl.JenaTripleCallback;

public class JSONLDCheck {

    public static void main(String[] args) throws IOException, JSONLDProcessingError {
     // Open a valid json(-ld) input file
        InputStream inputStream = new FileInputStream("test/input-expanded.jsonld");
        // Read the file into an Object (The type of this object will be a List, Map, String, Boolean,
        // Number or null depending on the root object in the file).
        Object jsonObject = JSONUtils.fromInputStream(inputStream);
        // Call whichever JSONLD function you want! (e.g. normalize)
//        Object normalized= JSONLD.normalize(jsonObject, new Options());
        // Print out the result (or don't, it's your call!)
//        System.out.println(JSONUtils.toString(normalized));
     // Create a JenaTripleCallback object
        JenaTripleCallback callback = new JenaTripleCallback();
//        callback.setJenaModel(jenaModel);
        // call the toRDF function
        JSONLD.toRDF(jsonObject, new Options("http://dummy.org/"), callback);
        // If you didn't use your own Jena Model, get the resulting one with:
        Model m = callback.getJenaModel();
        System.out.println("Model is ...");
        m.write(System.out, "Turtle");
    }
}
