/******************************************************************
 * File:        TestRegisterItem.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import org.junit.Test;
import static org.junit.Assert.*;

// Mosts tests are in the TestStoreImpl code
public class TestRegisterItem {

    @Test
    public void testValidation() {
        check("_bad", false);   // This is a legal pchar* but not a legal notation   
        check("OK.~foo", true);
        check("bad/foo", false);
        check("bad{foo", false);   
        check("bad:1", false);   // This is a legal pchar* but not a legal notation   
    }
    
    private void check(String target, boolean expected) {
        assertEquals(target, expected, RegisterItem.LEGAL_NOTATION.matcher(target).matches());
    }
}
