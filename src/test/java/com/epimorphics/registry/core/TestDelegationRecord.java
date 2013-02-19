/******************************************************************
 * File:        TestDelegationRecord.java
 * Created by:  Dave Reynolds
 * Created on:  19 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.ForwardingRecord.Type;
import com.epimorphics.registry.util.Util;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class TestDelegationRecord {
    public static final String BW_TEST_ENDPOINT = "http://environment.data.gov.uk/sparql/bwq/query";

    @Test
    public void testDelegation() {
        DelegationRecord dr = new DelegationRecord("/def/bw", BW_TEST_ENDPOINT, Type.DELEGATE);
        dr.setObject( ResourceFactory.createResource("http://environment.data.gov.uk/id/bathing-water/") );
        dr.setPredicate( ResourceFactory.createResource("http://reference.data.gov.uk/def/reference/uriSet") );

        List<Resource> members = dr.listMembers();
        assertTrue(members.size() > 500);

        Resource bw1 = ResourceFactory.createResource("http://environment.data.gov.uk/id/bathing-water/ukc1101-06000");
        Model m = dr.describeMember( bw1 );
        assertEquals("Seaton Carew North", RDFUtil.getStringValue(bw1.inModel(m), RDFS.label));

        m = ModelFactory.createDefaultModel();
        dr.fetchMembers(m, Util.listWindow(members, 5, 2) );
        assertEquals(2, m.listSubjects().toList().size());
        assertTrue( m.contains( members.get(5), RDF.type, (RDFNode)null ) );
        assertTrue( m.contains( members.get(6), RDF.type, (RDFNode)null ) );
    }
}
