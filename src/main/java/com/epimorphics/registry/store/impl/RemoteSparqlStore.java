package com.epimorphics.registry.store.impl;

import com.epimorphics.appbase.data.impl.RemoteSparqlSource;
import com.epimorphics.registry.store.Store;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.update.UpdateRequest;

import javax.ws.rs.NotSupportedException;
import java.util.ArrayList;
import java.util.List;

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
//        source.update(request);
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
//        queryQueue.add(builder.buildRequest().toString());
        source.update(builder.buildRequest());
    }

    @Override
    public void insertQuad(Quad t) {
        UpdateBuilder builder = new UpdateBuilder();
        builder.addInsert(t);
//        queryQueue.add(builder.buildRequest().toString());
        source.update(builder.buildRequest());
    }

    @Override
    public void removeQuad(Quad q) {
        UpdateBuilder builder = new UpdateBuilder();
        builder.addDelete(q);
//        queryQueue.add(builder.buildRequest().toString());
        source.update(builder.buildRequest());
    }

    @Override
    public void removeTriple(Triple t) {
        UpdateBuilder builder = new UpdateBuilder();
        builder.addDelete(t);
//        queryQueue.add(builder.buildRequest().toString());
        source.update(builder.buildRequest());
    }

    @Override
    public void addResource(Resource resource) {
        UpdateBuilder builder = new UpdateBuilder();
        resource.listProperties().forEachRemaining( statement -> builder.addInsert(statement.asTriple()) );
//        queryQueue.add(builder.buildRequest().toString());
        source.update(builder.buildRequest());
    }

    @Override
    public void addPropertyToResource(Resource resource, Property property, Resource object) {
        UpdateBuilder builder = new UpdateBuilder();
        builder.addInsert(
                new Triple(resource.asNode(), property.asNode(), object.asNode())
        );
//            queryQueue.add(builder.buildRequest().toString());
        source.update(builder.buildRequest());
    }

    @Override
    public void insertGraph(String name, Model graph) {
        if (!graph.isEmpty()) {
            UpdateBuilder builder = new UpdateBuilder();
            graph.listStatements().forEachRemaining(
                    stm -> builder.addInsert(Quad.create(NodeFactory.createURI(name), stm.asTriple()))
            );
//            queryQueue.add(builder.buildRequest().toString());
            source.update(builder.buildRequest());
        }
    }

    @Override
    public void deleteGraph(String name) {
        UpdateBuilder builder = new UpdateBuilder();

        Node graph = NodeFactory.createURI(name);

        builder.addDelete(Quad.create(graph, SUBJECT_G, PREDICATE_G, OBJECT_G));
        builder.addWhere(SUBJECT_G, PREDICATE_G, OBJECT_G);
//        queryQueue.add(builder.buildRequest().toString());
        source.update(builder.buildRequest());
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
//        queryQueue.add(builder.buildRequest().toString());
        source.update(builder.buildRequest());
    }

    @Override
    public void removeAll(Model model) {
        if (model.isEmpty()) return;
        UpdateBuilder builder = new UpdateBuilder();
        for (Statement stm : model.listStatements().toList()) {
            if (stm.getSubject().isURIResource() && !stm.getObject().asNode().isBlank()) {
                builder.addDelete(stm.asTriple());
            } else {
                Node subject = stm.getSubject().asNode();
                Node objekt = stm.getObject().asNode();
                if (!stm.getSubject().isURIResource()) {
                    subject = NodeFactory.createVariable(stm.getSubject().getId().getLabelString());
                    Expr filter = new ExprFactory().isBlank(subject);
                    builder.addFilter(filter);
                }
                if (stm.getObject().asNode().isBlank()) {
                    objekt = NodeFactory.createVariable(stm.getObject().toString());
                    Expr filter = new ExprFactory().isBlank(objekt);
                    builder.addFilter(filter);
                }
                builder.addDelete(subject, stm.getPredicate(), objekt);
                builder.addWhere(subject, stm.getPredicate(), objekt);
            }
        }
        source.update(builder.buildRequest());
    }

    @Override
    public Dataset asDataset() {
        throw new NotSupportedException("asDataset() not supported on RemoteSparqlStore");
    }

}
