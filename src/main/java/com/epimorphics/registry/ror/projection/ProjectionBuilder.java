package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Resource;

public interface ProjectionBuilder {
    Projection build(StoreAPI store, Resource resource);
}
