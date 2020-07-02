package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;

import java.util.HashMap;
import java.util.Map;

public class StoreProjectionBuilder implements ProjectionBuilder {
    private final Map<Property, Projection> nodes = new HashMap<>();

    public StoreProjectionBuilder property(Property prop) {
        nodes.put(prop, Projection.EMPTY);
        return this;
    }

    @Override
    public Projection build(StoreAPI store, Model model) {
        return new StoreProjection(store, nodes);
    }
}