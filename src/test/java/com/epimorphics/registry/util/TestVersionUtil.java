/******************************************************************
 * File:        TestVersionUtil.java
 * Created by:  Dave Reynolds
 * Created on:  27 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import java.util.Calendar;

import org.junit.Test;
import static org.junit.Assert.*;

import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.util.TestUtil;
import com.epimorphics.vocabs.Time;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

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

}
