/******************************************************************
 * File:        ValidationResponse.java
 * Created by:  Dave Reynolds
 * Created on:  6 Mar 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import javax.ws.rs.core.Response;

/**
 * Struct used to return the results of validating a request.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ValidationResponse {

    boolean ok = false;
    Response.Status status;
    String message;

    /**
     * Create a failure response
     */
    public ValidationResponse(Response.Status status, String message) {
        this.status = status;
        this.message = message;
        ok = false;
    }

    /**
     * Create a success response
     */
    public ValidationResponse() {
        status = Response.Status.OK;
        ok = true;
    }

    public boolean isOk() {
        return ok;
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public static final ValidationResponse OK = new ValidationResponse();

}
