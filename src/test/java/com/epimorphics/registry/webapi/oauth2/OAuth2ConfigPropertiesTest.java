package com.epimorphics.registry.webapi.oauth2;

import org.junit.Test;
import static org.mockito.Mockito.*;

import java.util.*;

import static org.junit.Assert.*;

public class OAuth2ConfigPropertiesTest {
	private final Properties props = new Properties();
	private final OAuth2Provider prov1 = mock(OAuth2Provider.class);
	private final OAuth2Provider prov2 = mock(OAuth2Provider.class);
	private final OAuth2Provider prov3 = mock(OAuth2Provider.class);

	private final OAuth2Config config;

	public OAuth2ConfigPropertiesTest() {
		props.setProperty("ldregistry.providers", "prov3,prov1");

		when(prov1.getName()).thenReturn("prov1");
		when(prov2.getName()).thenReturn("prov2");
		when(prov3.getName()).thenReturn("prov3");

		List<OAuth2Provider> providers = Arrays.asList(prov1, prov2, prov3);
		this.config = new OAuth2ConfigProperties(props, providers);
	}

	@Test
	public void getUseHttps_Enabled_ReturnsTrue() {
		props.setProperty("ldregistry.usehttps", "true");
		Boolean result = config.getUseHttps();
		assertEquals(true, result);
	}

	@Test
	public void getUseHttps_Disabled_ReturnsFalse() {
		props.setProperty("ldregistry.usehttps", "false");
		Boolean result = config.getUseHttps();
		assertEquals(false, result);
	}

	@Test
	public void getProvider_DoesNotExist_ReturnsNull() {
		OAuth2Provider result = config.getProvider("provX");
		assertNull(result);
	}

	@Test
	public void getProvider_Disabled_ReturnsNull() {
		OAuth2Provider result = config.getProvider("prov2");
		assertNull(result);
	}

	@Test
	public void getProvider_Enabled_ReturnsProvider() {
		OAuth2Provider result = config.getProvider("prov1");
		assertEquals(prov1, result);
	}

	@Test
	public void getProviders_ReturnsEnabledProviders_InOrder() {
		Collection<OAuth2Provider> results = config.getProviders();
		assertEquals(2, results.size());
		assertEquals(prov3, results.toArray()[0]);
		assertEquals(prov1, results.toArray()[1]);
	}
}