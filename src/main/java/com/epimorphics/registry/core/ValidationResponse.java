/******************************************************************
 * File:        ValidationResponse.java
 * Created by:  Dave Reynolds
 * Created on:  6 Mar 2013
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
