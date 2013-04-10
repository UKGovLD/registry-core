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
import java.util.regex.Pattern;

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
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Support for managing the (surprisingly complex) resource versioning model.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class VersionUtil {

    /**
     * Merges the version information onto a root VersionedThing.
     */
    public static void flatten(Resource root, Resource version) {
        ResourceUtils.renameResource(version, root.getURI());
        root.removeAll(Version.currentVersion);
        root.removeAll(Version.interval);
        root.removeAll(DCTerms.isVersionOf);
        root.removeAll(DCTerms.replaces);
    }

    public static String versionedURI(Resource r, int version) {
        return r.getURI() + ":" + version;
    }


    public static boolean isVersionedResource(Resource r) {
        return versionMatch.matcher(r.getURI()).matches();
    }
    static final Pattern versionMatch = Pattern.compile("^.*:\\d+");

    /**
     * Creates a new version of a given root resource, splitting the resource properties according to whether they are rigid or not.
     * If the resource already has an owl:versionInfo marker then the next version number in sequence will be used.
     *
     * @param root     the resource to be versioned, should be in flattened form
     * @param timemark the timestamp to use for the new version, if the root resource was previously verisoned then an
     *                 interval close assertion for the previous version will also be generated
     * @param rigidProps an optional list of properties that should be stored on the VersionedThing rather than the Version resource
     * @return the created version resource in a new Model containing the copied and separated property/values
     */
    public static Resource nextVersion(Resource root, Calendar timemark, Property... rigidProps) {
        Resource currentVersion = null;
        Model vModel = ModelFactory.createDefaultModel();
        int vNum = RDFUtil.getIntValue(root, OWL.versionInfo, 0);
        if (vNum != 0) {
            currentVersion = vModel.createResource( versionedURI(root, vNum) );
        }
        vNum++;
        Resource ver = vModel.createResource( versionedURI(root, vNum) );
        Resource newRoot = vModel.createResource( root.getURI() );

        Set<Property> rigids = new HashSet<Property>();
        for (Property p : rigidProps) rigids.add(p);
        rigids.add(RDF.type);

        for (StmtIterator si = root.listProperties(); si.hasNext();) {
            Statement s = si.next();
            Property p = s.getPredicate();
            boolean isRigid = rigids.contains(p);
            RDFNode value = s.getObject();
            if (value.isAnon() && (!isRigid || currentVersion == null)) {
                // Copy one level of bnodes across to handle entity references
                // but don't do that for rigid properties of roots which already exist, otherwise get duplicates
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
           .addProperty(DCTerms.isVersionOf, root);
        newRoot.addProperty(Version.currentVersion, ver);
        makeInterval(ver, timemark, null);
        if (currentVersion != null) {
            ver.addProperty(DCTerms.replaces, currentVersion);
            makeInterval(currentVersion, null, timemark);
        }
        return ver;
    }

    private static Resource makeTimePoint(Model vModel, Calendar timemark) {
        Literal timemarkL = vModel.createTypedLiteral(timemark);
        return vModel.createResource().addProperty(Time.inXSDDateTime, timemarkL);
    }

    private static Resource makeInterval(Resource version, Calendar start, Calendar end) {
        Model m = version.getModel();
        Resource interval = m.createResource( version.getURI() + "#interval" );
        if (start != null) {
            interval.addProperty(Time.hasBeginning, makeTimePoint(m, start));
        }
        if (end != null) {
            interval.addProperty(Time.hasEnd, makeTimePoint(m, end));
        }
        version.addProperty(Version.interval, interval);
        return interval;
    }
}
