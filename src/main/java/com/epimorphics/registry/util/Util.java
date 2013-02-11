/******************************************************************
 * File:        Util.java
 * Created by:  Dave Reynolds
 * Created on:  6 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;

/**
 * Misc. utilities
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Util {

    /**
     * Convert a time parameter string to a timestamp.
     * Allows an xsd:date, xsd:datetime or a simple long.
     * Returns -1 if no conversion is possible.
     */
    public static long asTimestamp(String time) {
        try {
            XSDDateTime dateTime = (XSDDateTime)XSDDatatype.XSDdateTime.parse(time);
            return dateTime.asCalendar().getTimeInMillis();
        } catch (Exception e) {}
        try {
            XSDDateTime dateTime = (XSDDateTime)XSDDatatype.XSDdate.parse(time);
            return dateTime.asCalendar().getTimeInMillis();
        } catch (Exception e) {}
        try {
            return Long.parseLong(time);

        } catch (Exception e) {}
        return -1;
    }
}
