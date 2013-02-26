/******************************************************************
 * File:        InitPrefixes.java
 * Created by:  Dave Reynolds
 * Created on:  25 Feb 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;

import com.hp.hpl.jena.util.FileManager;

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



    public static void main(String[] args) throws FileNotFoundException {
        PrintStream out = new PrintStream(TARGET);
        
        String preamble = FileManager.get().readWholeFileAsUTF8(Prefixes.PREFIXES_FILE);
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
