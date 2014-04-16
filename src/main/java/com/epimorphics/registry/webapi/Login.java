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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.security.RegAuthorizationInfo;
import com.epimorphics.registry.security.RegPermission;
import com.epimorphics.registry.security.RegToken;
import com.epimorphics.registry.security.UserInfo;
import com.epimorphics.registry.security.UserStore;
import com.epimorphics.server.webapi.WebApiException;

@Path("/system/security")
public class Login {
    static final Logger log = LoggerFactory.getLogger( Login.class );

    public static final String DEFAULT_PROVIDER = "https://www.google.com/accounts/o8/id";
    public static final String PROVIDER_COOKIE = "ukgovld-login-provider";
    
    public static final String NOCACHE_COOKIE = "nocache";

    // Session attribute names
    public static final String SA_OPENID_DISC = "openid_disc";
    public static final String SA_OPENID_PROVIDER = "openid_provider";
    public static final String SA_REGISTRATION = "isRegistration";
    public static final String SA_RETURN_URL = "returnURL";

    // Attribute parameter names
    public static final String AP_EMAIL = "email";
    public static final String AP_FIRST_NAME = "firstName";
    public static final String AP_LAST_NAME = "lastName";
    public static final String AP_FULL_NAME = "fullname";

    // Velocity binding names
    public static final String VN_SUBJECT = "subject";
    public static final String VN_REGISTRATION_STATUS = "registrationStatus";
    public static final String RS_NEW = "new";
    public static final String RS_ALREADY_REGISTERED = "already";
    public static final String RS_LOGIN = "login";

    private static ConsumerManager manager = null;
    static {
        try {
            manager = new ConsumerManager();
        } catch (Exception e) {
            log.error("Failed to initialize openid subsystem", e);
        }
    }

    protected @Context UriInfo uriInfo;
    protected @Context ServletContext servletContext;
    protected @Context HttpServletRequest request;

    @Path("/login")
    @POST
    public Response login(@FormParam("provider") String provider, @FormParam("return") String returnURL, @Context HttpServletResponse response) {
        processOpenID(request, response, provider, returnURL, false);
        return Response.ok().build();
    }

    @Path("/register")
    @POST
    public Response register(@FormParam("provider") String provider, @FormParam("return") String returnURL,  @Context HttpServletResponse response) {
        processOpenID(request, response, provider, returnURL, true);
        return Response.ok().build();
    }

    @Path("/logout")
    @POST
    public void logout(@Context HttpServletResponse response) throws IOException {
        // TODO set session attribute as part of Shiro realm
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(VN_SUBJECT);
            SecurityUtils.getSubject().logout();
        }
        removeNocache(response);
        String redirect = request.getServletContext().getContextPath();
        if (redirect == null || redirect.isEmpty()) {
            redirect = "/";
        }
        response.sendRedirect(redirect);
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
    public Response openIDResponse(@Context HttpServletResponse response) {
        return verifyResponse(request, response);
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
            path = "/ui/admin";
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
    public Response ungrant(@FormParam("minstolive") String minstolive) {
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
                return RequestProcessor.render("api-key-result.vm", uriInfo, servletContext, request, "password", pwd, "id", id);
            } catch (Exception e) {
                return error("Permission grant failed: " + e);
            }
        } else {
            return error("You must be logged in to do this");
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
        return RequestProcessor.render("error.vm", uriInfo, servletContext, request, "message", message);
    }

    private Response redirectTo(String path) {
        URI uri;
        try {
            uri = new URI(path);
            return Response.seeOther(uri).build();
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    protected void processOpenID(HttpServletRequest request, HttpServletResponse response, String provider, String returnURL, boolean isRegister) {
        HttpSession session = request.getSession();
        session.setAttribute(SA_REGISTRATION, isRegister);
        session.setAttribute(SA_OPENID_PROVIDER, provider);
        if (returnURL == null || returnURL.isEmpty()) {
            returnURL = "/ui/admin";
        }
        session.setAttribute(SA_RETURN_URL, returnURL);

        if (provider == null || provider.isEmpty()) {
            provider = DEFAULT_PROVIDER;
        }
        log.info("Authentication request for " + provider + (isRegister ? " (registration)" : ""));

        String responseURL = uriInfo.getBaseUri().toString() + "system/security/response";
        try
        {
            // perform discovery on the user-supplied identifier
            List discoveries = manager.discover(provider);

            // attempt to associate with the OpenID provider
            // and retrieve one service endpoint for authentication
            DiscoveryInformation discovered = manager.associate(discoveries);

            // store the discovery information in the user's session
            request.getSession().setAttribute(SA_OPENID_DISC, discovered);

            // obtain a AuthRequest message to be sent to the OpenID provider
            AuthRequest authReq = manager.authenticate(discovered, responseURL);

            if (isRegister) {
                // Attribute Exchange example: fetching the 'email' attribute
                FetchRequest fetch = FetchRequest.createFetchRequest();
                if (provider.contains("google.com")) {
//                    fetch.addAttribute(AP_EMAIL, "http://axschema.org/contact/email", false);
                    fetch.addAttribute(AP_FIRST_NAME, "http://axschema.org/namePerson/first", true);
                    fetch.addAttribute(AP_LAST_NAME, "http://axschema.org/namePerson/last", true);
                } else if (provider.contains("yahoo.com")) {
//                    fetch.addAttribute(AP_EMAIL, "http://axschema.org/contact/email", false);
                    fetch.addAttribute(AP_FULL_NAME, "http://axschema.org/namePerson", true);
                } else { //works for myOpenID
//                    fetch.addAttribute(AP_EMAIL, "http://schema.openid.net/contact/email", false);
                    fetch.addAttribute(AP_FULL_NAME, "http://schema.openid.net/namePerson", true);
                }

                // attach the extension to the authentication request
                authReq.addExtension(fetch);
            }

            // For version2 endpoints can do a form-redirect but this is easier,
            // Relies on payload being less ~ 2k, currently ~ 800 bytes
            response.sendRedirect(authReq.getDestinationUrl(true));
        }
        catch (Exception e)
        {
            throw new WebApiException(Status.BAD_REQUEST, "Login/registration action failed: " + e);
        }
    }

    @SuppressWarnings({ "unchecked" })
    public Response verifyResponse(HttpServletRequest request, HttpServletResponse httpresponse) {
        try {
            HttpSession session = request.getSession();

            // extract the parameters from the authentication response
            // (which comes in as a HTTP request from the OpenID provider)
            ParameterList response =
                    new ParameterList(request.getParameterMap());

            // retrieve the previously stored discovery information
            DiscoveryInformation discovered = (DiscoveryInformation)
                    session.getAttribute("openid-disc");

            // extract the receiving URL from the HTTP request
            StringBuffer receivingURL = request.getRequestURL();
            String queryString = request.getQueryString();
            if (queryString != null && queryString.length() > 0)
                receivingURL.append("?").append(request.getQueryString());

            // verify the response; ConsumerManager needs to be the same
            // (static) instance used to place the authentication request
            VerificationResult verification = manager.verify(
                    receivingURL.toString(),
                    response, discovered);

            // examine the verification result and extract the verified identifier
            Identifier verified = verification.getVerifiedId();
            if (verified != null) {
                AuthSuccess authSuccess =  (AuthSuccess) verification.getAuthResponse();
                String name = null;
                if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
                    FetchResponse fetchResp = (FetchResponse) authSuccess
                            .getExtension(AxMessage.OPENID_NS_AX);
                    Map<String, List<String>> attributes = fetchResp.getAttributes();
                    if (attributes.containsKey(AP_FULL_NAME)) {
                        name = attributes.get(AP_FULL_NAME).get(0);
                    } else {
                        name = attributes.get(AP_FIRST_NAME).get(0) + " " + attributes.get(AP_LAST_NAME).get(0);
                    }
                }
                log.info(String.format("Verified identity %s = %s", verified.getIdentifier(), name));
                UserStore userstore = Registry.get().getUserStore();
                boolean isRegistration = ((Boolean)session.getAttribute(SA_REGISTRATION)).booleanValue();
                String registrationStatus = RS_LOGIN;
                if (isRegistration) {
                    UserInfo userinfo = new UserInfo(verified.getIdentifier(), name);
                    if (userstore.register( userinfo )) {
                        registrationStatus = RS_NEW;
                    } else {
                        registrationStatus = RS_ALREADY_REGISTERED;
                    }
                }

                RegToken token = new RegToken(verified.getIdentifier(), true);
                Subject subject = SecurityUtils.getSubject();
                try {
                    subject.login(token);
                    session.setAttribute(VN_REGISTRATION_STATUS, registrationStatus);
                    String provider = (String)session.getAttribute(SA_OPENID_PROVIDER);
                    if (provider != null && !provider.isEmpty()) {
                        Cookie cookie = new Cookie(PROVIDER_COOKIE, provider);
                        cookie.setComment("Records the openid provider you last used to log in to a UKGovLD registry");
                        cookie.setMaxAge(60 * 60 * 24 * 30);
                        cookie.setHttpOnly(true);
                        cookie.setPath("/");
                        httpresponse.addCookie(cookie);
                    }
                    setNocache(httpresponse);
                    return redirectTo( session.getAttribute(SA_RETURN_URL).toString() );
//                    return RequestProcessor.render("admin.vm", uriInfo, servletContext, request, VN_SUBJECT, subject, VN_REGISTRATION_STATUS, registrationStatus);
                } catch (Exception e) {
                    log.error("Authentication failure: " + e);
                    return RequestProcessor.render("error.vm", uriInfo, servletContext, request, "message", "Could not find a registration for you.");
                }
            }
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
        return RequestProcessor.render("error.vm", uriInfo, servletContext, request, "message", "OpenID login failed");
    }
    
    private void setNocache(HttpServletResponse httpresponse) {
        setNocache(httpresponse, "cache bypass", 60 * 60 *24);
    }
    
    private void removeNocache(HttpServletResponse httpresponse) {
        setNocache(httpresponse, null, 0);
    }
    
    private void setNocache(HttpServletResponse httpresponse, String value, int age) {
        Cookie cookie = new Cookie(NOCACHE_COOKIE, value);
        cookie.setComment("Bypass proxy cache when logged in");
        cookie.setMaxAge(age);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        httpresponse.addCookie(cookie);
    }
}
