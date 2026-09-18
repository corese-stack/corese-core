package fr.inria.corese.core.next.query.impl.sparql.execution;


import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end dataset pipeline tests. */
class NextSparqlPipelineDatasetExecutorTest extends PipelineTestSupport {

    @Test
    @DisplayName("VALUES joins graph patterns at its position and rejects incompatible mappings")
    void inlineValuesJoinsGraphPatterns() {
        insert(iri(BOB), iri(KNOWS), iri(ALICE));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person ?friend WHERE {
                  VALUES ?person { <http://example.org/alice> <http://example.org/missing> }
                  ?person <http://example.org/knows> ?friend .
                }
                """);

        assertTrue(result.hasNext());
        var binding = result.next();
        assertEquals(ALICE, binding.getValue("person").stringValue());
        assertEquals(BOB, binding.getValue("friend").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("exists-graph-variable: EXISTS uses the outer graph binding")
    void existsGraphVariable() {
        String ex = "http://www.example.org/";
        IRI graph = iri(ex + "exists-graph-variable.ttl");
        // The W3C fixture is loaded as both default data and a named graph;
        // its relative <> resolves to the fixture's graph IRI.
        insert(iri(ex + "s1"), iri(ex + "p"), graph);
        insert(iri(ex + "s2"), iri(ex + "p"), iri(ex + "o2"));
        insertInGraph(iri(ex + "s1"), iri(ex + "p"), graph, graph);
        insertInGraph(iri(ex + "s2"), iri(ex + "p"), iri(ex + "o2"), graph);
        try (var result = executor.evaluateTuple("""
                PREFIX : <http://www.example.org/>
                SELECT ?s WHERE {
                  ?s :p ?g .
                  FILTER EXISTS { GRAPH ?g { ?s2 :p ?o2 } }
                }
                """)) {
            assertEquals(List.of(ex + "s1"),
                    result.stream().map(row -> row.getValue("s").stringValue()).toList());
        }
    }

    @Test
    @DisplayName("EXISTS waits for its GRAPH variable even after an unrelated BIND")
    void existsGraphVariableWaitsForOuterBinding() {
        String ex = "http://www.example.org/";
        IRI graph = iri(ex + "g");
        insert(iri(ex + "s1"), iri(ex + "p"), graph);
        insert(iri(ex + "s2"), iri(ex + "p"), iri(ex + "missing"));
        insertInGraph(iri(ex + "a"), iri(ex + "q"), iri(ex + "b"), graph);
        try (var result = executor.evaluateTuple("""
                PREFIX : <http://www.example.org/>
                SELECT ?s {
                  BIND(1 AS ?unrelated)
                  ?s :p ?g .
                  FILTER EXISTS { GRAPH ?g { ?s2 :q ?o2 } }
                }
                """)) {
            assertEquals(List.of(ex + "s1"),
                    result.stream().map(row -> row.getValue("s").stringValue()).toList());
        }
    }

    @Test
    @DisplayName("Default graph merge removes duplicate triples, preserving projected and named-graph rows")
    void defaultGraphMergePreservesSolutionMultiplicity() {
        IRI firstGraph = iri("http://example.org/g1");
        IRI secondGraph = iri("http://example.org/g2");
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), firstGraph);
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), secondGraph);
        insertInGraph(iri(ALICE), iri(KNOWS), iri("http://example.org/carol"), secondGraph);
        Dataset dataset = Dataset.builder().defaultGraph(firstGraph).defaultGraph(secondGraph)
                .namedGraph(firstGraph).namedGraph(secondGraph).build();
        try (var result = executor.evaluateTuple(
                "SELECT ?s { ?s <http://example.org/knows> ?o }", null, dataset, 0L)) {
            assertEquals(List.of(ALICE, ALICE),
                    result.stream().map(row -> row.getValue("s").stringValue()).toList());
        }
        try (var result = executor.evaluateTuple(
                "SELECT ?g { GRAPH ?g { ?s <http://example.org/knows> ?o } }", null, dataset, 0L)) {
            assertEquals(3, result.stream().count());
        }
    }

    @Test
    @DisplayName("SELECT with dataset FROM restricts results to the named graph")
    void selectWithDatasetFromRestrictsToNamedGraph() {
        String graph1 = "http://example.org/graph1";
        String graph2 = "http://example.org/graph2";
        String carol = "http://example.org/carol";

        // alice→knows→bob in graph1, alice→knows→carol in graph2
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri(graph1));
        insertInGraph(iri(ALICE), iri(KNOWS), iri(carol), iri(graph2));

        Dataset dataset = Dataset.builder().defaultGraph(iri(graph1)).build();
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?o WHERE { <" + ALICE + "> <" + KNOWS + "> ?o }",
                null, dataset, 0L);

        assertTrue(result.hasNext());
        assertEquals(BOB, result.next().getValue("o").stringValue());
        assertFalse(result.hasNext(), "Only data from graph1 should be visible");
    }

    @Test
    @DisplayName("SELECT with dataset FROM returns nothing when named graph is empty")
    void selectWithDatasetFromReturnsNothingForEmptyNamedGraph() {
        String emptyGraph = "http://example.org/empty";

        Dataset dataset = Dataset.builder().defaultGraph(iri(emptyGraph)).build();
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT * WHERE { ?s ?p ?o }",
                null, dataset, 0L);

        assertFalse(result.hasNext(), "No results expected for an empty named graph");
    }

    @Test
    @DisplayName("An explicit empty dataset hides repository data")
    void explicitEmptyDatasetHasNoDefaultGraph() {
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT * WHERE { ?s ?p ?o }",
                null, Dataset.empty(), 0L);

        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("A named-only dataset has an empty default graph")
    void namedOnlyDatasetHasNoDefaultGraph() {
        String graph = "http://example.org/graph";
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri(graph));
        Dataset dataset = Dataset.builder().namedGraph(iri(graph)).build();

        TupleQueryResult defaultResult = executor.evaluateTuple(
                "SELECT * WHERE { ?s ?p ?o }", null, dataset, 0L);

        assertFalse(defaultResult.hasNext());
    }

    // -------------------------------------------------------------------------
    // CONSTRUCT / graph evaluation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Graph evaluation rejects non-CONSTRUCT/DESCRIBE queries")
    void graphEvaluationRejectsSelectQuery() {
        assertThrows(
                IllegalArgumentException.class,
                () -> executor.evaluateGraph("SELECT * WHERE { ?s ?p ?o }"));
    }

    // -------------------------------------------------------------------------
    // Initial bindings — literal and blank-node values
    // -------------------------------------------------------------------------

    @Test
    void valuesInsideGraphPreservesUnboundGraphRows() {
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri("http://example.org/g1"));
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri("http://example.org/g2"));
        try (var result = executor.evaluateTuple("""
                SELECT ?g ?t WHERE {
                  GRAPH ?g {
                    VALUES (?g ?t) { (UNDEF "foo") (<http://example.org/g1> "bar") }
                  }
                }
                """)) {
            assertEquals(List.of("http://example.org/g1:bar", "http://example.org/g1:foo", "http://example.org/g2:foo"),
                    result.stream().map(row -> row.getValue("g").stringValue() + ":" + row.getValue("t").stringValue())
                            .sorted().toList());
        }
    }

    @Test
    void graphNameIsNotAnInputBindingForItsInnerBind() {
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri("http://example.org/g1"));
        try (var result = executor.evaluateTuple("SELECT ?g ?copy { GRAPH ?g { BIND(?g AS ?copy) } }")) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("http://example.org/g1", row.getValue("g").stringValue());
            assertFalse(row.hasBinding("copy"));
            assertFalse(result.hasNext());
        }
    }

    @Test
    void graphWithValuesRequiresAnExistingNamedGraph() {
        insertInGraph(iri(ALICE), iri(KNOWS), iri(BOB), iri("http://example.org/g1"));
        assertTrue(executor.evaluateBoolean("ASK { GRAPH <http://example.org/g1> { VALUES ?x { 1 } } }"));
        assertFalse(executor.evaluateBoolean("ASK { GRAPH <http://example.org/missing> { VALUES ?x { 1 } } }"));
        assertFalse(executor.evaluateBoolean("""
                ASK FROM NAMED <http://example.org/other>
                { GRAPH <http://example.org/g1> { VALUES ?x { 1 } } }
                """));
    }

}
