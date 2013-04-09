/******************************************************************
 * File:        RegAuthorizationInfo.java
 * Created by:  Dave Reynolds
 * Created on:  3 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;

import com.hp.hpl.jena.util.OneToManyMap;

/**
 * Representation of all the authorization information applicable
 * to a single user.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegAuthorizationInfo extends SimpleAuthorizationInfo implements AuthorizationInfo {
    private static final long serialVersionUID = 4749324135004512678L;

    public static final String ADMINSTRATOR_ROLE = "administrator";

    protected OneToManyMap<String, RegPermission> index;

    protected OneToManyMap<String, RegPermission> getIndex() {
        if (index == null) {
            index = new OneToManyMap<String, RegPermission>();
            Set<Permission> perms = getObjectPermissions();
            if (perms != null) {
                for (Permission p : perms) {
                    RegPermission rp = (RegPermission)p;
                    index.put(rp.getPath(), rp);
                    if (rp.getImpliedPath() != null) {
                        index.put(rp.getImpliedPath(), rp);
                    }
                }
            }
        }
        return index;
    }

    public void addAllPermissions(Collection<RegPermission> perms) {
        if (perms == null) return;
        for (RegPermission p : perms) {
            addObjectPermission(p);
        }
    }

    public boolean permits(RegPermission request) {
        if (getRoles() != null && getRoles().contains(ADMINSTRATOR_ROLE)) return true;

        String path = request.getPath();
        RegPermission residual = request;
        int split = -1;
        while (true) {
            split = path.indexOf('/', split+1);
            if (split == -1) break;
            if (split == 0) {
                // Special case start of path is "/" instead of ""
                residual = residualFromPath("/", residual);
            } else {
                residual = residualFromPath(path.substring(0, split), residual);
            }
            if (residual == null) {
                return true;
            }
        }
        residual = residualFromPath(path, residual);
        return residual == null;
    }

    protected RegPermission residualFromPath(String path, RegPermission request) {
        RegPermission residual = request;
        Iterator<RegPermission> i = getIndex().getAll(path);
        while (i.hasNext()) {
            RegPermission grants = i.next();
            residual = grants.residual(residual);

        }
        return residual;
    }
}
