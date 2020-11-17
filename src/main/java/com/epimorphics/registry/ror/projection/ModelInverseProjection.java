package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class ModelInverseProjection implements Projection {
    private final Resource resource;
    private final Property prop;
    private final Projection nested;

    ModelInverseProjection(Resource resource, Property prop, Projection nested) {
        this.resource = resource;
        this.prop = prop;
        this.nested = nested;
    }

    @Override public void project(Resource res) {
        resource.getModel().listStatements(null, prop, resource).forEachRemaining((stmt) -> {
            Model resModel = res.getModel();
            resModel.add(stmt);
            Resource value = stmt.getSubject().inModel(resModel);
            nested.project(value);
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
            return new com.epimorphics.registry.ror.projection.ModelInverseProjection(resource, prop, next.build(store, resource));
        }
    }
}