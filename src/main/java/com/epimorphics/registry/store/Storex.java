package com.epimorphics.registry.store;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Quad;

public interface Storex {
	<T> T read(ReadOperation<T> operation);
	void write(WriteOperation operation);

	interface Operation<T, R> {
		R execute(T txn);
	}

	interface ReadOperation<R> extends Operation<ReadTransaction, R> {}
	interface WriteOperation extends Operation<WriteTransaction, Void> {}
	interface UpdateOperation extends Operation<Model, Void> {}

	interface ReadTransaction {
		Model getGraph(String name);
		Model getDefaultModel();
		ResultSet query(String sparql);
	}

	interface WriteTransaction extends ReadTransaction {
		void insertTriple(Triple t); // default graph
		void insertQuad(Quad q);

		void addResource(Resource resource);
		void patchResource(Resource resource);

		void addAll(Model model); // default graph
		void removeAll(Model model); // default graph

		void insertGraph(String name, Model graph);
		void updateGraph(String name, Model graph);
		void deleteGraph(String name);
	}
}
