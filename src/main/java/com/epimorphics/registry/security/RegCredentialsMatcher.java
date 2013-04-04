/******************************************************************
 * File:        RegCredentialsMatcher.java
 * Created by:  Dave Reynolds
 * Created on:  4 Apr 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.security;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;

/**
 * Credentials which checks the token to test if it has already
 * been verified using OpenID.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegCredentialsMatcher extends HashedCredentialsMatcher implements CredentialsMatcher {
    public static final String DEFAULT_ALGORITHM = "SHA-512";
    public static final int    DEFAULT_ITERATIONS = 1;

    public RegCredentialsMatcher() {
        setHashAlgorithmName(DEFAULT_ALGORITHM);
        setHashIterations(DEFAULT_ITERATIONS);
    }

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        if (token instanceof RegToken) {
            if (((RegToken)token).isVerified()) return true;
        }
        return super.doCredentialsMatch(token, info);
    }

}
