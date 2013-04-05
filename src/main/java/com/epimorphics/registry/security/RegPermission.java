/******************************************************************
 * File:        RegPermission.java
 * Created by:  Dave Reynolds
 * Created on:  3 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authz.Permission;

import com.epimorphics.util.EpiException;
import static com.epimorphics.registry.security.RegAction.*;

/**
 * Represents registry permission structure. The basic string form is:
 * <pre>
 *   action:/path
 * </pre>
 * Where the <em>action</em> can be a comma separated list of actions and the
 * <em>path</em> is register or register item relative to the root of the registry.
 * Permissions inherit down the path structure so that some granted:
 * <pre>
 *   update:/def/reg
 * </pre>
 * is also implicitly granted:
 * <pre>
 *   update:/def/reg/sub
 * </pre>
 * <p>
 * The set of actions is constrained to match a {@link RegAction}.
 * </p>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegPermission implements Permission {

    protected Set<RegAction> actions;
    protected String path = "";

    protected static Map<String, RegAction[]> roleAliases = new HashMap<String, RegAction[]>();
    static {
        roleAliases.put("Manager", new RegAction[]{Register, Update, StatusUpdate, Grant});
        roleAliases.put("Maintainer", new RegAction[]{Update, Grant});
        roleAliases.put("Authorized", new RegAction[]{Register, Update, StatusUpdate});
    }

    public RegPermission(String permission) {
        int split = permission.indexOf(':');
        if (split != -1) {
            actions = parseActions( permission.substring(0, split) );
            path = permission.substring(split+1).trim();
        } else {
            actions = parseActions(permission);
        }
    }

    public RegPermission(String actions, String path) {
        this(parseActions(actions), path);
    }

    public RegPermission(RegAction action, String path) {
        this(singleAction(action), path);
    }

    public RegPermission(Set<RegAction> actions, String path) {
        this.actions = actions;
        this.path = path.trim();
    }

    private static Set<RegAction> singleAction(RegAction action) {
        Set<RegAction> actions = new HashSet<RegAction>(1);
        actions.add(action);
        return actions;
    }

    private static Set<RegAction> parseActions(String actionsStr) {
        if (actionsStr.trim().equals("*")) {
            return singleAction(RegAction.WildCard);
        }
        Set<RegAction> actions = new HashSet<RegAction>(5);
        for (String actionStr : actionsStr.split(",")) {
            if (roleAliases.containsKey(actionStr)) {
                for (RegAction action : roleAliases.get(actionStr)) {
                    actions.add(action);
                }
            } else {
                RegAction action = RegAction.forString(actionStr.trim());
                if (action != null) {
                    actions.add(action);
                } else {
                    throw new EpiException("Illegal action in permissions string: " + actionStr);
                }
            }
        }
        return actions;
    }


    public Set<RegAction> getActions() {
        return actions;
    }

    public void setActions(Set<RegAction> actions) {
        this.actions = actions;
    }

    public void addAction(RegAction action) {
        this.actions.add(action);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean samePath(RegPermission other) {
        return path.equals(other.getPath());
    }

    /**
     * Returns the part of p which is not covered by this permission.
     * If there is no match then this is simply p itself.
     * If p is fully granted then returns null.
     * Otherwise returns a new target permission that has the same path
     * but a reduced set of actions
     */
    public RegPermission residual(RegPermission p) {
        if ( ! p.path.startsWith(path) ) return p;

        if (actions.contains(RegAction.WildCard) || actions.containsAll(p.actions)) {
            return null;
        }

        Set<RegAction> residualActions = new HashSet<RegAction>( p.actions );
        residualActions.removeAll(actions);
        return new RegPermission(residualActions, p.path);
    }

    @Override
    public boolean implies(Permission p) {
        if ( ! (p instanceof RegPermission) ) return false;
        RegPermission other = (RegPermission)p;

        if (actions.contains(RegAction.WildCard) || actions.containsAll(other.actions)) {
            if (other.path.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        boolean started = false;
        for (RegAction action : actions) {
            if (started) {
                buff.append(",");
            } else {
                started = true;
            }
            buff.append(action.name());
        }
        buff.append(":");
        buff.append(path);
        return buff.toString();
    }
}
