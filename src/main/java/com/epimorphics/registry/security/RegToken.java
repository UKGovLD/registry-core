/******************************************************************
 * File:        RegToken.java
 * Created by:  Dave Reynolds
 * Created on:  4 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;

/**
 * Authentication tokens used for the RegRealm. Allows for a
 * "verified" flag to enable the OpenID authentication to create
 * pre-verified tokens. An OpenID token will have empty password
 * credentials but isVerified will return true. A password
 * token used for API access will have password credentials but
 * will not be verified. In all cases the "username" will be
 * the OpenID identifier for the subject.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegToken extends UsernamePasswordToken implements AuthenticationToken {
    private static final long serialVersionUID = 797348172301182843L;

    protected boolean verified;

    /**
     * Construct an OpenID token with an empty password.
     */
    public RegToken(String id, boolean isVerified) {
        super(id, "");
        verified = isVerified;
    }

    /**
     * Construct a token with password-style credentials
     */
    public RegToken(String id, String password) {
        super(id, password);
        verified = false;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

}
