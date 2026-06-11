/******************************************************************
 * File:        InitPrefixes.java
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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import org.apache.jena.util.FileUtils;

/**
 * Utility to generate a prefixes bootstrap file.
 * Could be packaged to part of a system build script.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class InitPrefixes {

    public static final String TARGET = "src/main/webapp/WEB-INF/prefixes-register.ttl";
    
    static final String TEMPLATE =
    "<system/prefixes/_$prefix> a reg:RegisterItem; \n" +
    "    rdfs:label \"$prefix\"@en; \n" +
    "    dct:description \"$prefix prefix registration\"@en; \n" +
    "    reg:notation \"$prefix\"; \n" +
    "    reg:itemClass owl:Ontology; \n" +
    "    reg:status reg:statusStable; \n" +
    "    reg:register <system/prefixes> ; \n" +
    "    reg:definition [ reg:entity <$ns> ]; \n" +
    "    version:currentVersion <system/prefixes/_$prefix>; \n" +
    "    . \n" +
    "<$ns> a owl:Ontology; \n" +
    "    rdfs:label \"$prefix\"@en; \n" +
    "    dct:description \"$prefix prefix registration\"@en; \n" +
    "    vann:preferredNamespacePrefix \"$prefix\"; \n" +
    "    vann:preferredNamespaceUri \"$ns\"; \n" +
    "    . \n\n";



    public static void main(String[] args) throws IOException {
        PrintStream out = new PrintStream(TARGET);
        
        String preamble = FileUtils.readWholeFileAsUTF8(Prefixes.PREFIXES_FILE);
        out.append(preamble);
        
        Map<String, String> prefixes = Prefixes.getDefault().getNsPrefixMap();
        for (String prefix : prefixes.keySet()) {
            String ns = prefixes.get(prefix);
            String template = TEMPLATE.replaceAll("\\$prefix", prefix).replaceAll("\\$ns", ns);
            out.append(template);
        }
        out.close();
    }
}
