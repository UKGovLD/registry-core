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
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SaltedAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.HashService;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

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
    protected HashService hashService;

    public RegRealm() {
        // Do we need to set a default name?

        setPermissionResolver( new RegPermissionResolver() );
        setCredentialsMatcher( new RegCredentialsMatcher() );
        DefaultHashService hashing = new DefaultHashService();
        hashing.setHashAlgorithmName( RegCredentialsMatcher.DEFAULT_ALGORITHM );
        hashing.setHashIterations( RegCredentialsMatcher.DEFAULT_ITERATIONS );
        hashService = hashing;

        userstore = Registry.get().getUserStore();
        userstore.setRealm(this);
    }

    public UserStore getUserStore() {
        return userstore;
    }

    public HashService getHashService() {
        return hashService;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(
            AuthenticationToken token) throws AuthenticationException {
        if (!(token instanceof RegToken)) {
            throw new IncorrectCredentialsException();
        }
        RegToken rtoken = (RegToken)token;
        String id = (String)rtoken.getPrincipal();
        SaltedAuthenticationInfo info = getUserStore().checkUser(id);
        return info;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(
            PrincipalCollection principals) {
        UserInfo user = (UserInfo)principals.getPrimaryPrincipal();
        return getUserStore().getPermissions(user.getOpenid());
    }

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

    /**
     * Clear cached authentication and authorization information
     * for an individual. Should be called from UserStore implementation
     * whenever a change is made.
     */
    protected void clearCacheFor(String id) {
        UserInfo principal = new UserInfo(id, null);
        PrincipalCollection pc = new SimplePrincipalCollection(principal, getName());
        clearCache(pc);
    }

    // Override implementation so that key used for tokens (openid URI) is also
    // used for princpals (UserInfo)
    @Override
    protected Object getAuthenticationCacheKey(PrincipalCollection pc) {
        return ((UserInfo)pc.getPrimaryPrincipal()).getOpenid();
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
