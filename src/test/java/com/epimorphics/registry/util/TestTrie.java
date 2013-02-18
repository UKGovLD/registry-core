/******************************************************************
 * File:        TestTrie.java
 * Created by:  Dave Reynolds
 * Created on:  17 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestTrie {

    @Test
    public void testTrie() {
        Trie<String> trie = new Trie<String>();

        trie.register("a/b/d", "1");
        trie.register("a/c",   "2");
        trie.register("a/b/e", "3");
        trie.register("f",     "4");

        assertEquals("1", trie.match("a/b/d/foo/bar").getMatch());
        assertEquals("foo/bar", trie.match("a/b/d/foo/bar").getPathRemainder());
        assertEquals("2", trie.match("a/c").getMatch());
        assertEquals("3", trie.match("a/b/e/g").getMatch());
        assertEquals("4", trie.match("f/g").getMatch());
        assertNull(trie.match("g"));
        assertNull(trie.match("a/b/g"));
        assertNull(trie.match("a/d"));

        trie.unregister("f");
        trie.unregister("a/b/e");
        assertEquals("1", trie.match("a/b/d/foo/bar").getMatch());
        assertEquals("2", trie.match("a/c").getMatch());
        assertNull( trie.match("a/b/e/g") );
        assertNull( trie.match("f/g") );

        trie.register("a/b/d", "1");
        assertEquals("1", trie.match("a/b/d/foo/bar").getMatch());
    }
}
