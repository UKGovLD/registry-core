/******************************************************************
 * File:        DBUserStore.java
 * Created by:  Dave Reynolds
 * Created on:  7 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.server.core.Service;
import com.hp.hpl.jena.util.FileManager;

public class DBUserStore extends BaseUserStore implements UserStore, Service {
    static final Logger log = LoggerFactory.getLogger( DBUserStore.class );

    public static final String DATABASE_PARAM = "dbfile";
    public static final String DATABASE_SCHEMA = "userdbinit.sql";

    protected static final String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    protected static final String protocol = "jdbc:derby:";

    protected Connection conn;

    @Override
    public void init(Map<String, String> config, ServletContext context) {
        super.init(config, context);
        try {
            Class.forName(driver).newInstance();
            String dbName = getRequiredParam(DATABASE_PARAM);
            conn = DriverManager.getConnection(protocol + dbName + ";create=true");
        } catch (Exception e) {
            log.error("*** Security configuration error ***", e);
        }
    }

    protected boolean initstore() {
        if (conn != null) {
            try {
                ResultSet tables = conn.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
                boolean exists = tables.next();
                tables.close();

                if (!exists) {
                    startTransaction();
                    String schema  = FileManager.get().readWholeFileAsUTF8(DATABASE_SCHEMA);
                    Statement s = conn.createStatement();
                    for (String statement : schema.split(";")) {
                        String sql = statement.trim();
                        if (!sql.isEmpty() && ! sql.startsWith("--")) {
                            s.execute(statement);
                        }
                    }
                    commit();
                }
                return ! exists;
            } catch (Exception e) {
                log.error("Failed to access security database", e);
            }
        }
        return false;
    }

    protected void startTransaction() {
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            log.error("Failed to access security database", e);
        }
    }

    protected void commit() {
        try {
            conn.commit();
        } catch (SQLException e) {
            log.error("Failed to access security database", e);
        }
    }

    @Override
    public void register(UserInfo user) {
        try {
            UserRecord record = new UserRecord(user.getOpenid(), user.getName());
            record.initSalt();
            PreparedStatement ps = conn.prepareStatement("INSERT INTO USERS VALUES (?, ?, ?, ?, ?, ?)");
            ps.setString(1, record.id);
            ps.setString(2, record.name);
            ps.setString(3, record.salt);
            ps.setNull(4, Types.VARCHAR);
            ps.setNull(5, Types.BIGINT);
            ps.setNull(6, Types.VARCHAR);
            ps.executeUpdate();
            commit();
        } catch (Exception e) {
            log.error("Failed to access security database", e);
        }
    }

    protected UserRecord getRecord(String id) {
        try {
            PreparedStatement s = conn.prepareStatement("SELECT NAME, SALT, PASSWORD, TIMEOUT, ROLE FROM USERS WHERE ID=?");
            s.setString(1, id);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                UserRecord record = new UserRecord(id, rs.getString(1));
                record.salt = rs.getString(2);
                record.password = rs.getString(3);
                record.timeout = rs.getLong(4);
                record.role = rs.getString(5);
                return record;
            }
        } catch (Exception e) {
            log.error("Failed to access security database", e);
        }
        return null;
    }

    protected Set<RegPermission> permissionsFor(String id) {
        try {
            PreparedStatement s = conn.prepareStatement("SELECT ACTION, PATH FROM PERMISSIONS WHERE ID=?");
            s.setString(1, id);
            Set<RegPermission> permissions = new HashSet<RegPermission>();
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                permissions.add( new RegPermission(rs.getString(1), rs.getString(2)) );
            }
            return permissions;
        } catch (Exception e) {
            log.error("Failed to access security database", e);
            return null;
        }
    }

    @Override
    public RegAuthorizationInfo getPermissions(String id) {
        RegAuthorizationInfo auth = new RegAuthorizationInfo();
        auth.addAllPermissions( permissionsFor(id) );
        auth.addAllPermissions( permissionsFor(AUTH_USER_ID) );
        String role = getRecord(id).role;
        if (role != null) {
            auth.addRole( role );
        }
        return auth;
    }

    @Override
    public void addPermision(String id, RegPermission permission) {
        try {
            PreparedStatement s = conn.prepareStatement("INSERT INTO PERMISSIONS VALUES (?, ?, ?)");
            s.setString(1, id);
            s.setString(2, permission.getActionString());
            s.setString(3, permission.getPath());
            s.executeUpdate();
            commit();
            realm.clearCacheFor(id);
        } catch (Exception e) {
            log.error("Failed to access security database", e);
        }
    }

    @Override
    public void removePermission(String id, String path) {
        try {
            PreparedStatement s = conn.prepareStatement("DELETE FROM PERMISSIONS WHERE ID=? AND PATH=?");
            s.setString(1, id);
            s.setString(2, path);
            s.executeUpdate();
            commit();
            realm.clearCacheFor(id);
        } catch (Exception e) {
            log.error("Failed to access security database", e);
        }
    }

    @Override
    public void unregister(String id) {
        try {
            PreparedStatement s = conn.prepareStatement("DELETE FROM USERS WHERE ID=?");
            s.setString(1, id);
            s.executeUpdate();
            commit();
        } catch (Exception e) {
            log.error("Failed to access security database", e);
        }
        realm.clearCacheFor(id);
    }

    @Override
    public void setCredentials(String id, ByteSource credentials, int minstolive) {
        try {
            UserRecord record = getRecord(id);
            record.setPassword(credentials, minstolive);

            PreparedStatement s = conn.prepareStatement("UPDATE USERS SET PASSWORD=?, TIMEOUT=? WHERE ID=?");
            s.setString(3, id);
            s.setString(1, record.password);
            s.setLong(2, record.timeout);
            s.executeUpdate();
            commit();
        } catch (Exception e) {
            log.error("Failed to access security database", e);
        }
        realm.clearCacheFor(id);
    }

    @Override
    public void removeCredentials(String id) {
        try {
            PreparedStatement s = conn.prepareStatement("UPDATE USERS SET PASSWORD=? WHERE ID=?");
            s.setString(2, id);
            s.setNull(1, Types.VARCHAR);
            s.executeUpdate();
            commit();
        } catch (Exception e) {
            log.error("Failed to access security database", e);
        }
        realm.clearCacheFor(id);
    }

    @Override
    public void setRole(String id, String role) {
        try {
            PreparedStatement s = conn.prepareStatement("UPDATE USERS SET ROLE=? WHERE ID=?");
            s.setString(2, id);
            if (role == null) {
                s.setNull(1, Types.VARCHAR);
            } else {
                s.setString(1, role);
            }
            s.executeUpdate();
            commit();
        } catch (Exception e) {
            log.error("Failed to access security database", e);
        }
        realm.clearCacheFor(id);
    }

}