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

public class CommandFactory {

    public static interface CommandFactoryI {
        public Command make(Operation operation, String target,  MultivaluedMap<String, String> parameters);
    }

    public static class DefaultImplementation implements CommandFactoryI {
        public Command make(Operation operation, String target,  MultivaluedMap<String, String> parameters) {
            switch (operation) {
            case Read:         return new CommandRead(operation, target, parameters);
            case Delete:       return new CommandDelete(operation, target, parameters);
            case Register:     return new CommandRegister(operation, target, parameters);
            case Update:       return new CommandUpdate(operation, target, parameters);
            case StatusUpdate: return new CommandStatusUpdate(operation, target, parameters);
            }
            return new CommandRead(operation, target, parameters);
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
