/******************************************************************
 * File:        MatchResult.java
 * Created by:  Dave Reynolds
 * Created on:  18 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
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
