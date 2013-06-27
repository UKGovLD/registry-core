/******************************************************************
 * File:        JSONLDCheck.java
 * Created by:  Dave Reynolds
 * Created on:  25 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.Options;
import com.github.jsonldjava.impl.JenaTripleCallback;
import com.github.jsonldjava.utils.JSONUtils;
import com.hp.hpl.jena.rdf.model.Model;


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
        Model m = (Model) JSONLD.toRDF(jsonObject, callback, new Options("http://dummy.org/"));
        // If you didn't use your own Jena Model, get the resulting one with:
        System.out.println("Model is ...");
        m.write(System.out, "Turtle");
    }
}
