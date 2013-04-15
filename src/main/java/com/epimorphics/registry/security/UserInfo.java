/******************************************************************
 * File:        UserInfo.java
 * Created by:  Dave Reynolds
 * Created on:  2 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import java.io.Serializable;

/**
 * Represents the primary information we know about a registered user.
 * Used as the "principal" for Shiro identity tracking.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class UserInfo implements Serializable {
    private static final long serialVersionUID = -4661235310974850596L;

    protected String openid;
    protected String name;

    public UserInfo(String openid, String name) {
        this.openid = openid;
        this.name = name;
    }

    /** The user's openID identifier URI */
    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    /** A human readable name for user, or a local alias */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof UserInfo) {
            return openid.equals( ((UserInfo)other).openid );
        }
        return false;
    }

    @Override
    public int hashCode() {
        return openid.hashCode();
    }

    @Override
    public String toString() {
        return String.format("User[%s,%s]", name, openid);
    }

}
