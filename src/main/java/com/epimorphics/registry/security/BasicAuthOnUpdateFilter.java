/******************************************************************
 * File:        BasicAuthOnUpdateFilter.java
 * Created by:  Dave Reynolds
 * Created on:  24 Nov 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;

import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;

/**
 * Shiro filter that will allow all get requests through or prompt for basic authentication for any other requests.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class BasicAuthOnUpdateFilter extends BasicHttpAuthenticationFilter {

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest httpRequest = WebUtils.toHttp(request);
        String method = httpRequest.getMethod();
        return  method.equals( HttpMethod.GET ) || method.equals( HttpMethod.HEAD) || super.isAccessAllowed(httpRequest, response, mappedValue);
    }
    
}
