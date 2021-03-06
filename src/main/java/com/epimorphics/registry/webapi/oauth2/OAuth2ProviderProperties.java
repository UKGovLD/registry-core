package com.epimorphics.registry.webapi.oauth2;

import java.util.Map;
import java.util.Properties;

class OAuth2ProviderProperties implements OAuth2Provider {
	private final Properties props;

	OAuth2ProviderProperties(Properties props) {
		this.props = props;
	}

	private String get(String prop) {
		return props.getProperty(prop);
	}

	@Override public String getName() {
		return get("name");
	}

	@Override public String getLabel() {
		String label = get("label");
		if (label == null) {
			return getName();
		}

		return label;
	}

	@Override public String getClientId() {
		return get("client.id");
	}

	@Override public String getClientSecret() {
		return get("client.secret");
	}

	@Override public String getAuthEndpoint() {
		return get("auth.endpoint");
	}

	@Override public String getTokenEndpoint() {
		return get("token.endpoint");
	}

	@Override public String getUserInfoEndpoint() {
		return get("userInfo.endpoint");
	}

	@Override public String getAuthScope() {
		String value = get("auth.scope");
		return value == null ? "openid email" : value;
	}

	@Override public String getUserId(Map<String, Object> entity) {
		String prop = get("userInfo.key");
		Object id = entity.get(prop);
		return id == null ? null : id.toString();
	}

	@Override public String getUserName(Map<String, Object> entity) {
		String prop = get("userInfo.name");
		if (prop == null) {
			return getUserId(entity);
		}

		Object name = entity.get(prop);
		return name == null ? null : name.toString();
	}
}
