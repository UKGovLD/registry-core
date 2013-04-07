/******************************************************************
 * File:        UserStore.java
 * Created by:  Dave Reynolds
 * Created on:  2 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import org.apache.shiro.authc.SaltedAuthenticationInfo;
import org.apache.shiro.util.ByteSource;

/**
 * Interface abstraction for the store of registered users. The actual
 * user credentials are not stored since we rely on OpenID for that.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface UserStore {

    /** ID of a pseudo user which stores the global permissions available to anyone logged in */
    public static final String AUTH_USER_ID = "http://localhost/anon";

    /**
     * Link this store to a specific authorization realm
     */
    public void setRealm(RegRealm realm);

    /**
     * Register a new user
     */
    public void register(UserInfo user);

    /**
     * Test if a user is registered. Returns their user information and credentials
     * if they are or null if not registered. Stored and returned credentials are salt-hashed
     * to make it easy to allow user defined passwords in the future, redundant for
     * generated passwords. If the user has no credentials or they have timed out then
     * the credentials will be null.
     *
     * @param id the openid identifier string authenticated by the user
     */
    public SaltedAuthenticationInfo checkUser(String id);

    /**
     * Unregister a user, removing them and any permissions from the store
     */
    public void unregister(String id);

    /**
     * Store new credentials for the user
     *
     * @param id the openid identifier string authenticated by the user
     * @param credentials the password to store
     * @param minstolive the time-to-livefor the credentials in minutes
     */
    public void setCredentials(String id, ByteSource credentials, int minstolive);

    /**
     * Remove the credentials for the user
     */
    public void removeCredentials(String id);


    /**
     * Return all the permissions and rolefor this user
     */
    public RegAuthorizationInfo getPermissions(String id);

    /**
     * Record a new permission for this user.
     */
    public void addPermision(String id, RegPermission permission);

    /**
     * Remove permissions from this user. If the permission has a path then all
     * permissions for that path will be removed.
     */
    public void removePermission(String id, RegPermission permission);

    /**
     * Set a role for the user.
     */
    public void setRole(String id, String role);


}
