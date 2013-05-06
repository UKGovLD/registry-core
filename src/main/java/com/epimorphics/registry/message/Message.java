/******************************************************************
 * File:        Message.java
 * Created by:  Dave Reynolds
 * Created on:  6 May 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.message;

import java.util.List;

import com.epimorphics.registry.commands.CommandStatusUpdate;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.RegisterItem;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Format for notification messages. These are used to allow for processing or
 * logging to be trigger by registration/change events. A variety of
 * message infrastructures may be used including MQ style.
 * <p>
 * For a status update the message will be the new status.
 * </p><p>
 * For an update the target will be the resource updated and the payload will be
 * the post-update description of the resource (i.e. after any patch processing).
 * </p><p>
 * For a registration the target will be the item registered.
 * </p>
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Message {

    String target;
    String operation;
    String[] types;
    Object message;
    
    /**
     * Create a empty message
     */
    public Message() {
    }

    
    public void setTarget(String target) {
        this.target = target;
    }


    public void setOperation(String operation) {
        this.operation = operation;
    }


    public void setTypes(String[] types) {
        this.types = types;
    }


    public void setMessage(Object message) {
        this.message = message;
    }


    /**
     * Create a message corresponding to a command.
     * If the command is a StatusUpdate then the message body will be set to the new status.
     */
    public Message(Command command) {
        target = command.getTarget();
        operation = command.getOperation().name();
        types = new String[0];
        if (command instanceof CommandStatusUpdate) {
            message = ((CommandStatusUpdate)command).getRequestedStatus();
        }
    }
    
    /**
     * Create a message corresponding to a command with a model payload.
     */
    public Message(Command command, Model model) {
        this(command);
        message = model;
    }
    
    /**
     * Create a message corresponding to the registration of an item
     */
    public Message(Command command, RegisterItem item) {
        this(command);
        Resource itemroot = item.getRoot();
        target = itemroot.getURI();
        message = itemroot.getModel();
        Resource entity = item.getEntity();
        if (entity != null) {
            setTypes(entity);
        }
    }

    /**
     * Record the types of the created/updated entity.
     */
    public void setTypes(Resource entity) {
        List<RDFNode> tys = entity.getModel().listObjectsOfProperty(entity, RDF.type).toList();
        types = new String[ tys.size() ];
        for (int i = 0; i < types.length; i++) {
            RDFNode ty = tys.get(i);
            if (ty.isURIResource()) {
                types[i] = ty.asResource().getURI();
            }
        }
    }

    /**
     * Return the URI of the target of the command
     */
    public String getTarget() {
        return target;
    }

    /**
     * Return the operation which triggered the message.
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Test if the entity registered or updated had the given type
     */
    public boolean hasType(String type) {
        for (String t : types) {
            if (t.equals(type)) return true;
        }
        return false;
    }

    /**
     * If the message is a model then return otherwise return null.
     */
    public Model getMessageAsModel() {
        if (message instanceof Model) {
            return (Model) message;
        }
        return null;
    }

    /**
     * If the message is a String then return otherwise return null.
     */
    public String getMessageAsString() {
        if (message instanceof String) {
            return (String) message;
        }
        return null;
    }
    

}
