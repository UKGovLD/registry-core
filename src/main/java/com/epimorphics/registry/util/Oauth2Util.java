package com.epimorphics.registry.util;
/**
 *       Copyright 2010 Newcastle University
 *
 *          http://research.ncl.ac.uk/smart/
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.apache.oltu.oauth2.common.OAuthProviderType;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.FileReader;
import java.util.Properties;

/**
 * Created by lizard on 04/12/2014.
 */
public class Oauth2Util {


    private Oauth2Util() {
    }

//    public static final String REDIRECT_URI = "http://localhost:8080/redirect";
//    public static final String DISCOVERY_URI = "http://localhost:8080";

    public static final String REG_TYPE_PULL = "pull";
    public static final String REG_TYPE_PUSH = "push";

    public static final String REQUEST_TYPE_QUERY= "queryParameter";
    public static final String REQUEST_TYPE_HEADER= "headerField";
    public static final String REQUEST_TYPE_BODY= "bodyParameter";

    public static final String GENERIC = "generic";

    public static final String FACEBOOK = OAuthProviderType.FACEBOOK.getProviderName();
    public static final String FACEBOOK_AUTHZ = OAuthProviderType.FACEBOOK.getAuthzEndpoint();
    public static final String FACEBOOK_TOKEN = OAuthProviderType.FACEBOOK.getTokenEndpoint();

    public static final String GOOGLE = OAuthProviderType.GOOGLE.getProviderName();
    public static final String GOOGLE_AUTHZ = OAuthProviderType.GOOGLE.getAuthzEndpoint();
    public static final String GOOGLE_TOKEN = OAuthProviderType.GOOGLE.getTokenEndpoint();

    public static final String LINKEDIN = OAuthProviderType.LINKEDIN.getProviderName();
    public static final String LINKEDIN_AUTHZ = OAuthProviderType.LINKEDIN.getAuthzEndpoint();
    public static final String LINKEDIN_TOKEN = OAuthProviderType.LINKEDIN.getTokenEndpoint();

    public static final String GITHUB = OAuthProviderType.GITHUB.getProviderName();
    public static final String GITHUB_AUTHZ = OAuthProviderType.GITHUB.getAuthzEndpoint();
    public static final String GITHUB_TOKEN = OAuthProviderType.GITHUB.getTokenEndpoint();

    public static final String TMP_OAUTH_FILE_LOCATION = "/opt/ldregistry/config/oauth.conf";
    public static final String TMP_CLIENT_ID_NAME = "ldregistry.client-id";
    public static final String TMP_CLIENT_SECRET_NAME = "ldregistry.client-secret";
    public static final Properties oauthCredentials = new Properties();

    static {

        try {
            FileReader reader = new FileReader(TMP_OAUTH_FILE_LOCATION);

            oauthCredentials.load(reader);

            reader.close();

        } catch (Exception e) {
            throw new RuntimeException("Unable to read oauth credentials", e);

        }


    }


    public static String getClientId() {
        return (String) oauthCredentials.getProperty(TMP_CLIENT_ID_NAME);
    }

    public static String getClientSecret() {
        return (String) oauthCredentials.getProperty(TMP_CLIENT_SECRET_NAME);
    }

//    public static void validateRegistrationParams(OAuthRegParams oauthParams) throws ApplicationException {
//
//        String regType = oauthParams.getRegistrationType();
//
//        String name = oauthParams.getName();
//        String url = oauthParams.getUrl();
//        String description = oauthParams.getDescription();
//        StringBuffer sb = new StringBuffer();
//
//        if (isEmpty(url)) {
//            sb.append("Application URL ");
//        }
//
//        if (REG_TYPE_PUSH.equals(regType)) {
//            if (isEmpty(name)) {
//                sb.append("Application Name ");
//            }
//
//            if (isEmpty(description)) {
//                sb.append("Application URL ");
//            }
//        } else if (!REG_TYPE_PULL.equals(regType)) {
//            throw new ApplicationException("Incorrect registration type: " + regType);
//        }
//
//        String incorrectParams = sb.toString();
//        if ("".equals(incorrectParams)) {
//            return;
//        }
//        throw new ApplicationException("Incorrect parameters: " + incorrectParams);
//
//    }

    public static void validateAuthorizationParams(OAuthParams oauthParams) throws ApplicationException {


        String authzEndpoint = oauthParams.getAuthzEndpoint();
        String tokenEndpoint = oauthParams.getTokenEndpoint();
        String clientId = oauthParams.getClientId();
        String clientSecret = oauthParams.getClientSecret();
        String redirectUri = oauthParams.getRedirectUri();

        StringBuffer sb = new StringBuffer();

        if (isEmpty(authzEndpoint)) {
            sb.append("Authorization Endpoint ");
        }

        if (isEmpty(tokenEndpoint)) {
            sb.append("Token Endpoint ");
        }

        if (isEmpty(clientId)) {
            sb.append("Client ID ");
        }

        if (isEmpty(clientSecret)) {
            sb.append("Client Secret ");
        }

//        if (!REDIRECT_URI.equals(redirectUri)) {
//            sb.append("Redirect URI");
//        }

        String incorrectParams = sb.toString();
        if ("".equals(incorrectParams)) {
            return;
        }
        throw new ApplicationException("Incorrect parameters: " + incorrectParams);

    }

    public static void validateTokenParams(OAuthParams oauthParams) throws ApplicationException {

        String authzEndpoint = oauthParams.getAuthzEndpoint();
        String tokenEndpoint = oauthParams.getTokenEndpoint();
        String clientId = oauthParams.getClientId();
        String clientSecret = oauthParams.getClientSecret();
        String redirectUri = oauthParams.getRedirectUri();
        String authzCode = oauthParams.getAuthzCode();

        StringBuffer sb = new StringBuffer();

        if (isEmpty(authzCode)) {
            sb.append("Authorization Code ");
        }

        if (isEmpty(authzEndpoint)) {
            sb.append("Authorization Endpoint ");
        }

        if (isEmpty(tokenEndpoint)) {
            sb.append("Token Endpoint ");
        }

        if (isEmpty(clientId)) {
            sb.append("Client ID ");
        }

        if (isEmpty(clientSecret)) {
            sb.append("Client Secret ");
        }

//        if (!REDIRECT_URI.equals(redirectUri)) {
//            sb.append("Redirect URI");
//        }

        String incorrectParams = sb.toString();
        if ("".equals(incorrectParams)) {
            return;
        }
        throw new ApplicationException("Incorrect parameters: " + incorrectParams);

    }

    public static boolean isEmpty(String value) {
        return value == null || "".equals(value);
    }


    public static String findCookieValue(HttpServletRequest request, String key) {
        Cookie[] cookies = request.getCookies();

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(key)) {
                return cookie.getValue();
            }
        }
        return "";
    }

    public static String isIssued(String value) {
        if (isEmpty(value)) {
            return "(Not issued)";
        }
        return value;
    }
}

