package fr.inria.corese.core.next.query.impl.sparql.execution;


import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.exception.QueryTimeoutException;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end core pipeline tests. */
class NextSparqlPipelineExecutorTest extends PipelineTestSupport {

    @Test
    @DisplayName("SELECT * WHERE { ?s ?p ?o } runs through the next pipeline")
    void selectSpoRunsEndToEnd() {
        TupleQueryResult result = executor.evaluateTuple("SELECT * WHERE { ?s ?p ?o }");

        assertEquals(List.of("s", "p", "o"), result.getBindingNames());
        assertTrue(result.hasNext());
        var binding = result.next();
        assertEquals(ALICE, binding.getValue("s").stringValue());
        assertEquals(KNOWS, binding.getValue("p").stringValue());
        assertEquals(BOB, binding.getValue("o").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("SELECT with repeated variables joins BGP patterns end-to-end")
    void selectJoinRunsEndToEnd() {
        insert(iri(BOB), iri(NAME), valueFactory.createLiteral("Bob"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?s ?name WHERE {
                  ?s <http://example.org/knows> ?friend .
                  ?friend <http://example.org/name> ?name .
                }
                """);

        assertEquals(List.of("s", "name"), result.getBindingNames());
        assertTrue(result.hasNext());
        var binding = result.next();
        assertEquals(ALICE, binding.getValue("s").stringValue());
        assertEquals("Bob", binding.getValue("name").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("SELECT REDUCED executes and returns projected bindings")
    void selectReducedExecutesAndReturnsBindings() {
        try (var result = executor.evaluateTuple("""
                SELECT REDUCED ?name WHERE {
                  VALUES (?name ?id) { ("Alice" 1) ("Bob" 3) }
                } ORDER BY ?name
                """)) {
            assertEquals(List.of("name"), result.getBindingNames());
            assertTrue(result.hasNext());
            assertEquals(valueFactory.createLiteral("Alice"), result.next().getValue("name"));
            assertTrue(result.hasNext());
            assertEquals(valueFactory.createLiteral("Bob"), result.next().getValue("name"));
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("SELECT REDUCED * executes without error")
    void selectReducedStarExecutes() {
        try (var result = executor.evaluateTuple("""
                SELECT REDUCED * WHERE { VALUES ?name { "Alice" } }
                """)) {
            assertEquals(List.of("name"), result.getBindingNames());
            assertTrue(result.hasNext());
            assertEquals(valueFactory.createLiteral("Alice"), result.next().getValue("name"));
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("BIND in a UNION branch leaves references outside its group unbound")
    void bindInUnionBranchDoesNotReceiveOuterBindings() {
        String age = "http://example.org/age";
        insert(iri(ALICE), iri(age), valueFactory.createLiteral(1));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?nextAge WHERE {
                  ?person <http://example.org/age> ?age .
                  { BIND(?age + 1 AS ?nextAge) } UNION { BIND(?age + 2 AS ?nextAge) }
                }
                """);

        var rows = result.stream().toList();
        assertEquals(2, rows.size());
        assertTrue(rows.stream().noneMatch(binding -> binding.hasBinding("nextAge")));
    }

    @Test
    @DisplayName("ORDER BY uses numeric value order rather than lexical order")
    void orderByUsesNativeRdfValueOrder() {
        String rank = "http://example.org/rank";
        insert(iri(ALICE), iri(rank), valueFactory.createLiteral(10));
        insert(iri(BOB), iri(rank), valueFactory.createLiteral(2));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?rank WHERE { ?person <http://example.org/rank> ?rank }
                ORDER BY ?rank
                """);

        assertEquals(2, ((fr.inria.corese.core.next.data.api.term.Literal)
                result.next().getValue("rank")).intValue());
        assertEquals(10, ((fr.inria.corese.core.next.data.api.term.Literal)
                result.next().getValue("rank")).intValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Inline VALUES preserves row order, duplicates, and UNDEF bindings")
    void inlineValuesPreservesBagSemanticsAndUndef() {
        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person ?rank WHERE {
                  VALUES (?person ?rank) {
                    (<http://example.org/alice> 1)
                    (<http://example.org/alice> 1)
                    (<http://example.org/bob> UNDEF)
                  }
                }
                """);

        assertEquals(List.of("person", "rank"), result.getBindingNames());
        var first = result.next();
        assertEquals(ALICE, first.getValue("person").stringValue());
        assertEquals(1, ((Literal) first.getValue("rank")).intValue());
        var duplicate = result.next();
        assertEquals(ALICE, duplicate.getValue("person").stringValue());
        assertEquals(1, ((Literal) duplicate.getValue("rank")).intValue());
        var unbound = result.next();
        assertEquals(BOB, unbound.getValue("person").stringValue());
        assertFalse(unbound.hasBinding("rank"));
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Query-level VALUES constrains the completed WHERE result")
    void queryLevelValuesConstrainWhereResults() {
        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person ?friend WHERE {
                  ?person <http://example.org/knows> ?friend .
                }
                VALUES ?person { <http://example.org/alice> }
                """);

        assertTrue(result.hasNext());
        assertEquals(ALICE, result.next().getValue("person").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Query-level VALUES constrains a semicolon-expanded basic graph pattern")
    void queryLevelValuesConstrainSemicolonExpandedPattern() {
        String title = "http://purl.org/dc/elements/1.1/title";
        String price = "http://example.org/ns#price";
        String secondBook = "http://example.org/book/book2";
        insert(iri(ALICE), iri(title), valueFactory.createLiteral("first"));
        insert(iri(ALICE), iri(price), valueFactory.createLiteral(1));
        insert(iri(secondBook), iri(title), valueFactory.createLiteral("second"));
        insert(iri(secondBook), iri(price), valueFactory.createLiteral(2));

        TupleQueryResult result = executor.evaluateTuple("""
                PREFIX dc: <http://purl.org/dc/elements/1.1/>
                PREFIX : <http://example.org/book/>
                PREFIX ns: <http://example.org/ns#>
                SELECT ?book ?title ?price {
                  ?book dc:title ?title ; ns:price ?price .
                }
                VALUES ?book { <http://example.org/alice> }
                """);

        assertTrue(result.hasNext());
        assertEquals(ALICE, result.next().getValue("book").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("BIND chains after VALUES and remains visible to following graph patterns")
    void bindAfterValuesIsVisibleToFollowingPatterns() {
        String rank = "http://example.org/rank";
        insert(iri(ALICE), iri(rank), valueFactory.createLiteral(2));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person ?nextRank WHERE {
                  VALUES ?person { <http://example.org/alice> }
                  ?person <http://example.org/rank> ?rank .
                  BIND(?rank + 1 AS ?nextRank)
                  VALUES ?nextRank { 3 }
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(3, ((Literal) result.next().getValue("nextRank")).intValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("VALUES UNDEF leaves an incoming binding available to following operations")
    void valuesUndefDoesNotClearIncomingBinding() {
        String rank = "http://example.org/rank";
        insert(iri(ALICE), iri(rank), valueFactory.createLiteral(2));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?nextRank WHERE {
                  ?person <http://example.org/rank> ?rank .
                  VALUES (?person ?rank) { (<http://example.org/alice> UNDEF) }
                  BIND(?rank + 1 AS ?nextRank)
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(3, ((Literal) result.next().getValue("nextRank")).intValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Query with generous timeout completes normally")
    void queryWithGenerousTimeoutCompletesNormally() {
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, 10_000L); // 10 seconds — far more than needed

        assertTrue(result.hasNext(), "Result should be non-empty");
    }

    @Test
    @DisplayName("Query with zero timeout runs without timeout enforcement")
    void queryWithZeroTimeoutRunsWithoutEnforcement() {
        // timeout = 0 means disabled; must complete normally
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT * WHERE { ?s ?p ?o }",
                null, null, 0L);

        assertTrue(result.hasNext(), "Result should be non-empty");
    }

    @Test
    @DisplayName("QueryTimeoutException is thrown when evaluation exceeds deadline")
    void queryTimeoutExceptionThrownWhenDeadlineExceeded() {
        // Insert many triples to create a cross-product query that takes longer than 1 ms.
        for (int i = 0; i < 200; i++) {
            insert(iri("http://example.org/s" + i), iri(KNOWS), iri("http://example.org/o" + i));
        }

        // Cross-product of 201 triples × 201 triples = 40 401 combinations — expensive enough
        // to reliably exceed a 1 ms timeout on any machine.
        assertThrows(QueryTimeoutException.class, this::evaluateAndConsumeTimeoutQuery);
    }

    @Test
    void trailingValuesDoesNotSupplyBindingsToWhere() {
        try (var result = executor.evaluateTuple("SELECT ?x ?y { BIND(?x AS ?y) } VALUES ?x { 1 }")) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("1", row.getValue("x").stringValue());
            assertFalse(row.hasBinding("y"));
            assertEquals(Set.of("x"), row.getBindingNames());
            var bindings = row.iterator();
            assertTrue(bindings.hasNext());
            assertEquals("x", bindings.next().name());
            assertFalse(bindings.hasNext());
            assertFalse(result.hasNext());
        }
    }

    @Test
    void unionPreservesRightBranchWhenLeftIsEmpty() {
        try (var result = executor.evaluateTuple("SELECT ?x { { VALUES ?x {} } UNION { VALUES ?x { 1 1 } } }")) {
            assertEquals(2, result.stream().count());
        }
    }

    @Test
    void selectStarDoesNotExposeExistentialBlankNodeVariables() {
        try (var result = executor.evaluateTuple("SELECT * { [] <http://example.org/knows> ?friend }")) {
            assertEquals(List.of("friend"), result.getBindingNames());
            assertTrue(result.hasNext());
            assertEquals(Set.of("friend"), result.next().getBindingNames());
        }
    }

    @Test
    void incompatibleValuesRowAfterUndefDoesNotPopIncomingBindings() {
        try (var result = executor.evaluateTuple("""
                SELECT ?x ?y ?z {
                  VALUES (?x ?y ?z) { (1 2 3) }
                  VALUES (?x ?y ?z) { (UNDEF 2 4) (UNDEF 2 3) }
                }
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("1", row.getValue("x").stringValue());
            assertEquals("2", row.getValue("y").stringValue());
            assertEquals("3", row.getValue("z").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    void valuesDistinguishesEmptyTableFromEmptySolution() {
        assertFalse(executor.evaluateBoolean("ASK {} VALUES ?x {}"));
        assertFalse(executor.evaluateBoolean("ASK { VALUES () {} }"));
        assertTrue(executor.evaluateBoolean("ASK { VALUES () { () } }"));
        try (var result = executor.evaluateTuple("SELECT * {} VALUES ?x {}")) {
            assertEquals(List.of("x"), result.getBindingNames());
            assertFalse(result.hasNext());
        }
        try (var result = executor.evaluateTuple("SELECT * { VALUES () { () () } }")) {
            assertEquals(2, result.stream().count());
        }
    }

    private void evaluateAndConsumeTimeoutQuery() {
        try (TupleQueryResult result = executor.evaluateTuple(
                "SELECT * WHERE { ?s1 ?p1 ?o1 . ?s2 ?p2 ?o2 }",
                null, null, 1L)) {
            while (result.hasNext()) {
                result.next();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

}
