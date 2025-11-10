/******************************************************************
 * File:        TestRegistry.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestRegistry {

    @Test
    public void testBaseURIHandling() {
        Registry r = new Registry();
        r.setBaseURI("http://environment.data.gov.uk/registry");
        assertEquals("http://environment.data.gov.uk/registry", r.getBaseURI());
        assertEquals("http://environment.data.gov.uk", r.getBaseDomain());
        assertEquals("/registry", r.getRootPath());

        r.setBaseURI("http://environment.data.gov.uk/");
        assertEquals("http://environment.data.gov.uk", r.getBaseURI());
        assertEquals("http://environment.data.gov.uk", r.getBaseDomain());
        assertEquals("", r.getRootPath());
    }
}
