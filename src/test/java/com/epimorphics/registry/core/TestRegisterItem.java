/******************************************************************
 * File:        TestRegisterItem.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2013
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

package com.epimorphics.registry.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(expected, RegisterItem.LEGAL_NOTATION.matcher(target).matches(), target);
    }
}
