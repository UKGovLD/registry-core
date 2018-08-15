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
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Quad;

public class RemoteSparqlStore implements Store, Storex, Storex.WriteTransaction {

    private final Node SUBJECT_G = NodeFactory.createVariable("s");
    private final Node PREDICATE_G = NodeFactory.createVariable("p");
    private final Node OBJECT_G = NodeFactory.createVariable("o");

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
    public void addResource(Resource resource) {
        UpdateBuilder builder = new UpdateBuilder();
        resource.listProperties().forEachRemaining( statement -> builder.addInsert(statement.asTriple()) );
        source.update(builder.buildRequest());
    }

    @Override
    public void patchResource(Resource resource) {
        // TODO: Review this to make sure it makes sense!
        UpdateBuilder builder = new UpdateBuilder();
        builder.addDelete(new Triple(SUBJECT_G, PREDICATE_G, OBJECT_G));
        builder.addWhere(NodeFactory.createURI(resource.getURI()), PREDICATE_G, OBJECT_G);
        resource.listProperties().forEachRemaining( statement -> builder.addInsert(statement.asTriple()) );
        source.update(builder.buildRequest());
    }

    @Override
    public void insertGraph(String name, Model graph) {
        source.getAccessor().add(name, graph);
    }

    @Override
    public void deleteGraph(String name) {
        source.getAccessor().deleteModel(name);
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
        source.update(builder.buildRequest());
    }

    @Override
    public void removeAll(Model model) {
        UpdateBuilder builder = new UpdateBuilder();
        model.listStatements().forEachRemaining(
                stm -> builder.addDelete(stm.asTriple())
        );
        source.update(builder.buildRequest());
    }

    private SelectBuilder selectAllBuilder() {
        SelectBuilder builder = new SelectBuilder();

        builder.addVar(SUBJECT_G);
        builder.addVar(PREDICATE_G);
        builder.addVar(OBJECT_G);

        builder.addWhere(new Triple(SUBJECT_G, PREDICATE_G, OBJECT_G));

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
