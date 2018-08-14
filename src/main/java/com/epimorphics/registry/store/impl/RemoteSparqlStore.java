package com.epimorphics.registry.store.impl;

import com.epimorphics.appbase.data.impl.RemoteSparqlSource;
import com.epimorphics.registry.store.Storex;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Quad;

public class RemoteSparqlStore implements Storex {

    private RemoteSparqlSource source = new RemoteSparqlSource();

    public RemoteSparqlStore(String endpoint, String updateEndpoint, String graphEndpoint, Long timeout) {
        source.setEndpoint(endpoint);
        source.setUpdateEndpoint(updateEndpoint);
        source.setGraphEndpoint(graphEndpoint);
        source.setRemoteTimeout(timeout);
    }

    @Override
    public Model getDefaultModel() {
        SelectBuilder builder = selectAllBuilder();

        ResultSet result = query(builder.buildString());
        return result.getResourceModel();
    }

    @Override
    public Model getGraph(String name) {
        SelectBuilder builder = selectAllBuilder();
        builder.from(name);

        ResultSet result = query(builder.buildString());
        return result.getResourceModel();
    }

    @Override
    public ResultSet query(String sparql) {
        return source.select(sparql);
    }

    @Override
    public void write(WriteOperation operation) {

    }

    private SelectBuilder selectAllBuilder() {
        SelectBuilder builder = new SelectBuilder();

        Node subject = NodeFactory.createVariable("s");
        Node predicate = NodeFactory.createVariable("p");
        Node objekt = NodeFactory.createVariable("o");

        builder.addVar(subject);
        builder.addVar(predicate);
        builder.addVar(objekt);

        builder.addWhere(new Triple(subject, predicate, objekt));

        return builder;
    }

    private class SparqlTransaction implements Transaction {
        @Override
        public void deleteGraph(String name) {

        }

        @Override
        public void insertGraph(String name, Model graph) {

        }

        @Override
        public void insertQuad(Quad t) {

        }

        @Override
        public void insertTriple(Triple t) {

        }

        @Override
        public void updateGraph(String name, Model graph) {
            
        }
    }
}
