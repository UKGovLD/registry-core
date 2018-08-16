/******************************************************************
 * File:        FacetService.java
 * Created by:  Dave Reynolds
 * Created on:  20 May 2013
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

package com.epimorphics.registry.webapi.facets;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.store.Store;
import com.epimorphics.registry.vocab.FacetVocab;
import com.epimorphics.util.EpiException;
import com.epimorphics.vocabs.SKOS;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.jena.enhanced.UnsupportedPolymorphismException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;

public class FacetService extends ComponentBase {
    static Logger log = LoggerFactory.getLogger(FacetService.class);

    protected String baseQuery;
    protected Store store;
    protected List<FacetSpec> specList = new ArrayList<>();

    public void setSpecFile(String file) {
        String specFile = expandFileLocation(file);
        Model spec = FileManager.get().loadModel( specFile );
        parseFacetSpec(spec);
        log.info("Loaded facet specification from " + specFile);
    }
    
    public void setStore(Store store) {
        this.store = store;
    }
    
    private void parseFacetSpec(Model spec) {
        ResIterator ri = spec.listSubjectsWithProperty(FacetVocab.facets);
        if (!ri.hasNext()) {
            throw new EpiException("Could not locate facet specification in file");
        }
        Resource root = ri.next();
        if (ri.hasNext()) {
            throw new EpiException("Ambiguous facet specification, found two roots");
        }
        try {
            Resource facetListR = root.getPropertyResourceValue(FacetVocab.facets);
            RDFList facets = facetListR.as(RDFList.class);
            for (RDFNode facet : facets.asJavaList()) {
                Resource facetR = facet.asResource();
                String label = RDFUtil.getLabel(facet);
                String varname = RDFUtil.getStringValue(facetR, SKOS.notation, label);
                String path = RDFUtil.getStringValue(facetR, FacetVocab.query);
                if (path == null) {
                    throw new EpiException("Could not find query for facet " + label);
                }
                specList.add( new FacetSpec(label, varname, path) );
            }
        } catch (UnsupportedPolymorphismException e) {
            throw new EpiException("Could not parse the list of facets as an RDF list");
        }
        baseQuery = RDFUtil.getStringValue(root, FacetVocab.query);
        if (baseQuery == null) {
            throw new EpiException("No base query specified");
        }
    }

    public List<FacetSpec> getSpecList() {
        return specList;
    }

    public String getBaseQuery() {
        return baseQuery;
    }
    
    public FacetResult query(String stateIn) {
        String state = StringEscapeUtils.unescapeHtml(stateIn);
        try {
            store.lock();
            FacetResult fr =  new FacetResult(baseQuery, state, specList, store.asDataset().getDefaultModel());
            fr.setPageSize( (int) Registry.get().getPageSize() );
            return fr;
        } catch (Exception e) {
            // Log error here so trace doesn't appear in UI
            log.error("Illegal facet state", e);
            // Return base state 
            return new FacetResult(baseQuery, "", specList, store.asDataset().getDefaultModel());
        } finally {
            store.end();
        }
    }
}
