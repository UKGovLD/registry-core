package com.epimorphics.registry.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;


public class OpenIdConnectDiscovery {

    private static final String AUTHZ_KEY = "authorization_endpoint";
    private static final String TOKEN_KEY = "token_endpoint";
    private static final String USERINFO_KEY = "userinfo_endpoint";

    private HttpCache httpCache;
    private Map<String, String> discDocMap;
    private String url;


    public OpenIdConnectDiscovery(String discoveryUrl) {

        httpCache =  new HttpCache();
        url = discoveryUrl;

    }

    public String getAuthzEndpoint() throws Exception {
        Map<String, String > discoMap =  getDocument();
        return discoMap.get(AUTHZ_KEY);
    }

    public String getTokenEndpoint() throws Exception {
        Map<String, String > discoMap =  getDocument();
        return discoMap.get(TOKEN_KEY);
    }

    public String getUserInfoEndpoint() throws Exception {
        Map<String, String > discoMap =  getDocument();
        return discoMap.get(USERINFO_KEY);
    }

    private Map<String, String> getDocument() throws Exception {

        String result = httpCache.getRequest(url);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result, Map.class);

    }


}
