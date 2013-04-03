/******************************************************************
 * File:        RegRealm.java
 * Created by:  Dave Reynolds
 * Created on:  2 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import java.util.Collection;
import java.util.List;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import com.epimorphics.registry.core.Registry;
import com.epimorphics.util.EpiException;

/**
 * A Shiro Realm designed to support the sort of authentication and
 * authorization we need for the Registry security model.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegRealm extends AuthorizingRealm {
    protected UserStore userstore;

    public RegRealm() {
        // Do we need to set a default name?

        // Force permission resolver here since implemention depends on it
        setPermissionResolver( new RegPermissionResolver() );
    }

    protected UserStore getUserStore() {
        if (userstore == null) {
            userstore = Registry.get().getUserStore();
        }
        return userstore;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(
            AuthenticationToken token) throws AuthenticationException {
        String id = (String)token.getPrincipal();
        UserInfo userinfo = getUserStore().checkUser(id);
        if (userinfo != null) {
            return new SimpleAuthenticationInfo(userinfo, "", getName());
        }
        return null;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(
            PrincipalCollection principals) {

        UserInfo user = (UserInfo)principals.getPrimaryPrincipal();
        RegAuthorizationInfo auth = new RegAuthorizationInfo();
        auth.addAllPermissions( getUserStore().getPermissions(user.getOpenid()) );
        auth.addAllPermissions( getUserStore().getPermissions( UserStore.AUTH_USER_ID ) );
        return auth;
    }

    // TODO add methods to update permissions, add here so can clear authorization cache then pass on to user store

    /**
     * Custom implementation of permission checking to handle inheritance
     * of permissions down the URI tree
     */
    protected boolean isPermitted(Permission permission, AuthorizationInfo info) {
        try {
            RegPermission rp = (RegPermission)permission;
            RegAuthorizationInfo auth = (RegAuthorizationInfo) info;
            return auth.permits(rp);
        } catch (Throwable t) {
            throw new EpiException("Internal typing error", t);
        }
    }

    // ----------- Replicated from parent class to over *private* visibility of base isPermitted operation --------------

    public boolean isPermitted(PrincipalCollection principals, Permission permission) {
        AuthorizationInfo info = getAuthorizationInfo(principals);
        return isPermitted(permission, info);
    }

    protected void checkPermission(Permission permission, AuthorizationInfo info) {
        if (!isPermitted(permission, info)) {
            String msg = "User is not permitted [" + permission + "]";
            throw new UnauthorizedException(msg);
        }
    }
    protected boolean[] isPermitted(List<Permission> permissions, AuthorizationInfo info) {
        boolean[] result;
        if (permissions != null && !permissions.isEmpty()) {
            int size = permissions.size();
            result = new boolean[size];
            int i = 0;
            for (Permission p : permissions) {
                result[i++] = isPermitted(p, info);
            }
        } else {
            result = new boolean[0];
        }
        return result;
    }

    protected boolean isPermittedAll(Collection<Permission> permissions, AuthorizationInfo info) {
        if (permissions != null && !permissions.isEmpty()) {
            for (Permission p : permissions) {
                if (!isPermitted(p, info)) {
                    return false;
                }
            }
        }
        return true;
    }

}
