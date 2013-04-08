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
    public boolean register(UserInfo user) {
        if (users.containsKey(user.getOpenid())) {
            return false;
        }
        UserRecord record = new UserRecord(user.getOpenid(), user.getName());
        record.initSalt();
        users.put(record.id, record);
        return true;
    }

    @Override
    protected UserRecord getRecord(String id) {
        return users.get(id);
    }

    @Override
    public RegAuthorizationInfo getPermissions(String id) {
        RegAuthorizationInfo auth = new RegAuthorizationInfo();
        auth.addAllPermissions( permissions.get(id) );
        auth.addAllPermissions( permissions.get(AUTH_USER_ID) );
        String role = getRecord(id).role;
        if (role != null) {
            auth.addRole( role );
        }
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
    public void removePermission(String id, String path) {
        Set<RegPermission> perms = permissions.get(id);
        List<RegPermission> toRemove = new ArrayList<RegPermission>();
        for (RegPermission p : perms) {
            if ( p.getPath().equals(path) ) {
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

    @Override
    public List<UserInfo> authorizedOn(String path) {
        List<UserInfo> matches = new ArrayList<UserInfo>();
        for (String id : permissions.keySet()) {
            Set<RegPermission> perms = permissions.get(id);
            for (RegPermission p : perms) {
                if (p.getPath().equals(path) || p.getImpliedPath().equals(path)) {
                    matches.add( new UserInfo(id, users.get(id).name) );
                }
            }
        }
        return matches;
    }

    @Override
    public List<UserInfo> listUsers(String match) {
        List<UserInfo> matches = new ArrayList<UserInfo>();
        for (UserRecord record : users.values()) {
            if (record.name.contains(match)) {
                matches.add( new UserInfo(record.id, record.name) );
            }
        }
        return matches;
    }

}
