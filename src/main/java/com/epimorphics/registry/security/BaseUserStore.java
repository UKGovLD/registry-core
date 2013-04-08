/******************************************************************
 * File:        BaseUserStore.java
 * Created by:  Dave Reynolds
 * Created on:  7 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.shiro.SecurityUtils;
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
 * Support for loading a new store from a boostrap file.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public abstract class BaseUserStore extends ServiceBase implements UserStore, Service {
    static final Logger log = LoggerFactory.getLogger( BaseUserStore.class );

    public static final String INITFILE_PARAM = "initfile";

    protected SecureRandomNumberGenerator rand = new SecureRandomNumberGenerator();
    protected BaseRegRealm realm;
    protected String initfile = null;

    @Override
    public void init(Map<String, String> config, ServletContext context) {
        super.init(config, context);

        initfile = config.get(INITFILE_PARAM);
        if (initfile != null) {
            initfile = ServiceConfig.get().expandFileLocation( initfile) ;
        }
    }

    @Override
    public void setRealm(BaseRegRealm realm) {
        this.realm = realm;
        // Can only initialize the store once we know the realm
        checkStore();
    }

    private void checkStore() {
        if ( !initstore() ) return;
        if (initfile == null) return;
        loadStore();
    }

    /**
     * Test if store is available, if not create a new empty
     * store and return true.
     */
    protected abstract boolean initstore();

    /**
     * Start a transaction if the store supports transactions
     */
    protected abstract void startTransaction();

    /**
     * Commit the transaction if the store supports transactions
     */
    protected abstract void commit();

    /**
     * Return the record for the identified user.
     */
    protected abstract UserRecord getRecord(String id);

    @Override
    public SaltedAuthenticationInfo checkUser(String id) {
        UserRecord record = getRecord(id);
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
                    null,
                    realm.getName());
        }
    }
    
    private void log(String message) {
        try {
            String user = ((UserInfo)SecurityUtils.getSubject().getPrincipal()).getName();
            log.info(user + " " + message);
        } catch (Exception e) {
            log.info("Bootstrap " + message);
        }
    }
        
    private void clearCache(String id) {
        realm.clearCacheFor(id);
    }
    
    @Override
    public boolean register(UserInfo user) {
        boolean success = doRegister(user);
        if (success) {
            log("Registered user " + user.getOpenid() + " (" + user.getName() + ")");
        }
        clearCache(user.getOpenid());
        return success;
    }
    public abstract boolean doRegister(UserInfo user);
    
    @Override
    public void unregister(String id) {
        doUnregister(id);
        clearCache(id);
        log("Removed registration for " + id);
    }
    public abstract void doUnregister(String id);

    @Override
    public void setCredentials(String id, ByteSource credentials, int minstolive) {
        doSetCredentials(id, credentials, minstolive);
        clearCache(id);
        log("Create a password for user " + id);
    }
    public abstract void doSetCredentials(String id, ByteSource credentials, int minstolive);
    
    @Override
    public void removeCredentials(String id) {
        doRemoveCredentials(id);
        clearCache(id);
        log("Cleared password for user " + id);
    }
    public abstract void doRemoveCredentials(String id);

    @Override
    public void addPermision(String id, RegPermission permission) {
        doAddPermision(id, permission);
        clearCache(id);
        log("Added permission " + permission + " for user " + id);
    }
    public abstract void doAddPermision(String id, RegPermission permission);

    @Override
    public void removePermission(String id, String path) {
        doRemovePermission(id, path);
        clearCache(id);
        log("Removed permissions for user " + id + " on path " + path);
    }
    public abstract void doRemovePermission(String id, String path);

    @Override
    public void setRole(String id, String role) {
        doSetRole(id, role);
        clearCache(id);
        log("Set role " + role + " for user " + id);
    }
    
    public abstract void doSetRole(String id, String role);


    private void loadStore() {
        startTransaction();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(initfile));
            String line = null;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("user")) {
                    Matcher patternMatch = USER_LINE_PATTERN.matcher(line);
                    if (patternMatch.matches()) {
                        String id = patternMatch.group(1);
                        UserInfo user = new UserInfo(id, patternMatch.group(2));
                        register(user);
                        String password = patternMatch.group(3);
                        if (password != null && !password.isEmpty()) {
                            setCredentials(id,  ByteSource.Util.bytes(password), 60);
                        }
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
                        setRole(id, RegAuthorizationInfo.ADMINSTRATOR_ROLE);
                    } else {
                        addPermision(id, permission);
                    }
                }
            }
            log.info("Load user store from " + initfile);
        } catch (Exception e) {
            log.error("Failed to load UserStore initialization file: " + initfile + " ", e);
        } finally {
            commit();
            try {
                in.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    static final Pattern USER_LINE_PATTERN = Pattern.compile("user\\s+([^\\s]+)\\s+\"([^\"]+)\"\\s*([^\\s]*)");


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
