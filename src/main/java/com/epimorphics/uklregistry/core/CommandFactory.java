/******************************************************************
 * File:        CommandFactory.java
 * Created by:  Dave Reynolds
 * Created on:  22 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import javax.ws.rs.core.MultivaluedMap;

import com.epimorphics.uklregistry.core.Command.Operation;
import com.epimorphics.uklregistry.store.StoreAPI;
import com.epimorphics.uklregistry.store.deflt.StoreImpl;

public class CommandFactory {

    public static interface CommandFactoryI {
        public Command make(Operation operation, String target,  MultivaluedMap<String, String> parameters);

        public StoreAPI getStore();
    }

    public static class DefaultImplementation implements CommandFactoryI {
        StoreAPI store;

        public Command make(Operation operation, String target,  MultivaluedMap<String, String> parameters) {
            StoreAPI s = getStore();
            switch (operation) {
            case Read:         return new CommandRead(operation, target, parameters, s);
            case Delete:       return new CommandDelete(operation, target, parameters, s);
            case Register:     return new CommandRegister(operation, target, parameters, s);
            case Update:       return new CommandUpdate(operation, target, parameters, s);
            case StatusUpdate: return new CommandStatusUpdate(operation, target, parameters, s);
            case Validate:     return new CommandValidate(operation, target, parameters, s);
            }
            return null;    // Should never get here but the compiler doesn't seem to know that
        }

        public StoreAPI getStore() {
            if (store == null) {
                store = new StoreImpl();
            }
            return store;
        }
    }

    static CommandFactoryI theFactory = new DefaultImplementation();

    public static CommandFactoryI get() {
        return theFactory;
    }

    public static void set(CommandFactoryI implementation) {
        theFactory = implementation;
    }
}
