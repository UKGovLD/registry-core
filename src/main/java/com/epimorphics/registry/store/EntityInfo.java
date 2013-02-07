/******************************************************************
 * File:        EntityInfo.java
 * Created by:  Dave Reynolds
 * Created on:  7 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import com.epimorphics.registry.core.Status;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Struct used to represent results of a search for an entity.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class EntityInfo {

    protected String entityURI;
    protected String itemURI;
    protected String registerURI;
    protected Status status;

    public EntityInfo(Resource entity, Resource item, Resource register, Resource status) {
        entityURI = entity.getURI();
        itemURI = item.getURI();
        registerURI = register.getURI();
        this.status = Status.forResource(status);
    }

    public String getEntityURI() {
        return entityURI;
    }

    public String getItemURI() {
        return itemURI;
    }

    public String getRegisterURI() {
        return registerURI;
    }

    public Status getStatus() {
        return status;
    }

}
