/******************************************************************
 * File:        MemUserStore.java
 * Created by:  Dave Reynolds
 * Created on:  2 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.shiro.authc.SaltedAuthenticationInfo;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.codec.Hex;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.util.EpiException;

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
public class MemUserStore extends ServiceBase implements UserStore, Service {
    static final Logger log = LoggerFactory.getLogger( MemUserStore.class );

    public static final String INITFILE_PARAM = "initfile";

    protected SecureRandomNumberGenerator rand = new SecureRandomNumberGenerator();
    protected RegRealm realm;

    protected Map<String, UserRecord> users = new HashMap<String, UserRecord>();
    protected Map<String, Set<RegPermission>> permissions = new HashMap<String, Set<RegPermission>>();

    protected String initfile = null;

    @Override
    public void init(Map<String, String> config, ServletContext context) {
        super.init(config, context);

        // Load a bootstrap initialization file, used for test purposes only
        initfile = config.get(INITFILE_PARAM);
        if (initfile != null) {
            initfile = ServiceConfig.get().expandFileLocation( initfile) ;
        }
    }

    @Override
    public void setRealm(RegRealm realm) {
        this.realm = realm;
        // Can only initialize the store once we know the realm
        initStore();
    }

    private void initStore() {
        if (initfile == null) return;
        try {
            BufferedReader in = new BufferedReader(new FileReader(initfile));
            String line = null;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("user")) {
                    Matcher patternMatch = USER_LINE_PATTERN.matcher(line);
                    if (patternMatch.matches()) {
                        String id = patternMatch.group(1);
                        UserRecord record = new UserRecord(id, patternMatch.group(2));
                        record.initSalt();
                        record.setPassword( ByteSource.Util.bytes( patternMatch.group(3) ), 60);
                        users.put(id, record);
                    } else {
                        throw new EpiException("Could not parse user declaration: " + line);
                    }
                } else {
                    String[] parts = line.split("\\s+");
                    if (parts.length != 2) {
                        throw new EpiException("Permissions line had wrong number of components: " + line);
                    }
                    String id = parts[0];
                    String perm = parts[1];
                    RegPermission permission = new RegPermission(perm);
                    if (permission.getActions().contains(RegAction.GrantAdmin)) {
                        // Make this user an administrator
                        users.get(id).role = RegAuthorizationInfo.ADMINSTRATOR_ROLE;
                    } else {
                        Set<RegPermission> current = permissions.get(id);
                        if (current == null) {
                            current = new HashSet<RegPermission>();
                            permissions.put(id, current);
                        }
                        current.add( permission );
                    }
                }
            }
            log.info("Load user store from " + initfile);
        } catch (Exception e) {
            log.error("Failed to load UserStore initialization file: " + initfile + " ", e);
        }
    }
    static final Pattern USER_LINE_PATTERN = Pattern.compile("user\\s+([^\\s]+)\\s+\"([^\"]+)\"\\s+([^\\s]+)");

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
