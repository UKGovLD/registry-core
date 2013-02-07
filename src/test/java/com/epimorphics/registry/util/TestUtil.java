/******************************************************************
 * File:        TestUtil.java
 * Created by:  Dave Reynolds
 * Created on:  6 Feb 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

public class TestUtil {

    @Test
    public void testTimeConversion() {
        assertEquals(1234, Util.asTimestamp("1234"));
        
        long time = Util.asTimestamp("2013-02-06");
        assertEquals("2013-02-06", new SimpleDateFormat("yyyy-MM-dd").format(new Date(time)));
        
        time = Util.asTimestamp("2013-02-06T16:10:45");
        assertEquals("2013-02-06T16:10:45",  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date(time)));
    }
}
