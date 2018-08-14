package com.epimorphics.registry.store.impl;

import com.epimorphics.appbase.data.impl.RemoteSparqlSource;
import com.epimorphics.registry.store.Storex;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.TriplePath;

public class RemoteSparqlStore implements Storex, Storex.ReadTransaction, Storex.WriteTransaction {

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
        operation.execute(this);
    }

    @Override
    public <T> T read(ReadOperation<T> operation) {
        return operation.execute(this);
    }

    @Override
    public void updateGraph(String name, Model graph) {
        deleteGraph(name);
        insertGraph(name, graph);
    }

    @Override
    public void insertTriple(Triple t) {
        UpdateBuilder builder = new UpdateBuilder();
        builder.addInsert(t);
        source.update(builder.buildRequest());
    }

    @Override
    public void insertQuad(Quad t) {
        UpdateBuilder builder = new UpdateBuilder();
        builder.addInsert(t);
        source.update(builder.buildRequest());
    }

    @Override
    public void insertGraph(String name, Model graph) {
        UpdateBuilder builder = new UpdateBuilder();
        builder.with(name);
        graph.listStatements().forEachRemaining(stm -> builder.addInsert(stm.asTriple()));
        source.update(builder.buildRequest());
    }

    @Override
    public void deleteGraph(String name) {
        UpdateBuilder builder = new UpdateBuilder();
        Node graph = NodeFactory.createURI(name);
        WhereHandler subQuery = new WhereHandler();
        Node subject = NodeFactory.createVariable("s");
        Node predicate = NodeFactory.createVariable("p");
        Node objekt = NodeFactory.createVariable("o");
        subQuery.addWhere(new TriplePath( new Triple(subject, predicate, objekt)));
        builder.addGraph(graph, subQuery);
        source.update(builder.buildRequest());
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
}
