package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.Map;

public class StoreProjection implements Projection {
    private final StoreAPI store;
    private final Map<Property, Projection> nodes;

    StoreProjection(StoreAPI store, Map<Property, Projection> nodes) {
        this.store = store;
        this.nodes = nodes;
    }

    @Override public void project(Resource res) {
        Description desc = store.getDescription(res.getURI());
        if (desc != null) {
            Resource root = desc.getRoot();
            nodes.forEach((prop, proj) -> {
                root.listProperties(prop).mapWith(Statement::getObject).forEachRemaining((value) -> {
                    res.addProperty(prop, value);

                    if (value.isURIResource()) {
                        Resource valueRes = value.asResource().inModel(res.getModel());
                        proj.project(valueRes);
                    }
                });
            });
        }
    }
}
