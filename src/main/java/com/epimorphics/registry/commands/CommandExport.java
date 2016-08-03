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

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.registry.core.Command;
import com.epimorphics.registry.webapi.RequestProcessor;
import com.sun.jersey.api.NotFoundException;

/**
 * Return an n-quad serialization of the entire internal RDF structure describing a register tree.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class CommandExport extends Command {

    @Override
    public Response doExecute() {
        final String itemURI = lastSegment.startsWith("_") ? target : parent +"/_" + lastSegment;
        if (store.getItem(itemURI, false) == null) {
            throw new NotFoundException();
        }
        
        StreamingOutput stream = new StreamingOutput() {
        	// Temporary logger support while testing abort fix
        	final Logger log = LoggerFactory.getLogger(CommandExport.class);
        	
            @Override
            public void write(OutputStream output) throws IOException,
                    WebApplicationException {
                StreamRDF rdfstream = StreamRDFWriter.getWriterStream(output, Lang.NQUADS);
                store.beginRead();
                try {
	                store.exportTree(itemURI, rdfstream);
                }
                catch (org.apache.jena.atlas.RuntimeIOException e){
                	log.info("Closed output stream detected in streaming export");
                	// IO errors ignored if the client closes the socket.
                }
                finally {
                	store.end();
                }

            }
        };
        
        String fname = lastSegment.startsWith("_") ? lastSegment.substring(1) : lastSegment;

        return Response.ok()
                .type( Lang.NQUADS.getHeaderString() )
                .entity(stream)
                .header(RequestProcessor.CONTENT_DISPOSITION_HEADER, String.format(RequestProcessor.CONTENT_DISPOSITION_FMT, fname, "nq"))
                .build();
    }

}
