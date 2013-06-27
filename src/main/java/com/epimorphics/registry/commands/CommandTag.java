/******************************************************************
 * File:        CommandTag.java
 * Created by:  Dave Reynolds
 * Created on:  22 Apr 2013
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
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.core.ValidationResponse;
import com.epimorphics.registry.store.RegisterEntryInfo;
import com.epimorphics.registry.vocab.Prov;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.server.webapi.WebApiException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class CommandTag extends Command {
    Register register;
    String tag;

    public void init(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super.init(operation, target, parameters, registry);
        tag = parameters.getFirst(Parameters.TAG);
    }


    @Override
    public ValidationResponse validate() {
        Description d = store.getCurrentVersion(target);
        if (d == null) {
            return new ValidationResponse(NOT_FOUND, "No such register");
        }
        if (!(d instanceof Register)) {
            return new ValidationResponse(BAD_REQUEST, "Can only tag registers");
        }
        register = d.asRegister();
        return ValidationResponse.OK;
    }

    @Override
    public Response doExecute() {
        Calendar now;
        Model collectionModel = ModelFactory.createDefaultModel();
        Resource collection = collectionModel.createResource(target + "?tag=" + tag)
                .addProperty(RDF.type, Prov.Collection);
        store.lock(target);
        try {
            now = Calendar.getInstance();
            long regVersion = RDFUtil.getLongValue(register.getRoot(), OWL.versionInfo);
            collection
                    .addProperty(Prov.generatedAtTime, collectionModel.createTypedLiteral(now))
                    .addProperty(Prov.wasDerivedFrom, collectionModel.createResource(target + ":" + regVersion))
                    .addProperty(RegistryVocab.tag, tag);
            Model regContents = ModelFactory.createDefaultModel();
            List<RegisterEntryInfo> members = register.getMembers();
            register.constructView(regContents, true, Status.Accepted, 0, -1, -1, null);
            for (RegisterEntryInfo member : members) {
                Resource item = regContents.getResource(member.getItemURI());
                Long version = RDFUtil.getLongValue(item, OWL.versionInfo);
                if (version != null) {
                    collection.addProperty(Prov.hadMember, collectionModel.createResource(member.getItemURI() + ":" + version));
                }
            }
            store.storeGraph(collection.getURI(), collectionModel);
            register.getRoot().addProperty(RegistryVocab.release, collection);
            store.update(register);
            return Response.created( new URI(collection.getURI()) ).build();
        } catch (URISyntaxException e) {
            throw new WebApiException(Response.Status.INTERNAL_SERVER_ERROR, "Illegal URL generated for tagged collection: " + collection);
        } finally {
            store.unlock(target);
        }
    }


}
