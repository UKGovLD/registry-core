package com.epimorphics.registry.store;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Quad;

public interface Storex {
	void write(WriteOperation operation);
	Model getGraph(String name);
	Model getDefaultModel();
	ResultSet query(String sparql);

	interface WriteOperation {
		void execute(Transaction txn);
	}

	interface Transaction {
		void insertTriple(Triple t);
		void insertQuad(Quad t);

		void insertGraph(String name, Model graph);
		void updateGraph(String name, Model graph);
		void deleteGraph(String name);
	}
}
