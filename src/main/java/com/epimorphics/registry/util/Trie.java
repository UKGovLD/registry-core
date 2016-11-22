/******************************************************************
 * File:        Trie.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Trie structure for matching URI prefixes. The trie is based on matching URI segments
 * rather then individual characters (unlike arq lib Trie) and does not support
 * path parameter patterns (unlike Modal TrieMatcher). Terminals can only appear on ends of paths.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Trie<T> {
    protected TrieNode<T> root = new TrieBranch<T>();

    public void register(String path, T match) {
        root.register(split(path), 0, match);
    }

    public void unregister(String path) {
        root.unregister(split(path), 0);
    }

    public MatchResult<T> match(String uri) {
        return root.match(split(uri), 0);
    }

    public List<T> findAll(String path, Predicate<T> filter) {
        List<T> results = new ArrayList<T>();
        root.findAll(split(path), 0, filter, results);
        return results;
    }

    private String[] split(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.isEmpty()) return new String[0];
        return path.split("/");
    }

    public static class MatchResult<T> {
        protected T match;
        protected String remainder;
        protected MatchResult(T match, String[] path, int index) {
            this.match = match;
            remainder = "";
            for (int i = index; i < path.length; i ++) {
                if (!remainder.isEmpty()) remainder += "/";
                remainder += path[i];
            }
        }

        public T getMatch() {
            return match;
        }

        public String getPathRemainder() {
            return remainder;
        }
    }

    static interface TrieNode<T> {
        public MatchResult<T> match(String[] path, int index);

        public void register(String[] path, int index, T match);

        public void unregister(String[] path, int index);

        public void findAll(String[] path, int index, Predicate<T> filter, List<T> results);
    }

    static class TrieTerminal<T> implements TrieNode<T> {
        protected T match;
        protected TrieTerminal(T match) {
            this.match = match;
        }

        public MatchResult<T> match(String[] path, int index) {
            return new MatchResult<T>(match, path, index);
        }

        @Override
        public void register(String[] path, int index, T match) {
        }

        @Override
        public void unregister(String[] path, int index) {
        }

        @Override
        public void findAll(String[] path, int index, Predicate<T> filter, List<T> results) {
            if (index >= path.length -1) {
                if (filter == null || filter.test(match)) {
                    results.add(match);
                }
            }
        }
    }

    static class TrieBranch<T> implements TrieNode<T> {
        protected Map<String, TrieNode<T>> branches = new HashMap<String, TrieNode<T>>();

        public MatchResult<T> match(String[] path, int index) {
            if (index >= path.length) return null;   // can "never" happen?
            TrieNode<T> next = branches.get(path[index]);
            if (next == null) {
                return null;
            } else {
                return next.match(path, index + 1);
            }
        }

        public void register(String[] path, int index, T match) {
            String segment = path[index];
            if (index >= path.length-1) {
                // end of path
                branches.put(segment, new TrieTerminal<T>(match));
            } else {
                TrieNode<T> node = branches.get(segment);
                if (node == null) {
                    node = new TrieBranch<T>();
                    branches.put(segment, node);
                }
                node.register(path, index + 1, match);
            }
        }

        @Override
        public void unregister(String[] path, int index) {
            String segment = path[index];
            if (index >= path.length-1) {
                branches.remove(segment);
            } else {
                TrieNode<T> node = branches.get(segment);
                if (node != null) {
                    node.unregister(path, index + 1);
                }
            }
        }

        @Override
        public void findAll(String[] path, int index, Predicate<T> filter, List<T> results) {
            if (index < path.length && path.length > 0) {
                TrieNode<T> next = branches.get(path[index]);
                if (next == null) {
                    return;  // no further matches down this path
                } else {
                    next.findAll(path, index + 1, filter, results);
                }
            } else {
                // End of match path, accumulate everything
                for (String key : branches.keySet()) {
                    branches.get(key).findAll(path, index, filter, results);
                }
            }
        }
    }
}
