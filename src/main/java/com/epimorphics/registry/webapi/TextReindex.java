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
package com.epimorphics.registry.webapi;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.registry.security.RegAuthorizationInfo;
import com.epimorphics.registry.store.impl.TDBStore;

/**
 * Support for recreating the text index
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
@Path("system/text-reindex")
public class TextReindex {
    static final Logger log = LoggerFactory.getLogger( TextReindex.class );
    
    @POST
    public Response reindex() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() && subject.hasRole(RegAuthorizationInfo.ADMINSTRATOR_ROLE)) {
            TDBStore store = AppConfig.getApp().getA(TDBStore.class);
            if (store == null) {
                log.warn("Attempted text-reindex but no Lucene index found");
                return Response.status(Status.BAD_REQUEST).entity("No text index found").build();
            } else {
                long indexed = store.textReindex();
                log.info("Indexed {} entries", indexed);
                return Response.ok("Indexed " + indexed + " entries").build();
            }
        } else {
            log.error("Attempted text-reindex by unauthorized user: {}", subject.getPrincipal());
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }

}
