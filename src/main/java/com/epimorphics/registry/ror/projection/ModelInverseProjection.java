package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.util.Closure;

public class ModelInverseProjection implements Projection {
    private final Model model;
    private final Property prop;
    private final Projection nested;

    ModelInverseProjection(Model model, Property prop, Projection nested) {
        this.model = model;
        this.prop = prop;
        this.nested = nested;
    }

    @Override public void project(Resource res) {
        model.listStatements(null, prop, res).forEachRemaining((stmt) -> {
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

        @Override public Projection build(StoreAPI store, Model model) {
            return new com.epimorphics.registry.ror.projection.ModelInverseProjection(model, prop, next.build(store, model));
        }
    }
}