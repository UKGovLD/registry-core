/******************************************************************
 * (c) Copyright 2015, Epimorphics Limited
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

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.ws.rs.core.Response;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.StreamRDF;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.security.RegAction;
import com.epimorphics.registry.security.RegPermission;

public class CommandImport extends Command {
    
    /**
     * Returns the permissions that will be required to authorize this
     * operation or null if no permissions are needed.
     */
    public RegPermission permissionRequired() {
        RegPermission permission = new RegPermission(RegAction.Update, "/" + path);
        permission.addAction( RegAction.Register );
        permission.addAction( RegAction.StatusUpdate );
        permission.addAction( RegAction.RealDelete );
        return permission;
    }

    @Override
    public Response doExecute() {
        try {
            StreamRDF stream = store.importTree(target);
            RDFParser.create().source(payloadStream).lang(Lang.NQUADS).parse(stream);
            store.commit();
            
            return Response.noContent().location(new URI(path)).build();
        } catch (URISyntaxException e) {
            return Response.noContent().build();
        }
    }

}
