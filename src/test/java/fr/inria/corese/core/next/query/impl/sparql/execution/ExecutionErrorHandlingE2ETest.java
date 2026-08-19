package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.storagemanager.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for consistent error handling across the next SPARQL pipeline.
 *
 * <p>The pipeline must surface failures with the right exception type at the right
 * stage (parse, bridge, evaluation) so callers can distinguish syntax errors,
 * unsupported features, and runtime evaluation failures.</p>
 *
 * <p>All tests use an in-memory storage manager and require no external dependencies.</p>
 */
class ExecutionErrorHandlingE2ETest {

    private static final String EX    = "http://example.org/";
    private static final String KNOWS = EX + "knows";

    private CoreseValueFactory vf;
    private MemoryStorageManager storage;
    private NextSparqlPipelineExecutor executor;

    @BeforeEach
    void setUp() {
        vf       = new CoreseValueFactory();
        storage  = MemoryStorageManager.builder().build();
        executor = new NextSparqlPipelineExecutor(storage);
        insert(iri(EX + "alice"), iri(KNOWS), iri(EX + "bob"));
    }


    @Nested
    @DisplayName("Syntax errors")
    class SyntaxErrors {

        @Test
        @DisplayName("Completely invalid SPARQL throws QuerySyntaxException")
        void completelyInvalidSparqlThrowsSyntaxException() {
            assertThrows(QuerySyntaxException.class, () -> {
                try (var ignored = executor.evaluateTuple("THIS IS NOT SPARQL")) {
                    fail("Should have thrown before returning a result");
                }
            });
        }

        @Test
        @DisplayName("SELECT without WHERE clause throws QuerySyntaxException")
        void selectWithoutWhereClauseThrowsSyntaxException() {
            assertThrows(QuerySyntaxException.class, () -> {
                try (var ignored = executor.evaluateTuple("SELECT ?s")) {
                    fail("Should have thrown before returning a result");
                }
            });
        }

        @Test
        @DisplayName("Unclosed curly brace throws QuerySyntaxException")
        void unclosedCurlyBraceThrowsSyntaxException() {
            assertThrows(QuerySyntaxException.class, () -> {
                try (var ignored = executor.evaluateTuple("SELECT * WHERE { ?s ?p ?o")) {
                    fail("Should have thrown before returning a result");
                }
            });
        }

        @Test
        @DisplayName("Malformed IRI in triple pattern throws QuerySyntaxException")
        void malformedIriThrowsSyntaxException() {
            assertThrows(QuerySyntaxException.class, () -> {
                try (var ignored = executor.evaluateTuple(
                        "SELECT * WHERE { <not a valid iri> ?p ?o }")) {
                    fail("Should have thrown before returning a result");
                }
            });
        }

        @Test
        @DisplayName("ASK with invalid syntax throws QuerySyntaxException")
        void askWithInvalidSyntaxThrowsSyntaxException() {
            assertThrows(QuerySyntaxException.class,
                    () -> executor.evaluateBoolean("ASK { ?s ?p"));
        }
    }


    @Nested
    @DisplayName("Wrong query form")
    class WrongQueryForm {

        @Test
        @DisplayName("evaluateTuple with an ASK query throws IllegalArgumentException")
        void evaluateTupleWithAskThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () -> {
                try (var ignored = executor.evaluateTuple("ASK WHERE { ?s ?p ?o }")) {
                    fail("Should have thrown before returning a result");
                }
            });
        }

        @Test
        @DisplayName("evaluateBoolean with a SELECT query throws IllegalArgumentException")
        void evaluateBooleanWithSelectThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> executor.evaluateBoolean("SELECT * WHERE { ?s ?p ?o }"));
        }

        @Test
        @DisplayName("evaluateTuple with a CONSTRUCT query throws IllegalArgumentException")
        void evaluateTupleWithConstructThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () -> {
                try (var ignored = executor.evaluateTuple(
                        "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }")) {
                    fail("Should have thrown before returning a result");
                }
            });
        }

        @Test
        @DisplayName("evaluateBoolean with a CONSTRUCT query throws IllegalArgumentException")
        void evaluateBooleanWithConstructThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> executor.evaluateBoolean(
                            "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }"));
        }

        @Test
        @DisplayName("evaluateGraph with a SELECT query throws IllegalArgumentException")
        void evaluateGraphWithSelectThrowsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () -> {
                try (var ignored = executor.evaluateGraph("SELECT * WHERE { ?s ?p ?o }")) {
                    fail("Should have thrown before returning a result");
                }
            });
        }
    }


    @Nested
    @DisplayName("Unsupported features (bridge rejects)")
    class UnsupportedFeatures {

        @Test
        @DisplayName("SELECT REDUCED throws UnsupportedQueryFeatureException")
        void selectReducedThrowsUnsupported() {
            assertThrows(UnsupportedQueryFeatureException.class, () -> {
                try (var ignored = executor.evaluateTuple(
                        "SELECT REDUCED * WHERE { ?s ?p ?o }")) {
                    fail("Should have thrown before returning a result");
                }
            });
        }

        @Test
        @DisplayName("SELECT with GROUP BY throws UnsupportedQueryFeatureException")
        void selectWithGroupByThrowsUnsupported() {
            assertThrows(UnsupportedQueryFeatureException.class, () -> {
                try (var ignored = executor.evaluateTuple(
                        "SELECT ?s (COUNT(*) AS ?c) WHERE { ?s ?p ?o } GROUP BY ?s")) {
                    fail("Should have thrown before returning a result");
                }
            });
        }

        @Test
        @DisplayName("SELECT with HAVING is rejected before reaching evaluation")
        void selectWithHavingIsRejectedByPipeline() {
            // HAVING requires GROUP BY; GROUP BY is rejected at the bridge stage with
            var ex = assertThrows(RuntimeException.class, () -> {
                try (var ignored = executor.evaluateTuple(
                        "SELECT ?s (COUNT(*) AS ?c) WHERE { ?s ?p ?o } GROUP BY ?s HAVING (?c > 1)")) {
                    fail("Should have thrown before returning a result");
                }
            });
            assertTrue(
                    ex instanceof UnsupportedQueryFeatureException
                            || ex instanceof QueryValidationException,
                    "Pipeline must reject HAVING queries — got: " + ex.getClass().getSimpleName());
        }

        @Test
        @DisplayName("SELECT with expression alias throws UnsupportedQueryFeatureException")
        void selectWithExpressionAliasThrowsUnsupported() {
            assertThrows(UnsupportedQueryFeatureException.class, () -> {
                try (var ignored = executor.evaluateTuple(
                        "SELECT (STR(?s) AS ?label) WHERE { ?s ?p ?o }")) {
                    fail("Should have thrown before returning a result");
                }
            });
        }

        @Test
        @DisplayName("SELECT with inline VALUES throws UnsupportedQueryFeatureException")
        void selectWithInlineValuesThrowsUnsupported() {
            assertThrows(UnsupportedQueryFeatureException.class, () -> {
                try (var ignored = executor.evaluateTuple("""
                        SELECT * WHERE { ?s ?p ?o }
                        VALUES ?s { <http://example.org/alice> }
                        """)) {
                    fail("Should have thrown before returning a result");
                }
            });
        }

        @Test
        @DisplayName("ASK with GROUP BY throws UnsupportedQueryFeatureException")
        void askWithGroupByThrowsUnsupported() {
            assertThrows(UnsupportedQueryFeatureException.class,
                    () -> executor.evaluateBoolean("ASK { ?s ?p ?o } GROUP BY ?s"));
        }

        @Test
        @DisplayName("Property path (sequence) throws UnsupportedQueryFeatureException")
        void propertyPathSequenceThrowsUnsupported() {
            assertThrows(UnsupportedQueryFeatureException.class, () -> {
                try (var ignored = executor.evaluateTuple(
                        "SELECT ?o WHERE { ?s <" + KNOWS + ">/<" + KNOWS + "> ?o }")) {
                    fail("Should have thrown before returning a result");
                }
            });
        }
    }


    @Nested
    @DisplayName("Empty results are not errors")
    class EmptyResults {

        @Test
        @DisplayName("SELECT with no matching triple returns an empty result, not an exception")
        void selectWithNoMatchReturnsEmptyResult() {
            try (var result = executor.evaluateTuple(
                    "SELECT * WHERE { ?s <" + EX + "unknown> ?o }")) {
                assertFalse(result.hasNext(), "No match must yield an empty result set");
            }
        }

        @Test
        @DisplayName("ASK with no matching triple returns false, not an exception")
        void askWithNoMatchReturnsFalse() {
            boolean found = executor.evaluateBoolean(
                    "ASK WHERE { ?s <" + EX + "unknown> ?o }");
            assertFalse(found);
        }

        @Test
        @DisplayName("CONSTRUCT with no matching triple returns an empty graph, not an exception")
        void constructWithNoMatchReturnsEmptyGraph() {
            try (var result = executor.evaluateGraph(
                    "CONSTRUCT { ?s ?p ?o } WHERE { ?s <" + EX + "unknown> ?o }")) {
                assertFalse(result.hasNext(), "No match must yield an empty graph result");
            }
        }
    }


    @Nested
    @DisplayName("Exception messages are informative")
    class ExceptionMessages {

        @Test
        @DisplayName("QuerySyntaxException message is non-empty")
        void syntaxExceptionHasNonEmptyMessage() {
            QuerySyntaxException ex = assertThrows(QuerySyntaxException.class, () -> {
                try (var ignored = executor.evaluateTuple("NOT SPARQL")) {
                    fail("Should have thrown before returning a result");
                }
            });
            assertNotNull(ex.getMessage());
            assertFalse(ex.getMessage().isBlank(), "Syntax exception message must not be blank");
        }

        @Test
        @DisplayName("UnsupportedQueryFeatureException message is non-empty")
        void unsupportedExceptionHasNonEmptyMessage() {
            UnsupportedQueryFeatureException ex = assertThrows(
                    UnsupportedQueryFeatureException.class, () -> {
                        try (var ignored = executor.evaluateTuple(
                                "SELECT REDUCED * WHERE { ?s ?p ?o }")) {
                            fail("Should have thrown before returning a result");
                        }
                    });
            assertNotNull(ex.getMessage());
            assertFalse(ex.getMessage().isBlank(),
                    "Unsupported feature message must not be blank");
        }

        @Test
        @DisplayName("IllegalArgumentException message names the actual query form")
        void illegalArgumentExceptionNamesQueryForm() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                try (var ignored = executor.evaluateTuple("ASK WHERE { ?s ?p ?o }")) {
                    fail("Should have thrown before returning a result");
                }
            });
            assertNotNull(ex.getMessage());
            assertTrue(ex.getMessage().contains("ASK") || ex.getMessage().contains("SELECT"),
                    "Message should mention the query form involved");
        }
    }


    private void insert(Resource subject, IRI predicate, Value object) {
        storage.getMutationOperations()
                .insertStatement(vf.createStatement(subject, predicate, object));
    }

    private IRI iri(String value) {
        return vf.createIRI(value);
    }
}
