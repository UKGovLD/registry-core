/******************************************************************
 * File:        TestPatchUtil.java
 * Created by:  Dave Reynolds
 * Created on:  1 Feb 2013
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.jena.riot.RDFDataMgr;
import org.junit.Test;

import com.epimorphics.util.TestUtil;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class TestPatchUtil {

    @Test
    public void testPatch() {
        // Overwrite everything
        Resource dest = makeTest(null, new String[]{ "a", "b", "c", "e"});
        Resource src = makeTest(new String[]{ "a", "b", "c", "d"}, null);
        PatchUtil.update(src, dest);
        assertTrue( dest.getModel().isIsomorphicWith( src.getModel() ) );

        // A rigid property 
        dest = makeTest(null, new String[]{ "a", "b", "c", "e"});
        PatchUtil.update(
                makeTest(new String[]{ "a", "b", "c", "d"}, null), dest, propSet("b"), propSet());
        assertTrue( dest.getModel().isIsomorphicWith( makeTest(new String[]{ "a", "c", "d"}, new String[]{"b"}).getModel() ) );

        // A rigid property and a preserved property
        dest = makeTest(null, new String[]{ "a", "b", "c", "e"});
        PatchUtil.update(
                makeTest(new String[]{ "a", "b", "d"}, null), dest, propSet("b"), propSet("c"));
        assertTrue( dest.getModel().isIsomorphicWith( makeTest(new String[]{ "a", "d"}, new String[]{"b", "c"}).getModel() ) );

        
        // Patch
        Resource destR = makeTest(null, new String[]{ "a", "b", "c", "e"});
        PatchUtil.patch(src, destR);
        assertTrue( destR.getModel().isIsomorphicWith( makeTest(new String[]{ "a", "b", "c", "d"}, new String[]{"e"}).getModel() ) );

        // Patch with a rigid property
        destR = makeTest(null, new String[]{ "a", "b", "c", "e"});
        PatchUtil.patch(src, destR, TestUtil.propertyFixture(null, "b"));
        assertTrue( destR.getModel().isIsomorphicWith( makeTest(new String[]{ "a", "c", "d"}, new String[]{"b", "e"}).getModel() ) );

    }
    
    @Test
    public void testWillChange() {
        assertTrue ( willChange("test/patch/src1.ttl", "test/patch/dest.ttl") );
        assertTrue ( willChange("test/patch/src1.ttl", "test/patch/dest.ttl", "p2") );
        assertFalse( willChange("test/patch/src1.ttl", "test/patch/dest.ttl", "p1") );
        assertTrue ( willChange("test/patch/src2.ttl", "test/patch/dest.ttl") );
        assertFalse( willChange("test/patch/src2.ttl", "test/patch/dest.ttl", "p3") );
    }

    private Property[] propSet(String...names) {
        Property[] set = new Property[ names.length ];
        for (int i = 0; i < names.length; i++) {
            set[i] = TestUtil.propertyFixture(null, names[i]);
        }
        return set;
    }

    private Resource makeTest(String[] srcProps, String[] destProps) {
        Resource result = TestUtil.resourceFixture(ModelFactory.createDefaultModel(), "root");
        Model m = result.getModel();
        if (srcProps != null) {
            for (String p : srcProps) {
                result.addProperty(TestUtil.propertyFixture(m, p), p + "src");
            }
        }
        if (destProps != null) {
            for (String p : destProps) {
                result.addProperty(TestUtil.propertyFixture(m, p), p + "dest");
            }
        }
        return result;
    }
    
    private boolean willChange(String srcFile, String destFile, String...rigid) {
        String BASE = "http://localhost/test#";
        Resource src  = RDFDataMgr.loadModel(srcFile).getResource(BASE + "r");
        Resource dest = RDFDataMgr.loadModel(destFile).getResource(BASE + "r");
        Property[] rigidProps = new Property[ rigid.length ];
        for (int i = 0; i < rigid.length; i++) {
            rigidProps[i] = ResourceFactory.createProperty(BASE + rigid[i]);
        }
        return PatchUtil.willChange(src, dest, rigidProps);
    }
}
