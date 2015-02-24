package com.epimorphics.registry.util;


import java.io.FileReader;
import java.util.Properties;

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

