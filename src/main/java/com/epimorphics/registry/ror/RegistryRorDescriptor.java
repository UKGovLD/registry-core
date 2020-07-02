package com.epimorphics.registry.ror;

import com.epimorphics.registry.ror.projection.ModelProjectionBuilder;
import com.epimorphics.registry.ror.projection.ProjectionBuilder;
import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;

public class RegistryRorDescriptor implements RorDescriptor {
    private static String RDF_FORMAT = "http://publications.europa.eu/resource/authority/file-type/RDF_XML";

    private static ProjectionBuilder REGISTRY_PROJECTION = new ModelProjectionBuilder()
            .property(DCTerms.title)
            .property(DCTerms.description)
            .property(DCTerms.accrualPeriodicity)
            .property(DCTerms.publisher, RegistryRorDescriptor::buildPublisher)
            .property(DCAT.dataset, (dataset) -> {
                dataset.construct(RegistryRorDescriptor::addDistribution);
            });

    static void buildPublisher(ModelProjectionBuilder builder) {
        builder.store((publisher) -> {
            publisher.property(RDF.type)
                    .property(FOAF.name)
                    .property(FOAF.mbox)
                    .property(FOAF.homepage);
        });
    }

    private static void addDistribution(Resource dataset) {
        Model model = dataset.getModel();
        String downloadUrl = dataset.getURI() + "?_format=ror";
        Resource distribution = model.createResource()
                .addProperty(DCTerms.format, model.createResource(RDF_FORMAT))
                .addProperty(DCAT.downloadURL, model.createResource(downloadUrl));
        dataset.addProperty(DCAT.distribution, distribution);
    }

    private final Resource catalog;

    RegistryRorDescriptor(Resource catalog) {
        this.catalog = catalog;
    }

    @Override public Model describe(StoreAPI store, Model model) {
        Model result = ModelFactory.createDefaultModel();
        result.setNsPrefixes(model);
        Resource catalog = this.catalog.inModel(result);
        catalog.addProperty(RDF.type, DCAT.Catalog);

        REGISTRY_PROJECTION.build(store, model).project(catalog);

        return result;
    }
}
