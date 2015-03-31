/******************************************************************
 * File:        RegRealm.java
 * Created by:  Dave Reynolds
 * Created on:  2 Apr 2013
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
import org.apache.shiro.subject.PrincipalCollection;

import com.epimorphics.util.EpiException;

/**
 * A Shiro Realm designed to support the sort of authentication and
 * authorization we need for the Registry security model.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegRealm extends BaseRegRealm {
    protected UserStore userstore;
   
    /**
     * Configure the user store for this realm
     */
    public void setUserStore(UserStore store) {
        userstore = store;
        store.setRealm(this);
    }
 
    public UserStore getUserStore() {
        return userstore;
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
