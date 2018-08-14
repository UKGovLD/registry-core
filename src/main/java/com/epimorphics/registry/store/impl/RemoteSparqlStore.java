package com.epimorphics.registry.store.impl;

import com.epimorphics.appbase.data.impl.RemoteSparqlSource;
import com.epimorphics.registry.store.Store;
import com.epimorphics.registry.store.Storex;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Quad;

public class RemoteSparqlStore implements Store, Storex, Storex.ReadTransaction, Storex.WriteTransaction {

    private RemoteSparqlSource source = new RemoteSparqlSource();

    public void setEndpoint(String endpoint) {
        source.setEndpoint(endpoint);
    }

    public void setUpdateEndpoint(String endpoint) {
        source.setUpdateEndpoint(endpoint);
    }

    public void setGraphEndpoint(String endpoint) {
        source.setGraphEndpoint(endpoint);
    }

    public void setRemoteTimeout(Long timeout) {
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
        graph.listStatements().forEachRemaining(
                stm -> builder.addInsert(Quad.create(NodeFactory.createURI(name), stm.asTriple()))
        );
        source.update(builder.buildRequest());
    }

    @Override
    public void deleteGraph(String name) {
        UpdateBuilder builder = new UpdateBuilder();

        Node graph = NodeFactory.createURI(name);
        Node subject = NodeFactory.createVariable("s");
        Node predicate = NodeFactory.createVariable("p");
        Node objekt = NodeFactory.createVariable("o");

        builder.addDelete(Quad.create(graph, subject, predicate, objekt));
        builder.addWhere(subject, predicate, objekt);
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


    @Override
    public Dataset asDataset() {
        return null;
    }

    @Override
    public void abort() {

    }

    @Override
    public void commit() {

    }

    @Override
    public void end() {

    }

    @Override
    public void lock() {

    }

    @Override
    public void lockWrite() {

    }
}
