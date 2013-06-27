/******************************************************************
 * File:        TestLibReg.java
 * Created by:  Dave Reynolds
 * Created on:  29 Mar 2013
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

package com.epimorphics.registry.webapi;

import org.junit.Test;
import static org.junit.Assert.*;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class TestLibReg {
    @Test
    public void testReservations() {
        LibReg.ReservationList rl = new LibReg.ReservationList();
        rl.add( nodeFor("a") );
        rl.add( nodeFor(1) );
        rl.add( nodeFor("b") );
        rl.add( nodeFor(3) );
        rl.add( nodeFor(4) );
        rl.add( nodeFor(5) );
        rl.add( nodeFor(9) );
        rl.add( nodeFor(10) );
        rl.add( nodeFor(15) );
        assertEquals("a, 1, b, 3-5, 9-10, 15", rl.getReservations());
        
        rl = new LibReg.ReservationList();
        rl.add( nodeFor(1) );
        assertEquals("1", rl.getReservations());
        
        rl = new LibReg.ReservationList();
        
        rl.add( nodeFor("a") );
        rl.add( nodeFor("b") );
        assertEquals("a, b", rl.getReservations());
    }

    private RDFNode nodeFor(Integer i) {
        return ResourceFactory.createTypedLiteral(i);
    }

    private RDFNode nodeFor(String s) {
        return ResourceFactory.createPlainLiteral(s);
    }
}
