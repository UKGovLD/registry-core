/******************************************************************
 * File:        Initializer.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.webapi;

import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.uklregistry.core.CommandFactory;
import com.epimorphics.uklregistry.store.Register;

/**
 * Runs useful configuration and set up actions during the post-init start up phase.
 * Current actions are:
 * <ul>
 *   <li>Ensure there is a root register, creating it from a configuration initialization 
 *   file if it does not yet exist.</li>
 * </ul>
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Initializer extends ServiceBase implements Service {
    public static final String ROOT_REGISTER_FILE_PARAM = "rootRegisterSpec";
    
    @Override
    public void postInit() {
        Register root = CommandFactory.get().getStore().getRegister("/");
        if (root == null) {
            // Blank store, need to install a root register
            String rootRegisterSrc = getRequiredFileParam(ROOT_REGISTER_FILE_PARAM);
            // TODO
        }
        
    }

}
