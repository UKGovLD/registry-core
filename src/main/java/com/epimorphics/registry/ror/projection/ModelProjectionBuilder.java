package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ModelProjectionBuilder implements ProjectionBuilder {
    private final List<ProjectionBuilder> builders = new ArrayList<>();

    public ModelProjectionBuilder property(Property prop) {
        builders.add(new ModelProjection.Builder(prop, Projection.EMPTY));
        return this;
    }

    public ModelProjectionBuilder property(Property prop, Consumer<ModelProjectionBuilder> nested) {
        ModelProjectionBuilder builder = new ModelProjectionBuilder();
        nested.accept(builder);
        builders.add(new ModelProjection.Builder(prop, builder));
        return this;
    }

    public ModelProjectionBuilder inverse(Property prop, Consumer<ModelProjectionBuilder> nested) {
        ModelProjectionBuilder builder = new ModelProjectionBuilder();
        nested.accept(builder);
        builders.add(new ModelInverseProjection.Builder(prop, builder));
        return this;
    }

    public ModelProjectionBuilder closure() {
        builders.add(new ModelClosureProjection.Builder());
        return this;
    }

    public ModelProjectionBuilder store(Consumer<StoreProjectionBuilder> nested) {
        StoreProjectionBuilder storeBuilder = new StoreProjectionBuilder();
        nested.accept(storeBuilder);
        builders.add(storeBuilder);
        return this;
    }

    public ModelProjectionBuilder construct(Consumer<Resource> construct) {
        builders.add(new ConstructedProjection(construct));
        return this;
    }

    public Projection build(StoreAPI store, Resource resource) {
        List<Projection> projections = builders.stream().map((builder) -> {
            return builder.build(store, resource);
        }).collect(Collectors.toList());

        return new Projection.Composite(projections);
    }
}
