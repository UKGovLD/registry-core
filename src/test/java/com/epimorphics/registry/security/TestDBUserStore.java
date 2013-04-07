/******************************************************************
 * File:        TestDBUserStore.java
 * Created by:  Dave Reynolds
 * Created on:  7 Apr 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.shiro.authc.SaltedAuthenticationInfo;
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
    public void testBasicStore() {
        store.register( new UserInfo(ALICE_ID, ALICE_NAME) );
        store.register( new UserInfo(BOB_ID, BOB_NAME) );
        UserRecord record = store.getRecord(ALICE_ID);
        assertEquals(ALICE_NAME, record.name);
        assertNotNull(record.salt);
        record = store.getRecord(BOB_ID);
        assertEquals(BOB_NAME, record.name);
        
        SaltedAuthenticationInfo info = store.checkUser(ALICE_ID);
        assertEquals(ALICE_NAME, ((UserInfo)info.getPrincipals().getPrimaryPrincipal()).getName());
        
        // Check credentials management
        // Check permissions management
        // Check removal
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
