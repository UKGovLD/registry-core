package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.util.Closure;

class ModelClosureProjection implements Projection {
    private final Model model;

    ModelClosureProjection(Model model) {
        this.model = model;
    }

    @Override public void project(Resource res) {
        Model closure = Closure.closure(res.inModel(model), false);
        res.getModel().add(closure);
    }

    static class Builder implements ProjectionBuilder {
        @Override public Projection build(StoreAPI store, Model model) {
            return new ModelClosureProjection(model);
        }
    }
}
