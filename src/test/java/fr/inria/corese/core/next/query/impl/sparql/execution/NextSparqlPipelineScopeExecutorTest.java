package fr.inria.corese.core.next.query.impl.sparql.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
     * dawg-optional-filter-005-simplified (W3C SPARQL 1.0 / SPARQL 1.1 simplified semantics):
     * In {@code OPTIONAL { { BGP . FILTER(?outerVar = x) } }}, the inner {@code { }} is
     * transparent — it does not create an evaluation-scope barrier for outer variables.
     * The FILTER is postponed and evaluated at merge time (when both the left solution
     * carrying {@code ?outerVar} and the right solution carrying the BGP bindings are
     * available), giving the correct result: only the row where the FILTER passes
     * receives the optional binding.
     *
     * <p>This is the "simplified" reading chosen by Corese, aligned with the
     * SPARQL 1.1 algebra where an inner group graph pattern with no structural
     * sub-patterns (OPTIONAL/MINUS/UNION) flattens into its parent.</p>
     */
    @Test
    @DisplayName("FILTER referencing outer variable inside nested OPTIONAL group — simplified semantics (issue #613)")
    void filterReferencingOuterVarInNestedOptionalGroupIsSimplified() {
        IRI dcTitle = iri("http://purl.org/dc/elements/1.1/title");
        IRI xPrice  = iri("http://example.org/ns#price");
        IRI book1   = iri("http://example.org/books#book1");
        IRI book2   = iri("http://example.org/books#book2");
        IRI book3   = iri("http://example.org/books#book3");

        insert(book1, dcTitle, valueFactory.createLiteral("TITLE 1"));
        insert(book1, xPrice,  valueFactory.createLiteral(10));
        insert(book2, dcTitle, valueFactory.createLiteral("TITLE 2"));
        insert(book2, xPrice,  valueFactory.createLiteral(20));
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
            // Only TITLE 2 passes the filter — it alone gets a price binding
            long withPrice = rows.stream().filter(r -> r.getValue("price") != null).count();
            assertEquals(1, withPrice, "Exactly one row (TITLE 2) should have a price binding");
            var title2Row = rows.stream()
                    .filter(r -> "TITLE 2".equals(r.getValue("title").stringValue()))
                    .findFirst().orElseThrow();
            assertEquals("20", title2Row.getValue("price").stringValue());
            rows.stream()
                    .filter(r -> !"TITLE 2".equals(r.getValue("title").stringValue()))
                    .forEach(r -> assertNull(r.getValue("price"),
                            "Titles other than TITLE 2 must not bind ?price"));
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
