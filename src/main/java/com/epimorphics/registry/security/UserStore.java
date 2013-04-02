/******************************************************************
 * File:        UserStore.java
 * Created by:  Dave Reynolds
 * Created on:  2 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;

/**
 * Interface abstraction for the store of registered users. The actual
 * user credentials are not stored since we rely on OpenID for that.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface UserStore {

    /** ID of a pseudo user which stores the global permissions available to anyone logged in */
    public static final String AUTH_USER_ID = "___auth";
    
    /**
     * Register a new user
     */
    public void register(UserInfo user);

    /**
     * Test if a user is registered. Returns their user information
     * if they are or null if not registered.
     * @param id the openid identifier string authenticated by the user
     */
    public UserInfo checkUser(String id);

    /**
     * Return all the permissions for this user
     */
    public AuthorizationInfo getPermissions(String id);

    /**
     * Record a new permission for this user.
     * Implementations may restrict the Permission implementation supported.
     */
    public void addPermision(String id, Permission permission);

    /**
     * Remove a permission from this user.
     * Implementations may restrict the Permission implementation supported.
     */
    public void removePermission(String id, Permission permission);

}
