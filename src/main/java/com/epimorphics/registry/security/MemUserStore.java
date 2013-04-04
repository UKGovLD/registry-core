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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authc.SaltedAuthenticationInfo;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.codec.Hex;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.util.ByteSource;

import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;

/**
 * Non-persistent memory implementation of a UserSore.
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class MemUserStore extends ServiceBase implements UserStore, Service {
    protected SecureRandomNumberGenerator rand = new SecureRandomNumberGenerator();
    protected RegRealm realm;

    protected Map<String, UserRecord> users = new HashMap<String, UserRecord>();
    protected Map<String, Set<RegPermission>> permissions = new HashMap<String, Set<RegPermission>>();

    @Override
    public void setRealm(RegRealm realm) {
        this.realm = realm;
    }

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
        auth.add(permission);
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
    }

    @Override
    public void unregister(String id) {
        users.remove(id);
    }

    @Override
    public void setCredentials(String id, ByteSource credentials, int minstolive) {
        users.get(id).setPassword(credentials, minstolive);
    }

    @Override
    public void removeCredentials(String id) {
        users.get(id).clearPassword();
    }

    @Override
    public void setRole(String id, String role) {
        users.get(id).role = role;
    }


    class UserRecord {
        protected String id;
        protected String name;
        protected String salt;
        protected String password;
        protected long timeout;
        protected String role;

        public UserRecord(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public void initSalt() {
            salt = rand.nextBytes().toHex();
        }

        public ByteSource getPasword() {
            if (password != null) {
                return ByteSource.Util.bytes( Hex.decode(password) );
            } else {
                return null;
            }
        }

        public ByteSource getSalt() {
            return ByteSource.Util.bytes( Hex.decode(salt) );
        }

        public void setPassword(ByteSource password, long minstolive) {
            timeout = System.currentTimeMillis() + minstolive * 60 * 1000;
            HashRequest request = new HashRequest.Builder()
                .setSource(password)
                .setSalt( getSalt() )
                .build();
            Hash hash = realm.getHashService().computeHash(request);
            this.password = hash.toHex();
        }

        public void clearPassword() {
            password = null;
            timeout = 0;
        }
    }
}
