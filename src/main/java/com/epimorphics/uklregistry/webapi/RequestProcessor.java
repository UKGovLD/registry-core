/******************************************************************
 * File:        RequestProcessor.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.webapi;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.epimorphics.uklregistry.core.Command;
import com.epimorphics.uklregistry.core.Command.Operation;
import com.epimorphics.uklregistry.core.Command.TargetType;

/**
 * Filter all requests as possible register API requests.
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RequestProcessor implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest hrequest = (HttpServletRequest) request;
            HttpServletResponse hresponse = (HttpServletResponse) response;
        
            Command command = determineCommand(hrequest);

            // TODO authenticate command
            
            // TODO process command,  if successful then done otherwise chain
            System.out.println("Command is: " + command);

        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
    
    public static Command determineCommand(HttpServletRequest request) {
        String target = request.getRequestURI();
        String method = request.getMethod();
        Command.Operation operation = null;
        Command.TargetType targetType = TargetType.REGISTER;
        if (method.equalsIgnoreCase("GET")) {
            operation = Operation.Read;
        } else if (method.equalsIgnoreCase("PUT") || (method.equalsIgnoreCase("PATCH"))) {
            operation = Operation.Update;
        } else if (method.equalsIgnoreCase("DELETE")) {
            operation = Operation.Delete;
        } else if (method.equalsIgnoreCase("POST")) {
            if (request.getParameter(Parameters.VALIDATE) != null) {
                operation = Operation.Validate;
            } else if (request.getParameter(Parameters.STATUS_UPDATE) != null) {
                operation = Operation.Update;
                targetType = TargetType.STATUS;
            } else {
                operation = Operation.Register;
            }
        }
        return new Command(operation, targetType, target, request.getParameterMap());
    }

}
