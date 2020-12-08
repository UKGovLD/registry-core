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

    /**
     * Add the given property and its values to the projection.
     * @param prop The property to add.
     * @return This builder instance.
     */
    public ModelProjectionBuilder property(Property prop) {
        builders.add(new ModelProjection.Builder(prop, Projection.EMPTY));
        return this;
    }

    /**
     * Add the given property and its values to the projection, then project properties on those values.
     * @param prop The property to add.
     * @param nested Build the projection on the property values.
     * @return This builder instance.
     */
    public ModelProjectionBuilder property(Property prop, Consumer<ModelProjectionBuilder> nested) {
        ModelProjectionBuilder builder = new ModelProjectionBuilder();
        nested.accept(builder);
        builders.add(new ModelProjection.Builder(prop, builder));
        return this;
    }

    /**
     * Add the inversion of the given property and its subjects to the projection, then project properties on those subjects.
     * @param prop The property to add.
     * @param nested Build the projection on the property subjects.
     * @return This builder instance.
     */
    public ModelProjectionBuilder inverse(Property prop, Consumer<ModelProjectionBuilder> nested) {
        ModelProjectionBuilder builder = new ModelProjectionBuilder();
        nested.accept(builder);
        builders.add(new ModelInverseProjection.Builder(prop, builder));
        return this;
    }

    /**
     * Add the BNode closure of the root resource to the projection.
     * @return This builder instance.
     */
    public ModelProjectionBuilder closure() {
        builders.add(new ModelClosureProjection.Builder());
        return this;
    }

    /**
     * Project properties on the root resource by reading them from the store.
     * @param nested Build the projection.
     * @return This builder instance.
     */
    public ModelProjectionBuilder store(Consumer<StoreProjectionBuilder> nested) {
        StoreProjectionBuilder storeBuilder = new StoreProjectionBuilder();
        nested.accept(storeBuilder);
        builders.add(storeBuilder);
        return this;
    }

    /**
     * Add externally constructed triples to the projection.
     * @param construct Add the properties and values to the root resource.
     * @return This builder instance.
     */
    public ModelProjectionBuilder construct(Consumer<Resource> construct) {
        builders.add(new ConstructedProjection(construct));
        return this;
    }

    /**
     * Create the full projection.
     * @param store The store to query for related resources.
     * @param resource The root resource.
     * @return The projection.
     */
    public Projection build(StoreAPI store, Resource resource) {
        List<Projection> projections = builders.stream().map((builder) -> {
            return builder.build(store, resource);
        }).collect(Collectors.toList());

        return new Projection.Composite(projections);
    }
}
