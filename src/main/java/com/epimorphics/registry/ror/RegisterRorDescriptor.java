package com.epimorphics.registry.ror;

import com.epimorphics.registry.core.Registry;
import com.epimorphics.registry.ror.projection.ModelProjectionBuilder;
import com.epimorphics.registry.ror.projection.ProjectionBuilder;
import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

public class RegisterRorDescriptor implements RorDescriptor {

    private static ProjectionBuilder REGISTER_PROJECTION = new ModelProjectionBuilder()
            .property(SKOS.prefLabel)
            .property(SKOS.definition)
            .property(DCTerms.accrualPeriodicity)
            .property(DCTerms.isPartOf, (catalog) -> {
                catalog.construct(c -> c.addProperty(RDF.type, DCAT.Catalog));
            })
            .property(DCTerms.publisher, RegistryRorDescriptor::buildPublisher)
            .property(ResourceFactory.createProperty("http://purl.org/vocommons/voaf#reliesOn"))
            .inverse(SKOS.inScheme, ModelProjectionBuilder::closure);

    private final Resource conceptScheme;

    RegisterRorDescriptor(Resource conceptScheme) {
        this.conceptScheme = conceptScheme;
    }

    @Override
    public Model describe(Registry registry, StoreAPI store, Model model) {
        Model result = ModelFactory.createDefaultModel();
        result.setNsPrefixes(model);
        Resource conceptScheme = this.conceptScheme.inModel(result);
        conceptScheme.addProperty(RDF.type, SKOS.ConceptScheme);

        REGISTER_PROJECTION.build(store, this.conceptScheme).project(conceptScheme);

        return result;
    }

    @Override
    public Resource[] rootTypes() {
        return new Resource[] { SKOS.Concept, SKOS.ConceptScheme };
    }
}
