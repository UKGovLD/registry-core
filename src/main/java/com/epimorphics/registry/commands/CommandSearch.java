/******************************************************************
 * File:        CommandSearch.java
 * Created by:  Dave Reynolds
 * Created on:  8 Feb 2013
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

import static com.epimorphics.registry.webapi.Parameters.FIRST_PAGE;
import static com.epimorphics.registry.webapi.Parameters.PAGE_NUMBER;
import static com.epimorphics.registry.webapi.Parameters.VIEW;
import static com.epimorphics.registry.webapi.Parameters.WITH_METADATA;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.store.SearchRequest;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.vocabs.API;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class CommandSearch extends Command {
    static int MAX_LEN = 1000;

    boolean withMetadata;

    public void init(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super.init(operation, target, parameters, registry);
        if (length == -1) {
            length = MAX_LEN;
        }
        withMetadata = hasParamValue(VIEW, WITH_METADATA);
        if (!paged) {
            parameters.put(FIRST_PAGE, new ArrayList<String>());
            paged = true;
            length = MAX_LEN;
        }
    }

    @Override
    public Response doExecute() {
        List<String> uris = store.search( extractSearchSpec() );
        Model result = ModelFactory.createDefaultModel();
        String resultURI = target + "?" + makeParamString(parameters, FIRST_PAGE, PAGE_NUMBER);
        Resource root = result.createResource( resultURI );
        RDFNode[] members = new RDFNode[uris.size()];
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
        Resource page = injectPagingInformation(result, root, uris.size() == length);
        page.addProperty(API.items, result.createList(members));

        return returnModel(result, resultURI);
    }

    protected SearchRequest extractSearchSpec() {
        SearchRequest request = new SearchRequest( parameters.getFirst(Parameters.QUERY) );
        request.setLimit(length).setOffset(pagenum * length);
        for (String key : parameters.keySet()) {
            if (key.equals(Parameters.QUERY) || key.startsWith("_")) continue;
            String value = parameters.getFirst(key);
            if (value != null && !value.isEmpty()) {
                if (key.equals(Parameters.STATUS)) {
                    request.setStatus(value);
                } else {
                    request.addFilter( Prefixes.getDefault().expandPrefix(key), value);
                }
            }
        }
        return request;
    }

}
