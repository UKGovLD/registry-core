/******************************************************************
 * File:        Registry.java
 * Created by:  Dave Reynolds
 * Created on:  26 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.rdfutil.ModelWrapper;
import com.epimorphics.registry.commands.CommandDelete;
import com.epimorphics.registry.commands.CommandRead;
import com.epimorphics.registry.commands.CommandRegister;
import com.epimorphics.registry.commands.CommandSearch;
import com.epimorphics.registry.commands.CommandStatusUpdate;
import com.epimorphics.registry.commands.CommandUpdate;
import com.epimorphics.registry.commands.CommandValidate;
import com.epimorphics.registry.core.Command.Operation;
import com.epimorphics.registry.store.CachingStore;
import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.server.core.Service;
import com.epimorphics.server.core.ServiceBase;
import com.epimorphics.server.core.ServiceConfig;
import com.epimorphics.server.templates.VelocityRender;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.rdf.model.Model;
import com.sun.jersey.api.uri.UriComponent;

/**
 * This the primary configuration point for the Registry.
 * Configuration bindings available are:
 * <ul>
 *   <li>baseURI - the effective base URI for the registry</li>
 *   <li>bootSpec - location of a bootstrap file defining the root register, and system registers</li>
 *   <li>store - named of a configuration service that provides the StoreAPI implementation in which the registry information is stored</li>
 *   <li>cacheSize - size of register cache to use, if not set then no caching is done, typical value 100</li>
 *   <li>pageSize - size to use for LDP pages, default 50 </li>
 * <ul>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Registry extends ServiceBase implements Service {
    static final Logger log = LoggerFactory.getLogger( Registry.class );

    public static final String VELOCITY_SERVICE = "velocity";

    public static final String BASE_URI_PARAM = "baseURI";
    public static final String SERVICE_NAME = "configuration";
    public static final String BOOT_FILE_PARAM = "bootSpec";
    public static final String STORE_PARAM = "store";
    public static final String CACHE_SIZE_PARAM = "cacheSize";
    public static final String PAGE_SIZE_PARAM = "pageSize";

    public static final int DEFAULT_PAGE_SIZE = 50;

    protected StoreAPI store;
    protected String baseURI;
    protected int pageSize;

    @Override
    public void init(Map<String, String> config, ServletContext context) {
        this.config = config;
        baseURI = getRequiredParam(BASE_URI_PARAM);
        if (baseURI.endsWith("/")) {
            baseURI = baseURI.substring(0, baseURI.length() -1 );
        }
        if (config.containsKey(PAGE_SIZE_PARAM)) {
            pageSize = Integer.parseInt(config.get(PAGE_SIZE_PARAM));
        } else {
            pageSize = DEFAULT_PAGE_SIZE;
        }
    }

    @Override
    public void postInit() {
        try {
            store = getNamedService(getRequiredParam(STORE_PARAM), StoreAPI.class);
            String cacheSizeStr = config.get(CACHE_SIZE_PARAM);
            if (cacheSizeStr != null) {
                int cacheSize = Integer.parseInt(cacheSizeStr);
                store = new CachingStore(store, cacheSize);
            }
        } catch (Exception e) {
            log.error("Misconfigured StoreAPI implementation", e);
        }


        Description root = store.getDescription(getBaseURI() + "/");
        if (root == null) {
            // Blank store, need to install a bootstrap root registers
            for(String bootSrc : getRequiredFileParam(BOOT_FILE_PARAM).split("\\|")) {
                store.loadBootstrap( bootSrc );
            }
            log.info("Installed bootstrap root register");
        }

        VelocityRender velocity = ServiceConfig.get().getServiceAs(Registry.VELOCITY_SERVICE, VelocityRender.class);
        if (velocity != null) {
            velocity.setPrefixes( Prefixes.get() );
        }

        registry = this;   // Assumes singleton registry
    }

    public String getBaseURI() {
        return baseURI;
    }

    public StoreAPI getStore() {
        return store;
    }

    public int getPageSize() {
        return pageSize;
    }

    /**
     * Factory for command instances, used for handling API requests from the request processor
     */
    public Command make(Operation operation, String target,  MultivaluedMap<String, String> parameters) {
        switch (operation) {
        case Read:         return new CommandRead(operation, target, parameters, this);
        case Delete:       return new CommandDelete(operation, target, parameters, this);
        case Register:     return new CommandRegister(operation, target, parameters, this);
        case Update:       return new CommandUpdate(operation, target, parameters, this);
        case StatusUpdate: return new CommandStatusUpdate(operation, target, parameters, this);
        case Validate:     return new CommandValidate(operation, target, parameters, this);
        case Search:       return new CommandSearch(operation, target, parameters, this);
        }
        return null;    // Should never get here but the compiler doesn't seem to know that
    }

    /**
     * Instantiate and invoke commands, used from the ui
     */
    public Response perform(String operation, String uriTarget) {
        Operation op = Operation.valueOf(operation);
        URI uri;
        try {
            uri = new URI(uriTarget);
        } catch (URISyntaxException e) {
            throw new EpiException(e);
        }
        String encPath = uri.getRawPath();
        String target = UriComponent.decode(encPath, UriComponent.Type.PATH);
        String queries = uriTarget;
        int split = queries.indexOf("?");
        if (split != -1) {
            queries = queries.substring(split+1);
        }
        MultivaluedMap<String, String> parameters = UriComponent.decodeQuery(queries, true);
        Command command = make(op, target, parameters);
        try {
            Response response = command.execute();
            if (response.getStatus() == 200 && response.getEntity() instanceof Model) {
                Model m = (Model)response.getEntity();
                m.setNsPrefixes(Prefixes.get());
                return Response.ok( new ModelWrapper( m ) ).build();
            } else {
                return response;
            }
        } catch (WebApplicationException wae) {
                return wae.getResponse();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    static Registry registry;
    public static Registry get() {
        return registry;
    }
}
