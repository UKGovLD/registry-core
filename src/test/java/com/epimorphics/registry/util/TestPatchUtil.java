/******************************************************************
 * File:        TestPatchUtil.java
 * Created by:  Dave Reynolds
 * Created on:  1 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.epimorphics.util.TestUtil;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class TestPatchUtil {

    @Test
    public void testPatch() {
        // Overwite everything
        Resource dest = makeTest(null, new String[]{ "a", "b", "c", "e"});
        Resource src = makeTest(new String[]{ "a", "b", "c", "d"}, null);
        PatchUtil.update(src, dest);
        assertTrue( dest.getModel().isIsomorphicWith( src.getModel() ) );

        // A rigid property to preserve
        dest = makeTest(null, new String[]{ "a", "b", "c", "e"});
        PatchUtil.update(
                makeTest(new String[]{ "a", "b", "c", "d"}, null), dest, TestUtil.propertyFixture(null, "b"));
        assertTrue( dest.getModel().isIsomorphicWith( makeTest(new String[]{ "a", "c", "d"}, new String[]{"b"}).getModel() ) );

        // Patch
        Resource destR = makeTest(null, new String[]{ "a", "b", "c", "e"});
        PatchUtil.patch(src, destR);
        assertTrue( destR.getModel().isIsomorphicWith( makeTest(new String[]{ "a", "b", "c", "d"}, new String[]{"e"}).getModel() ) );

        // Patch with a rigid property
        destR = makeTest(null, new String[]{ "a", "b", "c", "e"});
        PatchUtil.patch(src, destR, TestUtil.propertyFixture(null, "b"));
        assertTrue( destR.getModel().isIsomorphicWith( makeTest(new String[]{ "a", "c", "d"}, new String[]{"b", "e"}).getModel() ) );

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
}
