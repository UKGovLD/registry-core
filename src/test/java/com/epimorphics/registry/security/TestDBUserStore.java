/******************************************************************
 * File:        TestDBUserStore.java
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

import static org.junit.Assert.*;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authc.SaltedAuthenticationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.util.ByteSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.epimorphics.registry.security.BaseUserStore.UserRecord;

public class TestDBUserStore {
    DBUserStore store;

    static final String ALICE_ID = "http://example.com/alice";
    static final String ALICE_NAME = "Alice tester";

    static final String BOB_ID = "http://example.com/bob";
    static final String BOB_NAME = "Bob";

    @Before
    public void setUp() {
        Map<String, String> config = new HashMap<String, String>();
        config.put("dbfile", "memory:test");
        store = new DBUserStore();
        store.init(config, null);
        BaseRegRealm realm = new BaseRegRealm();
        store.setRealm(realm);
    }

    @Test
    public void testStore() throws InterruptedException {
        // Register
        store.register( new UserInfo(ALICE_ID, ALICE_NAME) );
        store.register( new UserInfo(BOB_ID, BOB_NAME) );
        UserRecord record = store.getRecord(ALICE_ID);
        assertEquals(ALICE_NAME, record.name);
        assertNotNull(record.salt);
        assertNull(record.getPasword());

        record = store.getRecord(BOB_ID);
        assertEquals(BOB_NAME, record.name);

        SaltedAuthenticationInfo info = store.checkUser(ALICE_ID);
        assertEquals(ALICE_NAME, ((UserInfo)info.getPrincipals().getPrimaryPrincipal()).getName());

        // Check credentials management
        record = store.getRecord(ALICE_ID);
        record.setPassword(ByteSource.Util.bytes("my password"), 10);
        String expectedPassword = record.password;
        store.setCredentials(ALICE_ID, ByteSource.Util.bytes("my password"), 10);
        record = store.getRecord(ALICE_ID);
        assertNotNull(record.getPasword());
        assertEquals(expectedPassword, record.password);

        store.removeCredentials(ALICE_ID);
        record = store.getRecord(ALICE_ID);
        assertNull(record.getPasword());

        store.setCredentials(ALICE_ID, ByteSource.Util.bytes("my password"), 0);
        Thread.sleep(10);
        info = store.checkUser(ALICE_ID);
        String password = (String) info.getCredentials();
        assertTrue(password == null || password.isEmpty());

        // Check permissions management
        store.addPermision(ALICE_ID, new RegPermission("Update", "/reg2"));
        Set<Permission> permissions = store.getPermissions(ALICE_ID).getObjectPermissions();
        assertEquals(1, permissions.size());
        assertEquals("Update:/reg2", permissions.iterator().next().toString());

        store.addPermision(ALICE_ID, new RegPermission("Register", "/reg1"));
        store.addPermision(ALICE_ID, new RegPermission("Register,StatusUpdate", "/reg2"));
        store.addPermision(BOB_ID, new RegPermission("StatusUpdate", "/reg2"));
        permissions = store.getPermissions(ALICE_ID).getObjectPermissions();
        assertEquals(3, permissions.size());

        List<UserPermission> authusers = store.authorizedOn("/reg2");
        assertEquals(3, authusers.size());
        Collections.sort(authusers, new Comparator<UserPermission>() {
            @Override
            public int compare(UserPermission o1, UserPermission o2) {
                int nameCompare = o1.getUser().getName().compareTo(o2.getUser().getName());
                if (nameCompare == 0) { 
                    return o1.getPermissions().compareTo(o2.getPermissions());
                } else {
                    return nameCompare;
                }
            }
        });
        assertEquals (ALICE_NAME, authusers.get(0).getUser().getName());
        assertEquals (ALICE_NAME, authusers.get(1).getUser().getName());
        assertEquals ("Update", authusers.get(1).getPermissions());
        assertEquals (BOB_NAME, authusers.get(2).getUser().getName());

        store.removePermission(ALICE_ID, "/reg2");
        permissions = store.getPermissions(ALICE_ID).getObjectPermissions();
        assertEquals(1, permissions.size());
        assertEquals("Register:/reg1", permissions.iterator().next().toString());

        store.addPermision(ALICE_ID, new RegPermission("Update", "/reg3/_item"));
        assertEquals(1, store.authorizedOn("/reg3/_item").size());
        assertEquals(1, store.authorizedOn("/reg3/item").size());

        RegAuthorizationInfo auth = store.getPermissions(ALICE_ID);
        Set<String> roles = auth.getRoles();
        assertTrue(roles == null || roles.isEmpty());
        assertEquals(0, store.listAdminUsers().size());
        store.setRole(ALICE_ID, RegAuthorizationInfo.ADMINSTRATOR_ROLE);
        roles = store.getPermissions(ALICE_ID).getRoles();
        assertFalse(roles.isEmpty());
        assertEquals(RegAuthorizationInfo.ADMINSTRATOR_ROLE, roles.iterator().next());
        List<UserInfo> admins = store.listAdminUsers();
        assertEquals(1, admins.size());
        assertEquals(ALICE_ID, admins.get(0).getOpenid());

        // Check listing users
        store.register( new UserInfo("http://example.com/bob2", "Sponge Bob") );
        store.register( new UserInfo("http://example.com/bob3", "Bob Le Ponge") );
        List<UserInfo> bobs = store.listUsers("Bob");
        assertTrue(bobs.size() == 3);
        assertEquals(BOB_NAME, bobs.get(0).getName());
        assertEquals("Sponge Bob", bobs.get(2).getName());

        // Check removal
        store.unregister(ALICE_ID);
        assertNull( store.checkUser(ALICE_ID) );
    }

    @After
    public void tearDown() throws SQLException {
        try {
            DriverManager.getConnection(DBUserStore.protocol + "memory:test;drop=true");
        } catch (SQLException se)  {
            if (( (se.getErrorCode() == 45000)
                    && ("08006".equals(se.getSQLState()) ))) {
                // normal shutdown
            } else {
                System.err.println("Problem during showdown: " + se);
            }
        }
    }

}
