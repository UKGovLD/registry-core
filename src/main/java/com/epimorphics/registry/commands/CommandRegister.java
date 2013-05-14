/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.core.ValidationResponse;
import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.security.RegAction;
import com.epimorphics.registry.security.RegPermission;
import com.epimorphics.registry.security.UserInfo;
import com.epimorphics.registry.store.EntityInfo;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.Ldbp;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.server.webapi.WebApiException;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.NameUtils;
import com.epimorphics.util.PrefixUtils;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Command processor to handle registering a new entry.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class CommandRegister extends Command {
    static final Logger log = LoggerFactory.getLogger( CommandRegister.class );

    static final String BULK_TYPES_REGISTER = "/system/bulkCollectionTypes";

    Status statusOverride = null;
    boolean needStatusPermission = false;

    Register parentRegister;

    @Override
    public ValidationResponse validate() {
        boolean isBatch = parameters.containsKey(Parameters.BATCH_MANAGED) || parameters.containsKey(Parameters.BATCH_REFERENCED);
        store.lock(target);
        try {
            Description d = store.getCurrentVersion(target);
            if (d == null) {
                return new ValidationResponse(NOT_FOUND, "No such register");
            }
            if (!(d instanceof Register)) {
                return new ValidationResponse(BAD_REQUEST, "Can only register items in a register");
            }
            parentRegister = d.asRegister();
        } finally {
            store.unlock(target);
        }

        for (Resource validationQuery : RDFUtil.allResourceValues(parentRegister.getRoot(), RegistryVocab.validationQuery)) {
            String query = RDFUtil.getStringValue(validationQuery, RegistryVocab.query);
            String expquery = PrefixUtils.expandQuery(query, Prefixes.get());
            QueryExecution qexec = QueryExecutionFactory.create(expquery, payload);
            try {
                if (qexec.execAsk() == true) {
                    String querymsg = RDFUtil.getStringValue(validationQuery, RDFS.label, query);
                    return new ValidationResponse(BAD_REQUEST, "Validation query found error in request: " + querymsg );
                }
            } finally {
                qexec.close();
            }
        }

        statusOverride = Status.forString(parameters.getFirst(Parameters.STATUS), null);

        needStatusPermission = statusOverride != null;

        for (ResIterator ri = payload.listSubjectsWithProperty(RDF.type, RegistryVocab.RegisterItem); ri.hasNext();) {
            Resource itemSpec = ri.next();
            StmtIterator i = itemSpec.listProperties(RegistryVocab.status);
            while (i.hasNext()) {
                RDFNode status = i.next().getObject();
                if (status != RegistryVocab.statusSubmitted) {
                    needStatusPermission = true;
                }
                if (!status.isResource()) {
                    return new ValidationResponse(BAD_REQUEST, "reg:status value which is not a resource " + status);
                }
            }

            if (!isBatch) {    // Validation of batch entries has to be relative to the batch parent so defer till then
                String parentURI = NameUtils.stripLastSlash( parentRegister.getRoot().getURI() );
                // String stripLastSlash needed to cope with the out-of-pattern URI for the root register
                RegisterItem item = RegisterItem.fromRIRequest(itemSpec, parentURI, true);
                if (store.getDescription(item.getRoot().getURI()) != null) {
                    return new ValidationResponse(Response.Status.FORBIDDEN, "Item already registered at request location: " + item.getRoot());
                }

                ValidationResponse entityValid = validateEntity(parentRegister, item.getEntity() );
                if (!entityValid.isOk()) {
                    return entityValid;
                }
            }
        }

        return ValidationResponse.OK;
    }

    private ValidationResponse validateEntity(Register parent, Resource entity) {
        if (entity == null) {
            return new ValidationResponse(BAD_REQUEST, "Missing entity");
        }

        if ( !entity.hasProperty(RDF.type) || !entity.hasProperty(RDFS.label) ) {
            return new ValidationResponse(BAD_REQUEST, "Missing required property (rdf:type or rdfs:label) on " + entity);
        }

        List<Resource> itemClasses = RDFUtil.allResourceValues(parent.getRoot(), RegistryVocab.containedItemClass);
        boolean foundLegalType = itemClasses.isEmpty();
        for (Resource itemClass : itemClasses) {
            if (entity.hasProperty(RDF.type, itemClass)) {
                foundLegalType = true;
                break;
            }
        }
        if (!foundLegalType) {
            return new ValidationResponse(BAD_REQUEST, "Entity does not have one of the types required by this register: " + entity);
        }
        return ValidationResponse.OK;
    }

    @Override
    public RegPermission permissionRequried() {
        RegPermission required = super.permissionRequried();
        if (needStatusPermission) {
            required.addAction(RegAction.StatusUpdate);
        }
        return required;
    }

    @Override
    public Response doExecute() {

        store.lock(target);
        try {
            Resource location = null;
            if (parameters.containsKey(Parameters.BATCH_MANAGED) || parameters.containsKey(Parameters.BATCH_REFERENCED)) {
                location = batchRegister(parentRegister);
            } else {
                if (payload.contains(null, RDF.type, RegistryVocab.RegisterItem)) {
                    for (ResIterator ri = payload.listSubjectsWithProperty(RDF.type, RegistryVocab.RegisterItem); ri.hasNext();) {
                        Resource itemSpec = ri.next();
                        location = register(parentRegister, itemSpec, true, false);
                    }
                } else {
                    location = register(parentRegister, findSingletonRoot(), false, false);
                }
            }
            try {
                return Response.created(new URI(location.getURI())).build();
            } catch (URISyntaxException e) {
                throw new EpiException(e);
            }
        } finally {
            store.unlock(target);
        }
    }

    private Resource batchRegister(Register parent) {
        ResultSet types = QueryUtil.selectAll(payload, "SELECT DISTINCT ?type WHERE {[] a ?type}");
        Resource type = null;
        RegisterItem bulkItem = null;
        while (types.hasNext()) {
            type = types.next().getResource("type");
            bulkItem = getBulkType(type);
            if (bulkItem != null) {
                break;
            }
        }
        if (bulkItem == null) {
            throw new WebApiException(BAD_REQUEST, "Could not find registered bulk type in payload");
        }
        List<Resource> roots = payload.listResourcesWithProperty(RDF.type, type).toList();
        for (Iterator<Resource> i = roots.iterator(); i.hasNext();) {
            if (i.next().isAnon()) i.remove();
        }
        if (roots.size() != 1) {
            throw new WebApiException(BAD_REQUEST, "Multiple instances of bulk collection type");
        }
        Resource root = roots.get(0);

        // Find membership property
        boolean isInverse = false;
        Property memberProp = RDFUtil.asProperty( bulkItem.getRoot().getPropertyResourceValue(Ldbp.membershipPredicate) );
        if (memberProp == null) {
            memberProp = RDFUtil.asProperty( bulkItem.getRoot().getPropertyResourceValue(RegistryVocab.inverseMembershipPredicate) );
            if (memberProp == null) {
                memberProp = RDFS.member;
            } else {
                isInverse = true;
            }
        }

        // Find children
        List<Resource> children = new ArrayList<Resource>();
        if (isInverse) {
            StmtIterator si = payload.listStatements(null, memberProp, root);
            while (si.hasNext()) {
                Statement s = si.next();
                children.add( s.getSubject() );
                si.remove();
            }
        } else {
            StmtIterator si = root.listProperties(memberProp);
            while (si.hasNext()) {
                Statement s = si.next();
                children.add( s.getObject().asResource() );
                si.remove();
            }
        }
        if (children.isEmpty()) {
            throw new WebApiException(BAD_REQUEST, "No children of bulk collection type found");
        }

        // Check if there are any suspiciously surplus resources
        Set<Resource> toIngest = new HashSet<Resource>();
        toIngest.add( root );
        toIngest.addAll( children );

        for (ResIterator ri = payload.listSubjectsWithProperty(RDF.type); ri.hasNext();) {
            Resource r = ri.next();
            if (r.isURIResource() && !toIngest.contains(r)) {
                throw new WebApiException(BAD_REQUEST, "Found resources other than batch root and child entities in the payload: " + r);
            }
        }

        // Register the collection
        root.addProperty(RDF.type, RegistryVocab.Register);
        if (!root.hasProperty(RegistryVocab.owner)) {
            if (root.hasProperty(DCTerms.publisher)) {
                root.addProperty(RegistryVocab.owner, root.getProperty(DCTerms.publisher).getObject());
            } else {
                // TODO make it the submitter
            }
        }
        root.addProperty(isInverse ? RegistryVocab.inverseMembershipPredicate : Ldbp.membershipPredicate, memberProp);
        Resource item = register(parent, root, false, false);
        String registerURI = item.getURI().replaceAll("/_([^/]*)$", "/$1");
        Register newReg = store.getCurrentVersion(registerURI).asRegister();

        boolean isReference = parameters.containsKey(Parameters.BATCH_REFERENCED);
        for (Resource child : children) {
            // TODO bypass the register versioning here
            Resource entity = child.inModel(Closure.closure(child, false));
            if (isReference) {
                Model em = entity.getModel();
                Resource ei = em.createResource()
                        .addProperty(RDF.type, RegistryVocab.RegisterItem)
                        .addProperty(RegistryVocab.definition, em.createResource().addProperty(RegistryVocab.entity, entity));
                if (entity.getURI().startsWith(registry.getBaseURI())) {
                    em.add( store.getDescription(entity.getURI()).getRoot().listProperties() );
                }
                register(newReg, ei, true, false);
            } else {
                register(newReg, entity, false, false);
            }
        }

        return item;
    }

    private RegisterItem getBulkType(Resource ty) {
        String bulkRegister = registry.getBaseURI() + BULK_TYPES_REGISTER;
        for (EntityInfo i : store.listEntityOccurences(ty.getURI())) {
            if (i.getRegisterURI().startsWith(bulkRegister)) {
                return store.getItem(i.getItemURI(), true);
            }
        }
        return null;
    }

    protected Resource register(Register parent, Resource itemSpec, boolean withItemSpec, boolean asGraph) {
        String parentURI = NameUtils.stripLastSlash( parent.getRoot().getURI() );
                // String stripLastSlash needed to cope with the out-of-pattern URI for the root register
        RegisterItem ri = null;
        if ( withItemSpec ) {
            ri = RegisterItem.fromRIRequest(itemSpec, parentURI, true);
        } else {
            ri = RegisterItem.fromEntityRequest(itemSpec, parentURI, true);
        }
        ri.setAsGraph(asGraph);

        if (store.getDescription(ri.getRoot().getURI()) != null) {
            // Item already exists
            throw new WebApiException(Response.Status.FORBIDDEN, "Item already registered at request location: " + ri.getRoot());
        }

        ValidationResponse entityValid = validateEntity(parent, ri.getEntity() );
        if (!entityValid.isOk()) {
            throw new WebApiException(entityValid.getStatus(), entityValid.getMessage());
        }
        ri.skolemize();

        if (statusOverride != null) {
            ri.getRoot().removeAll(RegistryVocab.status);
            ri.getRoot().addProperty(RegistryVocab.status, statusOverride.getResource());
        }

        // Santization
        for (Property p : RegisterItem.INTERNAL_PROPS) {
            ri.getRoot().removeAll(p);
        }

        // Submitter
        Model m = ri.getRoot().getModel();
        Resource submitter = m.createResource();
        try {
            UserInfo userinfo = (UserInfo) SecurityUtils.getSubject().getPrincipal();
            submitter
            .addProperty(FOAF.name, userinfo.getName())
            .addProperty(FOAF.openid, m.createResource( userinfo.getOpenid()) );
        } catch (UnavailableSecurityManagerException e) {
            // Occurs during bootstrap
            submitter.addProperty(FOAF.name, "bootstrap");
        }
        ri.getRoot().addProperty(RegistryVocab.submitter, submitter);

        Resource entity = ri.getEntity();
        // Normalization closures
        // TODO factor these out as SPARQL constructs in an external file?
        if( entity.hasProperty(RDF.type, RegistryVocab.Register) ) {
            // TODO fill in void description
            entity.addProperty(RDF.type, Ldbp.Container);
            log.info("Created new sub-register: " + ri.getNotation());
        }
        if (entity.hasProperty(RDF.type, RegistryVocab.FederatedRegister)
                || entity.hasProperty(RDF.type, RegistryVocab.NamespaceForward)
                || entity.hasProperty(RDF.type, RegistryVocab.DelegatedRegister)) {
            entity.addProperty(RDF.type, RegistryVocab.Delegated);
            ri.getRoot().addProperty(RegistryVocab.itemClass, RegistryVocab.Delegated);
            if (entity.hasProperty(RDF.type, RegistryVocab.DelegatedRegister)) {
                entity.addProperty(RDF.type, RegistryVocab.Register);
                ri.getRoot().addProperty(RegistryVocab.itemClass, RegistryVocab.Register);
            }
        }
        store.addToRegister(parent, ri);
        checkDelegation(ri);

        notify( new Message(this, ri) );

        return ri.getRoot();
    }



}
