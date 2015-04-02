/******************************************************************
 * File:        ProcessOpenID.java
 * Created by:  Dave Reynolds
 * Created on:  15 Jul 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
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

import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.security.RegToken;
import com.epimorphics.registry.security.UserInfo;
import com.epimorphics.registry.security.UserStore;

public class ProcessOpenID {
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

    protected UriInfo uriInfo;
    protected ServletContext servletContext;

    public ProcessOpenID(UriInfo uriInfo, ServletContext servletContext) {
        this.uriInfo = uriInfo;
        this.servletContext = servletContext;
    }
    
    @SuppressWarnings("rawtypes")
    protected void processOpenID(HttpServletRequest request, HttpServletResponse response, String provider, String returnURL, boolean isRegister) {
        HttpSession session = request.getSession();
        session.setAttribute(SA_REGISTRATION, isRegister);
        session.setAttribute(SA_OPENID_PROVIDER, provider);
        if (returnURL == null || returnURL.isEmpty()) {
            returnURL = "/ui/admin";
        }
        
        if (Registry.get().isRedirectToHttpsOnLogin()) {
            returnURL = returnURL.replaceFirst("^/", "");
            returnURL = uriInfo.getBaseUri().toString() + returnURL;
            log.info(String.format("OAuth returnURL is %s", returnURL));
            String secureReturnURL = returnURL.replace("http://", "https://");
                session.setAttribute(SA_RETURN_URL, secureReturnURL);
        } else {
            session.setAttribute(SA_RETURN_URL, returnURL);
        }        
        session.setAttribute(SA_RETURN_URL, returnURL);

        if (provider == null || provider.isEmpty()) {
            provider = DEFAULT_PROVIDER;
        }
        log.info("Authentication request for " + provider + (isRegister ? " (registration)" : ""));

        String responseURL = uriInfo.getBaseUri().toString() + "system/security/response";
        if (Registry.get().isRedirectToHttpsOnLogin()) {
            responseURL = responseURL.replace("http://", "https://");
        }
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
                    } else if (attributes.containsKey(AP_FIRST_NAME) && attributes.containsKey(AP_LAST_NAME)) {
                        name = attributes.get(AP_FIRST_NAME).get(0) + " " + attributes.get(AP_LAST_NAME).get(0);
                    } else {
                        name = verified.getIdentifier();
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
                    Login.setNocache(httpresponse);
                    return Login.redirectTo( session.getAttribute(SA_RETURN_URL).toString() );
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
    
}
