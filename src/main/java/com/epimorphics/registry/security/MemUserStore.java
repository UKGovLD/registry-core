/******************************************************************
 * File:        MemUserStore.java
 * Created by:  Dave Reynolds
 * Created on:  2 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authc.SaltedAuthenticationInfo;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.util.ByteSource;

import com.epimorphics.server.core.Service;

/**
 * Non-persistent memory implementation of a UserSore for testing use.
 * Can initialize this from a file with syntax:
 * <pre>
 * user http://id/user1 "name1"  password1
 * user http://id/user2 "name2"  password2
 *
 * http://id/user1 Manager:/reg1
 * http://id/user2 GrantAdmin
 *
 * </pre>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class MemUserStore extends BaseUserStore implements UserStore, Service {
    protected Map<String, UserRecord> users = new HashMap<String, UserRecord>();
    protected Map<String, Set<RegPermission>> permissions = new HashMap<String, Set<RegPermission>>();

    protected boolean initstore() {
        return true;
    }
    
    protected void startTransaction() {}
    protected void commit() {}

    @Override
    public void register(UserInfo user) {
        UserRecord record = new UserRecord(user.getOpenid(), user.getName());
        record.initSalt();
        users.put(record.id, record);
    }

    @Override
    public SaltedAuthenticationInfo checkUser(String id) {
        UserRecord record = users.get(id);
        if (record == null) {
            return null;
        }
        if (System.currentTimeMillis() < record.timeout) {
            return new SimpleAuthenticationInfo(
                    new UserInfo(record.id, record.name),
                    record.getPasword(),
                    record.getSalt(),
                    realm.getName());
        } else {
            return new SimpleAuthenticationInfo(
                    new UserInfo(record.id, record.name),
                    "",
                    realm.getName());
        }
    }

    @Override
    public RegAuthorizationInfo getPermissions(String id) {
        RegAuthorizationInfo auth = new RegAuthorizationInfo();
        auth.addAllPermissions( permissions.get(id) );
        auth.addAllPermissions( permissions.get(AUTH_USER_ID) );
        auth.addRole( users.get(id).role );
        return auth;
    }

    @Override
    public void addPermision(String id, RegPermission permission) {
        Set<RegPermission> auth = permissions.get(id);
        if (auth == null) {
            auth = new HashSet<RegPermission>();
            permissions.put(id, auth);
        }
        auth.add(permission);
        realm.clearCacheFor(id);
    }

    @Override
    public void removePermission(String id, RegPermission permission) {
        Set<RegPermission> perms = permissions.get(id);
        List<RegPermission> toRemove = new ArrayList<RegPermission>();
        for (RegPermission p : perms) {
            if ( p.getPath().equals(permission.getPath()) ) {
                toRemove.add(p);
            }
        }
        perms.removeAll(toRemove);
        realm.clearCacheFor(id);
    }

    @Override
    public void unregister(String id) {
        users.remove(id);
        realm.clearCacheFor(id);
    }

    @Override
    public void setCredentials(String id, ByteSource credentials, int minstolive) {
        users.get(id).setPassword(credentials, minstolive);
        realm.clearCacheFor(id);
    }

    @Override
    public void removeCredentials(String id) {
        users.get(id).clearPassword();
        realm.clearCacheFor(id);
    }

    @Override
    public void setRole(String id, String role) {
        users.get(id).role = role;
        realm.clearCacheFor(id);
    }
}
