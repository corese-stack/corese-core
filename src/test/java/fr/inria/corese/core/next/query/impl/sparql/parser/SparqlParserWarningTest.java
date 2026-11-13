package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.api.validation.QueryValidationResult;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.execution.SparqlQueryExecutor;
import fr.inria.corese.core.next.storage.Storages;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for non-fatal semantic warnings in the SPARQL validator (issue #462).
 */
@DisplayName("SPARQL semantic warning tests")
class SparqlParserWarningTest extends AbstractSparqlParserFeatureTest {


    private SparqlParser parserWith(boolean includeWarnings, boolean failOnWarnings) {
        return new SparqlParser(new SparqlParserOptions.Builder()
                .includeWarnings(includeWarnings)
                .failOnWarnings(failOnWarnings)
                .build());
    }


    @Nested
    @DisplayName("EBV coercion warnings in FILTER")
    class FilterEBVWarnings {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
                "SELECT ?s WHERE { ?s ?p ?o FILTER(42) }",
                "SELECT ?s WHERE { ?s ?p ?o FILTER(3.14) }",
                "SELECT ?s WHERE { ?s ?p ?o FILTER(\"text\") }",
                "SELECT ?s WHERE { ?s ?p ?o FILTER(true) }",
                "SELECT ?s WHERE { ?s ?p ?o FILTER(false) }"
        })
        @DisplayName("Should produce a WARNING for suspicious EBV coercion in FILTER")
        void shouldWarnOnSuspiciousFilterEBV(String query) {
            SparqlParser parser = parserWith(true, false);
            QueryValidationResult result = parser.validate(query);

            assertTrue(result.isValid(), "Query should be valid (warning is non-fatal)");
            List<QueryDiagnostic> warnings = result.diagnostics().stream()
                    .filter(d -> d.severity() == QueryDiagnostic.Severity.WARNING)
                    .toList();
            assertFalse(warnings.isEmpty(), "Expected at least one WARNING diagnostic");
            QueryDiagnostic first = warnings.getFirst();
            assertTrue(first.message().contains("EBV coercion")
                            || first.message().contains("Constant condition"),
                    "Warning should explain the suspicious condition: " + first.message());
            assertTrue(first.line() > 0, "FILTER warning should carry a valid line number, got: " + first.line());
            assertTrue(first.column() >= 0, "FILTER warning should carry a valid column, got: " + first.column());
        }

        @Test
        @DisplayName("Bare variable FILTER should produce an advisory warning")
        void shouldWarnOnBareVariableFilter() {
            QueryValidationResult result = parserWith(true, false)
                    .validate("SELECT ?s WHERE { ?s ?p ?o FILTER(?count) }");

            assertTrue(result.isValid());
            assertTrue(result.diagnostics().stream()
                    .anyMatch(d -> d.severity() == QueryDiagnostic.Severity.WARNING));
        }

        @Test
        @DisplayName("A variable used as an operand of a boolean operator should produce a warning")
        void shouldWarnOnImplicitEbvInBooleanOperator() {
            QueryValidationResult result = parserWith(true, false)
                    .validate("SELECT ?s WHERE { ?s ?p ?o FILTER(?count && true) }");

            assertTrue(result.isValid());
            assertTrue(result.diagnostics().stream()
                    .anyMatch(d -> d.severity() == QueryDiagnostic.Severity.WARNING));
        }

        @Test
        @DisplayName("A constant comparison should produce a constant-condition warning")
        void shouldWarnOnConstantComparison() {
            QueryValidationResult result = parserWith(true, false)
                    .validate("SELECT ?s WHERE { ?s ?p ?o FILTER(1 = 1) }");

            assertTrue(result.isValid());
            assertTrue(result.diagnostics().stream()
                    .anyMatch(d -> d.severity() == QueryDiagnostic.Severity.WARNING
                            && d.message().contains("Constant condition")));
        }

        @Test
        @DisplayName("Incompatible ordered comparison operands should produce a warning")
        void shouldWarnOnIncompatibleComparison() {
            QueryValidationResult result = parserWith(true, false)
                    .validate("SELECT ?s WHERE { ?s ?p ?o FILTER(\"text\" > 1) }");

            assertTrue(result.isValid());
            assertTrue(result.diagnostics().stream()
                    .anyMatch(d -> d.severity() == QueryDiagnostic.Severity.WARNING
                            && d.message().contains("Incompatible operand types")));
        }

        @Test
        @DisplayName("Normal boolean FILTER should not produce a warning")
        void shouldNotWarnOnBooleanFilter() {
            SparqlParser parser = parserWith(true, false);
            String query = "SELECT ?s WHERE { ?s ?p ?o FILTER(?x > 0) }";

            QueryValidationResult result = parser.validate(query);

            assertTrue(result.isValid());
            assertTrue(result.diagnostics().stream()
                            .noneMatch(d -> d.severity() == QueryDiagnostic.Severity.WARNING),
                    "No warnings expected for a proper boolean FILTER");
        }

        @Test
        @DisplayName("Boolean operands inside a boolean expression should not be warned as EBV coercions")
        void shouldNotWarnOnBooleanOperands() {
            QueryValidationResult result = parserWith(true, false)
                    .validate("SELECT ?s WHERE { ?s ?p ?o FILTER(?x > 0 && true) }");

            assertTrue(result.isValid());
            assertTrue(result.diagnostics().stream()
                    .noneMatch(d -> d.severity() == QueryDiagnostic.Severity.WARNING));
        }

        @Test
        @DisplayName("FILTER with function call should not produce a warning")
        void shouldNotWarnOnFunctionCallFilter() {
            SparqlParser parser = parserWith(true, false);
            String query = "SELECT ?s WHERE { ?s ?p ?o FILTER(BOUND(?x)) }";

            QueryValidationResult result = parser.validate(query);

            assertTrue(result.isValid());
            assertTrue(result.diagnostics().stream()
                            .noneMatch(d -> d.severity() == QueryDiagnostic.Severity.WARNING),
                    "No warnings expected for FILTER(BOUND(?x))");
        }
    }

    @Nested
    @DisplayName("EBV coercion warnings in HAVING")
    class HavingEBVWarnings {

        @Test
        @DisplayName("Numeric literal in HAVING should produce a WARNING")
        void shouldWarnOnNumericHaving() {
            SparqlParser parser = parserWith(true, false);
            String query = """
                    SELECT ?s (COUNT(?o) AS ?count) WHERE { ?s ?p ?o }
                    GROUP BY ?s
                    HAVING(1)
                    """;

            QueryValidationResult result = parser.validate(query);

            assertTrue(result.isValid(), "Query should be valid (warning is non-fatal)");
            QueryDiagnostic warning = result.diagnostics().stream()
                    .filter(d -> d.severity() == QueryDiagnostic.Severity.WARNING)
                    .findFirst()
                    .orElseThrow();
            assertTrue(warning.line() > 0, "HAVING warning should carry a valid line number, got: " + warning.line());
            assertTrue(warning.column() >= 0, "HAVING warning should carry a valid column, got: " + warning.column());
        }
    }


    @Nested
    @DisplayName("includeWarnings = false suppresses warnings")
    class IncludeWarningsFalse {

        @Test
        @DisplayName("Warnings are absent when includeWarnings is false")
        void shouldSuppressWarnings() {
            SparqlParser parser = parserWith(false, false);
            String query = "SELECT ?s WHERE { ?s ?p ?o FILTER(42) }";

            QueryValidationResult result = parser.validate(query);

            assertTrue(result.isValid());
            assertTrue(result.diagnostics().stream()
                            .noneMatch(d -> d.severity() == QueryDiagnostic.Severity.WARNING),
                    "No warnings should be present when includeWarnings=false");
        }
    }


    @Nested
    @DisplayName("failOnWarnings = true promotes warnings to errors")
    class FailOnWarnings {

        @Test
        @DisplayName("Suspicious FILTER causes isValid=false when failOnWarnings=true")
        void shouldFailOnWarning() {
            SparqlParser parser = parserWith(true, true);
            String query = "SELECT ?s WHERE { ?s ?p ?o FILTER(42) }";

            QueryValidationResult result = parser.validate(query);

            assertFalse(result.isValid(), "isValid() should be false when failOnWarnings=true");
            assertTrue(result.diagnostics().stream()
                            .anyMatch(d -> d.severity() == QueryDiagnostic.Severity.ERROR),
                    "Warning should have been promoted to ERROR");
        }

        @Test
        @DisplayName("parse() throws QueryValidationException when failOnWarnings=true and warning is present")
        void shouldThrowOnWarningWhenFailOnWarnings() {
            SparqlParser parser = parserWith(true, true);
            String query = "SELECT ?s WHERE { ?s ?p ?o FILTER(42) }";

            assertThrows(QueryValidationException.class, () -> parser.parse(query));
        }

        @Test
        @DisplayName("Normal boolean FILTER passes even with failOnWarnings=true")
        void shouldPassWithNoWarnings() {
            SparqlParser parser = parserWith(true, true);
            String query = "SELECT ?s WHERE { ?s ?p ?o FILTER(?x > 0) }";

            assertDoesNotThrow(() -> parser.parse(query));
        }
    }


    @Nested
    @DisplayName("Query execution is not blocked by warnings")
    class ExecutionTests {

        @Test
        @DisplayName("parse() returns a valid QueryAst for a query with a FILTER warning")
        void warningDoesNotBlockParse() {
            SparqlParser parser = parserWith(true, false);
            String query = "SELECT ?s WHERE { ?s ?p ?o FILTER(42) }";

            QueryAst ast = assertDoesNotThrow(() -> parser.parse(query),
                    "parse() must succeed even when a warning is present");
            assertNotNull(ast, "parse() must return a non-null AST");
            assertInstanceOf(SelectQueryAst.class, ast, "result must be a SELECT query");
        }

        @Test
        @DisplayName("parse() returns a valid QueryAst for a query with a HAVING warning")
        void havingWarningDoesNotBlockParse() {
            SparqlParser parser = parserWith(true, false);
            String query = "SELECT ?s (COUNT(?o) AS ?count) WHERE { ?s ?p ?o } GROUP BY ?s HAVING(1)";

            QueryAst ast = assertDoesNotThrow(() -> parser.parse(query),
                    "parse() must succeed even when a HAVING warning is present");
            assertNotNull(ast, "parse() must return a non-null AST");
            assertInstanceOf(SelectQueryAst.class, ast);
        }
    }


    @Nested
    @DisplayName("EBV coercion warnings for built-in functions")
    class BuiltInFunctionEBVWarnings {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
                "SELECT ?s WHERE { ?s ?p ?o FILTER(STR(?o)) }",
                "SELECT ?s WHERE { ?s ?p ?o FILTER(LCASE(?o)) }",
                "SELECT ?s WHERE { ?s ?p ?o FILTER(UCASE(?o)) }",
                "SELECT ?s WHERE { ?s ?p ?o FILTER(CONCAT(?s, ?o)) }",
                "SELECT ?s WHERE { ?s ?p ?o FILTER(STRLEN(?o)) }",
                "SELECT ?s WHERE { ?s ?p ?o FILTER(ABS(?o)) }"
        })
        @DisplayName("Should produce a WARNING for string/numeric built-in used as EBV in FILTER")
        void shouldWarnOnBuiltInFunctionEBV(String query) {
            SparqlParser parser = parserWith(true, false);
            QueryValidationResult result = parser.validate(query);

            assertTrue(result.isValid(), "Query should be valid (warning is non-fatal)");
            assertTrue(result.diagnostics().stream()
                            .anyMatch(d -> d.severity() == QueryDiagnostic.Severity.WARNING),
                    "Expected at least one WARNING diagnostic for: " + query);
        }
    }

    @Test
    @DisplayName("Each HAVING condition reports its own expression location")
    void havingConditionsHaveIndividualLocations() {
        String query = """
                SELECT ?s (COUNT(?o) AS ?count) WHERE { ?s ?p ?o }
                GROUP BY ?s
                HAVING (1)
                       (2)
                """;

        List<QueryDiagnostic> warnings = parserWith(true, false).validate(query).diagnostics().stream()
                .filter(d -> d.severity() == QueryDiagnostic.Severity.WARNING)
                .toList();

        assertEquals(2, warnings.size());
        assertEquals(3, warnings.get(0).line());
        assertEquals(4, warnings.get(1).line());
    }

    @Test
    @DisplayName("Boolean and IRI warnings are scoped to FILTER and HAVING expressions")
    void specializedWarningsAreScopedToFilterAndHaving() {
        QueryValidationResult result = parserWith(true, false).validate(
                "SELECT ?s WHERE { ?s ?p ?o FILTER(<urn:test:iri> && true) } "
                        + "ORDER BY (2 < <urn:test:iri>)");

        assertTrue(result.isValid());
        assertTrue(result.diagnostics().stream()
                .anyMatch(d -> d.source() != null
                        && d.source().equals("OperandArgumentBooleanTypeValidationRule")
                        && d.severity() == QueryDiagnostic.Severity.WARNING
                        && d.line() == 1));
        assertTrue(result.diagnostics().stream()
                .noneMatch(d -> d.source() != null
                        && d.source().equals("OperandArgumentIRITypeValidationRule")),
                "ORDER BY expressions must not inherit the FILTER warning location");
    }

    @Test
    @DisplayName("A query with an EBV warning still executes")
    void queryWithWarningExecutes() {
        try (var storage = Storages.create(StorageConfig.memory())) {
            var factory = Values.factory();
            storage.mutations().add(factory.createStatement(
                    new SimpleIRI("urn:test:s"),
                    new SimpleIRI("urn:test:p"),
                    new SimpleIRI("urn:test:o")));
            SparqlQueryExecutor executor = new SparqlQueryExecutor(storage);

            try (var result = executor.evaluateTuple(
                    "SELECT ?s WHERE { ?s ?p ?o FILTER(42) }")) {
                assertEquals(1, result.stream().count());
            }
        }
    }
}
