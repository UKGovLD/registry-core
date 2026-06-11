package com.epimorphics.registry.webapi.oauth2;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class OAuth2ServiceTest {

    private final File config;
    private final File providerDir;

    public OAuth2ServiceTest() throws IOException {
        File configDir = Files.createTempDirectory("OAuth2ServiceTest").toFile();
        configDir.deleteOnExit();

        this.config = new File(configDir, "oauth.conf");

        new FileWriter(config)
                .append("ldregistry.usehttps = true\n")
                .append("ldregistry.providers = google,github")
                .close();

        this.providerDir = new File(configDir, "provider");
        providerDir.mkdir();

        File googleConfig = new File(providerDir, "google.properties");
        new FileWriter(googleConfig)
                .append("name           = google\n")
                .append("label          = Google+\n")
                .append("client.id      = oauth2servicetest@google\n")
                .append("client.secret  = 123abc$%^\n")
                .append("auth.endpoint  = https://accounts.google.com/o/oauth2/auth\n")
                .append("token.endpoint = https://accounts.google.com/o/oauth2/token\n")
                .append("userInfo.endpoint   = https://www.googleapis.com/oauth2/v3/userinfo\n")
                .append("userInfo.key   = profile\n")
                .append("userInfo.name  = name")
                .close();

        File githubConfig = new File(providerDir, "github.properties");
        new FileWriter(githubConfig)
                .append("name           = github\n")
                .append("label          = GitHub\n")
                .append("client.id      = oauth2servicetest@github\n")
                .append("client.secret  = 456def&*(\n")
                .append("auth.endpoint  = https://github.com/login/oauth/authorize\n")
                .append("token.endpoint = https://github.com/login/oauth/access_token\n")
                .append("userInfo.endpoint   = https://api.github.com/user\n")
                .append("userInfo.key   = url")
                .append("userInfo.name  = name")
                .close();

        File bitbucketConfig = new File(providerDir, "bitbucket.properties");
        new FileWriter(bitbucketConfig)
                .append("name           = bitbucket\n")
                .append("label          = BitBucket\n")
                .append("client.id      = oauth2servicetest@bb\n")
                .append("client.secret  = 789ghi)_+\n")
                .append("auth.endpoint  = https://bitbucket.org/site/oauth2/authorize\n")
                .append("auth.scope     = email\n")
                .append("token.endpoint = https://bitbucket.org/site/oauth2/access_token\n")
                .append("userInfo.endpoint = https://api.bitbucket.org/2.0/userv")
                .append("userInfo.key   = username\n")
                .append("userInfo.name = display_name\n")
                .close();
    }

    @Test
    public void getConfig_ConfigFileNotFound_ReturnsNull() {
        OAuth2Config result = new OAuth2Service("not/a/file", providerDir.getAbsolutePath()).getConfig();
        assertNull(result);
    }

    @Test
    public void getConfig_WithoutProviders_HasNoProviders() {
        OAuth2Config result = new OAuth2Service(config.getAbsolutePath(), "not/a/dir").getConfig();
        assertNotNull(result);
        assertEquals(true, result.getUseHttps());
        assertNull(result.getProvider("google"));
        assertNull(result.getProvider("github"));
        assertNull(result.getProvider("bitbucket"));
        assertEquals(new ArrayList(), result.getProviders());
    }

    @Test
    public void getConfig_IsConfigured() {
        OAuth2Config result = new OAuth2Service(config.getAbsolutePath(), providerDir.getAbsolutePath()).getConfig();
        assertNotNull(result);
        assertEquals(true, result.getUseHttps());

        OAuth2Provider prov1 = result.getProvider("google");
        assertNotNull(prov1);
        assertEquals("google", prov1.getName());
        assertEquals("Google+", prov1.getLabel());
        assertEquals("oauth2servicetest@google", prov1.getClientId());
        assertEquals("123abc$%^", prov1.getClientSecret());
        assertEquals("https://accounts.google.com/o/oauth2/auth", prov1.getAuthEndpoint());
        assertEquals("https://accounts.google.com/o/oauth2/token", prov1.getTokenEndpoint());
        assertEquals("https://www.googleapis.com/oauth2/v3/userinfo", prov1.getUserInfoEndpoint());

        OAuth2Provider prov2 = result.getProvider("github");
        assertNotNull(prov2);
        assertEquals("github", prov2.getName());
        assertEquals("GitHub", prov2.getLabel());
        assertEquals("oauth2servicetest@github", prov2.getClientId());
        assertEquals("456def&*(", prov2.getClientSecret());
        assertEquals("https://github.com/login/oauth/authorize", prov2.getAuthEndpoint());
        assertEquals("https://github.com/login/oauth/access_token", prov2.getTokenEndpoint());
        assertEquals("https://api.github.com/user", prov2.getUserInfoEndpoint());

        assertNull(result.getProvider("bitbucket"));
        assertEquals(Arrays.asList(prov1, prov2), result.getProviders());
    }
}