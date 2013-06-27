/******************************************************************
 * File:        Util.java
 * Created by:  Dave Reynolds
 * Created on:  6 Feb 2013
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
