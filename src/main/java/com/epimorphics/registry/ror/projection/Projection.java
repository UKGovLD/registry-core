package com.epimorphics.registry.ror.projection;

import com.epimorphics.registry.store.StoreAPI;
import org.apache.jena.rdf.model.Resource;

import java.util.List;

public interface Projection {
    void project(Resource res);

    Empty EMPTY = new Empty();

    class Empty implements Projection, ProjectionBuilder {
        @Override public void project(Resource res) {
            // do nothing
        }

        @Override public Projection build(StoreAPI store, Resource resource) {
            return Projection.EMPTY;
        }
    }

    class Composite implements Projection {
        private final List<Projection> projections;

        Composite(List<Projection> projections) {
            this.projections = projections;
        }

        @Override public void project(Resource res) {
            projections.forEach(projection -> {
                projection.project(res);
            });
        }
    }
}
