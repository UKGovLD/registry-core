/******************************************************************
* File:        Login.java
 * Created by:  Dave Reynolds
 * Created on:  1 Apr 2013
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

package com.epimorphics.registry.webapi;

import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.security.*;
import com.epimorphics.appbase.webapi.WebApiException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Path("/system/security")
public class Login {
    static final Logger log = LoggerFactory.getLogger( Login.class );

    public static final String NOCACHE_COOKIE = "nocache";

    // Velocity binding names
    public static final String VN_SUBJECT = "subject";
    public static final String VN_REGISTRATION_STATUS = "registrationStatus";
    public static final String RS_NEW = "new";
    public static final String RS_ALREADY_REGISTERED = "already";
    public static final String RS_LOGIN = "login";

    protected @Context UriInfo uriInfo;
    protected @Context ServletContext servletContext;
    protected @Context HttpServletRequest request;
    protected @Context HttpServletResponse response;

    @Path("/login")
    @POST
    public Response login(@FormParam("provider") String provider, @FormParam("return") String returnURL) {
        new ProcessOpenID(uriInfo, servletContext).processOpenID(request, response, provider, returnURL, false);
        return Response.ok().build();
    }

    @Path("/register")
    @POST
    public Response register(@FormParam("provider") String provider, @FormParam("return") String returnURL) {
        new ProcessOpenID(uriInfo, servletContext).processOpenID(request, response, provider, returnURL, true);
        return Response.ok().build();
    }

    @Path("/loginoa")
    @POST
    public Response loginOauth2(@FormParam("provider") String provider, @FormParam("return") String returnURL) {
        new ProcessOauth2(uriInfo, servletContext).processOpenID(request, response, provider, returnURL, false);
        return Response.ok().build();
    }

    @Path("/registeroa")
    @POST
    public Response registerOauth2(@FormParam("provider") String provider, @FormParam("return") String returnURL) {
        new ProcessOauth2(uriInfo, servletContext).processOpenID(request, response, provider, returnURL, true);
        return Response.ok().build();
    }

    @Path("/logout")
    @POST
    public void logout(@Context HttpServletResponse response) throws IOException {
        // TODO set session attribute as part of Shiro realm
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(VN_SUBJECT);
            log.info("Logout by userid: " + SecurityUtils.getSubject().getPrincipal());
            SecurityUtils.getSubject().logout();
        }
        removeNocache(response);
        String redirect = Registry.get().getBaseURI();
        response.sendRedirect(redirect);
    }

    @Path("/pwlogin")
    @POST
    public Response pwlogin(@FormParam("userid") String userid, @FormParam("password") String password, @FormParam("return") String returnURL) {
        try {
            RegToken token = new RegToken(userid, password);
            Subject subject = SecurityUtils.getSubject();
            subject.login(token);
            log.info("Password login for userid " + userid);
            setNocache(response);
            if (returnURL == null || returnURL.isEmpty()) {
                returnURL = "/ui/admin";
            }
            return redirectTo( returnURL );
        } catch (Exception e) {
            log.warn(String.format("Password login failure for userid %s [%s]: %s", userid, e.getClass().toString(), e.getMessage()));
            return error("Login failed");
        }
    }

    @Path("/pwregister")
    @POST
    public Response pwregister(
            @FormParam("userid") String userid, 
            @FormParam("password") String password, 
            @FormParam("name") String name, 
            @FormParam("return") String returnURL) {
        if (userid == null || userid.isEmpty() || password == null || password.isEmpty() || name == null || name.isEmpty()) {
            return error( "You must supply all of a username, display name and password to register" );
        }
        UserStore userstore = Registry.get().getUserStore();
        UserInfo userinfo = new UserInfo(userid, name);
        if (userstore.register( userinfo )) {
            try {
                RegToken token = new RegToken(userid, true);
                Subject subject = SecurityUtils.getSubject();
                subject.login(token);
                
                userstore.setCredentials(userid, ByteSource.Util.bytes(password), Integer.MAX_VALUE);
                if (returnURL == null || returnURL.isEmpty()) {
                    returnURL = "/ui/admin";
                }
                return redirectTo( returnURL );
            } catch (Exception e) {
                return error("Failed to register the password: " + e);
            }            
        } else {
            return error( "That username is already registered" );
        }
    }

    @Path("/apilogin")
    @POST
    public Response apilogin(@FormParam("userid") String userid, @FormParam("password") String password) {
        try {
            RegToken token = new RegToken(userid, password);
            Subject subject = SecurityUtils.getSubject();
            subject.login(token);
            log.info("API Login for userid " + userid);
            return Response.ok().build();
        } catch (Exception e) {
            log.warn(String.format("API Login failure for userid %s [%s]: %s", userid, e.getClass().toString(), e.getMessage()));
        }
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    @Path("/response")
    @GET
    public Response openIDResponse() {
        return new ProcessOpenID(uriInfo, servletContext).verifyResponse(request, response);
    }

    @Path("/responseoa")
    @GET
    public Response openIDConnectResponse() {
        return new ProcessOauth2(uriInfo, servletContext).verifyResponse(request, response);
    }

    // Return the name of the loggedin user on this session, for test purposes
    @Path("/username")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getUsername() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            return ((UserInfo)subject.getPrincipal()).getName();
        } else {
            throw new WebApiException(Response.Status.UNAUTHORIZED, "No logged in user in this session");
        }
    }

    @Path("/listusers")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response listusers(@QueryParam("query") String query, @QueryParam("grant") String action, @QueryParam("uri") String uri) {
        if (SecurityUtils.getSubject().isPermitted("Grant:"+uri)) {
            List<UserInfo> users = Registry.get().getUserStore().listUsers(query);
            return RequestProcessor.render("user-list.vm", uriInfo, servletContext, request, "grant", action, "uri", uri, "users", users);
        } else {
            return error("You do not have sufficient privileges to grant further access");
        }
    }

    @Path("/grant")
    @POST
    public Response grant(@FormParam("user") String id, @FormParam("grant") String action, @FormParam("path") String inpath) {
        String path = inpath;
        if (path == null || path.isEmpty()) {
            // Occurrs when setting global admin permissions
            path = "/ui/administrators";
        }
        UserStore userstore = Registry.get().getUserStore();
        try {
            if (action.equals("administrator")) {
                userstore.setRole(id, RegAuthorizationInfo.ADMINSTRATOR_ROLE);
            } else {
                userstore.addPermision(id, new RegPermission(action, path));
            }
            return redirectTo(path);
        } catch (Exception e) {
            return error("Permission grant failed: " + e);
        }
    }

    @Path("/ungrant")
    @POST
    public Response ungrant(@FormParam("user") String id, @FormParam("path") String path) {
        UserStore userstore = Registry.get().getUserStore();
        try {
            userstore.removePermission(id, path);
            return redirectTo(path);
        } catch (Exception e) {
            return error("Permission grant failed: " + e);
        }
    }


    @Path("/createpassword")
    @POST
    public Response createpassword(@FormParam("minstolive") String minstolive) {
        int mins = 0;
        try {
            mins = Integer.parseInt(minstolive);
        } catch (Exception e) {
            return error("Minutes to live must be an integer");
        }
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            UserStore userstore = Registry.get().getUserStore();
            try {
                String id = ((UserInfo)subject.getPrincipal()).getOpenid();
                String pwd = userstore.createCredentials(id, mins);
                log.info("Created temporary password for user " + id);
                return RequestProcessor.render("api-key-result.vm", uriInfo, servletContext, request, "password", pwd, "id", id);
            } catch (Exception e) {
                return error("Password creation failed: " + e);
            }
        } else {
            return error("You must be logged in to do this");
        }
    }

    @Path("/setpassword")
    @POST
    public Response setPassword(@FormParam("currentPassword") String currentPassword, @FormParam("newPassword") String newPassword, @FormParam("return") String returnURL) {
        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            return error("You must be logged in to reset your password");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return error("Must give a new password");
        }
        String userid = ((UserInfo)subject.getPrincipal()).getOpenid();
        try {
            // Check current password in case left screen optn
            RegToken token = new RegToken(userid, currentPassword);
            subject.login(token);

            // Now set the password
            UserStore userstore = Registry.get().getUserStore();
            userstore.setCredentials(userid, ByteSource.Util.bytes(newPassword), Integer.MAX_VALUE);
            log.info("Changed password for user " + userid);
            setNocache(response);
            
            if (returnURL == null || returnURL.isEmpty()) {
                returnURL = "/ui/admin";
            }
            return redirectTo( returnURL );
        } catch (Exception e) {
            log.warn(String.format("Failed to change password for userid %s [%s]: %s", userid, e.getClass().toString(), e.getMessage()));
            return error("Failed to confirm login before changing password");
        }
    }

    @Path("/resetpassword")
    @POST
    public Response resetPassword(@FormParam("userid") String userid, @FormParam("newPassword") String newPassword, @FormParam("return") String returnURL) {
        if (userid == null || userid.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return error("Must give a user and a new password");
        }
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() && subject.hasRole(RegAuthorizationInfo.ADMINSTRATOR_ROLE)) {
            try {
                UserStore userstore = Registry.get().getUserStore();
                userstore.setCredentials(userid, ByteSource.Util.bytes(newPassword), Integer.MAX_VALUE);
                log.info("Administrator " + subject.getPrincipal() + " changed password for user " + userid);
                
                setNocache(response);
                if (returnURL == null || returnURL.isEmpty()) {
                    returnURL = "/ui/admin";
                }
                return redirectTo( returnURL );
            } catch (Exception e) {
                log.warn(String.format("Administrator failed to change password for userid %s [%s]: %s", userid, e.getClass().toString(), e.getMessage()));
                return error("Failed to change password: " + e);
            }
        } else {
            return error("You must be logged in as an adminstrator to do this");
        }
    }
    
    @Path("/listadmins")
    @GET
    public Response listadmins() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() && subject.hasRole(RegAuthorizationInfo.ADMINSTRATOR_ROLE)) {
            UserStore userstore = Registry.get().getUserStore();
            return RequestProcessor.render("admin-list.vm", uriInfo, servletContext, request, "admins", userstore.listAdminUsers());
        } else {
            return error("You must be logged in as an administrator to do this");
        }
    }

    @Path("/setrole")
    @POST
    public Response setrole(@FormParam("id") String id, @FormParam("role") String role) {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated() && subject.hasRole(RegAuthorizationInfo.ADMINSTRATOR_ROLE)) {
            UserStore userstore = Registry.get().getUserStore();
            try {
                userstore.setRole(id, role.isEmpty() ? null : role);
                return redirectTo("/ui/admin");
            } catch (Exception e) {
                return error("Role assignment failed: " + e);
            }
        } else {
            return error("You must be logged in as an administrator to do this");
        }
    }

    private Response error(String message) {
        setNocache(response);
        return RequestProcessor.render("error.vm", uriInfo, servletContext, request, "message", message);
    }

    public static Response redirectTo(String path) {
        URI uri;
        try {
            uri = new URI(path);
            return Response.seeOther(uri).build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void setNocache(HttpServletResponse httpresponse) {
        setNocache(httpresponse, "cache bypass", 60 * 60 *24);
    }
    
    private void removeNocache(HttpServletResponse httpresponse) {
        setNocache(httpresponse, null, 0);
    }
    
    private static void setNocache(HttpServletResponse httpresponse, String value, int age) {
        Cookie cookie = new Cookie(NOCACHE_COOKIE, value);
        cookie.setComment("Bypass proxy cache when logged in");
        cookie.setMaxAge(age);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        httpresponse.addCookie(cookie);
    }
}
