/******************************************************************
 * File:        TestUtil.java
 * Created by:  Dave Reynolds
 * Created on:  6 Feb 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import org.junit.Test;
import static  org.junit.Assert.*;

public class TestUtil {

    @Test
    public void testTimeConversion() {
        assertEquals(1234, Util.asTimestamp("1234"));
    }
}
