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

import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.core.ValidationResponse;
import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.security.RegAction;
import com.epimorphics.registry.security.RegPermission;
import com.epimorphics.registry.store.RegisterEntryInfo;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import javax.ws.rs.NotFoundException;


public class CommandStatusUpdate extends Command {
    public static final String STATUS_PARAM = "status";

    protected boolean isForce = false;
    protected String requestedStatus;

    @Override
    public ValidationResponse validate() {
        isForce = parameters.containsKey(Parameters.FORCE);
        
        requestedStatus = getRequestedStatus();
        if (requestedStatus == null) {
            return new ValidationResponse(BAD_REQUEST,  "Could not determine status to update to");
        }
        Status s = Status.forString(requestedStatus, null);
        if (s == null) {
            return new ValidationResponse(BAD_REQUEST, "Did not recognize status");
        }
        if ( s.equals(Status.Superseded) ) {
            if ( ! parameters.containsKey(Parameters.SUCCESSOR) || parameters.getFirst(Parameters.SUCCESSOR).isEmpty() ) {
                return new ValidationResponse(BAD_REQUEST,  "Must specify successor when superseding an item");
            }
        }
        return ValidationResponse.OK;
    }

    @Override
    public RegPermission permissionRequired() {
        RegPermission required = super.permissionRequired();
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
        RegisterItem ri = store.getItem(itemURI(), false);
        if (ri != null) {
            String requestedStatus = getRequestedStatus();
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
    }

    private void doStatusUpdate(RegisterItem ri, String requestedStatus) {
        // TODO handle verification for accepted
        Status previous = ri.getStatus();
        Resource status = parameters.containsKey(Parameters.FORCE) ? ri.forceStatus(requestedStatus): ri.setStatus(requestedStatus);
        if (status == null) {
            logResponse("Rejecting status update");
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (!previous.isA(Status.Valid) && Status.forResource(status).isA(Status.Valid)) {
            RDFUtil.timestamp(ri.getRoot(), DCTerms.dateAccepted);
        }
        if ( parameters.containsKey(Parameters.SUCCESSOR) ) {
            setSuccessor(ri, parameters.getFirst(Parameters.SUCCESSOR) );
        }
        store.update(ri, false);
        checkDelegation(ri);
    }
        
}
