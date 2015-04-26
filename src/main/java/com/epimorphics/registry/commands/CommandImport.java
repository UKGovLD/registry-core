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

import java.util.List;

import javax.ws.rs.core.Response;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.RegisterItem;
import com.hp.hpl.jena.rdf.model.Resource;

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
    
    public static void importRegister(List<Resource> importedItems, RegisterItem register) {
        // TODO
        // Get register contents
        // For each item check if patch or add
        // Register item should patch not update
        // Should be check if anything has actually changed?
    }
    
    // TODO setPayloadFromCSV

}
