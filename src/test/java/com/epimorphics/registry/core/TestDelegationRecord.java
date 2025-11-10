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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.ForwardingRecord.Type;
import com.epimorphics.registry.util.Util;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class TestDelegationRecord {
    public static final String BW_TEST_ENDPOINT = "https://environment.data.gov.uk/wales/bathing-waters/sparql/bwq/query";

    @Test
    public void testDelegation() {
        DelegationRecord dr = new DelegationRecord("/def/bw", BW_TEST_ENDPOINT, Type.DELEGATE);
        dr.setObject( ResourceFactory.createResource("http://environment.data.gov.uk/wales/bathing-waters/so/BathingWaterProfileFeature/") );
        dr.setPredicate( ResourceFactory.createResource("http://reference.data.gov.uk/def/reference/uriSet") );

        List<Resource> members = dr.listMembers();
        assertTrue(members.size() > 300);

        Resource bw1 = ResourceFactory.createResource("http://environment.data.gov.uk/wales/bathing-waters/id/bathing-water/ukl1100-40050");
        Model m = dr.describeMember( bw1 );
        assertEquals("Cemaes", RDFUtil.getStringValue(bw1.inModel(m), RDFS.label));

        m = ModelFactory.createDefaultModel();
        dr.fetchMembers(m, Util.listWindow(members, 5, 2) );
        assertEquals(2, m.listSubjects().toList().size());
        assertTrue( m.contains( members.get(5), RDF.type, (RDFNode)null ) );
        assertTrue( m.contains( members.get(6), RDF.type, (RDFNode)null ) );
    }
}
