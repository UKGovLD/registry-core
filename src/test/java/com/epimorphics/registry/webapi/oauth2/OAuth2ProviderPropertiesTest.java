package com.epimorphics.registry.webapi.oauth2;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class OAuth2ProviderPropertiesTest {
	private final Properties props = new Properties();
	private final OAuth2Provider provider = new OAuth2ProviderProperties(props);

	public OAuth2ProviderPropertiesTest() {
		props.setProperty("name", "prov");
		props.setProperty("label", "Provider");
		props.setProperty("client.id", "test-app");
		props.setProperty("client.secret", "a012345z");
		props.setProperty("auth.endpoint", "http://test-prov.org/auth");
		props.setProperty("token.endpoint", "http://test-prov.org/token");
		props.setProperty("userInfo.endpoint", "http://test-prov.org/user");
		props.setProperty("userInfo.key", "email");
		props.setProperty("userInfo.name", "fullName");
	}

	@Test
	public void getName_ReturnsName() {
		String result = provider.getName();
		assertEquals("prov", result);
	}

	@Test
	public void getLabel_WithLabel_ReturnsLabel() {
		String result = provider.getLabel();
		assertEquals("Provider", result);
	}

	@Test
	public void getLabel_WithoutLabel_ReturnsName() {
		props.remove("label");
		String result = provider.getLabel();
		assertEquals("prov", result);
	}

	@Test
	public void getClientId_ReturnsClientId() {
		String result = provider.getClientId();
		assertEquals("test-app", result);
	}

	@Test
	public void getClientSecret_ReturnsClientSecret() {
		String result = provider.getClientSecret();
		assertEquals("a012345z", result);
	}

	@Test
	public void getAuthEndpoint_ReturnsUrl() {
		String result = provider.getAuthEndpoint();
		assertEquals("http://test-prov.org/auth", result);
	}

	@Test
	public void getTokenEndpoint_ReturnsUrl() {
		String result = provider.getTokenEndpoint();
		assertEquals("http://test-prov.org/token", result);
	}

	@Test
	public void getUserInfoEndpoint_ReturnsUrl() {
		String result = provider.getUserInfoEndpoint();
		assertEquals("http://test-prov.org/user", result);
	}

	@Test
	public void getUserId_ReturnsKeyPropertyValue() {
		HashMap<String, Object> entity = new HashMap<>();
		entity.put("email", "user@test-prov.org");

		String result = provider.getUserId(entity);
		assertEquals("user@test-prov.org", result);
	}

	@Test
	public void getUserName_WithName_ReturnsNamePropertyValue() {
		HashMap<String, Object> entity = new HashMap<>();
		entity.put("fullName", "Test User");

		String result = provider.getUserName(entity);
		assertEquals("Test User", result);
	}

	@Test
	public void getUserName_WithoutName_ReturnsId() {
		props.remove("userInfo.name");

		HashMap<String, Object> entity = new HashMap<>();
		entity.put("email", "user@test-prov.org");

		String result = provider.getUserName(entity);
		assertEquals("user@test-prov.org", result);
	}

	@Test
	public void getAuthScope_WithoutAuthScope_ReturnsDefault() {
		String result = provider.getAuthScope();
		assertEquals("openid email", result);
	}

	@Test
	public void getAuthScope_WithAuthScope_ReturnsAuthScope() {
		props.setProperty("auth.scope", "email");
		String result = provider.getAuthScope();
		assertEquals("email", result);
	}
}