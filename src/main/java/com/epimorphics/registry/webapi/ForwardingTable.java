/******************************************************************
 * File:        ForwardingTable.java
 * Created by:  Dave Reynolds
 * Created on:  17 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.DelegationRecord;
import com.epimorphics.registry.core.ForwardingRecord;
import com.epimorphics.registry.core.ForwardingRecord.Type;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.util.Trie;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Global singleton for looking up possible forwarding/delegation/federation matches
 * for a URI request.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ForwardingTable {
    static Logger log = LoggerFactory.getLogger(ForwardingTable.class);

    public static interface ForwardingTableI {

        /** Register a new forwarding spec at its target path */
        public void register(ForwardingRecord record);

        /** Remove an existing forwarding instructions for the given path */
        public void unregister(String path);

        /** Look up a URI to find any forwarding instructions that match it */
        public MatchResult match(String path);

        /** Call to finalize installation of registrations, e.g. by configuring an external proxy */
        public void updateConfig();
    }

    /**
     * Update the forwarding table to reflect a change in registration or status
     * of an item.
     * @param item The registered item, with associated entity information
     */
    public static void update(RegisterItem item) {
        if (item.getStatus().isAccepted()) {
            ForwardingTable.get().register( recordFor(item) );
        } else {
            String loc = item.getEntity().getURI();
            String base = Registry.get().getBaseURI();
            if (loc.startsWith(base)) {
                loc = loc.substring(base.length());
                ForwardingTable.get().unregister(loc);
            }
        }
    }

    public static ForwardingRecord recordFor(RegisterItem item) {
        Resource record = item.getEntity();
        ForwardingRecord.Type type = Type.FORWARD;
        if (record.hasProperty(RDF.type, RegistryVocab.FederatedRegister)) {
            type = Type.FEDERATE;
        } else if (record.hasProperty(RDF.type, RegistryVocab.DelegatedRegister)) {
            type = Type.DELEGATE;
        }
        String target = record.getPropertyResourceValue(RegistryVocab.delegationTarget).getURI();
        if (type == Type.DELEGATE) {
            DelegationRecord dr = new DelegationRecord(record.getURI(), target, type);
            Resource s = record.getPropertyResourceValue(RegistryVocab.enumerationSubject);
            if (s != null) dr.setSubject(s);
            Resource p = record.getPropertyResourceValue(RegistryVocab.enumerationPredicate);
            if (p != null) dr.setPredicate(p);
            Resource o = record.getPropertyResourceValue(RegistryVocab.enumerationObject);
            if (o != null) dr.setObject(o);
            return dr;
        } else {
            ForwardingRecord fr = new ForwardingRecord(record.getURI(), target, type);
            int code = RDFUtil.getIntValue(record, RegistryVocab.forwardingCode, -1);
            if (code != -1) {
                fr.setForwardingCode( code );
            }
            return fr;
        }
    }

    public static class MatchResult {
        protected ForwardingRecord record;
        protected String pathRemainder;

        public MatchResult(ForwardingRecord record, String pathRemainder) {
            this.record = record;
            this.pathRemainder = pathRemainder;
        }

        public ForwardingRecord getRecord() {
            return record;
        }

        public String getPathRemainder() {
            return pathRemainder;
        }

    }

    protected static ForwardingTableI the = new ForwardingTableImpl();

    public static ForwardingTableI get() {
        return the;
    }

    public static void set(ForwardingTableI newtable) {
        the = newtable;
    }

    protected static class ForwardingTableImpl implements ForwardingTableI {
        protected Trie<ForwardingRecord> trie = new Trie<ForwardingRecord>();

        @Override
        public synchronized void register(ForwardingRecord record) {
            String loc = record.getLocation();
            String base = Registry.get().getBaseURI();
            if (loc.startsWith(base)) {
                loc = loc.substring(base.length());
            } else {
                log.error("Attempted to forward an external URL, will be ignored: " + loc);
                return;
            }
            log.info("Registering delegation path at " + loc);
            trie.register(loc, record);
        }

        @Override
        public synchronized void unregister(String path) {
            log.info("Unregistering delegation path: " + path);
            trie.unregister(path);
        }

        @Override
        public synchronized MatchResult match(String path) {
            Trie.MatchResult<ForwardingRecord> mr = trie.match(path);
            if (mr != null) {
                return new MatchResult(mr.getMatch(), mr.getPathRemainder());
            } else {
                return null;
            }
        }

        @Override
        public void updateConfig() {
            // TODO configure front end proxy, registrations should record 200 cases as a separate list

        }
    }
}


