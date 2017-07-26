/******************************************************************
 * File:        RegisterCache.java
 * Created by:  Dave Reynolds
 * Created on:  26 Jul 2017
 * 
 * (c) Copyright 2017, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.message.MessagingService;

/**
 * Optional cache of memership lists of registers.
 * Enabled through registry setting cacheRegisters.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class RegisterCache {
    static final Logger log = LoggerFactory.getLogger( RegisterCache.class );

    public static final int MAX_REGISTER_CACHE = 100;
    
    protected static LRUMap cache = new LRUMap(MAX_REGISTER_CACHE);
    protected static boolean listeningForChanges = false;
    
    public static synchronized Register getRegister(Resource root) {
        if ( Registry.get().isCacheRegisters() ) {
            return get( root );
        } else {
            return new Register(root);
        }
    }
    
    public static synchronized Register getRegister(Description d) {
        return getRegister( d.getRoot() );
    }
    
    private static Register get(Resource root) {
        Register register = (Register) cache.get( root.getURI() );
        if (register == null) {
            register = new Register(root);
            cache.put(root.getURI(), register);
            log.info("Caching register: " + root.getURI());
            listenForChanges();
        }
        return register;
    }
    
    private static synchronized void clear() {
        log.info("Clearing register cache");
        cache.clear();
    }
    
    private static void listenForChanges() {
        if (!listeningForChanges) {
            MessagingService.Process reset = new MessagingService.Process(){
                @Override
                public void processMessage(Message message) {
                    clear();
                }
            };
            Registry.get().getMessagingService().processMessages( reset );
            listeningForChanges = true;
        }
    }
}


