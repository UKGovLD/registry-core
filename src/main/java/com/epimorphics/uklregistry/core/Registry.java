/******************************************************************
 * File:        Registry.java
 * Created by:  Dave Reynolds
 * Created on:  26 Jan 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.uklregistry.commands.CommandDelete;
import com.epimorphics.uklregistry.commands.CommandRead;
import com.epimorphics.uklregistry.commands.CommandRegister;
import com.epimorphics.uklregistry.commands.CommandStatusUpdate;
import com.epimorphics.uklregistry.commands.CommandUpdate;
import com.epimorphics.uklregistry.commands.CommandValidate;
import com.epimorphics.uklregistry.core.Command.Operation;
import com.epimorphics.uklregistry.store.StoreAPI;

/**
 * This the primary configuration point for the Registry.
 * Configuration bindings available are:
 * <ul>
 *   <li>baseURI - the effective base URI for the registry</li>
 *   <li>bootSpec - location of a bootstrap file defining the root register, and system registers</li>
 *   <li>store - named of a configuration service that provides the StoreAPI implementation in which the registry information is stored</li>
 * <ul>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Registry extends ServiceBase implements Service {
    static final Logger log = LoggerFactory.getLogger( Registry.class );

    public static final String BASE_URI_PARAM = "baseURI";
    public static final String SERVICE_NAME = "configuration";
    public static final String BOOT_FILE_PARAM = "bootSpec";
    public static final String STORE_PARAM = "store";

    protected StoreAPI store;
    protected String baseURI;
    
    @Override
    public void init(Map<String, String> config, ServletContext context) {
        this.config = config;
        baseURI = getRequiredFileParam(BASE_URI_PARAM);
    }

    @Override
    public void postInit() {
        try {
            store = (StoreAPI) ServiceConfig.get().getService( getRequiredParam(STORE_PARAM) );
        } catch (Exception e) {
            log.error("Misconfigured StoreAPI implementation");
        }
        

        Description root = store.getDescription(getBaseURI() + "/", false);
        if (root == null) {
            // Blank store, need to install a bootstrap root registers
            for(String bootSrc : getRequiredFileParam(BOOT_FILE_PARAM).split("\\|")) {
                store.loadBootstrap( bootSrc );
            }
            log.info("Installed bootstrap root register");
        }
        
        registry = this;   // Assumes singleton registry
    }
    
    public String getBaseURI() {
        return baseURI;
    }

    public Command make(Operation operation, String target,  MultivaluedMap<String, String> parameters) {
        switch (operation) {
        case Read:         return new CommandRead(operation, target, parameters, this);
        case Delete:       return new CommandDelete(operation, target, parameters, this);
        case Register:     return new CommandRegister(operation, target, parameters, this);
        case Update:       return new CommandUpdate(operation, target, parameters, this);
        case StatusUpdate: return new CommandStatusUpdate(operation, target, parameters, this);
        case Validate:     return new CommandValidate(operation, target, parameters, this);
        }
        return null;    // Should never get here but the compiler doesn't seem to know that
    }

    static Registry registry;
    public static Registry get() {
        return registry;
    }
}
