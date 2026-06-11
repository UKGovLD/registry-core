/******************************************************************
 * File:        TestUtil.java
 * Created by:  Dave Reynolds
 * Created on:  6 Feb 2013
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

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
