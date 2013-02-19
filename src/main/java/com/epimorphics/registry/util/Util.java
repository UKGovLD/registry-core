/******************************************************************
 * File:        Util.java
 * Created by:  Dave Reynolds
 * Created on:  6 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Return a window on an list. A length of -1 can be used to indicate a window with no upper bound.
     */
    public static <T> List<T> listWindow(List<T> list, int offset, int length) {
        List<T> result = new ArrayList<T>( length == -1 ? (list.size() - offset) : length );
        int limit = length == -1 ? list.size() : Math.min(length + offset, list.size());
        for (int i = offset; i < limit; i++) {
            result.add( list.get(i) );
        }
        return result;
    }
}
