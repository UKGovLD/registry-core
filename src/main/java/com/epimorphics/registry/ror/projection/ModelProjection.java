package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class ModelProjection implements Projection {
    private final Model model;
    private final Property prop;
    private final Projection nested;

    ModelProjection(Model model, Property prop, Projection nested) {
        this.model = model;
        this.prop = prop;
        this.nested = nested;
    }

    @Override public void project(Resource res) {
        model.listObjectsOfProperty(res, prop).forEachRemaining((value) -> {
            res.addProperty(prop, value);

            if (value.isURIResource()) {
                Resource valueRes = value.asResource().inModel(res.getModel());
                nested.project(valueRes);
            }
        });
    }

    static class Builder implements ProjectionBuilder {
        private final Property prop;
        private final ProjectionBuilder next;

        Builder(Property prop, ProjectionBuilder next) {
            this.prop = prop;
            this.next = next;
        }

        @Override public Projection build(StoreAPI store, Model model) {
            return new com.epimorphics.registry.ror.projection.ModelProjection(model, prop, next.build(store, model));
        }
    }
}
