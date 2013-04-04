/******************************************************************
 * File:        TestSecurity.java
 * Created by:  Dave Reynolds
 * Created on:  4 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import java.util.Map;

import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.ApacheHttpClientConfig;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;

public class TestSecurity {

    @Test
    public void temp() {
        DefaultApacheHttpClientConfig cc = new DefaultApacheHttpClientConfig();
        cc.
        cc.setProperty(ApacheHttpClientConfig.PROPERTY_HANDLE_COOKIES, true);
        Client c = ApacheHttpClient.create(cc);
        Map<String, Object> props = c.getProperties();
        System.out.println("Size = " + props.size());
        for (String key : props.keySet()) {
            System.out.println(" " + key + " = " + props.get(key));
        }
    }
}
