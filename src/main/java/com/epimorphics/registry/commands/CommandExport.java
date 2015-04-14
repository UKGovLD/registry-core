/******************************************************************
 * (c) Copyright 2015, Epimorphics Limited
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
package com.epimorphics.registry.commands;

import static com.epimorphics.registry.webapi.Parameters.STATUS;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.csv.CSVRDFWriter;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.RequestProcessor;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.jersey.api.NotFoundException;

/**
 * Export an item or register to CSV.
 * This is a simplified version of read which doesn't handle metadata or versioning.
 * Not streaming, builds entire response in memory.
 */
public class CommandExport extends Command {
    public static final String STATUS_HEADER = "@status";
    
    protected Model view = ModelFactory.createDefaultModel();
    protected List<Resource> members = new ArrayList<>();
    
    @Override
    public Response doExecute() {
        
        // We want the item (so we can include the @status)
        String itemURI = lastSegment.startsWith("_") ? target : parent +"/_" + lastSegment;  
        RegisterItem ri = store.getItem(itemURI, true) ;
        if (ri == null) {
            throw new NotFoundException();
        }


        if (ri.isRegister()) {
            Status status = Status.forString( parameters.getFirst(STATUS), Status.Accepted );
            long timestamp = -1;
            Register register = new Register( ri.getEntity() );
            register.setStore( store );
            register.constructView(view, true, status, 0, -1, timestamp, members);            

        } else {
            Resource entity = ri.getEntity();
            view.add( ri.getRoot().getModel() );
            view.add( entity.getModel() );
            members.add( entity.inModel(view) );
        }
        
//        System.out.println( "Members = " + members );
//        view.write(System.out, "Turtle");

        return serializeToCSV();
    }
    
    // This part part could stream but little point given we are working from an in-memory model
    protected Response serializeToCSV() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVRDFWriter writer = new CSVRDFWriter(out, Prefixes.get());
        writer.addHeader(members);
        writer.addHeader(STATUS_HEADER);
        for (Resource member : members) {
            writer.write(member);
            List<Resource> statusL = QueryUtil.connectedResources(member, statusPath);
            if ( !statusL.isEmpty() ) {
                Status status = Status.forResource( statusL.get(0) );
                writer.write(STATUS_HEADER, status.name().toLowerCase());
            }
            writer.finishRow();
        }
        writer.close();
        String csv;
        try {
            csv = out.toString(StandardCharsets.UTF_8.name());
            String disposition = String.format("attachment; filename=\"%s.csv\"", lastSegment.startsWith("_") ? lastSegment.substring(1) : lastSegment);
            return Response.ok(csv, "text/csv").header(RequestProcessor.CONTENT_DISPOSITION_HEADER, disposition).build();
        } catch (UnsupportedEncodingException e) {
            throw new EpiException("Internal error accessing UTF-8", e);
        }
    }
    
    protected static final String statusPath = String.format("^<%s>/^<%s>/<%s>", RegistryVocab.entity, RegistryVocab.definition, RegistryVocab.status);

}
