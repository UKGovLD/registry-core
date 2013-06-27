/******************************************************************
 * File:        UserPermission.java
 * Created by:  Dave Reynolds
 * Created on:  21 Apr 2013
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

/**
 * Struct used to report the permissions that a user has over some path.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class UserPermission {

    protected UserInfo user;
    protected String permissions;
    
    public UserPermission(UserInfo user, String permissions) {
        this.user = user;
        this.permissions = permissions;
    }

    public UserInfo getUser() {
        return user;
    }

    public String getPermissions() {
        return permissions;
    }
}
