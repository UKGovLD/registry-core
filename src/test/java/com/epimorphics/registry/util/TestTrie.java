/******************************************************************
 * File:        TestTrie.java
 * Created by:  Dave Reynolds
 * Created on:  17 Feb 2013
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.epimorphics.util.TestUtil;

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
        
        TestUtil.testArray( trie.findAll("a/b", null), new String[] {"1", "3"});
        TestUtil.testArray( trie.findAll("a", null), new String[] {"1", "2", "3"});
        TestUtil.testArray( trie.findAll("a/b/d", null), new String[] {"1"});
        TestUtil.testArray( trie.findAll("f", null), new String[] {"4"});
        TestUtil.testArray( trie.findAll("g", null), new String[] {});
        TestUtil.testArray( trie.findAll("", null), new String[] {"1", "2", "3", "4"});
        TestUtil.testArray( trie.findAll("", new Predicate<String>() {
            @Override
            public boolean test(String o) {
                return Integer.parseInt(o) >= 3;
            }}), new String[] {"3", "4"});

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
