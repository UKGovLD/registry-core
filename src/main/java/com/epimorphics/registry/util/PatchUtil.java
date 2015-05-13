/******************************************************************
 * File:        PatchUtil.java
 * Created by:  Dave Reynolds
 * Created on:  1 Feb 2013
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

import static com.epimorphics.rdfutil.RDFUtil.allPropertiesOf;

import java.util.Set;

import com.epimorphics.rdfutil.RDFUtil;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.util.Closure;

/**
 * Support for merging updates into a current model.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class PatchUtil {

    /**
     * Install the src data over the top of the dest data.
     * Any rigid properties will not be changed.
     * Any preserved properties which are missing from the src will be preserved but if present on the source they will be updated.
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
    
    /**
     * Test if the given source patch will change any values on current destination resource
     * (omitting values of rigid properties)
     */
    public static boolean willChange(Resource src, Resource dest, Property...rigidProps) {
        Set<Property> toCopy = allPropertiesOf(src);
        for (Property rigid : rigidProps) {
            toCopy.remove(rigid);
        }

        // Brute force implementation that allows for bnodes
        // Could optimize by doing simple comparisons unless there really is a bnode value
        return ! extract(src, toCopy).isIsomorphicWith( extract(dest, toCopy) );
        
    }

    private static Model extract(Resource root, Set<Property> toCopy) {
        Model result = ModelFactory.createDefaultModel();
        for (Property p : toCopy) {
            for (StmtIterator i = root.listProperties(p); i.hasNext();) {
                RDFNode value = i.next().getObject();
                if (value.isAnon()) {
                    Closure.closure(value.asResource(), false, result);
                }
                result.add(root, p, value);
            }
        }
        return result;
    }
}
