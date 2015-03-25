package com.epimorphics.registry.util;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.cache.CacheResponseStatus;


import org.apache.http.client.methods.HttpGet;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClient;

import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;

public class HttpCache {

    private CachingHttpClient cachingClient;


    public HttpCache() {

        CacheConfig cacheCfg =new CacheConfig();
        cacheCfg.setMaxCacheEntries(1000);
        cacheCfg.setMaxObjectSize(8192);

        cachingClient = new CachingHttpClient(new DefaultHttpClient(), cacheCfg);

    }

    public String getRequest(String url) throws IOException {

        HttpContext context = new BasicHttpContext();
        HttpGet httpget = new HttpGet(url);
        HttpResponse response;
        String content = null;

        response = cachingClient.execute(httpget, context);

//        CacheResponseStatus responseStatus = (CacheResponseStatus) context.getAttribute(
//                CachingHttpClient.CACHE_RESPONSE_STATUS);
        EntityUtils.consume(response.getEntity());
        InputStream inStream = response.getEntity().getContent();
        content = IOUtils.toString(inStream, "UTF-8");

        return content;
    }


}
