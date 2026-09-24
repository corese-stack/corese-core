package fr.inria.corese.core.next.query.impl.sparql.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import fr.inria.corese.core.next.data.api.term.BNode;
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
