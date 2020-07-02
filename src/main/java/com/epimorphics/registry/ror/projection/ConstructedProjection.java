package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.util.function.Consumer;

public class ConstructedProjection implements Projection, ProjectionBuilder {
    private Consumer<Resource> construct;

    ConstructedProjection(Consumer<Resource> construct) {
        this.construct = construct;
    }

    @Override public void project(Resource res) {
        construct.accept(res);
    }

    @Override public Projection build(StoreAPI store, Model model) {
        return this;
    }
}
