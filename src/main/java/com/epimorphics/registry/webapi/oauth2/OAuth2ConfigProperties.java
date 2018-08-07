package com.epimorphics.registry.webapi.oauth2;

import java.util.*;

class OAuth2ConfigProperties implements OAuth2Config {
	private final Properties props;
	private final Map<String, OAuth2Provider> providersByName;

	OAuth2ConfigProperties(Properties props, Collection<OAuth2Provider> providers) {
		this.props = props;
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
		List<String> enabled = Arrays.asList(getEnabledProviders());

		providers.stream().filter(provider ->
			enabled.contains(provider.getName())
		).forEach(provider ->
			providersByName.put(provider.getName(), provider)
		);

		return providersByName;
	}

	@Override public Boolean getUseHttps() {
		return Boolean.parseBoolean(props.getProperty("ldregistry.usehttps"));
	}

	@Override public Collection<OAuth2Provider> getProviders() {
		return providersByName.values();
	}

	@Override public OAuth2Provider getProvider(String name) {
		return providersByName.get(name);
	}
}
