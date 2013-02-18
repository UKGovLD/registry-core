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

import com.epimorphics.registry.core.ForwardingRecord;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.util.Trie;

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
            trie.register(loc, record);
        }

        @Override
        public synchronized void unregister(String path) {
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


