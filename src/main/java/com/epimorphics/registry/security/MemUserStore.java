/******************************************************************
 * File:        MemUserStore.java
 * Created by:  Dave Reynolds
 * Created on:  2 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import java.util.HashMap;
import java.util.Map;

import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;

import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;

/**
 * Non-persistent memory implementation of a UserSore.
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class MemUserStore extends ServiceBase implements UserStore, Service {

    Map<String, UserInfo> users = new HashMap<String, UserInfo>();
    Map<String, SimpleAuthorizationInfo> permissions = new HashMap<String, SimpleAuthorizationInfo>();

    @Override
    public void register(UserInfo user) {
        users.put(user.getOpenid(), user);
        permissions.put(user.getOpenid(), new SimpleAuthorizationInfo());
    }

    @Override
    public UserInfo checkUser(String id) {
        return users.get(id);
    }

    @Override
    public AuthorizationInfo getPermissions(String id) {
        return permissions.get(id);
    }

    @Override
    public void addPermision(String id, Permission permission) {
        SimpleAuthorizationInfo auth = permissions.get(id);
        auth.addObjectPermission(permission);
    }

    @Override
    public void removePermission(String id, Permission permission) {
        SimpleAuthorizationInfo auth = permissions.get(id);
        auth.getObjectPermissions().remove(permission);
        // Assumes permissions are singletons
    }


}
