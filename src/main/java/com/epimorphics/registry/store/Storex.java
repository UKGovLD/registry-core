package com.epimorphics.registry.store;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Quad;

public interface Storex {
	<T> T read(ReadOperation<T> operation);
	void write(WriteOperation operation);

	interface ReadOperation<T> {
		T execute(ReadTransaction txn);
	}

	interface ReadTransaction {
		Model getGraph(String name);
		Model getDefaultModel();
		ResultSet query(String sparql);
	}

	interface WriteOperation {
		void execute(WriteTransaction txn);
	}

	interface WriteTransaction {
		void insertTriple(Triple t);
		void insertQuad(Quad t);

		void insertGraph(String name, Model graph);
		void updateGraph(String name, Model graph);
		void deleteGraph(String name);
	}
}
