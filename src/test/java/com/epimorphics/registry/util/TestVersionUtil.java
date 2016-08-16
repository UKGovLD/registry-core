/******************************************************************
 * File:        TestVersionUtil.java
 * Created by:  Dave Reynolds
 * Created on:  27 Jan 2013
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.junit.Test;

import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.util.TestUtil;
import com.epimorphics.vocabs.Time;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class TestVersionUtil {

    static final String BASE = "http://www.epimorphics.com/test/";

    @Test
    public void testCreateVersions() {
        Model m = ModelFactory.createDefaultModel();
        Resource item = m.createResource(BASE + "item")
                .addProperty(RDF.type, RegistryVocab.RegisterItem)
                .addProperty(RDFS.label, "an item")
                .addProperty(RegistryVocab.notation, "item");

        Model copy = ModelFactory.createDefaultModel().add(m);

        assertTrue("corrupted original data", copy.isIsomorphicWith(m));

        // Create first version and check
        Resource veritem = VersionUtil.nextVersion(item, Calendar.getInstance(), RegistryVocab.notation);
        Resource newitem = checkItem(veritem, item, 1, "an item");

        // Flatten and check
        VersionUtil.flatten(newitem, veritem);
        TestUtil.testResourcesMatch(item, newitem, OWL.versionInfo);

        // Update flattened version and reversion, check increments number
        newitem.removeAll(RDFS.label);
        newitem.addProperty(RDFS.label, "a renamed item");
        Resource ver2item = VersionUtil.nextVersion(newitem, Calendar.getInstance(), RegistryVocab.notation);
        checkItem(ver2item, item, 2, "a renamed item");

        Model m2 = ver2item.getModel();
        RDFNode end = veritem.inModel(m2).getPropertyResourceValue(Version.interval).getPropertyResourceValue(Time.hasEnd).getProperty(Time.inXSDDateTime).getObject();
        RDFNode start = ver2item.getPropertyResourceValue(Version.interval).getProperty(Time.hasBeginning).getProperty(Time.inXSDDateTime).getObject();
        assertEquals(end, start);
    }

    private Resource checkItem(Resource veritem, Resource item, long vnum, String label) {
        assertTrue(veritem.hasLiteral(OWL.versionInfo, vnum));
        assertTrue(veritem.hasLiteral(RDFS.label, label));
        assertTrue(veritem.hasProperty(DCTerms.isVersionOf, item));

        Resource newitem = veritem.getPropertyResourceValue(DCTerms.isVersionOf);
        assertTrue(newitem.hasLiteral(RegistryVocab.notation, "item"));
        assertTrue(newitem.hasProperty(Version.currentVersion, veritem));
        assertTrue(newitem.hasProperty(RDF.type, RegistryVocab.RegisterItem));
        return newitem;
    }

    @Test
    public void testVersionURIs() {
        Resource plain = ResourceFactory.createResource(BASE + "foo");
        Resource versioned = ResourceFactory.createResource(BASE + "foo:4");
        assertTrue( VersionUtil.isVersionedResource(versioned) );
        assertFalse( VersionUtil.isVersionedResource(plain) );
        Resource v = ResourceFactory.createResource( VersionUtil.versionedURI(plain, 6) );
        assertTrue( VersionUtil.isVersionedResource( v ) );
    }

}
