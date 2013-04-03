/******************************************************************
 * File:        RegPermissionResolver.java
 * Created by:  Dave Reynolds
 * Created on:  3 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.PermissionResolver;

/**
 * PermissionResolver implementation to wire RegPermissions into Shiro.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegPermissionResolver implements PermissionResolver {

    @Override
    public Permission resolvePermission(String permissionString) {
        return new RegPermission(permissionString);
    }

}
