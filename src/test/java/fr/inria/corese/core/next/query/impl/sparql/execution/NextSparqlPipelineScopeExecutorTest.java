package fr.inria.corese.core.next.query.impl.sparql.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Result-level regressions for blank-node scopes and SELECT projection sets. */
class NextSparqlPipelineScopeExecutorTest extends PipelineTestSupport {

    @ParameterizedTest
    @ValueSource(strings = { "?x ?x", "?x $x", "$x ?x" })
    void repeatedProjectionProducesOneColumn(String projection) {
        try (var result = executor.evaluateTuple("SELECT " + projection + " WHERE { VALUES ?x { 1 } }")) {
            assertEquals(List.of("x"), result.getBindingNames());
            assertTrue(result.hasNext());
            assertEquals("1", result.next().getValue("x").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    void projectedAliasMayBeReferencedAgain() {
        try (var result = executor.evaluateTuple("SELECT (1 AS ?x) $x (?x + 1 AS ?y) WHERE {}")) {
            assertEquals(List.of("x", "y"), result.getBindingNames());
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("1", row.getValue("x").stringValue());
            assertEquals("2", row.getValue("y").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FILTER EXISTS { ?s <http://example.org/knows> ?o }",
            "FILTER NOT EXISTS { ?s <urn:missing> ?o }"
    })
    void blankNodeStillJoinsTriplesAcrossExistsFilter(String filter) {
        insert(iri(ALICE), iri(NAME), valueFactory.createLiteral("Alice"));
        insert(iri(BOB), iri(NAME), valueFactory.createLiteral("Bob"));
        try (var result = executor.evaluateTuple("""
                SELECT ?name WHERE {
                  _:a <http://example.org/knows> ?friend .
                  %s
                  _:a <http://example.org/name> ?name
                }
                """.formatted(filter))) {
            assertTrue(result.hasNext());
            assertEquals("Alice", result.next().getValue("name").stringValue());
            assertFalse(result.hasNext(), "The same blank-node label must join on the same subject");
        }
    }

    /**
     * Regression for dawg-optional-filter-005-not-simplified: nested filters
     * retain their group scope before algebra simplification (SPARQL 1.1 §18.2.2).
     */
    @Test
    @DisplayName("FILTER referencing outer variable inside nested OPTIONAL group — preserved group scope (issue #613)")
    void filterReferencingOuterVarInNestedOptionalGroupKeepsItsScope() {
        IRI dcTitle = iri("http://purl.org/dc/elements/1.1/title");
        IRI xPrice = iri("http://example.org/ns#price");
        IRI book1 = iri("http://example.org/books#book1");
        IRI book2 = iri("http://example.org/books#book2");
        IRI book3 = iri("http://example.org/books#book3");

        insert(book1, dcTitle, valueFactory.createLiteral("TITLE 1"));
        insert(book1, xPrice, valueFactory.createLiteral(10));
        insert(book2, dcTitle, valueFactory.createLiteral("TITLE 2"));
        insert(book2, xPrice, valueFactory.createLiteral(20));
        insert(book3, dcTitle, valueFactory.createLiteral("TITLE 3"));

        try (var result = executor.evaluateTuple("""
                PREFIX dc: <http://purl.org/dc/elements/1.1/>
                PREFIX x:  <http://example.org/ns#>
                SELECT ?title ?price
                WHERE {
                  ?book dc:title ?title .
                  OPTIONAL {
                    { ?book x:price ?price .
                      FILTER (?title = "TITLE 2") . }
                  }
                }
                """)) {
            var rows = result.stream().toList();
            assertEquals(3, rows.size(), "All three titles must appear");
            assertEquals(Set.of("TITLE 1", "TITLE 2", "TITLE 3"),
                    rows.stream().map(r -> r.getValue("title").stringValue())
                            .collect(Collectors.toSet()));
            rows.forEach(row -> assertNull(row.getValue("price"),
                    "The nested filter cannot use ?title from the outer group"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{ ?s ?p ?o FILTER(?x = 1) } ?a ?b ?c",
            "?a ?b ?c { ?s ?p ?o FILTER(?x = 1) }",
            "{ { ?s ?p ?o FILTER(?x = 1) } ?a ?b ?c }"
    })
    void nestedOptionalFilterKeepsItsScopeAcrossSiblingPatterns(String pattern) {
        try (var result = executor.evaluateTuple("""
                SELECT * WHERE {
                  VALUES ?x { 1 }
                  OPTIONAL { %s }
                }
                """.formatted(pattern))) {
            var rows = result.stream().toList();
            assertEquals(1, rows.size());
            assertEquals("1", rows.get(0).getValue("x").stringValue());
            for (String variable : List.of("s", "p", "o", "a", "b", "c")) {
                assertNull(rows.get(0).getValue(variable),
                        "The nested filter cannot use the outer binding of ?x");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{ ?s ?p ?o FILTER(?x = 1) }",
            "{ { ?s ?p ?o FILTER(?x = 1) } }"
    })
    void nestedOptionalWrappersPreserveFilterScope(String pattern) {
        try (var result = executor.evaluateTuple("""
                SELECT * WHERE {
                  VALUES ?x { 1 2 }
                  OPTIONAL { %s }
                }
                """.formatted(pattern))) {
            var rows = result.stream().toList();
            assertEquals(2, rows.size());
            for (var row : rows) {
                assertNull(row.getValue("s"));
                assertNull(row.getValue("p"));
                assertNull(row.getValue("o"));
            }
        }
    }

    @Test
    void directOptionalFilterCanUseOuterBindings() {
        try (var result = executor.evaluateTuple("""
                SELECT * WHERE {
                  VALUES ?x { 1 2 }
                  OPTIONAL { ?s ?p ?o FILTER(?x = 1) }
                }
                """)) {
            var rows = result.stream().toList();
            assertEquals(2, rows.size());
            var matching = rows.stream().filter(r -> "1".equals(r.getValue("x").stringValue()))
                    .findFirst().orElseThrow();
            assertEquals(iri(ALICE), matching.getValue("s"));
            assertEquals(iri(KNOWS), matching.getValue("p"));
            assertEquals(iri(BOB), matching.getValue("o"));
            var nonMatching = rows.stream().filter(r -> "2".equals(r.getValue("x").stringValue()))
                    .findFirst().orElseThrow();
            assertNull(nonMatching.getValue("s"));
        }
    }

    @Test
    void constructLabelCreatesAFreshNodeInsteadOfReusingTheWhereBinding() {
        try (var result = executor.evaluateGraph("""
                CONSTRUCT { _:a <urn:friend> ?friend }
                WHERE { _:a <http://example.org/knows> ?friend }
                """)) {
            assertTrue(result.hasNext());
            var triple = result.next();
            assertInstanceOf(BNode.class, triple.getSubject());
            assertEquals(iri("urn:friend"), triple.getPredicate());
            assertEquals(iri(BOB), triple.getObject());
            assertFalse(result.hasNext());
        }
    }
}
