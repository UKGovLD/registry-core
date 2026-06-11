/******************************************************************
 * File:        CommandGraphRegister.java
 * Created by:  Dave Reynolds
 * Created on:  28 Apr 2013
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

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.ValidationResponse;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * Support registration of a complete graph as a managed entity.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class CommandGraphRegister extends CommandRegister {

    Resource root;

    @Override
    public ValidationResponse validate() {
        root = payload.getResource(target);

        // Check the parent register exists
        Description d = store.getCurrentVersion(parent);
        if (d == null) {
            return new ValidationResponse(NOT_FOUND, "No such register");
        }
        if (!(d instanceof Register)) {
            return new ValidationResponse(BAD_REQUEST, "Can only register items in a register");
        }
        parentRegister= d.asRegister();

        // Must register via entity
        if (lastSegment.startsWith("_")) {
            return new ValidationResponse(Status.BAD_REQUEST, "Can only store graphs against a managed entity, not the item metadata");
        }

        // Verify that the graph payload does at least contain a minimum description of the target
        if (!root.hasProperty(RDFS.label)) {
            return new ValidationResponse(Status.BAD_REQUEST, "Entity description must include an rdfs:label");
        }
        if (!root.hasProperty(RDF.type)) {
            return new ValidationResponse(Status.BAD_REQUEST, "Entity description must include a type");
        }

        return ValidationResponse.OK;
    }

    @Override
    public Response doExecute() {
        try {
            // Check if this entity is already registered
            RegisterItem item = store.getItem(parent +"/_" + lastSegment, false);

            if (item != null) {
                // This is actually an update
                item.setEntity(root);
                item.setAsGraph(true);
                item.updateForEntity(false, Calendar.getInstance());
                String versionURI = store.update(item, true);
                store.commit();
                return Response.noContent().location(new URI(versionURI)).build();

            } else {
                Resource location = register(parentRegister, root, false, true);
                store.commit();
                return Response.created(new URI(location.getURI())).build();

            }
        } catch (URISyntaxException e) {
            throw new WebApplicationException(e);
        }
    }

}
