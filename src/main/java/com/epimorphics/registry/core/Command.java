/******************************************************************
 * File:        Command.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.core;

import static com.epimorphics.registry.webapi.Parameters.FIRST_PAGE;
import static com.epimorphics.registry.webapi.Parameters.PAGE_NUMBER;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.vocab.Ldbp;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.server.webapi.WebApiException;
import com.epimorphics.util.EpiException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Wraps up a registry request as a command object to modularize
 * processing such as authorization and audit trails.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public abstract class Command {
    static final Logger log = LoggerFactory.getLogger( Command.class );

    public enum Operation { Read, Register, Delete, Update, StatusUpdate, Validate, Search };

    protected Operation operation;
    protected String target;
    protected MultivaluedMap<String, String> parameters;
    protected Model payload;

    protected String requestor;

    protected String parent;
    protected String lastSegment;
    protected boolean paged;
    protected int length = -1;
    protected int pagenum = 0;

    protected Registry registry;
    protected StoreAPI store;
    /**
     * Constructor
     * @param operation   operation request, as determined by HTTP verb
     * @param targetType  type of thing to act on, may be amended or set later after more analysis
     * @param target      the URI to which the operation was targeted, omits the assumed base URI
     * @param parameters  the query parameters
     */
    public Command(Operation operation, String target,  MultivaluedMap<String, String> parameters, Registry registry) {
        this.operation = operation;
        this.target = registry.getBaseURI() + (target.isEmpty() ? "/" : "/" + target);
        this.parameters = parameters;
        this.registry = registry;
        this.store = registry.getStore();
        Matcher segmatch = LAST_SEGMENT.matcher(this.target);
        if (segmatch.matches()) {
            this.lastSegment = segmatch.group(2);
            this.parent = segmatch.group(1);
        } else {
            // Root register
            this.lastSegment = "";
            this.parent = registry.getBaseURI();
        }

        // Extract paging parameters, if any
        if (parameters.containsKey(FIRST_PAGE)) {
            paged = true;
            length = registry.getPageSize();
        } else if (parameters.containsKey(PAGE_NUMBER)) {
            paged = true;
            length = registry.getPageSize();
            try {
                pagenum = Integer.parseInt( parameters.getFirst(PAGE_NUMBER) );
            } catch (NumberFormatException e) {
                throw new WebApiException(javax.ws.rs.core.Response.Status.BAD_REQUEST, "Illegal page number");
            }
        }
    }
    static final Pattern LAST_SEGMENT = Pattern.compile("(^.*)/([^/]+)$");

    public Model getPayload() {
        return payload;
    }

    public void setPayload(Model payload) {
        this.payload = payload;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getTarget() {
        return target;
    }

    public MultivaluedMap<String, String> getParameters() {
        return parameters;
    }

    public String getRequestor() {
        return requestor;
    }

    public void setRequestor(String requestor) {
        this.requestor = requestor;
    }

    @Override
    public String toString() {
        return String.format("Command: %s on %s", operation, target);
    }

    public abstract Response doExecute() ;

    public Response execute()  {
        // TODO - authorization

        Response response = null;
        try {
            response = doExecute();
        } catch (WebApplicationException wae) {
            response =  wae.getResponse();
        } catch (Exception e) {
            log.error("Internal error", e);
            response = Response.serverError().entity(e.getMessage()).build();
        }

        // TODO - logging notification

        Date now = new Date(System.currentTimeMillis());
        log.info(String.format("%s [%s] %s \"%s?%s\"%s %d",
                requestor,
                new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z").format(now),
                operation.toString(),
                target,
                makeParamString(parameters),
                (response.getStatus() == 201) ? " -> " + response.getMetadata().get("Location") : "",
                response.getStatus()));

        return response;
    }

    protected boolean hasParamValue(String param, String value) {
        List<String> values = parameters.get(param);
        if (values != null) {
            return values.contains(value);
        }
        return false;
    }

    protected String notation() {
        if (lastSegment.startsWith("_")) {
            return lastSegment.substring(0, lastSegment.length() - 1);
        } else {
            return lastSegment;
        }
    }

    protected String entityURI() {
        if (lastSegment.startsWith("_")) {
            return parent + "/" + notation();
        } else {
            return target;
        }
    }

    protected String itemURI() {
        if (lastSegment.startsWith("_")) {
            return target;
        } else {
            return parent + "/_" + lastSegment;
        }
    }

    protected Resource findSingletonRoot() {
        List<Resource> roots = payload.listSubjectsWithProperty(RDF.type).toList();
        if (roots.isEmpty()) {
            roots = payload.listSubjects().toList();
        }
        if (roots.size() != 1) {
            throw new WebApiException(Response.Status.BAD_REQUEST, "Could not find unique entity root to register");
        }
        return roots.get(0);
    }

    protected String makeParamString(MultivaluedMap<String, String> parameters, String...omit) {
        StringBuffer params = new StringBuffer();
        boolean startedParams = false;
        for (String p : parameters.keySet()) {
            boolean skip = false;
            for (String o : omit) {
                if (p.equals(o)) {
                    skip = true;
                    break;
                }
            }
            if (skip) continue;
            if (startedParams) {
                params.append("&");
            } else {
                startedParams = true;
            }
            List<String> values = parameters.get(p);
            params.append(p);
            if (values == null || values.isEmpty()) continue;
            if (values.size() == 1 && values.get(0) == null) continue;
            params.append("=");
            boolean started = false;
            for (String value: values) {
                if (started) {
                    params.append(",");
                } else {
                    started = true;
                }
//                params.append( value );
                try {
                    params.append( URLEncoder.encode(value, "UTF-8") );
                } catch (UnsupportedEncodingException e) {
                    throw new EpiException(e);  // Can't happen :)
                }
            }
        }
        return params.toString();
    }

    protected Response returnModel(Model m, String location) {
        URI uri;
        try {
            uri = new URI( location );
        } catch (URISyntaxException e) {
            throw new WebApplicationException(e);
        }
        return Response.ok().location(uri).entity( m ).build();
    }

    protected Resource injectPagingInformation(Model m, Resource root,  boolean more) {
        String url = target + "?" + makeParamString(parameters);
        Resource page = m.createResource(url)
            .addProperty(RDF.type, Ldbp.Page)
            .addProperty(Ldbp.pageOf, root);
        if (more) {
            String pageParams = "?" + PAGE_NUMBER + "=" + (pagenum+1);
            String otherParams = makeParamString(parameters, FIRST_PAGE, PAGE_NUMBER);
            if (!otherParams.isEmpty()) {
                pageParams += "&" + otherParams;
            }
            page.addProperty(Ldbp.nextPage, m.createResource( target + pageParams ));
        }
        return page;
    }

    protected void checkDelegation(RegisterItem item) {
        if (item.getRoot().hasProperty(RegistryVocab.itemClass, RegistryVocab.Delegated)) {
            if (item.getEntity() == null) {
                store.getEntity(item);
            }
            ForwardingService fs = Registry.get().getForwarder();
            if (fs != null) {
                fs.update(item);
            } else {
                log.error("No forwarder configured, delegation request for " + item.getRoot() + " can't be honoured");
            }
        }
    }
}
