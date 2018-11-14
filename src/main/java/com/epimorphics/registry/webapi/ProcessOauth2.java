
package com.epimorphics.registry.webapi;

import com.epimorphics.appbase.core.AppConfig;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.security.RegToken;
import com.epimorphics.registry.security.UserInfo;
import com.epimorphics.registry.security.UserStore;
import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.registry.webapi.oauth2.OAuth2Config;
import com.epimorphics.registry.webapi.oauth2.OAuth2Service;
import com.epimorphics.registry.webapi.oauth2.OAuth2Provider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
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
import java.util.Map;
import java.util.UUID;

public class ProcessOauth2 {
    static final Logger log = LoggerFactory.getLogger( Login.class );

    private static final String PROVIDER_COOKIE = "ukgovld-login-provider";

    // Session attribute names
    private static final String SA_OPENID_PROVIDER = "openid_provider";
    private static final String SA_REGISTRATION = "isRegistration";
    private static final String SA_RETURN_URL = "returnURL";
    private static final String SA_RESPONSE_URL = "responseURL";
    private static final String SA_STATE = "state";


    // Velocity binding names
    public static final String VN_SUBJECT = "subject";
    public static final String VN_REGISTRATION_STATUS = "registrationStatus";
    public static final String RS_NEW = "new";
    public static final String RS_ALREADY_REGISTERED = "already";
    public static final String RS_LOGIN = "login";

    private final UriInfo uriInfo;
    private final ServletContext servletContext;

    private static OAuth2Config getConfig() {
        OAuth2Service service = AppConfig.getApp().getComponentAs("oauth2", OAuth2Service.class);
        return (service == null ? new OAuth2Service() : service).getConfig();
    }

    private static final OAuth2Config config = getConfig();

    public ProcessOauth2(UriInfo uriInfo, ServletContext servletContext) {
        this.uriInfo = uriInfo;
        this.servletContext = servletContext;
    }

    protected void processOpenID(HttpServletRequest request, HttpServletResponse response, String providerName, String returnURL, boolean isRegister) {
        if (config == null) {
            throw new WebApplicationException("OAuth not configured.");
        }

        OAuth2Provider provider = config.getProvider(providerName);
        if (provider == null) {
            String msg = "OAuth provider " + providerName + " not configured or not enabled.";
            log.error("OAuth login failed: " + msg);
            throw new WebApplicationException(msg);
        }

        HttpSession session = request.getSession();
        String state = generateState();
        session.setAttribute(SA_REGISTRATION, isRegister);
        session.setAttribute(SA_OPENID_PROVIDER, providerName);
        session.setAttribute(SA_STATE, state);

        if (returnURL == null || returnURL.isEmpty()) {
            returnURL = "/ui/admin";
        }
        if (config.getUseHttps()) {
            returnURL = returnURL.replaceFirst("^/", "");
            returnURL = uriInfo.getBaseUri().toString() + returnURL;
            log.info(String.format("OAuth returnURL is %s", returnURL));
            String secureReturnURL = returnURL.replace("http://", "https://");
                session.setAttribute(SA_RETURN_URL, secureReturnURL);
        } else {
            session.setAttribute(SA_RETURN_URL, returnURL);
        }

        log.info("Authentication request for " + provider.getLabel() + (isRegister ? " (registration)" : ""));

        String responseURL = uriInfo.getBaseUri().toString() + "system/security/responseoa";
        if (config.getUseHttps()) {
            responseURL = responseURL.replace("http://", "https://");
        }

        log.info(String.format("response URL for auth request: %s", responseURL));
        session.setAttribute(SA_RESPONSE_URL, responseURL);

        try {
            // obtain a AuthRequest message to be sent to the OpenID provider
            OAuthClientRequest oauthRequest = OAuthClientRequest
                    .authorizationLocation(provider.getAuthEndpoint())
                    .setClientId(provider.getClientId())
                    .setRedirectURI(responseURL)
                    .setResponseType(ResponseType.CODE.toString())
                    .setScope(provider.getAuthScope())
                    .setState(state)
                    .buildQueryMessage();

            // For version2 endpoints can do a form-redirect but this is easier,
            // Relies on payload being less ~ 2k, currently ~ 800 bytes
            response.sendRedirect(oauthRequest.getLocationUri());
        } catch (Exception e) {
            throw new WebApiException(Status.BAD_REQUEST, "Login/registration action failed: " + e);
        }
    }

    public Response verifyResponse(HttpServletRequest request, HttpServletResponse httpresponse) {
        HttpSession session = request.getSession();
        String providerName = session.getAttribute(SA_OPENID_PROVIDER).toString();
        OAuth2Provider provider = config.getProvider(providerName);

        try {
            OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            String code = oar.getCode();
            String responseUrl = (String) session.getAttribute(SA_RESPONSE_URL);

            OAuthClientRequest authzRequest = OAuthClientRequest
                    .tokenLocation(provider.getTokenEndpoint())
                    .setClientId(provider.getClientId())
                    .setClientSecret(provider.getClientSecret())
                    .setRedirectURI(responseUrl)
                    .setCode(code)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .buildBodyMessage();

            authzRequest.setHeader("Accept", "application/json");
            OAuthClient client = new OAuthClient(new URLConnectionClient());
            OAuthJSONAccessTokenResponse oauthResponse = client.accessToken(authzRequest, OAuthJSONAccessTokenResponse.class);

            OAuthClientRequest resRequest = new OAuthBearerClientRequest(provider.getUserInfoEndpoint())
                    .setAccessToken(oauthResponse.getAccessToken())
                    .buildHeaderMessage();

            OAuthClient resClient = new OAuthClient(new URLConnectionClient());
            OAuthResourceResponse resourceResponse = resClient.resource(resRequest, null, OAuthResourceResponse.class);

            boolean verified = false;
            if (resourceResponse.getResponseCode() == 200) {
                verified = true;
            } else {
                log.error("Could not access user info resource: " + resourceResponse.getResponseCode() + " " + resourceResponse.getBody());
            }

            if (verified) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> mapObject = mapper.readValue(resourceResponse.getBody(), new TypeReference<Map<String, Object>>() {});
                String identifier = provider.getUserId(mapObject);
                String name = provider.getUserName(mapObject);

                Boolean isRegistration = ((Boolean) session.getAttribute(SA_REGISTRATION));

                if (identifier == null) {
                    String msg = isRegistration ?
                            "Unable to register user. Make sure your account is configured to support authentication using OAuth and OpenID Connect."
                            : "Unable to identify user. Make sure you are logging in with the same provider that you registered with.";
                    return renderError(request, msg);
                }

                log.info(String.format("Verified identity via %s: %s", provider.getLabel(), identifier));

                UserStore userstore = Registry.get().getUserStore();

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
                    if (providerName != null && !providerName.isEmpty()) {
                        Cookie cookie = new Cookie(PROVIDER_COOKIE, providerName);
                        cookie.setComment("Records the OAuth provider you last used to log in to a UKGovLD registry");
                        cookie.setMaxAge(60 * 60 * 24 * 30);
                        cookie.setHttpOnly(true);
                        cookie.setPath("/");
                        httpresponse.addCookie(cookie);
                    }

                    Login.setNocache(httpresponse);
                    return Login.redirectTo(session.getAttribute(SA_RETURN_URL).toString());
                } catch (Exception e) {
                    log.error("Authentication failure: " + e);
                    return renderError(request, "Could not find a registration for you.");
                }
            }
        } catch (Exception e) {
            log.error("OAuth login failed: " + e.getMessage());
            return renderError(request, "OAuth login failed. The OAuth provider may not be configured correctly.");
        }

        return renderError(request, "OAuth login failed. Make sure your account is configured to support authentication using OAuth and OpenID Connect.");
    }

    private Response renderError(HttpServletRequest request, String msg) {
        return RequestProcessor.render("error.vm", uriInfo, servletContext, request, "message", msg);
    }

    private static String generateState() {
        return UUID.randomUUID().toString();
    }
}
