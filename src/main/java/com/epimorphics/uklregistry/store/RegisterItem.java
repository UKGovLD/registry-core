/******************************************************************
 * File:        RegisterItem.java
 * Created by:  Dave Reynolds
 * Created on:  23 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.store;

import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.server.webapi.BaseEndpoint;
import com.epimorphics.uklregistry.vocab.Registry;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Abstraction for creation and access to the details of a register item.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegisterItem extends Description {

    Resource entity;
    String notation;
    String parentURI;

    /**
     * Construct a new register item from a loaded description
     */
    public RegisterItem(Resource root) {
        super(root);
    }

    /**
     * Construct a register item from a description
     * @param root  the item resource in a model that can be modified
     * @param parentURI the URI of the target register in which the registration is taking place
     */
    private RegisterItem(Resource root, String parentURI) {
        super(root);
        this.parentURI = parentURI;
    }

    /**
     * Construct a register item from a description
     * @param root  the item resource in a model that can be modified
     * @param parentURI the URI of the target register in which the registration is taking place
     * @param notation the local name of the item within the parent register
     */
    private RegisterItem(Resource root, String parentURI, String notation) {
        super(root);
        this.parentURI = parentURI;
        this.notation = notation;
    }

    public String getNotation() {
        if (notation == null) {
            determineLocation();
        }
        return notation;
    }

    public Resource getEntity() {
        return entity;
    }

    /**
     * Takes a register item resource from a request payload, determines and checks
     * the intended URI for both it and the entity, fills in blanks on the register item,
     * returns the constructed item representation.
     */
    public static RegisterItem fromRIRequest(Resource ri, String parentURI) {
        Model d = Closure.closure(ri, false);
        Resource entity = findRequiredEntity(ri);
        Closure.closure(entity, false, d);
        RegisterItem item = new RegisterItem( ri.inModel(d), parentURI );
        item.relocate();
        item.relocateEntity(entity);
        return item;
    }

    /**
     * Constructs a RegisterIem for an entity in a request payload that doesn't have
     * any explict RegisterItem specification.
     */
    public static RegisterItem fromEntityRequest(Resource e, String parentURI) {
        Model d = Closure.closure(e, false);
        String notation = riNotationFromEntity(e, parentURI);
        String riURI = parentURI + "/_" + notation;
        Resource ri = d.createResource(riURI)
                .addProperty(RDF.type, Registry.RegisterItem)
                .addProperty(Registry.notation, notation);
        RegisterItem item = new RegisterItem( ri, parentURI, notation );
        Resource entity = e.inModel(d);
        item.relocateEntity(entity);
        return item;
    }

    private void relocate() {
        String uri = parentURI + "/_" + getNotation();
        if ( ! uri.equals(root.getURI()) ) {
            ResourceUtils.renameResource(root, uri);
        }
        root = root.getModel().createResource(uri);
    }

    private void relocateEntity(Resource srcEntity) {
        Model m = root.getModel();
        String uri = entityURI(srcEntity);
        if ( ! uri.equals(srcEntity.getURI()) ) {
            ResourceUtils.renameResource(srcEntity.inModel( m ), uri);
        }
        entity = m.createResource(uri);
        updateFor(entity);
    }

    /**
     * Update a register item to reflect the given entity being registered.
     */
    private void updateFor(Resource entity) {
        RDFUtil.timestamp(root, DCTerms.dateSubmitted);
        if ( !root.hasProperty(Registry.status)) {
            root.addProperty(Registry.status, Registry.statusSubmitted);
        }
        if (!root.hasProperty(Registry.notation)) {
            root.addProperty(Registry.notation, notation);
        }
        RDFUtil.copyProperty(entity, root, RDFS.label);
        RDFUtil.copyProperty(entity, root, SKOS.prefLabel);
        RDFUtil.copyProperty(entity, root, SKOS.altLabel);
        RDFUtil.copyProperty(entity, root, DCTerms.description);
        for (StmtIterator si = entity.listProperties(RDF.type); si.hasNext();) {
            root.addProperty(Registry.itemClass, si.next().getObject());
        }
        Resource entityref = root.getModel().createResource( root.getURI() + "#entityref" )
                .addProperty(Registry.entity, entity);
        root.addProperty(Registry.definition, entityref);
        
        // TODO the reg:submitter may be set automatically to an identifier for the user making the submission
    }


    private void determineLocation() {
        if (root.isURIResource()) {
            String uri = root.getURI();
            if (uri.equals(BaseEndpoint.DUMMY_BASE_URI)) {
                // Empty relative URI specified
                notation = getExplicitNotation(root);
            } else if (uri.startsWith(BaseEndpoint.DUMMY_BASE_URI)) {
                // relative URI
                notation = validateNotation( uri.substring(BaseEndpoint.DUMMY_BASE_URI.length() + 1));
            } else if (uri.startsWith(parentURI)) {
                notation = validateNotation( uri.substring(parentURI.length() + 1) );
            } else {
                // Register Item has explicit URI which is not relative to the target parent register
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
        } else {
            notation = getExplicitNotation(root);
        }
        if (notation == null) {
            notation = UUID.randomUUID().toString();
        } else if (notation.contains("/")) {
            // TODO check pchar* syntax more fully
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    private static String getExplicitNotation(Resource root) {
        if (root.hasProperty(Registry.notation)) {
            String location = RDFUtil.getStringValue(root, Registry.notation);
            if (location.startsWith("_")) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
            return location;
        }
        return null;
    }

    private static String validateNotation(String notation) {
        if (notation.startsWith("_")) {
            return notation.substring(1);
        } else {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    private static String riNotationFromEntity(Resource entity, String parentURI) {
        String uri = entity.getURI();
        String notation = null;
        if (entity.isAnon() || uri.equals(BaseEndpoint.DUMMY_BASE_URI)) {
            notation = UUID.randomUUID().toString();
        } else if (uri.startsWith(BaseEndpoint.DUMMY_BASE_URI)) {
            // relative URI
            notation = uri.substring(BaseEndpoint.DUMMY_BASE_URI.length() + 1);
        } else if (uri.startsWith(parentURI)) {
            notation = uri.substring(parentURI.length() + 1);
        } else {
            // Absolute URI, just use it and create anotation for the item
            notation = UUID.randomUUID().toString();
        }
        return notation;
    }

    private String entityURI(Resource entity) {
        String uri = entity.getURI();
        if (entity.isAnon() || uri.equals(BaseEndpoint.DUMMY_BASE_URI)) {
            uri = makeEntityURI();
        } else if (uri.startsWith(BaseEndpoint.DUMMY_BASE_URI)) {
            // relative URI
            String eNotation = uri.substring(BaseEndpoint.DUMMY_BASE_URI.length() + 1);
            if (!eNotation.equals(notation)) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
            uri = makeEntityURI();
        } else if (uri.startsWith(parentURI)) {
            String eNotation = uri.substring(parentURI.length() + 1);
            if (!eNotation.equals(notation)) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
            uri = makeEntityURI();
        }
        return uri;
    }

    private String makeEntityURI() {
        return parentURI + "/" + notation;
    }

    private static Resource findRequiredEntity(Resource ri) {
        Resource definition = ri.getPropertyResourceValue(Registry.definition);
        if (definition != null) {
            Resource entity = definition.getPropertyResourceValue(Registry.entity);
            if (entity != null){
                return entity;
            }
        }
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
}
