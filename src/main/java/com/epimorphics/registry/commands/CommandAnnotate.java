/******************************************************************
 * File:        CommandAnnotate.java
 * Created by:  Dave Reynolds
 * Created on:  29 Apr 2013
 * 
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.commands;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.Response;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.sun.jersey.api.NotFoundException;

public class CommandAnnotate extends Command {

    @Override
    public Response doExecute() {
        String graphURI = target + "?annotation=" + parameters.getFirst(Parameters.ANNOTATION);
        store.lock(target);
        try {
            RegisterItem item = store.getItem(target, false);
            if (item == null) {
                throw new NotFoundException();
            }
            store.storeGraph(graphURI, getPayload());
            item.getRoot().addProperty(RegistryVocab.annotation, ResourceFactory.createResource(graphURI));
            store.update(item, false);
            
            // notify event
            Message message = new Message(this);
            message.setMessage( payload );
            notify(message);
            
            try {
                return Response.created(new URI(graphURI)).build();
            } catch (URISyntaxException e) {
                throw new EpiException(e);
            }
        } finally {
            store.unlock(target);
        }
    }

}
