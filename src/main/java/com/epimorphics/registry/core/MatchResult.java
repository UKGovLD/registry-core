/******************************************************************
 * File:        MatchResult.java
 * Created by:  Dave Reynolds
 * Created on:  18 Feb 2013
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


/**
 * Struct to package up result of looking matching a URI request
 * to a set of forwarding rules.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */

public  class MatchResult {
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
