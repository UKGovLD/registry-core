/******************************************************************
 * File:        LibReg.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.webapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;

import com.epimorphics.rdfutil.ModelWrapper;
import com.epimorphics.rdfutil.RDFNodeWrapper;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.security.RegAuthorizationInfo;
import com.epimorphics.registry.security.RegPermission;
import com.epimorphics.registry.security.UserInfo;
import com.epimorphics.registry.store.RegisterEntryInfo;
import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.templates.LibPlugin;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;

/**
 * Some supporting methods to help Velocity UI access the registry store.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class LibReg extends ServiceBase implements LibPlugin, Service {

    /**
     * Raw access to the registry store
     */
    public StoreAPI getStore() {
        return Registry.get().getStore();
    }

    /**
     * Return a resource known to the store, wrapped for scripting
     */
    public RDFNodeWrapper getResource(String uri) {
        if ( ! uri.startsWith("http") ) {
            uri = Registry.get().getBaseURI() + uri;
        }
        Description d = getStore().getCurrentVersion(uri);
        if (d == null) {
            return null;
        }
        return wrapNode( d.getRoot() );
    }

    private ModelWrapper wrapModel(Model m) {
        m.setNsPrefixes( Prefixes.get() );
        return new ModelWrapper( m );
    }

    private RDFNodeWrapper wrapNode(Resource root) {
        return wrapModel( root.getModel() ).getNode(root);
    }

    /**
     * Helper to list members of a register
     */
    public List<RegisterEntryInfo> listMembers(Object arg) {
        Register reg = null;;
        if (arg instanceof String) {
            reg = getStore().getCurrentVersion((String)arg).asRegister();
        } else if (arg instanceof RDFNodeWrapper) {
            reg = new Register( ((RDFNodeWrapper)arg).asResource() );
        } else if (arg instanceof Register) {
            reg = (Register) arg;
        } else {
            return null;
        }
        return getStore().listMembers(reg);
    }

    /**
     * Convert a resource, maybe wrapped, to a status code
     */
    public Status asStatus(Object state) {
        if (state instanceof Status) {
            return (Status)state;
        } else if (state instanceof Resource) {
            return Status.forResource((Resource)state);
        } else if (state instanceof RDFNodeWrapper) {
            return Status.forResource(((RDFNodeWrapper)state).asResource());
        } else {
            return null;
        }
    }

    /**
     * List the legal next states after this state.
     */
    public List<Status> nextStates(RDFNodeWrapper state) {
        Status current = asStatus(state);
        if (current == null) return new ArrayList<Status>();
        List<Status> next = current.nextStates();
        next.remove(current);
        return next;
    }

    /**
     * Check if the given action(s) are permitted on the given URI for the current subject
     */
    public boolean isPermitted(String action, String uri) {
        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            return false;
        }
        return subject.isPermitted(action + ":/" + uri);
    }

    /**
     * Return the subjected (logged in user if any).
     * Needed for simple UI pages that aren't rendered as part of visiting the registry body
     */
    public Subject getSubject() {
        try {
            return SecurityUtils.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * List permissions for current authenticated user (if any), ordered by path
     */
    public List<RegPermission> getPermissions() {
        Subject subject = getSubject();
        if (subject.isAuthenticated()) {
            String id = ((UserInfo)subject.getPrincipal()).getOpenid();
            RegAuthorizationInfo auth = Registry.get().getUserStore().getPermissions(id);
            List<RegPermission> perms = new ArrayList<RegPermission>( );
            if (auth.getObjectPermissions() != null) {
                for (Permission p : auth.getObjectPermissions()) {
                    perms.add( (RegPermission) p);
                }
                Collections.sort(perms, new Comparator<RegPermission>(){
                    @Override
                    public int compare(RegPermission arg0, RegPermission arg1) {
                        return arg0.getPath().compareTo(arg1.getPath());
                    }
                });
            }
            return perms;
        } else {
            return new ArrayList<RegPermission>();
        }
    }

    /**
     * Return string giving the Turtle prefixes header for a model/node
     */
    public String turtlePrefixes(Object obj) {
        PrefixMapping prefixes;
        if (obj instanceof ModelWrapper) {
            prefixes = ((ModelWrapper)obj).getPrefixes();
        } else if (obj instanceof RDFNodeWrapper) {
            prefixes = ((RDFNodeWrapper)obj).getModelW().getPrefixes();
        } else if (obj instanceof Model) {
            prefixes = (Model)obj;
        } else {
            throw new EpiException("Not a type with prefixes: " + obj);
        }
        StringBuffer result = new StringBuffer();
        for (Map.Entry<String, String> mapping : prefixes.getNsPrefixMap().entrySet()) {
            result.append(String.format("@prefix %s: <%s>. \n", mapping.getKey(), mapping.getValue()));
        }
        return result.toString();
    }
    
    /**
     * Convert the URI for a managed entity or an item to a path relative to the registry base
     */
    public String pathFor(String uri) {
        String base = Registry.get().getBaseURI();
        if (uri.startsWith(base)) {
            return uri.substring(base.length() + 1);
        }
        return uri;
    }
    
    /**
     * Utility for incrementally building up compacted range notation
     * for reserved entries
     */
    public ReservationList addReserved(ReservationList reservations, RDFNode notation) {
        reservations.add(notation);
        return reservations;
    }

    public ReservationList startReservationList() {
        return new ReservationList();
    }

    public static class ReservationList {
        StringBuffer reservations = new StringBuffer();
        int last;
        int rangeStart;
        boolean pendingNumeric = false;
        boolean inRange = false;

        public void add(RDFNode notation) {
            Literal l = notation.asLiteral();
            Object value = l.getValue();
            if (value instanceof Integer) {
                int n = ((Integer) value).intValue();
                if (pendingNumeric) {
                    if (n == last + 1) {
                        if (!inRange) {
                            rangeStart = last;
                            inRange = true;
                        }
                    } else {
                        finishNumeric();
                    }
                }
                last = n;
                pendingNumeric = true;
            } else {
                finishNumeric();
                if (reservations.length() != 0) {
                    reservations.append(", ");
                }
                reservations.append(l.getLexicalForm());
            }
        }

        private void finishNumeric() {
            if (pendingNumeric) {
                if (reservations.length() != 0) {
                    reservations.append(", ");
                }
                if (inRange) {
                    reservations.append(rangeStart);
                    reservations.append("-");
                }
                reservations.append(last);
                inRange = false;
                pendingNumeric = false;
            }
        }

        public String getReservations() {
            if (pendingNumeric) {
                finishNumeric();
            }
            return reservations.toString();
        }

        public boolean isEmpty() {
            return reservations.length() == 0 && !pendingNumeric;
        }

        @Override
        public String toString() {
            return getReservations();
        }
    }

}
