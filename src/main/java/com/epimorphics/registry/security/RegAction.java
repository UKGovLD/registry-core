/******************************************************************
 * File:        RegAction.java
 * Created by:  Dave Reynolds
 * Created on:  3 Apr 2013
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
    RealDelete,
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
