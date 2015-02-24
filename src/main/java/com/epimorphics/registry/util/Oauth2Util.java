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


import com.sun.org.apache.xpath.internal.operations.Bool;
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



    public static final String TMP_OAUTH_FILE_LOCATION = "/opt/ldregistry/config/oauth.conf";
    public static final String TMP_CLIENT_ID_NAME = "ldregistry.client-id";
    public static final String TMP_CLIENT_SECRET_NAME = "ldregistry.client-secret";
    public static final String USE_HTTPS = "ldregistry.usehttps";
    public static final Properties oauthParams = new Properties();

    static {

        try {
            FileReader reader = new FileReader(TMP_OAUTH_FILE_LOCATION);

            oauthParams.load(reader);

            reader.close();

        } catch (Exception e) {
            throw new RuntimeException("Unable to read oauth credentials", e);

        }


    }


    public static String getClientId() {
        return oauthParams.getProperty(TMP_CLIENT_ID_NAME);
    }

    public static String getClientSecret() {
        return oauthParams.getProperty(TMP_CLIENT_SECRET_NAME);
    }

    public static boolean istUseHttps() {
        return Boolean.parseBoolean(oauthParams.getProperty(USE_HTTPS));
    }


    public static boolean isEmpty(String value) {
        return value == null || "".equals(value);
    }


    public static String isIssued(String value) {
        if (isEmpty(value)) {
            return "(Not issued)";
        }
        return value;
    }
}

