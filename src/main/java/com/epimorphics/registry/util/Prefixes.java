/******************************************************************
 * File:        Prefixes.java
 * Created by:  Dave Reynolds
 * Created on:  27 Jan 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.FileManager;

/**
 * Set of default prefixes used in registry descriptions and queries.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Prefixes {
    static final String PREFIXES_FILE = "prefixes.ttl";

    static PrefixMapping prefixes;
    
    static {
        prefixes = FileManager.get().loadModel(PREFIXES_FILE);
    }
    
    public static PrefixMapping get() {
        return prefixes;
    }

}
