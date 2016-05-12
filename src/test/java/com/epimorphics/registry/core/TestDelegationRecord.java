/******************************************************************
 * File:        TestDelegationRecord.java
 * Created by:  Dave Reynolds
 * Created on:  19 Feb 2013
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
        assertTrue(members.size() > 300);

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
