/******************************************************************
 * File:        Command.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.uklregistry.core;

import java.util.Map;

/**
 * Wraps up a registry request as a command object to modularize
 * processing such as authorization and audit trails.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Command {

    enum Operation {
        read_register,
        create_register,
        update_register_metadata,
        delete_register,
        read_entity,
        register_entity,
        register_batch,
        update_entity,
        update_item_metadata,
        change_status,
        delete_entity,
        validate
    };

    Operation operation;
    String register;        // relative to assumed base
    String item;            // relative to register
    String entity;
    Map<String, String[]> parameters;  // Query parameters

    // TODO - constructor and accessors
}
