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

import jakarta.ws.rs.core.Response;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.store.RegisterEntryInfo;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;


public class CommandDelete extends Command {

    private final List<String> deletedItems = new ArrayList<>();

    @Override
    public Response doExecute() {
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
            store.commit();

            deletedItems.forEach(item -> {
                notify(new Message(this, item));
            });

            return Response.noContent().build();
        } else {
            throw new NotFoundException();
        }
    }

    private void doDelete(RegisterItem ri) {
        ri.setStatus(Status.Invalid);
        store.update(ri, false);
        deletedItems.add(ri.getRoot().getURI());
    }
}
