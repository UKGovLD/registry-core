/******************************************************************
 * File:        TestTop.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.webapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.epimorphics.server.webapi.BaseEndpoint;


@Path("/")
public class TestTop extends BaseEndpoint {

    @GET
    public String get() {
        return "Hello from " + uriInfo.getPath();
    }
}
