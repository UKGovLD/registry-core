/******************************************************************
 * File:        PatchUtil.java
 * Created by:  Dave Reynolds
 * Created on:  1 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import static com.epimorphics.rdfutil.RDFUtil.allPropertiesOf;

import java.util.Set;

import com.epimorphics.rdfutil.RDFUtil;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Support for merging updates into a current model.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class PatchUtil {

    /**
     * Install the src data over the top of the dest data.
     * Any rigid properties will not be changed.
     * Any preserved properties which are missing from the src will be preserved by if present on the source they will be updated.
     */
    public static void update(Resource src, Resource dest, Property[] rigidProps, Property[] preserveProps) {
        Set<Property> toCopy = allPropertiesOf(src);
        Set<Property> toRemove = allPropertiesOf(dest);
        for (Property rigid : rigidProps) {
            toCopy.remove(rigid);
            toRemove.remove(rigid);
        }
        for (Property p : preserveProps) {
            if (!toCopy.contains(p)) {
                toRemove.remove(p);
            }
        }
        for (Property p : toRemove){
            dest.removeAll(p);
        }
        for (Property p : toCopy) {
            RDFUtil.copyProperty(src, dest, p);
        }
    }

    public static final Property[] EMPTY = new Property[0];

    public static void update(Resource src, Resource dest) {
        update(src, dest, EMPTY, EMPTY);
    }

    /**
     * Copy the properties of the source replacing any existing values on the dest,
     * omits any properties on the rigid list.
     * Any properties of dest that are not mentioned in src are left intact.
     */
    public static void patch(Resource src, Resource dest, Property...rigidProps) {
        Set<Property> toCopy = allPropertiesOf(src);
        for (Property rigid : rigidProps) {
            toCopy.remove(rigid);
        }
        for (Property p : toCopy) {
            dest.removeAll(p);
            RDFUtil.copyProperty(src, dest, p);
        }
    }

}
