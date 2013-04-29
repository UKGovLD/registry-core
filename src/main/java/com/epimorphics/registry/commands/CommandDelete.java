/******************************************************************
 * File:        CommandRead.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import javax.ws.rs.core.Response;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.store.RegisterEntryInfo;
import com.sun.jersey.api.NotFoundException;


public class CommandDelete extends Command {

    @Override
    public Response doExecute() {
        store.lock(target);
        try {
            RegisterItem ri = store.getItem(itemURI(), false);
            if (ri != null) {
                if (ri.isRegister()) {
                    Register register = ri.getAsRegister(store);
                    for (RegisterEntryInfo entry : store.listMembers(register)) {
                        RegisterItem i = store.getItem(entry.getItemURI(), false);
                        doDelete(i);
                    }
                    doDelete(ri);
                } else {
                    doDelete(ri);
                }
                return Response.noContent().build();
            } else {
                throw new NotFoundException();
            }
        } finally {
            store.unlock(target);
        }
    }

    private void doDelete(RegisterItem ri) {
        ri.setStatus(Status.Invalid);
        store.update(ri, false);
    }
}
