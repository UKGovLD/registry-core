package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

public class ModelProjection implements Projection {
    private final Resource resource;
    private final Property prop;
    private final Projection nested;

    ModelProjection(Resource resource, Property prop, Projection nested) {
        this.resource = resource;
        this.prop = prop;
        this.nested = nested;
    }

    @Override public void project(Resource res) {
        resource.listProperties(prop).mapWith(Statement::getObject).forEachRemaining((value) -> {
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

        @Override public Projection build(StoreAPI store, Resource resource) {
            return new ModelProjection(resource, prop, next.build(store, resource));
        }
    }
}
