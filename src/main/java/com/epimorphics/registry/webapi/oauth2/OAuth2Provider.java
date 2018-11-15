package com.epimorphics.registry.webapi.oauth2;

import java.util.Map;

/**
 * Defines the behaviour of a single OAuth2 provider, eg. Google, Github.
 */
public interface OAuth2Provider {
	/**
	 * @return The uniquely identifying name of the provider.
	 */
	String getName();

	/**
	 * @return The user-friendly name of the provider.
	 */
	String getLabel();

	/**
	 * @return The ID of the application which is requesting authorisation.
	 */
	String getClientId();

	/**
	 * @return The secret which authenticates the application.
	 */
	String getClientSecret();

	/**
	 * @return The URL of the endpoint to request authorisation from this provider.
	 */
	String getAuthEndpoint();

	/**
	 * @return The URL of the endpoint to request an access token from this provider.
	 */
	String getTokenEndpoint();

	/**
	 * @return The URL of the endpoint to request information about the user.
	 */
	String getUserInfoEndpoint();

	/**
	 * @return The name of the scope which defines what user information will be shared.
	 */
	String getAuthScope();

	/**
	 * @param entity The body of the deserialized response returned from the user information request.
	 * @return The unique identifier for the user. Return null if and only if the provider is unable to identify the user.
	 */
	String getUserId(Map<String, Object> entity);

	/**
	 * @param entity The body of the deserialized response returned from the user information request.
	 * @return The user-friendly name of the user. Return null if and only if {@link #getUserId} returns null.
	 */
	String getUserName(Map<String, Object> entity);
}
