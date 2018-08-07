package com.epimorphics.registry.webapi.oauth2;

import java.util.Collection;

/**
 * Defines the global characteristics of the OAuth2 authorization process.
 */
public interface OAuth2Config {
	/**
	 * @return Determine whether to use HTTPS in the OAuth redirect URL.
	 */
	Boolean getUseHttps();

	/**
	 * @return A list of enabled providers.
	 */
	Collection<OAuth2Provider> getProviders();

	/**
	 * @param name The identifying name of a provider.
	 * @return The provider associated with the given name, if it is enabled. Otherwise, return null.
	 */
	OAuth2Provider getProvider(String name);
}
