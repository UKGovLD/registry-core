/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import java.util.List;


import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.store.EntityInfo;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.server.webapi.WebApiException;


public class CommandValidate extends Command {

    public CommandValidate(Operation operation, String target,
            MultivaluedMap<String, String> parameters, Registry registry) {
        super(operation, target, parameters, registry);
    }

    @Override
    public Response doExecute() {
        StringBuffer msg = new StringBuffer();
        boolean valid = true;
        for (String uri : parameters.get(Parameters.VALIDATE)) {
            uri = uri.trim();
            if (uri.isEmpty()) continue;
            boolean thisValid = false;
            List<EntityInfo> infos = store.listEntityOccurences(uri);
            for (EntityInfo info : infos) {
                if (info.getRegisterURI().startsWith(target) && info.getStatus().isA(Status.Valid)) {
                    thisValid = true;
                    break;
                }
            }
            // TODO validate in delegated registers as well
            if (!thisValid) {
                if (infos.isEmpty()) {
                    msg.append("URI not found anywhere: ");
                } else {
                    msg.append("URI known but not marked as valid within this register subtree: ");
                }
                msg.append(uri);  msg.append("\n");
                valid = false;
            }
        }
        if (valid) {
            return Response.ok().build();
        } else {
            throw new WebApiException(BAD_REQUEST, msg.toString());
        }
    }

}
