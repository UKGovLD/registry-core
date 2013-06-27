/******************************************************************
 * File:        RegCredentialsMatcher.java
 * Created by:  Dave Reynolds
 * Created on:  4 Apr 2013
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
