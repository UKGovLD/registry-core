/******************************************************************
 * File:        Backup.java
 * Created by:  Dave Reynolds
 * Created on:  31 Mar 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.epimorphics.registry.core.Registry;

@Path("/system/backup")
public class Backup {

    @POST
    public Response startBackup() throws URISyntaxException {
        Registry.get().getBackupService().scheduleBackup();
        URI redirect = new URI("/ui/backups");
        return Response.seeOther(redirect).build();
    }
    
    @GET
    @Produces("text/plain")
    public String getBackupStatus() {
        return Registry.get().getBackupService().getStatus();
    }
}
