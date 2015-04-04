/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epimorphics.registry.commands;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.Response;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.message.Message;

public class CommandRealDelete extends Command {

    @Override
    public Response doExecute() {
        store.lock();
        try {
            store.delete(target);
            store.commit();
            notify( new Message(this) );
            return Response.noContent().location(new URI("/")).build();
        } catch (URISyntaxException e) {
            return Response.noContent().build();
        } finally {
            store.end();
        }
    }
}
