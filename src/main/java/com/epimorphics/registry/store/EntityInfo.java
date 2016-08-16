/******************************************************************
 * File:        EntityInfo.java
 * Created by:  Dave Reynolds
 * Created on:  7 Feb 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import com.epimorphics.registry.core.Status;
import org.apache.jena.rdf.model.Resource;

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
