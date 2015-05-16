/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
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

package com.epimorphics.registry.commands;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.webapi.WebApiException;
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
import com.epimorphics.registry.store.EntityInfo;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.Ldp;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.NameUtils;
import com.epimorphics.util.PrefixUtils;
import com.epimorphics.vocabs.SKOS;
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
    List<RegisterItem> notifications = new ArrayList<>();

    @Override
    public ValidationResponse validate() {
        boolean isBatch = parameters.containsKey(Parameters.BATCH_MANAGED) || parameters.containsKey(Parameters.BATCH_REFERENCED);
        Description d = store.getCurrentVersion(target);
        if (d == null) {
            return new ValidationResponse(NOT_FOUND, "No such register");
        }
        if (!(d instanceof Register)) {
            return new ValidationResponse(BAD_REQUEST, "Can only register items in a register");
        }
        parentRegister = d.asRegister();

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

        boolean foundEntries = false;
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
            foundEntries = true;
        }
        if (!isBatch && !foundEntries) {
            // Check for direct registration of entities
            for (Resource entity : findEntities()) {
                ValidationResponse entityValid = validateEntity(parentRegister, entity );
                if (!entityValid.isOk()) {
                    return entityValid;
                }
                foundEntries = true;
            }
        }

        if (!isBatch && !foundEntries) {
            return new ValidationResponse(BAD_REQUEST, "No items found in request");
        }
        return ValidationResponse.OK;
    }

    @Override
    public RegPermission permissionRequired() {
        RegPermission required = super.permissionRequired();
        if (needStatusPermission) {
            required.addAction(RegAction.StatusUpdate);
        }
        return required;
    }

    @Override
    public Response doExecute() {

        store.lock();
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
                    Collection<Resource> entities = findEntities();
                    if (entities.isEmpty()) {
                        throw new WebApiException(Response.Status.BAD_REQUEST, "No items or entities found");
                    }
                    for (Resource entity : entities) {
                        location = register(parentRegister, entity, false, false);
                    }
                }
            }
            // Update the register itself only after all the items have been registered
            // TODO could have consistent date stamp across these if we want
            store.update(parentRegister);
            store.commit();
            
            for (RegisterItem item : notifications) {
                notify( new Message(this, item) );
            }

            try {
                return Response.created(new URI(location.getURI())).build();
            } catch (URISyntaxException e) {
                throw new EpiException(e);
            }
        } finally {
            store.end();
        }
    }

    private Resource batchRegister(Register parent) {
        ResultSet types = QueryUtil.selectAll(payload, "SELECT DISTINCT ?type WHERE {[] a ?type}");
        Resource type = null;
        RegisterItem bulkItem = null;
        while (types.hasNext()) {
            Resource ty = types.next().getResource("type");
            RegisterItem bt = getBulkType(ty);
            if (bt != null) {
                if (type == null || type.equals(RegistryVocab.Register) ) {
                    // Allow more specific bulk items to take precedence over reg:Register
                    type = ty;
                    bulkItem = bt;
                }
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
        Resource rt = bulkItem.getRoot();
        Property memberProp = Register.getMembershipPredicate( rt );
        if (memberProp == null) {
            memberProp = Register.getInvMembershipPredicate(rt);
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
        root.addProperty(isInverse ? Ldp.isMemberOfRelation : Ldp.hasMemberRelation, memberProp);
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
                if (entity.hasProperty(SKOS.notation)) {
                    ei.addProperty(RegistryVocab.notation, entity.getRequiredProperty(SKOS.notation).getObject());
                }
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

        if (statusOverride != null) {
            ri.getRoot().removeAll(RegistryVocab.status);
            ri.getRoot().addProperty(RegistryVocab.status, statusOverride.getResource());
        }
        
        notifications.add(ri);
        return addToRegister(parent, ri);
    }



}
