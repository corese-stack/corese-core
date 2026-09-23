package fr.inria.corese.core.next.query.impl.engine.storage;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.query.Repositories;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.storage.StorageModels;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests that verify named-graph data does not bleed into default-graph
 * triple patterns, and vice versa (SPARQL 1.1 dataset isolation).
 */
@DisplayName("Graph isolation - default graph vs named graphs")
class GraphIsolationTest {

    private static final String EX = "http://example.org/";

    private static IRI iri(String local) {
        return Values.factory().createIRI(EX + local);
    }

    /**
     * Builds a shared storage + model, queries it via an open connection,
     * then closes both. The {@code setup} lambda populates the model before
     * the query is executed.
     */
    private int countRows(ModelSetup setup, String sparql) throws Exception {
        StorageManager storage = MemoryStorageManager.builder().build();
        Model model = StorageModels.create(storage);
        setup.accept(model);
        try (Repository repo = Repositories.create(storage);
             RepositoryConnection conn = repo.getConnection();
             TupleQueryResult result = conn.prepareTupleQuery(sparql).evaluate()) {
            int rows = 0;
            while (result.hasNext()) {
                result.next();
                rows++;
            }
            return rows;
        }
    }

    private List<String> graphValues(ModelSetup setup) throws Exception {
        StorageManager storage = MemoryStorageManager.builder().build();
        Model model = StorageModels.create(storage);
        setup.accept(model);
        try (Repository repo = Repositories.create(storage);
             RepositoryConnection conn = repo.getConnection();
             TupleQueryResult result = conn.prepareTupleQuery(
                     "SELECT ?g WHERE { GRAPH ?g { ?s ?p ?o } }").evaluate()) {
            List<String> graphs = new ArrayList<>();
            while (result.hasNext()) {
                graphs.add(result.next().getValue("g").stringValue());
            }
            return graphs;
        }
    }

    @FunctionalInterface
    interface ModelSetup {
        void accept(Model model) throws Exception;
    }

    @Test
    @DisplayName("graph-02: only named graph data — default graph query returns 0 rows")
    void namedGraphOnlyDefaultQueryReturnsZero() throws Exception {
        int rows = countRows(
                model -> model.add(iri("s"), iri("p"), iri("o"), iri("g1")),
                "SELECT * { ?s ?p ?o }");
        assertEquals(0, rows,
                "Triples stored only in a named graph must not appear in a default-graph pattern");
    }

    @Test
    @DisplayName("graph-05: default + named — default graph query returns only default rows")
    void defaultAndNamedQueryReturnsOnlyDefault() throws Exception {
        int rows = countRows(model -> {
                    // 2 triples in default graph (null context)
                    model.add(iri("s1"), iri("p"), iri("o1"));
                    model.add(iri("s2"), iri("p"), iri("o2"));
                    // 1 triple in named graph
                    model.add(iri("s3"), iri("p"), iri("o3"), iri("g1"));
                },
                "SELECT * { ?s ?p ?o }");
        assertEquals(2, rows,
                "Default-graph query must return only the 2 triples from the null context");
    }

    @Test
    @DisplayName("GRAPH ?g — iterates only named graphs, never the default graph")
    void graphVariableIteratesOnlyNamedGraphs() throws Exception {
        List<String> graphs = graphValues(model -> {
            // 1 triple in default graph — must NOT appear via GRAPH ?g
            model.add(iri("s0"), iri("p"), iri("o0"));
            // 2 triples in distinct named graphs
            model.add(iri("s1"), iri("p"), iri("o1"), iri("g1"));
            model.add(iri("s2"), iri("p"), iri("o2"), iri("g2"));
        });
        assertEquals(List.of(EX + "g1", EX + "g2"), graphs.stream().sorted().toList(),
                "GRAPH ?g must bind to exactly the 2 named-graph IRIs");
    }

    @Test
    @DisplayName("UNION of default and named graphs produces combined results")
    void unionDefaultAndNamedReturnsAll() throws Exception {
        int rows = countRows(model -> {
                    model.add(iri("s1"), iri("p"), iri("o1")); // default
                    model.add(iri("s2"), iri("p"), iri("o2")); // default
                    model.add(iri("s3"), iri("p"), iri("o3"), iri("g1")); // named
                },
                "SELECT * { { ?s ?p ?o } UNION { GRAPH ?g { ?s ?p ?o } } }");
        assertEquals(3, rows,
                "UNION of default (2) and named (1) must return 3 distinct rows");
    }

    @Test
    @DisplayName("JOIN default + GRAPH — only subjects present in both graphs match")
    void joinDefaultAndNamedGraphFiltersToCommonSubjects() throws Exception {
        int rows = countRows(model -> {
                    // subject s1 appears in default AND named graph
                    model.add(iri("s1"), iri("p"), iri("o1"));          // default
                    model.add(iri("s1"), iri("q"), iri("v1"), iri("g1")); // named
                    // subject s2 appears only in default
                    model.add(iri("s2"), iri("p"), iri("o2"));
                },
                "SELECT * { ?s ?p ?o . GRAPH ?g { ?s ?q ?v } }");
        assertEquals(1, rows,
                "JOIN must produce exactly 1 row: the subject appearing in both default and named");
    }
}
