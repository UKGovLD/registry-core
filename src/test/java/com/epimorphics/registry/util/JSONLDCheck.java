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

import org.apache.jena.rdf.model.Model;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdTripleCallback;
import com.github.jsonldjava.utils.JsonUtils;


public class JSONLDCheck {

    public static void main(String[] args) throws IOException, JsonLdError {
     // Open a valid json(-ld) input file
        InputStream inputStream = new FileInputStream("test/input-expanded.jsonld");
        Object jsonObject = JsonUtils.fromInputStream(inputStream);

        JsonLdTripleCallback callback = new JenaTripleCallBack();
        Model m = (Model) JsonLdProcessor.toRDF(jsonObject, callback, new JsonLdOptions("http://dummy.org/"));

        // If you didn't use your own Jena Model, get the resulting one with:
        System.out.println("Model is ...");
        m.write(System.out, "Turtle");
    }
   
    
}
