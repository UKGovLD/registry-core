/******************************************************************
 * File:        BaseRegRealm.java
 * Created by:  Dave Reynolds
 * Created on:  7 Apr 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.HashService;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

/**
 * A realm that provides access to a hash service compatible with the
 * RegCredentialsMatcher and which can clear the caches for a user id.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class BaseRegRealm extends AuthorizingRealm {
    protected HashService hashService;

    public BaseRegRealm() {
        setPermissionResolver( new RegPermissionResolver() );
        setCredentialsMatcher( new RegCredentialsMatcher() );
        DefaultHashService hashing = new DefaultHashService();
        hashing.setHashAlgorithmName( RegCredentialsMatcher.DEFAULT_ALGORITHM );
        hashing.setHashIterations( RegCredentialsMatcher.DEFAULT_ITERATIONS );
        hashService = hashing;
    }

    public HashService getHashService() {
        return hashService;
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

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(
            PrincipalCollection principals) {
        // Dummy, will be overriden in RegRealm
        return null;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(
            AuthenticationToken token) throws AuthenticationException {
        // Dummy, will be overriden in RegRealm
        return null;
    }

    // Override implementation so that key used for tokens (openid URI) is also
    // used for princpals (UserInfo)
    @Override
    protected Object getAuthenticationCacheKey(PrincipalCollection pc) {
        return ((UserInfo)pc.getPrimaryPrincipal()).getOpenid();
    }
}
