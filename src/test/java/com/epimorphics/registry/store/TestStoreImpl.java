/******************************************************************
 * File:        TestStoreImpl.java
 * Created by:  Dave Reynolds
 * Created on:  30 Jan 2013
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

package com.epimorphics.registry.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.store.impl.TDBStore;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.util.NameUtils;
import com.epimorphics.util.TestUtil;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class TestStoreImpl {
    private static final String EXT_BLACK = "http://example.com/colours/black";
    static final String BOOTSTRAP_FILE = "src/test/webapp/WEB-INF/root-register.ttl";
    static final String ROOT_REGISTER = "http://location.data.gov.uk/";
    static final String REG1 = ROOT_REGISTER + "reg1";
    static final String REG3 = ROOT_REGISTER + "reg1/reg3";

    Store basestore;
    StoreAPI store;

    @Before
    public void setup() {
        basestore = new TDBStore();
        ((ComponentBase)basestore).startup(null);

        StoreBaseImpl store = new StoreBaseImpl();
        store.setStore(basestore);
        this.store = store;
        store.loadBootstrap(BOOTSTRAP_FILE);
    }

    @After
    public void tearDown() {
        basestore = null;
        store = null;
    }


    @Test
    public void testBaseLoad() {
        Description rootreg = store.getCurrentVersion(ROOT_REGISTER);
        assertTrue(rootreg instanceof Register);
        Resource r = rootreg.getRoot();
        assertTrue(r.hasProperty(RDF.type, RegistryVocab.Register));
        assertEquals("root", RDFUtil.getStringValue(r, RDFS.label));
        assertFalse(r.hasProperty(Version.currentVersion));
    }

    @Test
    public void testMetadataUpdate() {
        Register rootreg = (Register)store.getCurrentVersion(ROOT_REGISTER);
        rootreg.setProperty(RDFS.label, ResourceFactory.createPlainLiteral("new root"));
        store.lock();
        store.update(rootreg);
        store.commit();
        store.end();

        Resource updatedroot = store.getDescription(ROOT_REGISTER).getRoot();
        assertTrue( TestUtil.isOnlyValue(updatedroot, Version.currentVersion, null) );

        Register updatedreg = store.getCurrentVersion(ROOT_REGISTER).asRegister();
        assertEquals("new root", RDFUtil.getStringValue(updatedreg.getRoot(), RDFS.label));

        List<VersionInfo> versions = store.listVersions(ROOT_REGISTER);
        assertTrue(versions.size() >= 2);
        VersionInfo vi1 = versions.get(0);
        VersionInfo vi2 = versions.get(1);
        assertEquals("1", vi1.getVersion());
        assertEquals("2", vi2.getVersion());
        assertEquals(vi1.getToTime(), vi2.getFromTime());
        assertNotSame(-1, vi1.getToTime());

        String versionURI = vi1.getUri();
        Register oldreg = store.getVersion(versionURI, false).asRegister();
        assertEquals("root", RDFUtil.getStringValue(oldreg.getRoot(), RDFS.label));
    }

    @Test
    public void testRegisterCreate() {
        Register rootreg = store.getCurrentVersion(ROOT_REGISTER).asRegister();
        addEntry("file:test/reg1.ttl", ROOT_REGISTER);

        // Check the item pointing to the new subregister
        String expectedItemURI = ROOT_REGISTER + "_reg1";
        RegisterItem ri = store.getCurrentVersion(expectedItemURI).asRegisterItem();
        assertEquals("register 1", RDFUtil.getStringValue(ri.getRoot(), RDFS.label));
        assertEquals("Example register 1", RDFUtil.getStringValue(ri.getRoot(), DCTerms.description));
        assertTrue(TestUtil.isOnlyValue(ri.getRoot(), DCTerms.dateSubmitted, null));
        assertTrue(TestUtil.isOnlyValue(ri.getRoot(), RegistryVocab.status, RegistryVocab.statusSubmitted));
        assertTrue( ri.getRoot().hasProperty(RDF.type, RegistryVocab.RegisterItem));

        // Check the subregister itself
        Resource subregE = store.getEntity(ri);
        assertTrue (subregE.hasProperty(RDF.type, RegistryVocab.Register));
//        assertTrue (subregE.hasProperty(RegistryVocab.containedItemClass, SKOS.Concept));

        // Check the root register shows this
        List<RegisterEntryInfo> members = store.listMembers(rootreg);
        assertEquals(1, members.size());
        RegisterEntryInfo entry = members.get(0);
        assertEquals(ROOT_REGISTER + "reg1", entry.getEntityURI());
        assertEquals("reg1", entry.getNotation());
        assertEquals(Status.Submitted, entry.getStatus());
        TestUtil.testArray(entry.getTypes().toArray(), new Object[]{ RegistryVocab.Register, SKOS.Collection});

        assertTrue( store.contains(rootreg, "reg1") );
        assertFalse( store.contains(rootreg, "reg2") );
    }

    private long addEntry(String defFile, String parentURI) {
        store.lock();
        
        Register parent = store.getCurrentVersion(parentURI).asRegister();
        String target = NameUtils.stripLastSlash(parentURI);
        String base = NameUtils.ensureLastSlash(parentURI);
        Model subregM = ModelFactory.createDefaultModel();
        subregM.read(defFile, base, FileUtils.langTurtle);
        Resource subregR = RDFUtil.findRoot( subregM );

        Calendar now = Calendar.getInstance();
        RegisterItem subregItem = RegisterItem.fromEntityRequest(subregR, target, true, now);

        store.addToRegister(parent, subregItem, now);
        
        store.commit();
        store.end();
        return now.getTimeInMillis();
    }

    @Test
    public void testEntryManagement() {
        addEntry("file:test/reg1.ttl", ROOT_REGISTER);
        addEntry("file:test/blue.ttl", REG1);
        addEntry("file:test/red.ttl", REG1);

        Register reg1 = store.getCurrentVersion(REG1).asRegister();
        assertEquals(2, store.listMembers(reg1).size());

        RegisterItem ri = store.getItem(ROOT_REGISTER + "reg1/_red", true);
        checkItemWithEntity(ri, "red");

        assertNull(store.getItem(ROOT_REGISTER + "reg1/_red", false).getEntity());

        List<RegisterItem> members = members(reg1, true);
        assertEquals(2, members.size());
        if (members.get(0).getRoot().getURI().endsWith("red")) {
            checkItemWithEntity(members.get(0), "red");
            checkItemWithEntity(members.get(1), "blue");
        } else {
            checkItemWithEntity(members.get(1), "red");
            checkItemWithEntity(members.get(0), "blue");
        }

        // Update item metadata
        store.lock();
        ri = store.getItem(ROOT_REGISTER + "reg1/_red", false);
        ri.setProperty(RegistryVocab.status, RegistryVocab.statusAccepted);
        store.update(ri, false);
        store.commit(); store.end();

        ri = store.getItem(ROOT_REGISTER + "reg1/_red", true);
        assertTrue(ri.getRoot().hasProperty(RegistryVocab.status, RegistryVocab.statusAccepted));
        assertEquals("red", RDFUtil.getStringValue(ri.getRoot(), RDFS.label));
        assertEquals(2, RDFUtil.getIntValue(ri.getRoot(), OWL.versionInfo, -1));

        // Update item content
        long ts = doUpdate(ROOT_REGISTER + "reg1/_red", "reddish");

        ri = store.getItem(ROOT_REGISTER + "reg1/_red", true);
        assertTrue(ri.getRoot().hasProperty(RegistryVocab.status, RegistryVocab.statusAccepted));
        assertEquals("reddish", RDFUtil.getStringValue(ri.getRoot(), RDFS.label));
        assertEquals("reddish", RDFUtil.getStringValue(ri.getEntity(), RDFS.label));
        assertEquals(ts, RDFUtil.asTimestamp( ri.getRoot().getProperty(DCTerms.modified).getObject() ) );

        assertEquals(3, RDFUtil.getIntValue(ri.getRoot(), OWL.versionInfo, -1));

        assertNotNull( store.getDescription(ROOT_REGISTER + "reg1/red") );
    }

    @Test
    public void testBNodeSubmitter() {
        addEntry("file:test/reg1.ttl", ROOT_REGISTER);

        Register parent = store.getCurrentVersion(REG1).asRegister();
        String base = NameUtils.ensureLastSlash(REG1);
        Model m = ModelFactory.createDefaultModel();
        m.read("file:test/red-submitter-test.ttl", base, FileUtils.langTurtle);

        store.lock();
        Calendar now = Calendar.getInstance();
        for (ResIterator i = m.listResourcesWithProperty(RDF.type, RegistryVocab.RegisterItem); i.hasNext();) {
            RegisterItem item = RegisterItem.fromRIRequest(i.next(), REG1, true, now);
            store.addToRegister(parent, item);
        }
        store.commit(); store.end();


        RegisterItem ri = store.getItem(REG1 + "/_red", true);
        List<Statement> submitters = ri.getRoot().listProperties(RegistryVocab.submitter).toList();
        assertEquals(1, submitters.size());

        store.lock();
        ri.getEntity().addProperty(RDFS.comment, "Updated");
        store.update(ri, true);
        store.commit(); store.end();

        ri = store.getItem(REG1 + "/_red", true);
        submitters = ri.getRoot().listProperties(RegistryVocab.submitter).toList();
        assertEquals(1, submitters.size());

    }

    private List<RegisterItem> members(Register reg, boolean withEntity) {
        List<String> itemURIs = new ArrayList<String>();
        for (RegisterEntryInfo info : store.listMembers(reg)) {
            itemURIs.add( info.getItemURI() );
        }
        return store.fetchAll(itemURIs, withEntity);
    }

    @Test
    public void testVersionRetrieval() throws InterruptedException {
        addEntry("file:test/reg1.ttl", ROOT_REGISTER);
        long ts0 = Calendar.getInstance().getTimeInMillis();
        Thread.sleep(5);
        addEntry("file:test/red.ttl", REG1);
        String itemURI = ROOT_REGISTER + "reg1/_red";
        String entity = ROOT_REGISTER + "reg1/red";
        checkLiveVersion("red", entity);

        long ts1 = doUpdate(itemURI, "red1");
        checkLiveVersion("red1", entity);

        long ts2 = doUpdate(itemURI, "red2");
        checkLiveVersion("red2", entity);

        long ts3 = doUpdate(itemURI, "red3");
        checkLiveVersion("red3", entity);

        checkVersionAt(itemURI, ts1-1, "red");
        checkVersionAt(itemURI, ts2-1, "red1");
        checkVersionAt(itemURI, ts3-1, "red2");
        checkVersionAt(itemURI, ts3+1, "red3");
        checkVersionAt(itemURI, ts0, null);

//        RegisterItem item = store.getItemWithVersion(itemURI, true);
//        assertNotNull(item);
//        assertNotNull(item.getRoot());
////        item.getRoot().getModel().write(System.out, "Turtle");
//        assertNotNull(item.getEntity());
//        assertEquals("red3", RDFUtil.getStringValue(item.getEntity(), RDFS.label));
    }

    private void checkLiveVersion(String label, String uri) {
        store.lockStoreRead();
        Resource current = basestore.asDataset().getDefaultModel().getResource(uri);
        assertEquals(label, RDFUtil.getStringValue(current, RDFS.label));
        store.unlockStoreRead();
    }

    private long doUpdate(String item, String label) {
        store.lock();
        try {
            Thread.sleep(10, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        RegisterItem ri = store.getItem(item, true);
        Resource e = ri.getEntity();
        e.removeAll(RDFS.label).addProperty(RDFS.label, label);
        Calendar now = Calendar.getInstance();
        ri.updateForEntity(false, now);
        store.update(ri, true, now);
        
        store.commit(); store.end();
        return now.getTimeInMillis();
    }

    private void checkVersionAt(String uri, long ts, String label) {
        Description d = store.getVersionAt(uri, ts);
        if (label == null) {
            assertNull(d);
        } else {
            assertNotNull(d);
            RegisterItem item = d.asRegisterItem();
            assertEquals(label, RDFUtil.getStringValue(item.getRoot(), RDFS.label));
            Resource entity = store.getEntity(item);
            assertEquals(label, RDFUtil.getStringValue(entity, RDFS.label));
        }
    }

    private void checkItemWithEntity(RegisterItem ri, String colour) {
        assertTrue(ri.getRoot().hasProperty(RegistryVocab.status, RegistryVocab.statusSubmitted));
        assertEquals(colour, RDFUtil.getStringValue(ri.getRoot(), RDFS.label));
        assertTrue(ri.getRoot().hasProperty(RegistryVocab.itemClass, SKOS.Concept));

        Resource entity = ri.getEntity();
        assertEquals(ROOT_REGISTER + "reg1/" + colour, entity.getURI());
        assertEquals(colour, RDFUtil.getStringValue(entity, RDFS.label));
        assertTrue(entity.hasProperty(RDF.type, SKOS.Concept));
    }

    @Test
    public void testExternalEntity() {
        addEntry("file:test/reg1.ttl", ROOT_REGISTER);

        Register parent = store.getCurrentVersion(REG1).asRegister();
        Model m = ModelFactory.createDefaultModel();
        m.read("file:test/absolute-black.ttl", REG1 + "/", FileUtils.langTurtle);
        Resource ri = m.listSubjectsWithProperty(RDF.type, RegistryVocab.RegisterItem).next();

        RegisterItem item = RegisterItem.fromRIRequest(ri, REG1, true);
        store.lock();
        store.addToRegister(parent, item);
        store.commit(); store.end();

        item = store.getItem(ROOT_REGISTER + "reg1/_black", true);
        assertEquals(ROOT_REGISTER + "reg1/_black", item.getRoot().getURI());
        assertEquals("black", item.getNotation());
        assertEquals("black", RDFUtil.getStringValue(item.getRoot(), RDFS.label));

        assertEquals(EXT_BLACK, item.getEntity().getURI());
        assertEquals("black", RDFUtil.getStringValue(item.getEntity(), RDFS.label));

        List<EntityInfo> occurances = store.listEntityOccurences(EXT_BLACK);
        assertEquals(1, occurances.size());
        EntityInfo info = occurances.get(0);
        assertEquals(EXT_BLACK, info.getEntityURI());
        assertEquals(ROOT_REGISTER + "reg1/_black", info.getItemURI());
        assertEquals(ROOT_REGISTER + "reg1", info.getRegisterURI());
        assertEquals(Status.Submitted, info.getStatus());
    }

    @Test
    public void testRegisterReconstruction() {
        addEntry("file:test/reg1.ttl", ROOT_REGISTER);
        Register reg1 = store.getCurrentVersion(REG1).asRegister();

        long ts0 = addEntry("file:test/red.ttl", REG1);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) { }
        long ts1 = addEntry("file:test/blue.ttl", REG1);
        String itemURI = ROOT_REGISTER + "reg1/_red";
        long ts2 = doUpdate(itemURI, "red1");
        long ts3 = doUpdate(itemURI, "red2");

        checkRegisterList(reg1, ts0+1, "red");
        checkRegisterList(reg1, ts1+1, "red", "blue");
        checkRegisterList(reg1, ts2+1, "red1", "blue");
        checkRegisterList(reg1, ts3+1, "red2", "blue");
        checkRegisterList(reg1, ts0-1);
    }

    @Test
    public void testRealDelete() {
        long baseSize = sizeSig();
        
        addEntry("file:test/reg1.ttl", ROOT_REGISTER);
        addEntry("file:test/blue.ttl", REG1);
        addEntry("file:test/red.ttl", REG1);
        addEntry("file:test/reg1.ttl", ROOT_REGISTER);

        String itemURI = ROOT_REGISTER + "reg1/_red";
        doUpdate(itemURI, "red1");

        addEntry("file:test/reg3.ttl", REG1);
        addEntry("file:test/green.ttl", REG3);
        assertTrue( sizeSig() > baseSize);
        
        store.lock();
        store.delete(REG1);
        store.commit();
        store.end();
        
        assertEquals(baseSize, sizeSig());
    }
    
    private long sizeSig() {
        long graphs = getCount("SELECT (COUNT(DISTINCT ?G) as ?n) WHERE { GRAPH ?G {}}", "n");
        long triples = getCount("SELECT (COUNT(1) as ?n) WHERE { ?s ?p ?o }", "n");
        return graphs * 10000 + triples;
    }
    
    private long getCount(String query, String var) {
        ResultSet results = store.query(query);
        return results.next().getLiteral(var).asLiteral().getLong();
    }
    
//    private void printStore() {
//        basestore.lock();
//        try {
//            Dataset ds =  basestore.asDataset();
//            Model store = ds.getDefaultModel();
//            System.out.println("Default model:");
//            store.write(System.out, "Turtle");
//            for (Iterator<String> i = ds.listNames(); i.hasNext(); ) {
//                String graphName = i.next();
//                Model graph = ds.getNamedModel(graphName);
//                System.out.println("Graph " + graphName + ":");
//                graph.write(System.out, "Turtle");
//            }
//        } finally {
//            basestore.end();
//        }
//    }
    
    private void checkRegisterList(Register register, long ts, String...labels) {
        List<RegisterItem> members = StoreUtil.fetchMembersAt(store, register, ts, false);
        assertEquals(labels.length, members.size());
        for (String label : labels) {
            boolean found = false;
            for (RegisterItem item : members) {
                if (RDFUtil.getStringValue(item.getRoot(), RDFS.label).equals(label)) {
                    found = true; break;
                }
            }
            assertTrue(found);
        }
    }

    @SuppressWarnings("unused")
    private void dumpAll() {
        dumpAll( basestore.asDataset().getDefaultModel() );
    }

    private void dumpAll(Model m) {
        m.setNsPrefixes( Prefixes.get() );
        m.write(System.out, FileUtils.langTurtle);
    }
}
