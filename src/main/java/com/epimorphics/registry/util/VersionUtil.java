/******************************************************************
 * File:        VersionUtil.java
 * Created by:  Dave Reynolds
 * Created on:  27 Jan 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.vocabs.Time;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * Support for managing the (surprisingly complex) resource versioning model.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class VersionUtil {

    public static void flatten(Resource root, Resource version) {
        ResourceUtils.renameResource(version, root.getURI());
        root.removeAll(Version.currentVersion);
        root.removeAll(DCTerms.isVersionOf);
        root.removeAll(DCTerms.replaces);
    }

    public static String versionedURI(Resource r, int version) {
        return r.getURI() + ":" + version;
    }

    public static Resource nextVersion(Resource root, Calendar timemark, Property... rigidProps) {
        Resource currentVersion = null; 
        Model vModel = ModelFactory.createDefaultModel();
        int vNum = RDFUtil.getIntValue(root, OWL.versionInfo, 0);
        if (vNum != 0) {
            currentVersion = vModel.createResource( versionedURI(root, vNum) );
        }
        Resource ver = vModel.createResource( versionedURI(root, vNum+1) );
        Resource newRoot = vModel.createResource( root.getURI() );
        
        Set<Property> rigids = new HashSet<Property>();
        for (Property p : rigidProps) rigids.add(p);
        
        for (StmtIterator si = root.listProperties(); si.hasNext();) {
            Statement s = si.next();
            Property p = s.getPredicate();
            RDFNode value = s.getObject();
            if (value.isAnon()) {
                // Copy one level of bnodes across to handle entity references
                Resource newValue = vModel.createResource();
                for (StmtIterator vi = value.asResource().listProperties(); vi.hasNext();) {
                    Statement vs = vi.next();
                    newValue.addProperty(vs.getPredicate(), vs.getObject());
                }
                value = newValue;
            }
            if (p.equals(OWL.versionInfo) || p.equals(Version.currentVersion)) continue;
            if (rigids.contains( p )) {
                newRoot.addProperty(p, value);
            } else {
                ver.addProperty(p, value);
            }
        }
        
        ver.addLiteral(OWL.versionInfo, vNum)
           .addProperty(DCTerms.isVersionOf, ver);
        newRoot.addProperty(Version.currentVersion, ver);
        Literal timemarkL = vModel.createTypedLiteral(timemark);
        Resource tpoint = vModel.createResource().addProperty(Time.inXSDDateTime, timemarkL);
        ver.addProperty(Version.interval, vModel.createResource().addProperty(Time.hasBeginning, tpoint));
        if (currentVersion != null) {
            ver.addProperty(DCTerms.replaces, currentVersion);
            root.getPropertyResourceValue(Version.interval).addProperty(Time.hasEnd, tpoint);
        }
        return ver;
    }

}
