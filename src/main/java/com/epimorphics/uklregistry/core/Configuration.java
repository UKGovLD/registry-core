/******************************************************************
 * File:        Configuration.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import java.util.Map;

import javax.servlet.ServletContext;

import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.core.ServiceConfig;

/**
 * Encapsulates configuration information for the registry such as base URI.
 * Parameters can be set by the web.xml entry for this service.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Configuration extends ServiceBase implements Service {

    public static final String BASE_URI_PARAM = "baseURI";
    public static final String SERVICE_NAME = "configuration";

    @Override
    public void init(Map<String, String> config, ServletContext context) {
        this.config = config;
    }

    static String baseURI;     // Cache since we look this up a *lot*
    public static String getBaseURI() {
        if (baseURI == null) {
            baseURI = get().getRequiredParam(BASE_URI_PARAM);
        }
        return baseURI;
    }

    public static Configuration get() {
        return (Configuration)ServiceConfig.get().getService(SERVICE_NAME);
    }
}
