/******************************************************************
 * File:        RegAction.java
 * Created by:  Dave Reynolds
 * Created on:  3 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

/**
 * Set of actions that can be performed on the registry and are
 * subject to authorization (and thus permissions checking).
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public enum RegAction {

    Register,
    Update,
    StatusUpdate,
    Force,
    Grant,
    GrantAdmin,
    WildCard;

    public static RegAction forString(String param) {
        if (param.equals("*")) {
            return WildCard;
        }
        for (RegAction s : RegAction.values()) {
            if (s.name().equalsIgnoreCase(param)) {
                return s;
            }
        }
        return null;
    }

}
