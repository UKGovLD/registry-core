/******************************************************************
 * File:        LibReg.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2013
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

package com.epimorphics.registry.webapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;

import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.rdfutil.PropertyValue;
import com.epimorphics.appbase.templates.LibPlugin;
import com.epimorphics.rdfutil.ModelWrapper;
import com.epimorphics.rdfutil.QueryUtil;
import com.epimorphics.rdfutil.RDFNodeWrapper;
import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.core.Status;
import com.epimorphics.registry.security.RegAuthorizationInfo;
import com.epimorphics.registry.security.RegPermission;
import com.epimorphics.registry.security.UserInfo;
import com.epimorphics.registry.store.RegisterEntryInfo;
import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.registry.util.TypedTemplateIndex;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.facets.FacetResultEntry;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.PrefixUtils;
import com.epimorphics.vocabs.SKOS;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * Some supporting methods to help Velocity UI access the registry store.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class LibReg extends ComponentBase implements LibPlugin {

    /**
     * Raw access to the registry store
     */
    public StoreAPI getStore() {
        return Registry.get().getStore();
    }

    private StoreAPI beginRead() {
        StoreAPI store = getStore();
        store.beginRead();
        return store;
    }
    
    /**
     * Return a resource known to the store, wrapped for scripting
     */
    public RDFNodeWrapper getResource(String uri) {
        StoreAPI store = beginRead();
        try {
            if ( ! uri.startsWith("http") ) {
                uri = Registry.get().getBaseURI() + uri;
            }
            Description d = getStore().getCurrentVersion(uri);
            if (d == null) {
                return null;
            }
            return wrapNode( d.getRoot() );
        } finally {
            store.end();
        }
    }

    private ModelWrapper wrapModel(Model m) {
        m.setNsPrefixes( Prefixes.get() );
        return new ModelWrapper( m );
    }

    private RDFNodeWrapper wrapNode(Resource root) {
        return wrapModel( root.getModel() ).getNode(root);
    }

    /**
     * Helper to list members of a register
     */
    public List<RegisterEntryInfo> listMembers(Object arg) {
        StoreAPI store = beginRead();
        try {
            Register reg = null;;
            if (arg instanceof String) {
                String uri = (String)arg;
                if ( ! uri.startsWith("http") ) {
                    uri = Registry.get().getBaseURI() + uri;
                }
                Description d = getStore().getCurrentVersion(uri);
                if (d != null) {
                    reg = d.asRegister();
                } else {
                    return null;
                }
            } else if (arg instanceof RDFNodeWrapper) {
                reg = new Register( ((RDFNodeWrapper)arg).asResource() );
            } else if (arg instanceof Register) {
                reg = (Register) arg;
            } else {
                return null;
            }
            return getStore().listMembers(reg);
        } finally {
            store.end();
        }
    }

    /**
     * Convert a resource, maybe wrapped, to a status code
     */
    public Status asStatus(Object state) {
        if (state instanceof Status) {
            return (Status)state;
        } else if (state instanceof Resource) {
            return Status.forResource((Resource)state);
        } else if (state instanceof RDFNodeWrapper) {
            return Status.forResource(((RDFNodeWrapper)state).asResource());
        } else {
            return null;
        }
    }

    /**
     * List the legal next states after this state.
     */
    public Collection<Status> nextStates(RDFNodeWrapper state) {
        List<Status> next = new ArrayList<>();
        Status current = asStatus(state);
        if (current != null) {
            next.addAll( current.nextStates() );
            // Crude topological sort to put dead ends like Invalid last
            Collections.sort(next, new Comparator<Status>() {
                @Override
                public int compare(Status o1, Status o2) {
                    Integer succ1 = o1.nextStates().size();
                    Integer succ2 = o2.nextStates().size();
                    return succ2.compareTo(succ1);
                }
            });
        }
        return next;
    }

    /**
     * Check if the given action(s) are permitted on the given URI for the current subject
     */
    public boolean isPermitted(String action, String uri) {
        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            return false;
        }
        return subject.isPermitted(action + ":/" + uri);
    }

    /**
     * Return the subjected (logged in user if any).
     * Needed for simple UI pages that aren't rendered as part of visiting the registry body
     */
    public Subject getSubject() {
        try {
            return SecurityUtils.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * List permissions for current authenticated user (if any), ordered by path
     */
    public List<RegPermission> getPermissions() {
        Subject subject = getSubject();
        if (subject.isAuthenticated()) {
            String id = ((UserInfo)subject.getPrincipal()).getOpenid();
            RegAuthorizationInfo auth = Registry.get().getUserStore().getPermissions(id);
            List<RegPermission> perms = new ArrayList<RegPermission>( );
            if (auth.getObjectPermissions() != null) {
                for (Permission p : auth.getObjectPermissions()) {
                    perms.add( (RegPermission) p);
                }
                Collections.sort(perms, new Comparator<RegPermission>(){
                    @Override
                    public int compare(RegPermission arg0, RegPermission arg1) {
                        return arg0.getPath().compareTo(arg1.getPath());
                    }
                });
            }
            return perms;
        } else {
            return new ArrayList<RegPermission>();
        }
    }

    /**
     * Return string giving the Turtle prefixes header for a model/node
     */
    public String turtlePrefixes(Object obj) {
        PrefixMapping prefixes;
        if (obj instanceof ModelWrapper) {
            prefixes = ((ModelWrapper)obj).getPrefixes();
        } else if (obj instanceof RDFNodeWrapper) {
            prefixes = ((RDFNodeWrapper)obj).getModelW().getPrefixes();
        } else if (obj instanceof Model) {
            prefixes = (Model)obj;
        } else {
            throw new EpiException("Not a type with prefixes: " + obj);
        }
        StringBuffer result = new StringBuffer();
        for (Map.Entry<String, String> mapping : prefixes.getNsPrefixMap().entrySet()) {
            result.append(String.format("@prefix %s: <%s>. \n", mapping.getKey(), mapping.getValue()));
        }
        return result.toString();
    }

    /**
     * Convert the URI for a managed entity or an item to a path relative to the registry base
     */
    public String pathFor(String uri) {
        String base = Registry.get().getBaseURI();
        if (uri.startsWith(base)) {
            return uri.substring(base.length() + 1);
        }
        return uri;
    }

    /**
     * Test whether RegisterItem property should be allowed into an edit dialog
     */
    protected static Set<Resource> NonEditableRIProps = new HashSet<Resource>();
    static {
        NonEditableRIProps.add( RegistryVocab.definition);
        NonEditableRIProps.add( RegistryVocab.itemClass);
        NonEditableRIProps.add( RegistryVocab.notation);
        NonEditableRIProps.add( RegistryVocab.register);
        NonEditableRIProps.add( RegistryVocab.submitter);
        NonEditableRIProps.add( DCTerms.dateAccepted );
        NonEditableRIProps.add( DCTerms.dateSubmitted );
        NonEditableRIProps.add( OWL.versionInfo );
//        NonEditableRIProps.add( RDFS.label );
        NonEditableRIProps.add( RDF.type );
    }

    public boolean isEditable(Object prop) {
        Resource p;
        if (prop instanceof RDFNodeWrapper) {
            p = ((RDFNodeWrapper)prop).asResource();
        } else if (prop instanceof Resource) {
            p = (Resource)prop;
        } else {
            throw new EpiException("Illegal type");
        }
        return ! NonEditableRIProps.contains(p);
    }

    /**
     * Test if a URI corresponds to a RegisterItem
     */
    public boolean isItem(String uri) {
        return uri.matches(".*/_[^/]+$");
    }

    /**
     * Create a list of item/entity pairs corresponding to a list of (wrapper) entities
     * in a paged register listing
     */
    public List<ItemMember> asItemList(List<RDFNodeWrapper> members) {
        List<ItemMember> items = new ArrayList<ItemMember>();
        Set<RDFNodeWrapper> seenItems = new HashSet<RDFNodeWrapper>();
        for (RDFNodeWrapper member : members) {
            List<RDFNodeWrapper> linkedItems = member.connectedNodes("^reg:entity/^reg:definition");
            if (linkedItems.isEmpty()) {
                items.add( new ItemMember(member, null) );
            } else {
                Collections.sort(linkedItems, new Comparator<RDFNodeWrapper>(){
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    @Override
                    public int compare(RDFNodeWrapper arg0, RDFNodeWrapper arg1) {
                        Object o0 = arg0.getPropertyValue(RegistryVocab.notation).asLiteral().getValue();
                        Object o1 = arg1.getPropertyValue(RegistryVocab.notation).asLiteral().getValue();
                        if (o0 instanceof Comparable && o1 instanceof Comparable) {
                            return ((Comparable)o0).compareTo((Comparable)o1);
                        } else {
                            return (o0.toString()).compareTo(o1.toString());
                        }
                    }
                });
                for (RDFNodeWrapper item : linkedItems) {
                    if (seenItems.add(item)) {
                        items.add( new ItemMember(member, item) );
                    }
                }
            }
        }
        return items;
    }

    public class ItemMember {
        protected RDFNodeWrapper member;
        protected RDFNodeWrapper item;
        public ItemMember(RDFNodeWrapper member, RDFNodeWrapper item) {
            this.member = member;
            this.item = item;
        }
        public RDFNodeWrapper getMember() {
            return member;
        }
        public RDFNodeWrapper getItem() {
            return item;
        }
    }


    /**
     * Run a sparql query, expanding prefixes from the prefix registry, return as an array of variable bindings
     */
    public List<Map<String, RDFNodeWrapper>> query(String query, Object... params) {
        String expandedQuery = PrefixUtils.expandQuery(query, Prefixes.get());
        expandedQuery = QueryUtil.substituteInQuery(expandedQuery, params);
        ResultSet rs = performQuery(expandedQuery);

        ModelWrapper mw = new ModelWrapper( ModelFactory.createDefaultModel() );
        List<Map<String, RDFNodeWrapper>> result = new ArrayList<Map<String,RDFNodeWrapper>>();
        while (rs.hasNext()) {
            QuerySolution soln = rs.next();
            Map<String, RDFNodeWrapper> map = new HashMap<String, RDFNodeWrapper>();
            for (Iterator<String> ni = soln.varNames(); ni.hasNext(); ) {
                String name = ni.next();
                RDFNode node = soln.get(name);
                if (node != null) {
                    map.put(name,  new RDFNodeWrapper(mw, node) );
                }
            }
            result.add( map );
        }
        return result;
    }
    
    /**
     * Run a sparql query, expanding prefixes from the prefix registry, for each RegisterItem bound to ?item fetch
     * its description with its entity description and return the aggregate as a wrapped model 
     */
    public ModelWrapper describeAll(String query, Object... params) {
        String expandedQuery = PrefixUtils.expandQuery(query, Prefixes.get());
        expandedQuery = QueryUtil.substituteInQuery(expandedQuery, params);
        ResultSet rs = performQuery(expandedQuery);

        List<String> itemURIs = new ArrayList<>();
        while (rs.hasNext()) {
            QuerySolution soln = rs.next();
            itemURIs.add( soln.getResource("item").getURI() );
        }
        
        Model aggregate = ModelFactory.createDefaultModel();
        for (RegisterItem ri : fetchItems(itemURIs, true)) {
            aggregate.add( ri.getRoot().getModel() );
            aggregate.add( ri.getEntity().getModel() );
        }
        return new ModelWrapper(aggregate);
    }
    
    private List<RegisterItem> fetchItems(List<String> itemURIs, boolean withEntity) {
        StoreAPI store = beginRead();
        try {
            return store.fetchAll(itemURIs, withEntity);
        } finally {
            store.end();
        }
    }
    
    /**
     * Run a sparql query, expanding prefixes from the prefix registry, for each RegisterItem bound to ?item fetch
     * its description with its entity description and return as a sorted list of ItemMembers 
     */
    public List<ItemMember> describeAsItems(String query, Object... params) {
        String expandedQuery = PrefixUtils.expandQuery(query, Prefixes.get());
        expandedQuery = QueryUtil.substituteInQuery(expandedQuery, params);
        ResultSet rs = performQuery(expandedQuery);

        List<String> itemURIs = new ArrayList<>();
        while (rs.hasNext()) {
            QuerySolution soln = rs.next();
            itemURIs.add( soln.getResource("item").getURI() );
        }
        
        Collections.sort(itemURIs);
        
        Model aggregate = ModelFactory.createDefaultModel();
        aggregate.setNsPrefixes( Prefixes.get() );
        ModelWrapper mw = new ModelWrapper(aggregate);
        List<ItemMember> results = new ArrayList<>(itemURIs.size());
        for (RegisterItem ri : fetchItems(itemURIs, true)) {
            aggregate.add( ri.getRoot().getModel() );
            aggregate.add( ri.getEntity().getModel() );
            
            Resource entity = ri.getEntity().inModel(aggregate);
            Resource item = ri.getRoot().inModel(aggregate);
            
            results.add( new ItemMember(new RDFNodeWrapper(mw, entity), new RDFNodeWrapper(mw, item)) );
        }
        return results;
    }

    /**
     * Utility for incrementally building up compacted range notation
     * for reserved entries
     */
    public ReservationList addReserved(ReservationList reservations, RDFNode notation) {
        reservations.add(notation);
        return reservations;
    }

    public ReservationList startReservationList() {
        return new ReservationList();
    }

    public static class ReservationList {
        StringBuffer reservations = new StringBuffer();
        int last;
        int rangeStart;
        boolean pendingNumeric = false;
        boolean inRange = false;

        public void add(RDFNode notation) {
            Literal l = notation.asLiteral();
            Object value = l.getValue();
            if (value instanceof Integer) {
                int n = ((Integer) value).intValue();
                if (pendingNumeric) {
                    if (n == last + 1) {
                        if (!inRange) {
                            rangeStart = last;
                            inRange = true;
                        }
                    } else {
                        finishNumeric();
                    }
                }
                last = n;
                pendingNumeric = true;
            } else {
                finishNumeric();
                if (reservations.length() != 0) {
                    reservations.append(", ");
                }
                reservations.append(l.getLexicalForm());
            }
        }

        private void finishNumeric() {
            if (pendingNumeric) {
                if (reservations.length() != 0) {
                    reservations.append(", ");
                }
                if (inRange) {
                    reservations.append(rangeStart);
                    reservations.append("-");
                }
                reservations.append(last);
                inRange = false;
                pendingNumeric = false;
            }
        }

        public String getReservations() {
            if (pendingNumeric) {
                finishNumeric();
            }
            return reservations.toString();
        }

        public boolean isEmpty() {
            return reservations.length() == 0 && !pendingNumeric;
        }

        @Override
        public String toString() {
            return getReservations();
        }
    }

    protected TypedTemplateIndex typedTemplateIndex;

    /**
     * Return the name of the template, if any, to use for rendering the given entity
     * @param arg can be a wrapped resource, raw resource or a register item
     */
    public String templateFor(Object arg) {
        if (arg instanceof RDFNodeWrapper) {
            return templateForResource( ((RDFNodeWrapper)arg).asResource() );
        } else if (arg instanceof RegisterItem) {
            return templateForResource( ((RegisterItem)arg).getEntity() );
        } else if (arg instanceof Resource) {
            return templateForResource( (Resource)arg );
        } else {
            return null;
        }
    }

    private String templateForResource(Resource r) {
        StoreAPI store = beginRead();
        try {
            if (typedTemplateIndex == null) {
                typedTemplateIndex = new TypedTemplateIndex();
            }
            return typedTemplateIndex.templateFor(r);
        } finally {
            store.end();
        }
    }

    /**
     * Take a list of facet search results (assumed over register items), fetches
     * the corresponding register items to a local model and returns a list of wrapped nodes
     * over that model.
     */
    public List<RDFNodeWrapper> wrap(List<FacetResultEntry> results) {
        StoreAPI store = beginRead();
        try {
            List<RDFNodeWrapper> wrappedResults = new ArrayList<>(results.size());
            Model model = ModelFactory.createDefaultModel();
            ModelWrapper modelw = new ModelWrapper(model);
            model.setNsPrefixes( Prefixes.get() );
            for (FacetResultEntry result : results) {
                RDFNode value = result.getItem();
                if (value.isResource()) {
                    Resource valueR = value.asResource();
                    RegisterItem item = store.getItem(valueR.getURI(), true);
                    Resource root = item.getRoot();
                    model.add( root.getModel() );
                    model.add( item.getEntity().getModel() );
                    wrappedResults.add( new RDFNodeWrapper(modelw, root.inModel(model)) );
                }
            }
            return wrappedResults;
        } finally {
            store.end();
        }
    }
    
    /**
     * Text string helper for summarizing text fields that might have mark up
     */
    public String stripHtml(String src, int limit) {
        String stripped = src.replaceAll("\\<.*?>","");
        if (stripped.length() > limit) {
            return stripped.substring(0, limit-3) + "...";
        } else {
            return stripped;
        }
    }
    
    /**
     * Enrich the model behind an entity with labels for 
     * the given set of linked resources.
     * Assumes the model is a simple, local in-memory model with no locking requirements.
     */
    public void addLabelsFor(RDFNodeWrapper entity, Collection<RDFNodeWrapper> links) {
        if (!entity.isResource()) return;   // No enrichment possible
        StringBuffer list = new StringBuffer();
        for (RDFNodeWrapper link : links) {
            if (link.isResource()) {
                list.append("<" + link.getURI() + ">\n");
            }
        }
        String query = LABEL_QUERY.replace("$LIST$", list.toString());
        query = PrefixUtils.expandQuery(query, Prefixes.get());
        ResultSet results = performQuery(query);
        Model model = entity.getModelW().getModel();
        while (results.hasNext()) {
            QuerySolution row = results.next();
            RDFNode rv = row.get("resource");
            if (rv.isResource()) {
                Resource r = rv.asResource();
                for (int i = 0; i < VARS.length; i++) {
                    String var = VARS[i];
                    RDFNode value = row.get(var);
                    if (value != null) {
                        model.add(r, PROPS[i], value);
                    }
                }
            }
        }
    }
    
    
    /**
     * Enrich the model behind an entity with labels for 
     * all resources linked to the entity.
     * Assumes the model is a simple, local in-memory model with no locking requirements.
     */
    public void addLabels(RDFNodeWrapper entity) {
        Set<RDFNodeWrapper> links = new HashSet<>();
        findResources(entity, links);
        links.remove(entity);
        addLabelsFor(entity, links);
    }
    
    private void findResources(RDFNodeWrapper base, Set<RDFNodeWrapper> links) {
        if ( links.add(base) ) {
            for (PropertyValue pv : base.listProperties()) {
            	// Include properties in the list of things to get labels for
            	links.add(pv.getProp());
                for (RDFNodeWrapper link : pv.getValues()) {
                    if (link.isResource() && !links.contains(link)) {
                        findResources(link, links);
                    }
                }
            }
        }
    }
    
    static final String LABEL_QUERY =
              "SELECT * WHERE {\n"
              + "    {\n"
              + "        VALUES ?resource {$LIST$}\n"
              + "        OPTIONAL {?resource rdfs:label ?label}\n"
              + "        OPTIONAL {?resource foaf:name  ?name}\n"
              + "        OPTIONAL {?resource skos:prefLabel ?pref}\n"
              + "        OPTIONAL {?resource skos:altLabel  ?alt}\n"
              + "        OPTIONAL {?resource dct:title  ?title}\n"
              + "    } UNION {\n"
              + "        VALUES ?resource {$LIST$}\n"
              + "        ?resource version:currentVersion ?current ."
              + "        OPTIONAL {?current rdfs:label ?label}\n"
              + "        OPTIONAL {?current foaf:name  ?name}\n"
              + "        OPTIONAL {?current skos:prefLabel ?pref}\n"
              + "        OPTIONAL {?current skos:altLabel  ?alt}\n"
              + "        OPTIONAL {?current dct:title  ?title}\n"
              + "    }\n"
              + "}";
    static final String[] VARS = new String[]{"label", "name", "pref", "alt", "title"};
    static final Property[] PROPS = new Property[]{RDFS.label, FOAF.name, SKOS.prefLabel, SKOS.altLabel, DCTerms.title};
    
    private ResultSet performQuery(String q) {
        StoreAPI store = Registry.get().getStore();
        store.beginRead();
        try {
            return store.query(q);
        } finally {
            store.end();
        }
    }
    
    /**
     * Transform a query string into a conjuctive, wildcard lucene query
     */
    public String asLucenceQuery(String in) {
        String[] words = in.split("([\\+\\-\\&\\|!\\(\\)\\{\\}\\[\\]^\"~/\\.,]|\\s)+");
        // Note this doesn't include ' which may need to be treated as a special case
        
        if (words.length >= 1) {
            String query = "(";
            int i = 0;
            while (i < words.length -1) {
                query += words[i++] + "* AND ";
            }
            query += words[i] + "*)";
            return query.replace("'", "\\'");
        } else {
            return words[0];
        }
    }
}