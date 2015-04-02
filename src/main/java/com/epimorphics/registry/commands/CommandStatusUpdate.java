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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.ValidationResponse;
import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.security.RegAction;
import com.epimorphics.registry.security.RegPermission;
import com.epimorphics.registry.store.RegisterEntryInfo;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.appbase.webapi.WebApiException;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.sun.jersey.api.NotFoundException;


public class CommandStatusUpdate extends Command {
    public static final String STATUS_PARAM = "status";

    protected boolean isForce = false;

    @Override
    public ValidationResponse validate() {
        isForce = parameters.containsKey(Parameters.FORCE);
        return ValidationResponse.OK;
    }

    @Override
    public RegPermission permissionRequried() {
        RegPermission required = super.permissionRequried();
        if (isForce) {
            required.addAction(RegAction.Force);
        }
        return required;
    }

    public String getRequestedStatus() {
        return parameters.getFirst(STATUS_PARAM);
    }
    
    @Override
    public Response doExecute() {
        store.lock();
        RegisterItem ri = store.getItem(itemURI(), false);
        try {
            if (ri != null) {
                String requestedStatus = getRequestedStatus();
                if (requestedStatus == null) {
                    throw new WebApiException(BAD_REQUEST, "Could not determine status to update to");
                }
                if (ri.isRegister() && !lastSegment.startsWith("_")) {
                    for (RegisterEntryInfo member : store.listMembers( ri.getAsRegister(store) )) {
                        doStatusUpdate(store.getItem(member.getItemURI(), false), requestedStatus);
                    }
                } else {
                    if (!lastSegment.startsWith("_")) {
                        throw new WebApiException(BAD_REQUEST, "Can only update the status of a register item or whole register");
                    }
                    doStatusUpdate(ri, requestedStatus);
                }
                store.commit();
                
                // Notify event
                Message message = new Message(this);
                message.setMessage( getRequestedStatus() );
                notify(message);
                
                return Response.noContent().build();
            } else {
                throw new NotFoundException();
            }
        } finally {
            store.end();
        }
    }

    private void doStatusUpdate(RegisterItem ri, String requestedStatus) {
        // TODO handle verification for accepted
        Resource status = parameters.containsKey(Parameters.FORCE) ? ri.forceStatus(requestedStatus): ri.setStatus(requestedStatus);
        if (status == null) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (status.equals(RegistryVocab.statusExperimental) || status.equals(RegistryVocab.statusStable)) {
            RDFUtil.timestamp(ri.getRoot(), DCTerms.dateAccepted);
        }
        store.update(ri, false);
        checkDelegation(ri);
    }
        
}
