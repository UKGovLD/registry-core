package com.epimorphics.registry.webapi.oauth2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

class OAuth2ConfigProperties implements OAuth2Config {
	private final Logger log = LoggerFactory.getLogger(OAuth2ConfigProperties.class);
	private final Properties props;
	private final List<String> enabled;
	private final Map<String, OAuth2Provider> providersByName;

	OAuth2ConfigProperties(Properties props, Collection<OAuth2Provider> providers) {
		this.props = props;
		this.enabled = Arrays.asList(getEnabledProviders());
		this.providersByName = getProvidersByName(providers);
	}

	private String get(String prop) {
		return props.getProperty(prop);
	}

	private String[] getEnabledProviders() {
		return get("ldregistry.providers").split(",");
	}

	private Map<String, OAuth2Provider> getProvidersByName(Collection<OAuth2Provider> providers) {
		Map<String, OAuth2Provider> providersByName = new HashMap<>();
		for (OAuth2Provider provider : providers) {
			providersByName.put(provider.getName(), provider);
		}

		return providersByName;
	}

	@Override public Boolean getUseHttps() {
		return Boolean.parseBoolean(props.getProperty("ldregistry.usehttps"));
	}

	@Override public Collection<OAuth2Provider> getProviders() {
		return enabled.stream().map(providersByName::get).collect(Collectors.toList());
	}

	@Override public OAuth2Provider getProvider(String name) {
		return enabled.contains(name) ? providersByName.get(name) : null;
	}
}
