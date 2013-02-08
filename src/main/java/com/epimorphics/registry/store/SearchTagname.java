/******************************************************************
 * File:        SearchTagnames.java
 * Created by:  Dave Reynolds
 * Created on:  8 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import com.epimorphics.registry.vocab.RegistryVocab;
import com.hp.hpl.jena.rdf.model.Resource;

public enum SearchTagname  {

    Type(RegistryVocab.itemClass),
    Status(RegistryVocab.status),
    Category(RegistryVocab.category);

    protected String key;

    private SearchTagname(Resource r) {
        this.key = r.getURI();
    }

    public static SearchTagname forString(String param) {
        for (SearchTagname s : SearchTagname.values()) {
            if (s.name().equalsIgnoreCase(param)) {
                return s;
            }
        }
        return null;
    }

    public static String expandKey(String key) {
        SearchTagname tag = forString(key);
        if (tag == null) {
            return key;
        } else {
            return tag.key;
        }
    }

}
