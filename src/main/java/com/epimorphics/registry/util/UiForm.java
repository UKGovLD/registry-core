/******************************************************************
 * File:        UiForm.java
 * Created by:  Dave Reynolds
 * Created on:  25 Mar 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.util;

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import com.epimorphics.rdfutil.RDFUtil;
import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.vocab.Ui;
import com.epimorphics.server.webapi.WebApiException;
import com.epimorphics.vocabs.SKOS;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Utility to instantiate an RDF resource based on
 * a UI specification and form response.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class UiForm {
    public static final String FORM_TYPE_FIELD = "form-type";  // gives URI of the spec

    Resource spec;

    public UiForm(Resource spec) {
        this.spec = spec;
    }

    public static Resource create(MultivaluedMap<String, String> form, String target) {
        String specURI = form.getFirst(FORM_TYPE_FIELD);
        UiForm proc = new UiForm(Registry.get().getStore().getCurrentVersion(specURI).getRoot());
        return proc.make(form, target);
    }

    public Resource make(MultivaluedMap<String, String> form, String target) {
        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource(target + form.getFirst("id"));
        Resource prototype = spec.getPropertyResourceValue(Ui.prototype);
        if (prototype != null) {
            PatchUtil.patch(prototype, resource);
        }
        List<RDFNode> fields = spec.getPropertyResourceValue(Ui.formFields).as(RDFList.class).asJavaList();
        for (RDFNode fieldN : fields) {
            Resource field = fieldN.asResource();
            String notation = RDFUtil.getStringValue(field, SKOS.notation);
            Property p = field.getPropertyResourceValue(Ui.property).as(Property.class);
            String value = form.getFirst(notation);
            if (value == null || value.isEmpty()) {
                if (RDFUtil.getBooleanValue(field, Ui.required, false)) {
                    throw new WebApiException(400, "Required field missing: " + notation);
                }
            } else {
                if (field.hasProperty(Ui.fieldType, Ui.anyURIField)) {
                    for (String uriIn : value.split(",")) {
                        String uri = uriIn.trim();
                        if (!uri.isEmpty()) {
                            resource.addProperty(p, model.createResource( Prefixes.get().expandPrefix( uri ) ) );
                        }
                    }
                } else {
                    resource.addProperty(p, value);
                }
            }
        }

        return resource;
    }
}
