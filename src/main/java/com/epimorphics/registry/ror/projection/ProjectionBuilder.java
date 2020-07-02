package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Model;

public interface ProjectionBuilder {
    Projection build(StoreAPI store, Model model);
}
