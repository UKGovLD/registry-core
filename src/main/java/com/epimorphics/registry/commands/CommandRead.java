/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import static com.epimorphics.registry.webapi.Parameters.COLLECTION_METADATA_ONLY;
import static com.epimorphics.registry.webapi.Parameters.ENTITY_LOOKUP;
import static com.epimorphics.registry.webapi.Parameters.STATUS;
import static com.epimorphics.registry.webapi.Parameters.VERSION_AT;
import static com.epimorphics.registry.webapi.Parameters.VERSION_LIST;
import static com.epimorphics.registry.webapi.Parameters.VIEW;
import static com.epimorphics.registry.webapi.Parameters.WITH_METADATA;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.DelegationRecord;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.store.EntityInfo;
import com.epimorphics.registry.store.VersionInfo;
import com.epimorphics.registry.util.Util;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.vocab.Version;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.server.webapi.WebApiException;
import com.epimorphics.vocabs.API;
import com.epimorphics.vocabs.Time;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.sun.jersey.api.NotFoundException;


public class CommandRead extends Command {

    boolean withMetadata;
    boolean versionList;
    boolean versioned;
    boolean timestamped;
    boolean entityLookup;
    boolean tagRetieval;

    public CommandRead(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super(operation, target, parameters, registry);
        withMetadata = hasParamValue(VIEW, WITH_METADATA);
        versionList = hasParamValue(VIEW, VERSION_LIST);
        versioned = lastSegment.contains(":");
        timestamped = parameters.containsKey(VERSION_AT);
        entityLookup = parameters.containsKey(ENTITY_LOOKUP);
        tagRetieval = parameters.containsKey(Parameters.TAG);
    }

    @Override
    public Response doExecute() {
        if (tagRetieval) {
            String uri = target + "?tag=" + parameters.getFirst(Parameters.TAG);
            Model m = store.getGraph(uri);
            return returnModel(m, uri);
        }
        if (entityLookup) {
            return entityLookup();
        }
        Description d = null;
        boolean entityWithMetadata = false;
        boolean graphEntity = false;
        try {
            if (lastSegment.startsWith("_")) {
                // An item
                if (versioned) {
                    // An explicit item version
                    d = store.getVersion(target, true);
                } else {
                    //  plain item
                    d = store.getItem(target, true) ;
                    if (versionList) {
                        injectVersionHistory(d);
                    }
                }
            } else {
                // An entity
                if (versioned) {
                    // This case only arises for registers
                    d = store.getVersion(target, true);
                } else  if ( withMetadata ) {
                    // Entity with metadata
                    entityWithMetadata = true;
                    d = store.getItem(parent +"/_" + lastSegment, true);
                } else {
                    // plain entity
                    d = store.getCurrentVersion(target);
                    if (d == null) {
                        // Check for graph entities
                        d = store.getItem(parent +"/_" + lastSegment, true);
                        graphEntity = true;
                    }
                }
                    
            }
        } catch (Exception e) {
            // Typically an attempt to retrieve the version of something which doesn't not exist
            // Fall through to "not found" case
        }

        if (d == null) {
            throw new NotFoundException();
        }

        Model m = d.getRoot().getModel();
        // Include any entity in the response
        if (d instanceof RegisterItem) {
            RegisterItem ri = d.asRegisterItem();
            if (graphEntity) {
                Resource entityRef = ri.getRoot().getPropertyResourceValue(RegistryVocab.definition);
                StmtIterator si = entityRef.listProperties(RegistryVocab.sourceGraph);
                while (si.hasNext()) {
                    RDFNode graphname = si.next().getObject();
                    if (graphname.isURIResource()) {
                        Model graph = store.getGraph( graphname.asResource().getURI() );
                        if (graph != null) {
                            m.add(graph);
                        }
                    }
                }
            } else if (ri.getEntity() != null) {
                m.add( ri.getEntity().getModel() );
            }
        }
        if (entityWithMetadata) {
            d = Description.descriptionFrom(d.asRegisterItem().getEntity(), store);
        }
        if (!parameters.containsKey(COLLECTION_METADATA_ONLY)) {
            if (d instanceof Register) {
                // add this way round so as not to put members in the cached copy of the register description
                m = registerRead(d.asRegister()).add(m);
            } else if (d instanceof RegisterItem) {
                Resource entity = d.asRegisterItem().getEntity();
                if (entity != null && entity.hasProperty(RDF.type, RegistryVocab.Register)) {
                    m.add( registerRead( Description.descriptionFrom(entity, store).asRegister() ) );
                }
            }
        }

        if (d == null) {
            throw new WebApiException(Response.Status.INTERNAL_SERVER_ERROR, "Failed to reconstruct description, possible damaged repository");
        }

        return returnModel(m, d.getRoot().getURI() );
    }

    private void injectVersionHistory(Description d) {
        Model m = d.getRoot().getModel();
        for (VersionInfo vi : store.listVersions(target)) {
            Resource interval = m.createResource();
            addTimestamp(interval, Time.hasBeginning, vi.getFromTime());
            addTimestamp(interval, Time.hasEnd, vi.getToTime());

            Resource ver = m.createResource( vi.getUri() )
                .addProperty(DCTerms.isVersionOf, d.getRoot())
                .addProperty(RDF.type, RegistryVocab.RegisterItem)
                .addProperty(RDF.type, Version.Version)
                .addProperty(Version.interval, interval);
            if (vi.getReplaces() != null) {
                ver.addProperty(DCTerms.replaces, m.createResource(vi.getReplaces()));
            }
            if (vi.getToTime() == -1) {
                m.add(d.getRoot(), Version.currentVersion, ver);
            }
        }
    }

    private void addTimestamp(Resource interval, Property p, long time) {
        if (time != -1) {
            Resource mark = interval.getModel().createResource()
                    .addProperty(Time.inXSDDateTime, RDFUtil.fromDateTime(time));
            interval.addProperty(p, mark);
        }
    }

    private Response entityLookup() {
        Model result = ModelFactory.createDefaultModel();
        String uri = parameters.getFirst(ENTITY_LOOKUP);
        Status statusFilter = Status.forString(parameters.getFirst(STATUS), Status.Any);
        for (EntityInfo entityInfo : store.listEntityOccurences(uri)) {
            if (entityInfo.getStatus().isA(statusFilter)) {
                if (entityInfo.getRegisterURI().startsWith(target)) {
                    if (withMetadata) {
                        RegisterItem ri = store.getItem(entityInfo.getItemURI(), true);
                        result.add( ri.getRoot().getModel() );
                        result.add( ri.getEntity().getModel() );
                    } else {
                        Description d = store.getCurrentVersion(entityInfo.getEntityURI());
                        result.add(d.getRoot().getModel());
                    }
                }
            }
        }

        // Check for contained delegation points
        Resource entity = ResourceFactory.createResource(uri);
        for (DelegationRecord delegation : registry.getForwarder().listDelegations(path)) {
            Model member = delegation.describeMember(entity);
            if (member != null) {
                result.add( member );
                if (withMetadata && member.contains(entity, RDF.type, (RDFNode)null)) {
                    // Add pseudo RegisterItem so we know which register it was in
                    Resource entityRef = result.createResource().addProperty(RegistryVocab.entity, entity);
                    Resource registerRef = result.createResource(delegation.getLocation())
                            .addProperty(RDF.type, RegistryVocab.Register)
                            .addProperty(RDF.type, RegistryVocab.DelegatedRegister);
                    result.createResource()
                        .addProperty(RegistryVocab.definition, entityRef)
                        .addProperty(RegistryVocab.register, registerRef);
                }
            }
        }
        return returnModel(result, target);
    }

    Model registerRead(Register register) {
        if (parameters.containsKey(COLLECTION_METADATA_ONLY)) {
            return register.getRoot().getModel();
        } else {
            Model view = ModelFactory.createDefaultModel();
            List<Resource> members = (paged) ? new ArrayList<Resource>(length) : null;
            if (!paged && withMetadata) {
                members = new ArrayList<Resource>();
            }
            boolean complete = false;

            if (delegation != null && delegation instanceof DelegationRecord) {
                register.constructDelegatedView(view, (DelegationRecord) delegation, pagenum * length, length, members);
                if (length == -1 || members.size() < length) {
                    complete = true;
                }
                if (withMetadata) {
                    // No item information so add a consistent way to enumerate members
                    for (Resource member : members) {
                        member.addProperty(RDFS.member, register.getRoot());
                    }
                }

            } else {

                // Status filter option
                Status status = Status.forString( parameters.getFirst(STATUS), Status.Accepted );

                // Select version of view
                long timestamp = -1;
                if (versioned) {
                    timestamp = store.versionStartedAt(target);
                } else {
                    timestamp = Util.asTimestamp( parameters.getFirst(VERSION_AT) );
                }
                complete = register.constructView(view, withMetadata, status, pagenum * length, length, timestamp, members);
            }

            // Paging parameters
            if (paged) {
                Resource page = injectPagingInformation(view, register.getRoot(), !complete);
                page.addProperty(API.items, view.createList(members.iterator()));
            }

            return view;
        }
    }

}
