/******************************************************************
 * File:        ProcessOpenID.java
 * Created by:  Dave Reynolds
 * Created on:  15 Jul 2014
 * 
 * (c) Copyright 2014, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.security.RegToken;
import com.epimorphics.registry.security.UserInfo;
import com.epimorphics.registry.security.UserStore;
import com.epimorphics.registry.util.OAuthParams;
import com.epimorphics.registry.util.Oauth2Util;
import com.epimorphics.server.webapi.WebApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.jwt.JWT;
import org.apache.oltu.oauth2.jwt.io.JWTClaimsSetWriter;
import org.apache.oltu.oauth2.jwt.io.JWTHeaderWriter;
import org.apache.oltu.openidconnect.client.response.OpenIdConnectResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.message.ParameterList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

public class ProcessOauth2 {
    static final Logger log = LoggerFactory.getLogger( Login.class );

    public static final String DEFAULT_PROVIDER = "https://www.google.com/accounts/o8/id";
    public static final String PROVIDER_COOKIE = "ukgovld-login-provider";

    public static final String NOCACHE_COOKIE = "nocache";

    public static final String TMP_AUTHZ_ENDPOINT = "https://accounts.google.com/o/oauth2/auth";
    public static final String TMP_TOKEN_ENDPOINT = "https://accounts.google.com/o/oauth2/token";
    public static final String TMP_RESOURCE_ENDPOINT = "https://www.googleapis.com/plus/v1/people/me/openIdConnect";
    public static final String TMP_SCOPE = "openid email";
    public static final String TMP_APP = "google";

    // Session attribute names
    public static final String SA_OPENID_DISC = "openid_disc";
    public static final String SA_OPENID_PROVIDER = "openid_provider";
    public static final String SA_REGISTRATION = "isRegistration";
    public static final String SA_RETURN_URL = "returnURL";
    public static final String SA_RESPONSE_URL = "responseURL";
    public static final String SA_STATE = "state";

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


    protected UriInfo uriInfo;
    protected ServletContext servletContext;

    public ProcessOauth2(UriInfo uriInfo, ServletContext servletContext) {
        this.uriInfo = uriInfo;
        this.servletContext = servletContext;
    }
    
    @SuppressWarnings("rawtypes")
    protected void processOpenID(HttpServletRequest request, HttpServletResponse response, String provider, String returnURL, boolean isRegister) {
        HttpSession session = request.getSession();
        String state = generateCSRFToken();
        session.setAttribute(SA_REGISTRATION, isRegister);
        session.setAttribute(SA_OPENID_PROVIDER, provider);
        session.setAttribute(SA_STATE, state);
        if (returnURL == null || returnURL.isEmpty()) {
            returnURL = "/ui/admin";
        }
        session.setAttribute(SA_RETURN_URL, returnURL);

        if (provider == null || provider.isEmpty()) {
            provider = DEFAULT_PROVIDER;
        }
        log.info("Authentication request for " + provider + (isRegister ? " (registration)" : ""));


        String responseURL = uriInfo.getBaseUri().toString() + "system/security/responseoa";
        session.setAttribute(SA_RESPONSE_URL, responseURL);

        try
        {
//            // perform discovery on the user-supplied identifier
//            List discoveries = manager.discover(provider);
//
//            // attempt to associate with the OpenID provider
//            // and retrieve one service endpoint for authentication
//            DiscoveryInformation discovered = manager.associate(discoveries);
//
//            // store the discovery information in the user's session
//            request.getSession().setAttribute(SA_OPENID_DISC, discovered);

            // obtain a AuthRequest message to be sent to the OpenID provider
            OAuthClientRequest oauthRequest = OAuthClientRequest
                    .authorizationLocation(TMP_AUTHZ_ENDPOINT)
                    .setClientId(Oauth2Util.getClientId())
                    .setRedirectURI(responseURL)
                    .setResponseType(ResponseType.CODE.toString())
                    .setScope(TMP_SCOPE)
                    .setState(state)
                    .buildQueryMessage();

//            .setParameter("","") will be required for openid realm when retrieving openid id later


//            AuthRequest authReq = manager.authenticate(discovered, responseURL);

//            if (isRegister) {
//                // Attribute Exchange example: fetching the 'email' attribute
//                FetchRequest fetch = FetchRequest.createFetchRequest();
//                if (provider.contains("google.com")) {
////                    fetch.addAttribute(AP_EMAIL, "http://axschema.org/contact/email", false);
//                    fetch.addAttribute(AP_FIRST_NAME, "http://axschema.org/namePerson/first", true);
//                    fetch.addAttribute(AP_LAST_NAME, "http://axschema.org/namePerson/last", true);
//                } else if (provider.contains("yahoo.com")) {
////                    fetch.addAttribute(AP_EMAIL, "http://axschema.org/contact/email", false);
//                    fetch.addAttribute(AP_FULL_NAME, "http://axschema.org/namePerson", true);
//                } else { //works for myOpenID
////                    fetch.addAttribute(AP_EMAIL, "http://schema.openid.net/contact/email", false);
//                    fetch.addAttribute(AP_FULL_NAME, "http://schema.openid.net/namePerson", true);
//                }
//
//                // attach the extension to the authentication request
//                authReq.addExtension(fetch);
//            }

            // For version2 endpoints can do a form-redirect but this is easier,
            // Relies on payload being less ~ 2k, currently ~ 800 bytes
            response.sendRedirect(oauthRequest.getLocationUri());
        }
        catch (Exception e)
        {
            throw new WebApiException(Status.BAD_REQUEST, "Login/registration action failed: " + e);
        }
    }

    @SuppressWarnings({ "unchecked" })
    public Response verifyResponse(HttpServletRequest request, HttpServletResponse httpresponse) {
        OAuthParams oauthParams = new OAuthParams();
        oauthParams.setClientId(Oauth2Util.getClientId());
        oauthParams.setTokenEndpoint(TMP_TOKEN_ENDPOINT);
        oauthParams.setResourceUrl(TMP_RESOURCE_ENDPOINT);
        try {
            HttpSession session = request.getSession();

            // extract the parameters from the authentication response
            // (which comes in as a HTTP request from the OpenID provider)
            ParameterList response =
                    new ParameterList(request.getParameterMap());

            // retrieve the previously stored discovery information
//            DiscoveryInformation discovered = (DiscoveryInformation)
//                    session.getAttribute("openid-disc");

            // extract the receiving URL from the HTTP request
            StringBuffer receivingURL = request.getRequestURL();
            String queryString = request.getQueryString();
            if (queryString != null && queryString.length() > 0)
                receivingURL.append("?").append(request.getQueryString());


            // Create the response wrapperhttps://accounts.google.com/o/oauth2/auth
            OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);

            // verify the response; ConsumerManager needs to be the same
            // (static) instance used to place the authentication request
//            VerificationResult verification = manager.verify(
//                    receivingURL.toString(),
//                    response, discovered);
//
//            // examine the verification result and extract the verified identifier
//            Identifier verified = verification.getVerifiedId();

            // Get Authorization Code
            String code = oar.getCode();
//            oauthParams.setAuthzCode(code);
            String responseUrl = (String) session.getAttribute(SA_RESPONSE_URL);

            String accessToken = oar.getAccessToken();

            OAuthClientRequest authzRequest = OAuthClientRequest
                    .tokenLocation(Oauth2Util.GOOGLE_TOKEN)
                    .setClientId(Oauth2Util.getClientId())
                    .setClientSecret(Oauth2Util.getClientSecret())
                    .setRedirectURI(responseUrl)
                    .setCode(code)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .buildBodyMessage();

            OAuthClient client = new OAuthClient(new URLConnectionClient());


            OAuthAccessTokenResponse oauthResponse = null;
            Class<? extends OAuthAccessTokenResponse> cl = OAuthJSONAccessTokenResponse.class;
            cl = OpenIdConnectResponse.class;

            oauthResponse = client.accessToken(authzRequest, cl);


            oauthParams.setAccessToken(oauthResponse.getAccessToken());
            oauthParams.setExpiresIn(oauthResponse.getExpiresIn());
            oauthParams.setRefreshToken(Oauth2Util.isIssued(oauthResponse.getRefreshToken()));

            // if (Oauth2Util.GOOGLE.equalsIgnoreCase(app)){

            OpenIdConnectResponse openIdConnectResponse = ((OpenIdConnectResponse) oauthResponse);
            JWT idToken = openIdConnectResponse.getIdToken();
            oauthParams.setIdToken(idToken.getRawString());

            oauthParams.setHeader(new JWTHeaderWriter().write(idToken.getHeader()));
            oauthParams.setClaimsSet(new JWTClaimsSetWriter().write(idToken.getClaimsSet()));

            URL url = new URL(oauthParams.getTokenEndpoint());

            oauthParams.setIdTokenValid(openIdConnectResponse.checkId(url.getHost(), oauthParams.getClientId()));

            //   }


            OAuthClientRequest resRequest = new OAuthBearerClientRequest(oauthParams.getResourceUrl()).setAccessToken(oauthParams.getAccessToken()).buildHeaderMessage();


            OAuthClient resClient = new OAuthClient(new URLConnectionClient());
            OAuthResourceResponse resourceResponse = resClient.resource(resRequest, oauthParams.getRequestMethod(), OAuthResourceResponse.class);

            boolean verified = false;
            if (resourceResponse.getResponseCode() == 200) {
                oauthParams.setResource(resourceResponse.getBody());
                verified = true;
            } else {
                oauthParams.setErrorMessage(
                        "Could not access resource: " + resourceResponse.getResponseCode() + " " + resourceResponse.getBody());
            }

            if (verified) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> mapObject = mapper.readValue(oauthParams.getResource(), new TypeReference<Map<String, Object>>() {
                });
                String name = (String) mapObject.get("name");
                String email = (String) mapObject.get("email");
                String identifier = (String) mapObject.get("profile");

                log.info(String.format("Verified identithttps://accounts.google.com/o/oauth2/authy %s = %s", identifier, name));


                UserStore userstore = Registry.get().getUserStore();
                boolean isRegistration = ((Boolean) session.getAttribute(SA_REGISTRATION)).booleanValue();
                String registrationStatus = RS_LOGIN;
                if (isRegistration) {
                    UserInfo userinfo = new UserInfo(identifier, name);
                    if (userstore.register(userinfo)) {
                        registrationStatus = RS_NEW;
                    } else {
                        registrationStatus = RS_ALREADY_REGISTERED;
                    }
                }

                RegToken token = new RegToken(identifier, true);
                Subject subject = SecurityUtils.getSubject();
                try {
                    subject.login(token);
                    session.setAttribute(VN_REGISTRATION_STATUS, registrationStatus);
                    String provider = (String) session.getAttribute(SA_OPENID_PROVIDER);
                    if (provider != null && !provider.isEmpty()) {
                        Cookie cookie = new Cookie(PROVIDER_COOKIE, provider);
                        cookie.setComment("Records the openid provider you last used to log in to a UKGovLD registry");
                        cookie.setMaxAge(60 * 60 * 24 * 30);
                        cookie.setHttpOnly(true);
                        cookie.setPath("/");
                        httpresponse.addCookie(cookie);
                    }
                    Login.setNocache(httpresponse);
                    return Login.redirectTo(session.getAttribute(SA_RETURN_URL).toString());
    //                    return RequestProcessor.render("admin.vm", uriInfo, servletContext, request, VN_SUBJECT, subject, VN_REGISTRATION_STATUS, registrationStatus);
                } catch (Exception e) {
                    log.error("Authentication failure: " + e);
                    return RequestProcessor.render("error.vm", uriInfo, servletContext, request, "message", "Could not find a registration for you.");
                }
            }

        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
        return RequestProcessor.render("error.vm", uriInfo, servletContext, request, "message", "OpenID Connect login failed");
    }

    private static String generateCSRFToken() {
        return UUID.randomUUID().toString();
    }
    
}
