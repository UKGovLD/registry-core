/******************************************************************
 * File:        BaseRegRealm.java
 * Created by:  Dave Reynolds
 * Created on:  7 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.cache.Cache;
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
        if (id.equals(UserStore.AUTH_USER_ID)) {
            // For the anonymous user have to lose whole cache because this affects every user
            Cache<Object, AuthorizationInfo> cache = getAuthorizationCache();
            if (cache != null) {
                cache.clear();
            }
        }
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
