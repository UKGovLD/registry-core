/******************************************************************
 * File:        Command.java
 * Created by:  Dave Reynolds
 * Created on:  21 Jan 2013
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

package com.epimorphics.registry.core;

import static com.epimorphics.rdfutil.RDFUtil.getAPropertyValue;
import static com.epimorphics.rdfutil.RDFUtil.labelProps;
import static com.epimorphics.registry.webapi.Parameters.FIRST_PAGE;
import static com.epimorphics.registry.webapi.Parameters.PAGE_NUMBER;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.epimorphics.registry.commands.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.csv.CSVRDFWriter;
import com.epimorphics.registry.csv.RDFCSVUtil;
import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.message.MessagingService;
import com.epimorphics.registry.security.RegAction;
import com.epimorphics.registry.security.RegPermission;
import com.epimorphics.registry.security.UserInfo;
import com.epimorphics.registry.store.EntityInfo;
import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.util.PatchUtil;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.vocab.Ldbp_orig;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.RequestProcessor;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.NameUtils;

/**
 * Wraps up a registry request as a command object to modularize
 * processing such as authorization and audit trails.
 * <p>
 * Contains some processing utilities that are reused across multiple comamnd instances.
 * </p>
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public abstract class Command {
    static final protected Logger log = LoggerFactory.getLogger( Command.class );

    public enum Operation {
        Read(CommandRead.class),
        Register(CommandRegister.class, RegAction.Register),
        GraphRegister(CommandGraphRegister.class, RegAction.Register),
        Delete(CommandDelete.class, RegAction.StatusUpdate),
        Update(CommandUpdate.class, RegAction.Update),
        StatusUpdate(CommandStatusUpdate.class, RegAction.StatusUpdate),
        Validate(CommandValidate.class),
        Search(CommandSearch.class),
        Tag(CommandTag.class, RegAction.StatusUpdate),
        Annotate(CommandAnnotate.class, true),
        Edit(CommandEdit.class, RegAction.Update),
        Export(CommandExport.class),
        Import(CommandImport.class, true),
        RealDelete(CommandRealDelete.class, RegAction.RealDelete),
        Compare(CommandCompare.class);

        protected RegAction action;
        protected Class<?> implementation;
        protected boolean isUpdate;

        private Operation(Class<?> implementation,  boolean isUpdate, RegAction action) {
            this.action = action;
            this.implementation = implementation;
            this.isUpdate = isUpdate;
        }
        
        private Operation(Class<?> implementation, boolean isUpdate) {
            this(implementation, isUpdate, null);
        }
        
        private Operation(Class<?> implementation,  RegAction action) {
            this(implementation, true, action);
        }
        
        private Operation(Class<?> implementation) {
            this(implementation, false, null);
        }

        public RegAction getAuthorizationAction() {
            return action;
        }

        public boolean isUpdate() {
            return isUpdate;
        }
        
        public Command makeCommandInstance() {
            try {
                return (Command) implementation.newInstance();
            } catch (Exception e) {
                throw new EpiException(e);
            }
        }
    };

    protected Operation operation;

    protected String target;            // The absolute target URI
    protected String path;              // The relative request path
    protected MultivaluedMap<String, String> parameters;
    protected Model payload;
    protected InputStream payloadStream;
    protected String mediaType;         // The requested media type, may only be set for specific types like CSV

    protected String requestor;

    protected String parent;            // The URI of the parent register
    protected String lastSegment;       // Last segment in the request
    protected boolean paged;
    protected int length = -1;
    protected int pagenum = 0;

    protected Registry registry;
    protected StoreAPI store;

    protected ForwardingRecord delegation;

    /**
     * Initialize a command instance
     * @param operation   operation request, as determined by HTTP verb
     * @param target      the URI to which the operation was targeted, omits the assumed base URI
     * @param parameters  the query parameters
     */
    public void init(Operation operation, String target,  MultivaluedMap<String, String> parameters, Registry registry) {
        this.operation = operation;
        String t = target.endsWith("/") ? target.substring(0, target.length()-1) : target;
        t = t.isEmpty() ? "/" : "/" + t;
        this.target = registry.getBaseURI() + t;
        this.path = target;
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
            length = (int)registry.getPageSize();
        } else if (parameters.containsKey(PAGE_NUMBER)) {
            paged = true;
            length = (int)registry.getPageSize();
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
    
    public void setPayloadStream(InputStream stream) {
        this.payloadStream = stream;
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
        if (requestor == null) {
            requestor = ((UserInfo) SecurityUtils.getSubject().getPrincipal()).toString();
        }
        return requestor;
    }

    public void setRequestor(String requestor) {
        this.requestor = requestor;
    }

    public ForwardingRecord getDelegation() {
        return delegation;
    }

    public void setDelegation(ForwardingRecord delegation) {
        this.delegation = delegation;
    }
       

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public String toString() {
        return String.format("Command: %s on %s", operation, target);
    }

    /**
     * Carries out the actual command
     */
    public abstract Response doExecute() ;

    /**
     * Test that the request is legal. Subclasses should provide
     * an appropriate implementation.
     */
    public ValidationResponse validate() {
        return ValidationResponse.OK;
    }


    public Response authorizedExecute() {
        performValidate();
        return performExecute();
    }

    public Response execute()  {
        performValidate();
        if (!isAuthorized()) {
            throw new WebApiException(Response.Status.UNAUTHORIZED, "Either not logged in or not authorized for this action");
        }
        return performExecute();
    }

    protected void performValidate() {
        store.beginRead();
        try {
            ValidationResponse validity = validate();
            if (!validity.isOk()) {
                throw new WebApiException(validity.getStatus(), validity.getMessage());
            }
        } finally {
            store.end();
        }
    }

    // Called after validation and authorization
    protected Response performExecute() {
        Response response = null;
        try {
            if (operation.isUpdate()) {
                store.beginWrite();
            } else {
                store.beginRead();
            }
            response = doExecute();
        } catch (WebApplicationException wae) {
            if (operation.isUpdate) {
                store.abort();
            }
            response =  wae.getResponse();
        } catch (Exception e) {
            log.error("Internal error", e);
            if (operation.isUpdate) {
                store.abort();
            }
            response = Response.serverError().entity(e.getMessage()).build();
        } finally {
            store.end();
        }
        
        logResponse(response);

        if ( operation.isUpdate && registry.getRequestLogger() != null ) {
            try {
                registry.getRequestLogger().writeLog(this);
            } catch (IOException e) {
                log.error("Failed to write log of payload", e);
            }
        }

        return response;
    }
    
    protected void logResponse(Response response) {
        String responseText = 
                ((response.getStatus() == 201) ? " -> " + response.getMetadata().get("Location") : "") 
                + " " + response.getStatus();
        logResponse(responseText);
    }
    
    protected void logResponse(String response) {
        Date now = new Date(System.currentTimeMillis());
        log.info( String.format("%s [%s] %s \"%s?%s\"%s",
                requestor == null ? null : NameUtils.decodeSafeName(requestor),
                new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z").format(now),
                operation.toString(),
                target,
                makeParamString(parameters),
                response) );
    }

    /**
     * Notify the command event out to message listeners
     */
    // TODO these will currently go out as actions are performed even it the transaction is subsequently aborted
    // Options include (a) batch up messages until commit happens, (b) include transaction start/abort/commit message, (c) scrap this
    // version of the notification system and rethink
    public void notify(Message message) {
        MessagingService ms = registry.getMessagingService();
        if (ms != null) {
            ms.sendMessage(message);
        }
    }

    /**
     * Returns the permissions that will be required to authorize this
     * operation or null if no permissions are needed.
     */
    public RegPermission permissionRequired() {
        RegAction action = operation.getAuthorizationAction();
        if (action == null) {
            return null;
        } else {
            return new RegPermission(action, "/" + path);
        }
    }

    /**
     * Test if the user of authorized to execute this command
     */
    public boolean isAuthorized() {
        RegPermission required = permissionRequired();
        if (required != null) {
            try {
                Subject subject = SecurityUtils.getSubject();
                if (subject.isPermitted(required)) {
                    return true;
                } else {
                    log.warn("Authorization failure for " + subject.getPrincipal() + ", requested permission " + required);
                    return false;
                }
            } catch (UnavailableSecurityManagerException e) {
                log.warn("Security is not configured, assuming test mode");
                return true;
            }
        } else {
            return true;
        }
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

    // Locate a set of entities within a payload - all non-anonymous typed resources or root resources
    protected Collection<Resource> findEntities() {
        Set<Resource> roots = new HashSet<Resource>();
        
        roots.addAll( RDFUtil.findRoots(payload) );
        roots.addAll( payload.listSubjectsWithProperty(RDF.type).toList() );
        for (Iterator<Resource> i = roots.iterator(); i.hasNext();) {
            Resource root = i.next();
            if (root.isAnon()) {
                i.remove();
            }
        }
        return roots;
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
        m.setNsPrefixes(Prefixes.get());
        URI uri;
        try {
            uri = new URI( location );
        } catch (URISyntaxException e) {
            throw new WebApplicationException(e);
        }
        return Response.ok().location(uri).entity( m ).header("Vary", "Accept").build();
    }

    protected Resource injectPagingInformation(Model m, Resource root,  boolean more) {
        String url = target + "?" + makeParamString(parameters);
        Resource page = m.createResource(url)
            .addProperty(RDF.type, Ldbp_orig.Page)
            .addProperty(Ldbp_orig.pageOf, root);
        if (more) {
            String pageParams = "?" + PAGE_NUMBER + "=" + (pagenum+1);
            String otherParams = makeParamString(parameters, FIRST_PAGE, PAGE_NUMBER);
            if (!otherParams.isEmpty()) {
                pageParams += "&" + otherParams;
            }
            page.addProperty(Ldbp_orig.nextPage, m.createResource( target + pageParams ));
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
        
    /**
     * Perform an update operation - updating an entity (if present) and optionally the item metadata as well.
     * Caller is expected to lock/commit the store.
     *  
     * @param currentItem the currently RegisterItem which is the target of the update
     * @param newitem a RegisterItem representing the new item to be applied, may or may not have an associated entity
     * @param isPatch  if true both item and entity updates should be treated as patches, otherwise they replace the existing entity
     * @param updateItem if true the item itself should be updated, otherwise only the entity is updated 
     * @return the URI for the new version of the item
     */
    protected String applyUpdate(RegisterItem currentItem, RegisterItem newitem, boolean isPatch, boolean updateItem) {
        boolean isRegister = currentItem.isRegister();
        Resource entity = newitem.getEntity();
        boolean withEntity = entity != null;
        if (withEntity) {
            if (isPatch){
                if (isRegister) {
                    PatchUtil.patch(entity, currentItem.getEntity(), RegistryVocab.subregister);
                } else {
                    PatchUtil.patch(entity, currentItem.getEntity());
                }
            } else {
                currentItem.setEntity(entity);
            }
            currentItem.updateForEntity(false, Calendar.getInstance());
        }

        if (updateItem) {
            // TODO filter container membership property values from 
            if (isPatch) {
                PatchUtil.patch(newitem.getRoot(), currentItem.getRoot(), RegisterItem.RIGID_PROPS);
            } else {
                PatchUtil.update(newitem.getRoot(), currentItem.getRoot(), RegisterItem.RIGID_PROPS, RegisterItem.REQUIRED_PROPS);
            }
        }
        String versionURI = store.update(currentItem, withEntity);
        checkDelegation(currentItem);
        
        return versionURI;
    }
    
    /**
     * Validate an entity for registration in a given register
     */
    protected ValidationResponse validateEntity(Register parent, Resource entity) {
        if (entity == null) {
            return new ValidationResponse(BAD_REQUEST, "Missing entity");
        }
        if (!entity.listProperties().hasNext()) {
            return new ValidationResponse(BAD_REQUEST, "No properties for entity, incorrect URI? " + entity);
        }
        if ( !entity.hasProperty(RDF.type) || getAPropertyValue(entity, labelProps) == null ) {
            return new ValidationResponse(BAD_REQUEST, "Missing required property (rdf:type or rdfs:label) on " + entity);
        }
        for (StmtIterator i = entity.listProperties(RDF.type); i.hasNext();) {
            if ( i.next().getObject().isLiteral() ) {
                return new ValidationResponse(BAD_REQUEST, "Illegal type (must be a resource) on " + entity);
            }
        }

        List<Resource> itemClasses = RDFUtil.allResourceValues(parent.getRoot(), RegistryVocab.containedItemClass);
        boolean foundLegalType = itemClasses.isEmpty();
        for (Resource itemClass : itemClasses) {
            if (entity.hasProperty(RDF.type, itemClass)) {
                foundLegalType = true;
                break;
            }
        }
        if (!foundLegalType) {
            return new ValidationResponse(BAD_REQUEST, "Entity does not have one of the types required by this register: " + entity);
        }
        return ValidationResponse.OK;
    }
        
    /**
     * Add an item to a register. It is up to the caller to lock/commit the store.
     */
    protected Resource addToRegister(Register parent, RegisterItem item) {

        if (store.getDescription(item.getRoot().getURI()) != null) {
            // Item already exists
            throw new WebApiException(Response.Status.FORBIDDEN, "Item already registered at request location: " + item.getRoot());
        }

        ValidationResponse entityValid = validateEntity(parent, item.getEntity() );
        if (!entityValid.isOk()) {
            throw new WebApiException(entityValid.getStatus(), entityValid.getMessage());
        }
        item.skolemize();

        // Santization
        for (Property p : RegisterItem.INTERNAL_PROPS) {
            item.getRoot().removeAll(p);
        }

        // Submitter
        Model m = item.getRoot().getModel();
        Resource submitter = m.createResource();
        try {
            UserInfo userinfo = (UserInfo) SecurityUtils.getSubject().getPrincipal();
            submitter
            .addProperty(FOAF.name, userinfo.getName())
            .addProperty(FOAF.accountName, userinfo.getOpenid() );
        } catch (UnavailableSecurityManagerException e) {
            // Occurs during bootstrap
            submitter.addProperty(FOAF.name, "bootstrap");
        }
        item.getRoot().addProperty(RegistryVocab.submitter, submitter);

        Resource entity = item.getEntity();
        // Normalization closures
        // TODO factor these out as SPARQL constructs in an external file?
        if( entity.hasProperty(RDF.type, RegistryVocab.Register) ) {
            // TODO fill in void description
            entity.addProperty(RDF.type, Ldbp_orig.Container);
            log.info("Created new sub-register: " + item.getNotation());
        }
        if (entity.hasProperty(RDF.type, RegistryVocab.FederatedRegister)
                || entity.hasProperty(RDF.type, RegistryVocab.NamespaceForward)
                || entity.hasProperty(RDF.type, RegistryVocab.DelegatedRegister)) {
            entity.addProperty(RDF.type, RegistryVocab.Delegated);
            item.getRoot().addProperty(RegistryVocab.itemClass, RegistryVocab.Delegated);
            if (entity.hasProperty(RDF.type, RegistryVocab.DelegatedRegister)) {
                entity.addProperty(RDF.type, RegistryVocab.Register);
                item.getRoot().addProperty(RegistryVocab.itemClass, RegistryVocab.Register);
            }
        }
        store.addToRegister(parent, item);
        checkDelegation(item);

        return item.getRoot();
    }

    
    // This part part could stream but little point given we are working from an in-memory model
    protected Response serializeToCSV(List<Resource> members, String location, boolean withMetadata) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVRDFWriter writer = new CSVRDFWriter(out, Prefixes.get());
        writer.addHeader(members);
        if (withMetadata) {
            writer.addHeader(RDFCSVUtil.STATUS_HEADER);
            writer.addHeader(RDFCSVUtil.NOTATION_HEADER);
        }
        for (Resource member : members) {
            writer.write(member);
            if (withMetadata){
                List<Resource> itemL = QueryUtil.connectedResources(member, itemPath);
                if (itemL.size() != 1) {
                    throw new EpiException("Internal inconsistency, can't find register item for extracted member");
                }
                Resource item = itemL.get(0);
                Status status = Status.forResource( RDFUtil.getResourceValue(item, RegistryVocab.status) );
                writer.write(RDFCSVUtil.STATUS_HEADER, status.getLabel());
                String notation = RDFUtil.getStringValue(item, RegistryVocab.notation);
                writer.write(RDFCSVUtil.NOTATION_HEADER, notation);
            }
            writer.finishRow();
        }
        writer.close();
        String csv;
        try {
            csv = out.toString(StandardCharsets.UTF_8.name());
            String disposition = String.format("attachment; filename=\"%s.csv\"", lastSegment.startsWith("_") ? lastSegment.substring(1) : lastSegment);
            URI uri;
            try {
                uri = new URI( location );
            } catch (URISyntaxException e) {
                throw new WebApplicationException(e);
            }
            return Response.ok(csv, "text/csv").location(uri).header("Vary", "Accept").header(RequestProcessor.CONTENT_DISPOSITION_HEADER, disposition).build();
        } catch (UnsupportedEncodingException e) {
            throw new EpiException("Internal error accessing UTF-8", e);
        }
    }
    
    protected static final String itemPath = String.format("^<%s>/^<%s>", RegistryVocab.entity, RegistryVocab.definition);
    
    /**
     * Set a successor relation from a register item to another URI which may be a register item.
     * It is up to the caller to manage a surrounding write transaction and to save the updated version of the olditem.
     */
    protected void setSuccessor(RegisterItem olditem, String newuri) {
        RegisterItem newitem = store.getItem(newuri, false);
        if (newitem == null) {
            // See if we have been given an entity instead
            for (EntityInfo ei : store.listEntityOccurences(newuri)) {
                if (ei.getItemURI() != null) {
                    newitem = store.getItem(ei.getItemURI(), false);
                    break;
                }
            }
        }
        if (newitem != null) {
            newitem.setProperty(RegistryVocab.predecessor, olditem.getRoot());
            store.update(newitem, false);
        }
        Resource newr = newitem == null ? ResourceFactory.createResource(newuri) : newitem.getRoot();
        olditem.setProperty(RegistryVocab.successor, newr);
    }
}
