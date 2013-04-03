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

import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;

/**
 * Non-persistent memory implementation of a UserSore.
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class MemUserStore extends ServiceBase implements UserStore, Service {

    Map<String, UserInfo> users = new HashMap<String, UserInfo>();
    Map<String, Set<RegPermission>> permissions = new HashMap<String, Set<RegPermission>>();

    @Override
    public void register(UserInfo user) {
        users.put(user.getOpenid(), user);
        permissions.put(user.getOpenid(), new HashSet<RegPermission>());
    }

    @Override
    public UserInfo checkUser(String id) {
        return users.get(id);
    }

    @Override
    public Set<RegPermission> getPermissions(String id) {
        return permissions.get(id);
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


}
