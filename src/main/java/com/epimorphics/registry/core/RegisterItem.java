/******************************************************************
 * File:        RegisterItem.java
 * Created by:  Dave Reynolds
 * Created on:  23 Jan 2013
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

import java.util.Calendar;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.vocab.RegistryVocab;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.util.Closure;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * Abstraction for creation and access to the details of a register item plus its entity.
 *
 * The entity, if present, should be kept in a separate graph so that it can be stored
 * independently of the item without making any assumptions like CBD.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegisterItem extends Description {

    Resource entity;
    String notation;
    String parentURI;
    boolean entityAsGraph = false;

    /** Properties that should not be changed once set */
    public static final Property[] RIGID_PROPS = new Property[] {
                                RegistryVocab.register, RegistryVocab.notation,
                                RegistryVocab.itemClass, // RegistryVocab.predecessor,
                                RegistryVocab.submitter, RDF.type,
                                DCTerms.dateSubmitted, OWL.versionInfo};

    /** Properties that must be present so cannot be removed but may be updated */
    public static final Property[] REQUIRED_PROPS = new Property[] {RegistryVocab.status, RDFS.label};

    /** Properties that are set internally and should not be set by register/update payload */
    public static final Property[] INTERNAL_PROPS = new Property[] {RegistryVocab.register, RegistryVocab.subregister, OWL.versionInfo};

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

    /**
     * Record the address of an associated entity.
     * Should be in a separate model to allowed to be saved on its own.
     * Does NOT create reg:definition/reg:entity links.
     */
    public void setEntity(Resource entity) {
        this.entity = entity;
    }

    /**
     * Flag that the entity should stored as a whole graph, not as a simple closure
     */
    public void setAsGraph(boolean asGraph) {
        this.entityAsGraph = asGraph;
    }


    /**
     * Test if the entity should stored as a whole graph, not as a simple closure
     */
    public boolean isGraph() {
        return entityAsGraph;
    }

    public String getNotation() {
        if (notation == null) {
            determineLocation();
        }
        return notation;
    }

    /**
     * Return the associated entity resource, if any. A RegisterItem can be created
     * or retrieved without also setting the entity so this may return null in those cases.
     */
    public Resource getEntity() {
        return entity;
    }

    /**
     * Return the entity specified by reg:definition/reg:entity.
     * For a well-formed RegisterItem this should never return null, even
     * if the entity itself hasn't been retrieved and attached to this wrapper object.
     */
    public Resource getEntitySpec() {
        Resource def = root.getPropertyResourceValue(RegistryVocab.definition);
        if (def != null) {
            return def.getPropertyResourceValue(RegistryVocab.entity);
        } else {
            return null;
        }
    }

    public Status getStatus() {
        return Status.forResource( root.getPropertyResourceValue(RegistryVocab.status) );
    }

    public String getRegisterURI() {
        if (parentURI == null) {
            Resource reg = root.getPropertyResourceValue(RegistryVocab.register);
            if (reg != null) {
                parentURI = reg.getURI();
            }
        }
        return parentURI;
    }

    public boolean isRegister() {
        return root.hasProperty(RegistryVocab.itemClass, RegistryVocab.Register);
    }

    public Register getAsRegister(StoreAPI store) {
        return Description.descriptionFrom( store.getEntity(this), store ).asRegister();
    }

    public Boolean isRegisterItem() {
        return true;
    }

    public Boolean isEntity() {
        return false;
    }

    /**
     * Takes a register item resource from a request payload, determines and checks
     * the intended URI for both it and the entity, fills in blanks on the register item,
     * returns the constructed item representation.
     */
    public static RegisterItem fromRIRequest(Resource ri, String parentURI, boolean isNewSubmission) {
        return fromRIRequest(ri, parentURI, isNewSubmission, Calendar.getInstance());
    }

    public static RegisterItem fromRIRequest(Resource ri, String parentURI, boolean isNewSubmission, Calendar now) {
        Model d = Closure.closure(ri, false);
        Resource entity = findRequiredEntity(ri);
        RegisterItem item = new RegisterItem( ri.inModel(d), parentURI );
        item.relocate();
        if (entity != null) {
            entity = entity.inModel( Closure.closure(entity, false) );
//            item.relocateEntity(entity);
            item.setEntity(entity);   // Don't relocate in this case, so that bNode reservations are preserved
            item.updateForEntity(isNewSubmission, now);
        }
        return item;
    }

    /**
     * Constructs a RegisterIem for an entity in a request payload that doesn't have
     * any explict RegisterItem specification.
     */
    public static RegisterItem fromEntityRequest(Resource e, String parentURI, boolean isNewSubmission) {
        return fromEntityRequest(e, parentURI, isNewSubmission, Calendar.getInstance());
    }

    public static RegisterItem fromEntityRequest(Resource e, String parentURI, boolean isNewSubmission, Calendar now) {
        String notation = notationFromEntity(e, parentURI);
        String riURI = makeItemURI(parentURI, notation);
        Resource ri = ModelFactory.createDefaultModel().createResource(riURI)
                .addProperty(RDF.type, RegistryVocab.RegisterItem);
        try {
            int notationInt = Integer.parseInt(notation);
            ri.addLiteral(RegistryVocab.notation, notationInt);
        } catch (NumberFormatException ex) {
            ri.addProperty(RegistryVocab.notation, notation);
        }
        RegisterItem item = new RegisterItem( ri, parentURI, notation );
        Resource entity = e.inModel( Closure.closure(e, false) );
        item.relocateEntity(entity);
        item.updateForEntity(isNewSubmission, now);
        return item;
    }

    /**
     * Set the status of the item
     */
    public Resource setStatus(String status) {
        return setStatus( Status.forString(status, null) );
    }

    /**
     * Set the status of the item, bypassing lifecycle checks
     */
    public Resource forceStatus(String status) {
        return forceStatus( Status.forString(status, null) );
    }

    /**
     * Set the status of the item, by passing lifecycle check
     */
    public Resource forceStatus(Status s) {
        if (s != null) {
            return setStatus( s.getResource() );
        }
        return null;
    }

    /**
     * Set the status of the item
     */
    public Resource setStatus(Status s) {
        if (s != null) {
            if (!getStatus().legalNextState(s)) {
                return null;
            }
            return setStatus( s.getResource() );
        }
        return null;
    }

    private Resource setStatus(Resource status) {
        root.removeAll(RegistryVocab.status);
        root.addProperty(RegistryVocab.status, status);
        return status;
    }

    /**
     * If the entity is a bNode then skolemize it
     */
    public void skolemize() {
        if (entity != null && entity.isAnon()) {
            String skolemURI = Registry.get().baseURI + "/.well-known/skolem/" + entity.getId();
            ResourceUtils.renameResource(entity, skolemURI);
            entity = entity.getModel().createResource( skolemURI );
        }
    }

    /**
     * Flatten versioning information for the register item and, if present, the entity
     */
    public RegisterItem flatten() {
        doFlatten(root);
        if (entity != null) {
            doFlatten(entity);
        }
        return this;
    }

    // -----------  internal helpers for sorting out the different notation cases ---------------------------

    private void relocate() {
        String uri = makeItemURI(parentURI, getNotation());
        if ( ! uri.equals(root.getURI()) ) {
            ResourceUtils.renameResource(root, uri);
        }
        root = root.getModel().createResource(uri);
    }

    private void relocateEntity(Resource srcEntity) {
        String uri = entityURI(srcEntity);
        if ( ! uri.equals(srcEntity.getURI()) ) {
            ResourceUtils.renameResource(srcEntity, uri);
            entity = srcEntity.getModel().createResource(uri);
        } else {
            entity = srcEntity;
        }
    }

    /**
     * Update a register item to reflect the current state of the entity
     */
    public void updateForEntity(boolean isNewSubmission, Calendar time) {
        if (isNewSubmission) {
            root.removeAll(DCTerms.dateSubmitted);
            root.addProperty(DCTerms.dateSubmitted, root.getModel().createTypedLiteral(time));
        } else {
            root.removeAll(DCTerms.modified);
            root.addProperty(DCTerms.modified, root.getModel().createTypedLiteral(time));
        }
        if ( !root.hasProperty(RegistryVocab.status) && isNewSubmission) {
            root.addProperty(RegistryVocab.status, RegistryVocab.statusSubmitted);
        }
        if (!root.hasProperty(RegistryVocab.notation)) {
            root.addProperty(RegistryVocab.notation, notation);
        }

        root.removeAll(RDFS.label)
            .removeAll(DCTerms.description)
            .removeAll(RegistryVocab.itemClass);
        for (Property labelProp : RDFUtil.labelProps) {
            if (entity.hasProperty(labelProp)) {
                RDFUtil.copyProperty(entity, root, labelProp, RDFS.label);
                break;
            }
        }
        RDFUtil.copyProperty(entity, root, DCTerms.description);
        for (Statement s : entity.listProperties(RDF.type).toList()) {
            root.addProperty(RegistryVocab.itemClass, s.getObject());
        }
        // Omit entity reference link itself, this will be created per-version when added to store
    }

    public String getEntityRefURI() {
        return root.getURI() + "#entityref";
    }

    private void determineLocation() {
        notation = getExplicitNotation(root);

        // Presumably a item under construction from a web request
        if (notation == null && root.isURIResource()) {
            String uri = root.getURI();
            if (parentURI != null && uri.startsWith(parentURI)) {
                notation = validateNotation( uri.substring(parentURI.length() + 1) );
            } else {
                // Register Item has explicit URI which is not relative to the target parent register
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
        }
        if (notation == null) {
            notation = UUID.randomUUID().toString();
        } else if ( ! LEGAL_NOTATION.matcher(notation).matches() ) {
            throw new WebApiException(Response.Status.BAD_REQUEST, "Proposed notation for item is not a legal pchar or starts with '_' - " + notation);
        }
    }

//    static final Pattern LEGAL_NOTATION = Pattern.compile("^[a-zA-Z0-9][\\w\\.\\-~%@=!&'()*+,;=]*$");
    static final Pattern LEGAL_NOTATION = Pattern.compile("^[a-zA-Z0-9\\.\\-~%@=!&'()*+,;=][\\w\\.\\-~%@=!&'()*+,;=]*$");

    private static String getExplicitNotation(Resource root) {
        if (root.hasProperty(RegistryVocab.notation)) {
            String location = RDFUtil.getStringValue(root, RegistryVocab.notation);
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

    public static String notationFromEntity(Resource entity, String parentURI) {
        String uri = entity.getURI();
        String notation = null;
        if (entity.isAnon()) {
            notation = UUID.randomUUID().toString();
        } else if (uri.startsWith(parentURI)) {
            notation = uri.substring(parentURI.length() + 1);
        } else {
            // Absolute URI, just use it and create anotation for the item
            notation = UUID.randomUUID().toString();
        }
        if ( ! LEGAL_NOTATION.matcher(notation).matches() ) {
            throw new WebApiException(Response.Status.BAD_REQUEST, "Proposed notation for item is not a legal pchar or starts with '_' - " + notation);
        }
        return notation;
    }

    private String entityURI(Resource entity) {
        String uri = entity.getURI();
        if (entity.isAnon()) {
            uri = makeEntityURI();
        } else if (uri.startsWith(parentURI)) {
            String eNotation = uri.substring(parentURI.length() + 1);
            if (!eNotation.equals(notation)) {
                throw new WebApiException(Response.Status.BAD_REQUEST, "Entity path doesn't match its notation");
            }
            uri = makeEntityURI();
        }
        return uri;
    }

    private String makeEntityURI() {
        return makeEntityURI(parentURI, notation);
    }

    private static String makeEntityURI(String parentURI, String notation) {
        return baseParentURI(parentURI) + "/" + notation;
    }

    private static String makeItemURI(String parentURI, String notation) {
        return baseParentURI(parentURI) + "/_" + notation;
    }

    private static String baseParentURI(String parentURI){
        if (parentURI.endsWith("/")) {
            return parentURI.substring(0, parentURI.length() - 1);
        } else {
            return parentURI;
        }
    }

    private static Resource findRequiredEntity(Resource ri) {
        Resource definition = ri.getPropertyResourceValue(RegistryVocab.definition);
        if (definition != null) {
            Resource entity = definition.getPropertyResourceValue(RegistryVocab.entity);
            return entity;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("RegisterItem %s (entity=%s)", root, entity);
    }
}
