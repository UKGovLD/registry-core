/******************************************************************
 * File:        TypedTemplatedIndex.java
 * Created by:  Dave Reynolds
 * Created on:  6 May 2013
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

package com.epimorphics.registry.util;

import java.util.Iterator;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.OneToManyMap;
import org.apache.jena.vocabulary.RDF;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.message.Message;
import com.epimorphics.registry.message.MessagingService;
import com.epimorphics.registry.message.ProcessIfChanges;
import com.epimorphics.registry.vocab.Ui;


/**
 * Utility to keep track of type-specific UI templates registered
 * in the /system/typed-templates register.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TypedTemplateIndex {
    public static final String REGISTER = "/system/typed-templates";

    private OneToManyMap<Resource, Template> rawTemplates = null;
    protected boolean listeningForChanges = false;

    /**
     * Constructor. Should not be called until after the registry has been initialized
     */
    public TypedTemplateIndex() {
    }

    /**
     * Find the best template for the different types of this resource
     */
    public String templateFor(Resource res) {
        OneToManyMap<Resource, Template> templates = getTemplates();
        int bestPriority = -1;
        String template = null;
        for (StmtIterator si = res.listProperties(RDF.type); si.hasNext(); ) {
            Resource ty = si.next().getObject().asResource();
            Iterator<Template> i = templates.getAll(ty);
            while (i.hasNext()) {
                Template candidate = i.next();
                if (candidate.priority > bestPriority) {
                    bestPriority = candidate.priority;
                    template = candidate.template;
                }
            }
        }
        return template;
    }

    protected OneToManyMap<Resource, Template> getTemplates() {
        if (rawTemplates == null) {
            loadTemplates();
        }
        return rawTemplates;
    }

    protected void resetCache() {
        rawTemplates = null;
    }

    private void loadTemplates() {
        rawTemplates  = new OneToManyMap<>();
        String registerURI = Registry.get().getBaseURI() + REGISTER;
        Model rootModel = ModelFactory.createDefaultModel();
        Register register = new Register( rootModel.createResource( registerURI ) );
        register.setStore( Registry.get().getStore() );
        for (Resource templateR : register.getAllEntities()) {
            Template template = new Template(templateR);
            rawTemplates.put(template.type, template);
        }

        listenForChanges();
    }

    private void listenForChanges() {
        if (!listeningForChanges) {
            MessagingService.Process reset = new MessagingService.Process(){
                @Override
                public void processMessage(Message message) {
                    resetCache();
                }
            };
            String target = Registry.get().getBaseURI() + REGISTER;
            Registry.get().getMessagingService().processMessages( new ProcessIfChanges(reset, target) );
            listeningForChanges = true;
        }
    }

    static class Template {
        Resource type;
        int priority;
        String template;

        public Template(Resource t) {
            type = t.getPropertyResourceValue(Ui.type);
            priority = RDFUtil.getIntValue(t, Ui.templatePriority, 0);
            template = RDFUtil.getStringValue(t, Ui.template);
        }
    }


}
