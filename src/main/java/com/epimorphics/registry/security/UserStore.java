/******************************************************************
 * File:        UserStore.java
 * Created by:  Dave Reynolds
 * Created on:  2 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import java.util.Collection;

import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;

/**
 * Interface abstraction for the store of registered users. The actual
 * user credentials are not stored since we rely on OpenID for that.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface UserStore {

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

    /**
     * Global permissions for a given path which apply to any authenticated user
     */
    public Collection<Permission> getGlobalPermissions(String path);

    /**
     * Set global permissions for a given path which apply to any authenticated user
     */
    public void setGlobalPermissions(String path, Collection<Permission> permissions);
}
