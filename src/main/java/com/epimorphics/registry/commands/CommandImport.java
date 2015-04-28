/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epimorphics.registry.commands;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.util.PatchUtil;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Import provides a bulk update/patch capability.
 * Payload expected to contain a set of register items for a register
 * (or, future extension, register tree). For each item if it
 * already exists in the register then it is patched to match the request,
 * otherwise it is added to the register.
 */
public class CommandImport extends Command {

    @Override
    public Response doExecute() {
        // TODO Auto-generated method stub
        return null;
    }
    
    // TODO general import items + register
    
    /**
     * Take a set of items intended for a single register and import them - adding or patching as required
     */
    public void importRegister(List<RegisterItem> importItems, Register register) {
        // Extract the current members as a batch (performance/scaling tradeoff)
        Model view = ModelFactory.createDefaultModel();
        List<Resource> members = new ArrayList<>();
        register.constructView(view, true, null, 0, -1, -1, members);
        
        for (RegisterItem importItem : importItems) {
            Resource itemR = importItem.getRoot();
            if (view.contains(importItem.getRoot(), RDF.type)) {
                // An existing item
                RegisterItem currentItem = new RegisterItem( itemR.inModel(view) );
                currentItem.setEntity( getEntity(itemR) );
                applyUpdate(currentItem, importItem, true, true);
                
            } else {
                // A new item to add
            }
        }
        
        // TODO
        // Get register contents
        // For each item check if patch or add
        // Register item should patch not update
        // Should be check if anything has actually changed?
    }
    
    // TODO setPayloadFromCSV
    
    private static Resource getEntity(Resource item) {
        Resource def = RDFUtil.getResourceValue(item, RegistryVocab.definition);
        if (def != null) {
            return RDFUtil.getResourceValue(def, RegistryVocab.entity);
        } else {
            return null;
        }
    }

}
