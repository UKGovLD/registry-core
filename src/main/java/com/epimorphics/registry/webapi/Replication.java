/******************************************************************
 * File:        Replication.java
 * Created by:  Dave Reynolds
 * Created on:  24 Nov 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import java.io.InputStream;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.message.RequestLogger;

/**
 * Support for replaying logged updates. Useful when replicating from a master
 * to slave registries or reconstructing state from a log of actions.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
@Path("/system/replay")
public class Replication {
    static final Logger log = LoggerFactory.getLogger( Replication.class );
    
    @POST
    public Response replay(@Context HttpHeaders hh, InputStream body) {
        try {
            Subject subject = SecurityUtils.getSubject();
            if ( ! subject.isPermitted("Replication:/")) {   // Can we test against groups?
                log.warn("Reject unauthorized replay request");
                throw new WebApiException(Status.UNAUTHORIZED, "Insufficient permissions for replay");
            }
        } catch (UnavailableSecurityManagerException e) {
            log.warn("Security is not configured, assuming test mode");
        }
        
        RequestLogger requestLogger = Registry.get().getRequestLogger();
        if (requestLogger == null) {
            throw new WebApiException(Status.SERVICE_UNAVAILABLE, "No request log/replay configured for this registry");
        }
        try {
            Command command = requestLogger.getLog(body);
            return command.execute();
        } catch (Exception e) {
            throw new WebApiException(Status.BAD_REQUEST, "Failed to read the log action you sent: " + e);
        }
    }
}
