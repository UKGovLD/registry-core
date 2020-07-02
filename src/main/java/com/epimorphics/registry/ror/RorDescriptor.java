package com.epimorphics.registry.ror;

import com.epimorphics.registry.store.StoreAPI;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.vocabs.SKOS;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;

public interface RorDescriptor {
    Model describe(StoreAPI store, Model model);

    default Resource[] rootTypes() {
        return new Resource[]{};
    }

    class Empty implements RorDescriptor {
        @Override public Model describe(StoreAPI store, Model model) {
            return model;
        }
    }

    static RorDescriptor getInstance(Model model) {
        ResIterator catalogIt = instances(model, DCAT.Catalog);
        if (catalogIt.hasNext()) {
            return new RegistryRorDescriptor(catalogIt.nextResource());
        } else {
            ResIterator registerIt = instances(model, SKOS.ConceptScheme); // find root instead
            if (registerIt.hasNext()) {
                return new RegisterRorDescriptor(registerIt.nextResource());
            } else {
                return new RorDescriptor.Empty();
            }
        }
    }

    static ResIterator instances(Model model, Resource type) {
        return model.listSubjectsWithProperty(RDF.type, type);
    }
}
