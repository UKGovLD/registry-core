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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.store.EntityInfo;
import com.epimorphics.registry.vocab.Ldbp;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.server.webapi.WebApiException;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.NameUtils;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.util.Closure;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.sun.jersey.api.NotFoundException;

/**
 * Command processor to handle registering a new entry.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class CommandRegister extends Command {
    static final Logger log = LoggerFactory.getLogger( CommandRegister.class );

    static final String BULK_TYPES_REGISTER = "/system/bulkCollectionTypes";

    Status statusOverride = null;

    public CommandRegister(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super(operation, target, parameters, registry);
    }

    @Override
    public Response doExecute() {

        statusOverride = Status.forString(parameters.getFirst(Parameters.STATUS), null);

        store.lock(target);
        try {
            Description d = store.getCurrentVersion(target);
            if (d == null) {
                throw new NotFoundException();
            }
            if (!(d instanceof Register)) {
                throw new WebApiException(BAD_REQUEST, "Can only register items in a register");
            }
            Register parent = d.asRegister();

            Resource location = null;
            if (parameters.containsKey(Parameters.BATCH_MANAGED) || parameters.containsKey(Parameters.BATCH_REFERENCED)) {
                location = batchRegister(parent);
            } else {
                if (payload.contains(null, RDF.type, RegistryVocab.RegisterItem)) {
                    for (ResIterator ri = payload.listSubjectsWithProperty(RDF.type, RegistryVocab.RegisterItem); ri.hasNext();) {
                        Resource itemSpec = ri.next();
                        location = register(parent, itemSpec, true);
                    }
                } else {
                    location = register(parent, findSingletonRoot(), false);
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

//        // Relocate any relative children
//        if (root.getURI().startsWith(BaseEndpoint.DUMMY_BASE_URI)) {
//            int striplen = root.getURI().length();
//            for (int i = 0; i < children.size(); i++) {
//                Resource c = children.get(i);
//                if (c.getURI().startsWith(BaseEndpoint.DUMMY_BASE_URI)) {
//                    String uri = BaseEndpoint.DUMMY_BASE_URI + c.getURI().substring(striplen);
//                    children.set(i, ResourceUtils.renameResource(c, uri));
//                }
//            }
//        }

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
        Resource item = register(parent, root, false);
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
                register(newReg, ei, true);
            } else {
                register(newReg, entity, false);
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

    private Resource register(Register parent, Resource itemSpec, boolean withItemSpec) {
        String parentURI = NameUtils.stripLastSlash( parent.getRoot().getURI() );
                // String stripLastSlash needed to cope with the out-of-pattern URI for the root register
        RegisterItem ri = null;
        if ( withItemSpec ) {
            ri = RegisterItem.fromRIRequest(itemSpec, parentURI, true);
        } else {
            ri = RegisterItem.fromEntityRequest(itemSpec, parentURI, true);
        }

        if (store.getDescription(ri.getRoot().getURI()) != null) {
            // Item already exists
            throw new WebApiException(Response.Status.FORBIDDEN, "Item already registered at request location: " + ri.getRoot());
        }

        // TODO validate completeness of description

        if (statusOverride != null) {
            ri.getRoot().removeAll(RegistryVocab.status);
            ri.getRoot().addProperty(RegistryVocab.status, statusOverride.getResource());
        }
        
        // Santization
        for (Property p : RegisterItem.INTERNAL_PROPS) {
            ri.getRoot().removeAll(p);
        }

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
        return ri.getRoot();
    }

}
