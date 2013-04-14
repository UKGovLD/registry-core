/******************************************************************
 * File:        CommandSearch.java
 * Created by:  Dave Reynolds
 * Created on:  8 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import static com.epimorphics.registry.webapi.Parameters.FIRST_PAGE;
import static com.epimorphics.registry.webapi.Parameters.PAGE_NUMBER;
import static com.epimorphics.registry.webapi.Parameters.VIEW;
import static com.epimorphics.registry.webapi.Parameters.WITH_METADATA;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.server.indexers.LuceneResult;
import com.epimorphics.server.webapi.WebApiException;
import com.epimorphics.vocabs.API;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class CommandSearch extends Command {
    static int MAX_LEN = 1000;

    boolean withMetadata;

    public CommandSearch(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super(operation, target, parameters, registry);
        if (length == -1) {
            length = MAX_LEN;
        }
        withMetadata = hasParamValue(VIEW, WITH_METADATA);
        if (!paged) {
            parameters.put(FIRST_PAGE, new ArrayList<String>());
            paged = true;
            length = registry.getPageSize();
        }
    }

    @Override
    public Response doExecute() {
        String query = parameters.getFirst(Parameters.QUERY);
        LuceneResult[] hits = store.search(query, length * pagenum, length, extractSearchSpec());
        List<String> uris = new ArrayList<String>(hits.length);
        for (LuceneResult hit : hits) {
            uris.add( hit.getURI() );
        }
        Model result = ModelFactory.createDefaultModel();
        String resultURI = target + "?" + makeParamString(parameters, FIRST_PAGE, PAGE_NUMBER);
        Resource root = result.createResource( resultURI );
        RDFNode[] members = new RDFNode[hits.length];
        int i = 0;
        for (String uri: uris) {
            RegisterItem ri = store.getItem(uri, true);
            Resource entity = ri.getEntity();
            if (entity == null) {
                throw new WebApiException(Status.INTERNAL_SERVER_ERROR, "No entity found for search result " + ri.getRoot());
            }
            result.add( entity.getModel() );
            if (withMetadata) {
                result.add( ri.getRoot().getModel() );
            }
            root.addProperty(RDFS.member, entity);
            members[i++] = entity;
        }
        Resource page = injectPagingInformation(result, root, hits.length == length);
        page.addProperty(API.items, result.createList(members));

        return returnModel(result, resultURI);
    }

    protected String[] extractSearchSpec() {
        List<String> searchkeys = new ArrayList<String>();
        List<String> searchvalues = new ArrayList<String>();
        for (String key : parameters.keySet()) {
            if (key.equals(Parameters.QUERY) || key.startsWith("_")) continue;
            String value = parameters.getFirst(key);
            if (value != null && !value.isEmpty()) {
                searchkeys.add(key);
                searchvalues.add(value);
            }
        }
        String[] searchSpec = new String[ searchkeys.size() * 2];
        for (int i = 0; i < searchkeys.size(); i++) {
            searchSpec[i] = searchkeys.get(i);
            searchSpec[i+1] = searchvalues.get(i);
        }
        return searchSpec;
    }

}
