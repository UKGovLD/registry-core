/******************************************************************
 * File:        UserPermission.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

/**
 * Struct used to report the permissions that a user has over some path.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class UserPermission {

    protected UserInfo user;
    protected String permissions;
    
    public UserPermission(UserInfo user, String permissions) {
        this.user = user;
        this.permissions = permissions;
    }

    public UserInfo getUser() {
        return user;
    }

    public String getPermissions() {
        return permissions;
    }
}
