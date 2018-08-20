package com.epimorphics.registry.store.impl;

import com.epimorphics.appbase.data.impl.RemoteSparqlSource;
import com.epimorphics.registry.store.Store;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.update.UpdateRequest;

import javax.ws.rs.NotSupportedException;
import java.util.ArrayList;

public class RemoteSparqlStore implements Store {

    private final Node SUBJECT_G = NodeFactory.createVariable("s");
    private final Node PREDICATE_G = NodeFactory.createVariable("p");
    private final Node OBJECT_G = NodeFactory.createVariable("o");

    private RemoteSparqlSource source = new RemoteSparqlSource();

    private ArrayList<String> queryQueue = new ArrayList<>();

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
    public void abort() {
        queryQueue = new ArrayList<>();
    }

    @Override
    public void commit() {
        UpdateRequest request = new UpdateRequest();
        request.add(String.join(";", queryQueue));
        source.update(request);
        queryQueue = new ArrayList<>();
    }

    @Override
    public void end() {
        // TODO
    }

    @Override
    public void lock() {
        // TODO
    }

    @Override
    public void lockWrite() {
        // TODO
    }

    @Override
    public Model getDefaultModel() {
        return source.getAccessor().getModel();
    }

    @Override
    public Model getGraph(String name) {
        return source.getAccessor().getModel(name);
    }

    @Override
    public ResultSet query(String sparql) {
        return source.select(sparql);
    }

    @Override
    public void insertTriple(Triple t) {
        UpdateBuilder builder = new UpdateBuilder();
        builder.addInsert(t);
        queryQueue.add(builder.buildRequest().toString());
    }

    @Override
    public void insertQuad(Quad t) {
        UpdateBuilder builder = new UpdateBuilder();
        builder.addInsert(t);
        queryQueue.add(builder.buildRequest().toString());
    }

    @Override
    public void addResource(Resource resource) {
        UpdateBuilder builder = new UpdateBuilder();
        resource.listProperties().forEachRemaining( statement -> builder.addInsert(statement.asTriple()) );
        queryQueue.add(builder.buildRequest().toString());
    }

    @Override
    public void patchResource(Resource resource) {
        String query = "DELETE WHERE {\n" +
            "  <" + resource.getURI() + "> ?p ?o .\n" +
            "}";
        UpdateRequest request = new UpdateRequest().add(query);
        queryQueue.add(request.toString());

        UpdateBuilder insertBuilder = new UpdateBuilder();
        resource.listProperties().forEachRemaining( statement -> insertBuilder.addInsert(statement.asTriple()) );
        queryQueue.add(insertBuilder.buildRequest().toString());
    }

    @Override
    public void insertGraph(String name, Model graph) {
        UpdateBuilder builder = new UpdateBuilder();
        graph.listStatements().forEachRemaining(
                stm -> builder.addInsert(Quad.create(NodeFactory.createURI(name), stm.asTriple()))
        );
        queryQueue.add(builder.buildRequest().toString());
    }

    @Override
    public void deleteGraph(String name) {
        UpdateBuilder builder = new UpdateBuilder();

        Node graph = NodeFactory.createURI(name);

        builder.addDelete(Quad.create(graph, SUBJECT_G, PREDICATE_G, OBJECT_G));
        builder.addWhere(SUBJECT_G, PREDICATE_G, OBJECT_G);
        queryQueue.add(builder.buildRequest().toString());
    }


    @Override
    public void updateGraph(String name, Model graph) {
        deleteGraph(name);
        insertGraph(name, graph);
    }

    @Override
    public void addAll(Model model) {
        UpdateBuilder builder = new UpdateBuilder();
        model.listStatements().forEachRemaining(
                stm -> builder.addInsert(stm.asTriple())
        );
        queryQueue.add(builder.buildRequest().toString());
    }

    @Override
    public void removeAll(Model model) {
        UpdateBuilder builder = new UpdateBuilder();
        model.listStatements().forEachRemaining(
                stm -> builder.addDelete(stm.asTriple())
        );
        queryQueue.add(builder.buildRequest().toString());
    }

    @Override
    public Dataset asDataset() {
        throw new NotSupportedException("asDataset() not supported on RemoteSparqlStore");
    }

}
