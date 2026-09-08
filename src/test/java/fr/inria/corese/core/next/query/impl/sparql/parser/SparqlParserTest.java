package fr.inria.corese.core.next.query.impl.sparql.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import java.util.stream.Stream;

import fr.inria.corese.core.next.query.impl.sparql.ast.SparqlQueryAst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.api.validation.QueryTextValidator;
import fr.inria.corese.core.next.query.api.validation.QueryValidationResult;
import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;

/**
 * Unit tests for {@link SparqlParser}: parse overloads, config, and error handling.
 */
class SparqlParserTest {

    private static final String VALID_SELECT_QUERY = "SELECT * WHERE { ?s ?p ?o }";

    @Test
    void parseStringReturnsSameResultAsParseReader() {
        SparqlParser parser = new SparqlParser(new SparqlParserOptions.Builder().build());

        SparqlQueryAst fromString = (SparqlQueryAst) parser.parse(VALID_SELECT_QUERY);
        SparqlQueryAst fromReader = (SparqlQueryAst) parser.parse(new StringReader(VALID_SELECT_QUERY));

        assertNotNull(fromString);
        assertNotNull(fromReader);
        assertEquals(fromString.whereClause().patterns().size(),
                fromReader.whereClause().patterns().size());
        BgpAst bgp = (BgpAst) fromString.whereClause().patterns().getFirst();
        assertEquals("s", ((VarAst) bgp.triples().getFirst().subject()).name());
    }

    @Test
    void parseInputStreamUsesUtf8() {
        SparqlParser parser = new SparqlParser(new SparqlParserOptions.Builder().build());
        byte[] bytes = VALID_SELECT_QUERY.getBytes(StandardCharsets.UTF_8);

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse(new ByteArrayInputStream(bytes));

        assertNotNull(ast);
        assertNotNull(ast.whereClause());
    }

    @Test
    void parseInputStreamWithBaseIriAcceptsBaseIri() {
        SparqlParser parser = new SparqlParser(new SparqlParserOptions.Builder().build());
        byte[] bytes = VALID_SELECT_QUERY.getBytes(StandardCharsets.UTF_8);

        QueryAst ast = parser.parse(new ByteArrayInputStream(bytes), "http://example.org/");

        assertNotNull(ast);
    }

    @Test
    void parseReaderWithoutBaseIriUsesConfigBaseIri() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .baseIRI("http://config.org/")
                .build();
        SparqlParser parser = new SparqlParser(opts);

        QueryAst ast = parser.parse(new StringReader(VALID_SELECT_QUERY));

        assertNotNull(ast);
    }

    @Test
    void getConfigReturnsSetConfig() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .failFast(false)
                .build();
        SparqlParser parser = new SparqlParser(opts);

        assertSame(opts, parser.getConfig());
        assertFalse(((SparqlParserOptions) parser.getConfig()).isFailFast());
    }

    @Test
    void setConfigParseUsesNewConfig() {
        SparqlParser parser = new SparqlParser(
                new SparqlParserOptions.Builder().failFast(true).build());
        parser.setConfig(
                new SparqlParserOptions.Builder().failFast(false).collectErrors(true).build());

        assertThrows(QuerySyntaxException.class, () ->
                parser.parse("SELECT * WHERE { ?s ?p . }"));
    }

    @Test
    void parseCancellationWithSyntaxCauseReturnsOriginalSyntaxException() {
        QuerySyntaxException cause = new QuerySyntaxException("expected syntax error");

        QuerySyntaxException actual = SparqlParser.toQuerySyntaxException(
                new ParseCancellationException(cause),
                null);

        assertSame(cause, actual);
    }

    @Test
    void parseCancellationUsesCollectedDiagnosticsWhenAvailable() {
        SparqlParserOptions options = new SparqlParserOptions.Builder()
                .failFast(false)
                .collectErrors(true)
                .build();
        SparqlErrorListener listener = new SparqlErrorListener(options);
        listener.syntaxError(null, null, 3, 7, "mismatched input", null);

        QuerySyntaxException exception = SparqlParser.toQuerySyntaxException(
                new ParseCancellationException(),
                listener);

        assertTrue(exception.getMessage().contains("mismatched input"));
    }

    @Test
    void parserWithNullConfigParseStillWorks() {
        SparqlParser parser = new SparqlParser((SparqlParserOptions) null);

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse(VALID_SELECT_QUERY);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());
    }

    @Test
    void parseReaderIoExceptionWrapsInQueryEvaluationException() {
        SparqlParser parser = new SparqlParser(new SparqlParserOptions.Builder().build());
        Reader failingReader = newFailingReader();

        QueryEvaluationException ex = assertThrows(QueryEvaluationException.class, () ->
                parser.parse(failingReader, null));

        assertTrue(ex.getMessage().contains("Failed to parse") || ex.getCause() instanceof IOException);
    }

    @Test
    void validateReaderIoExceptionWrapsInQueryEvaluationException() {
        SparqlParser parser = new SparqlParser(new SparqlParserOptions.Builder().build());
        Reader failingReader = newFailingReader();

        QueryEvaluationException ex = assertThrows(QueryEvaluationException.class, () ->
                parser.validate(failingReader, null));

        assertTrue(ex.getMessage().contains("Failed to validate") || ex.getCause() instanceof IOException);
    }

    @Test
    void defaultConstructorUsesDefaultOptions() {
        SparqlParser parser = new SparqlParser();

        QueryAst ast = parser.parse(VALID_SELECT_QUERY);

        assertNotNull(ast);
        assertNotNull(parser.getConfig());
        assertInstanceOf(SparqlParserOptions.class, parser.getConfig());
    }

    @Test
    void validateReturnsNoDiagnosticsForValidQuery() {
        SparqlParser parser = new SparqlParser();

        QueryValidationResult result = parser.validate(VALID_SELECT_QUERY);

        assertTrue(result.isValid());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void validateReturnsSyntaxDiagnosticInsteadOfThrowing() {
        SparqlParser parser = new SparqlParser();

        QueryValidationResult result = parser.validate("SELECT * WHERE { ?s ?p . }");

        assertFalse(result.isValid());
        assertEquals(1, result.diagnostics().size());
        assertEquals(QueryDiagnostic.Kind.SYNTAX_ERROR, result.diagnostics().getFirst().kind());
    }

    @Test
    void validateReturnsLexerDiagnosticInsteadOfThrowing() {
        SparqlParser parser = new SparqlParser();
        String query = "SELECT * WHERE { ?s ?p ?o \u0001 }";

        QueryValidationResult result = parser.validate(query);

        assertFalse(result.isValid());
        assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.kind() == QueryDiagnostic.Kind.LEXER_ERROR));
    }

    @Test
    void validateReturnsMultipleSemanticDiagnostics() {
        SparqlParser parser = new SparqlParser();

        QueryValidationResult result = parser.validate("""
                SELECT ?x ?y WHERE {
                    ?s ?p ?o
                }
                ORDER BY ?z ?a
                """);

        assertFalse(result.isValid());
        assertEquals(2, result.diagnostics().size());
        assertTrue(result.diagnostics().stream()
                .allMatch(diagnostic -> diagnostic.kind() == QueryDiagnostic.Kind.SEMANTIC_ERROR));
    }

    @Test
    void validateReturnsConstructSemanticDiagnostic() {
        SparqlParser parser = new SparqlParser();

        QueryValidationResult result = parser.validate("""
                CONSTRUCT {
                    ?s ?p ?o
                }
                WHERE {
                    ?s ?p ?o
                }
                ORDER BY ?z
                """);

        assertFalse(result.isValid());
        assertEquals(1, result.diagnostics().size());
        assertEquals(QueryDiagnostic.Kind.SEMANTIC_ERROR, result.diagnostics().getFirst().kind());
        assertEquals("Variable ?z used in ORDER BY is not visible in WHERE clause",
                result.diagnostics().getFirst().message());
        assertEquals(QueryDiagnostic.Severity.ERROR, result.diagnostics().getFirst().severity());
        assertEquals("OrderByScopeValidationRule", result.diagnostics().getFirst().source());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validQueryValidationCases")
    void validateAcceptsValidQueries(String testName, String query) {
        SparqlParser parser = new SparqlParser();
        QueryValidationResult result = parser.validate(query);
        assertTrue(result.isValid());
        assertTrue(result.diagnostics().isEmpty());
    }

    private static Stream<Arguments> validQueryValidationCases() {
        return Stream.of(
                Arguments.of("Construct ORDER BY variable visible in VALUES", """
                        CONSTRUCT {
                            ?s ?p ?o
                        }
                        WHERE {
                            ?s ?p ?o
                        }
                        ORDER BY ?rank
                        VALUES ?rank { 1 }
                        """),
                Arguments.of("SELECT expression referencing an unbound variable", """
                        SELECT (STR(?x) AS ?label) WHERE {
                            ?s ?p ?o
                        }
                        """),
                Arguments.of("SELECT expression using earlier alias", """
                        SELECT (?p AS ?price) (STR(?price) AS ?label) WHERE {
                            ?s ?p ?o
                        }
                        """),
                Arguments.of("Implicit aggregate projection", """
                        SELECT (COUNT(?o) AS ?count) WHERE {
                            ?s ?p ?o
                        }
                        """),
                Arguments.of("GROUP BY expression alias projection", """
                        SELECT ?key WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY (CONCAT(STR(?s), STR(?o)) AS ?key)
                        """),
                Arguments.of("HAVING using GROUP BY expression alias", """
                        SELECT ?key WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY (CONCAT(STR(?s), STR(?o)) AS ?key)
                        HAVING (BOUND(?key))
                        """),
                Arguments.of("ORDER BY using GROUP BY expression alias", """
                        SELECT ?key WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY (CONCAT(STR(?s), STR(?o)) AS ?key)
                        ORDER BY ?key
                        """),
                Arguments.of("ORDER BY using projected aggregate alias", """
                        SELECT ?s (COUNT(?o) AS ?count) WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY ?s
                        ORDER BY ?count
                        """)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("semanticDiagnosticQueryCases")
    void validateReturnsSemanticDiagnostic(String testName, String query, String expectedSource) {
        SparqlParser parser = new SparqlParser();
        QueryValidationResult result = parser.validate(query);
        assertFalse(result.isValid());
        assertEquals(1, result.diagnostics().size());
        assertEquals(expectedSource, result.diagnostics().getFirst().source());
    }

    private static Stream<Arguments> semanticDiagnosticQueryCases() {
        return Stream.of(
                Arguments.of("ASK query with out-of-scope ORDER BY", """
                        ASK
                        WHERE {
                            ?s ?p ?o
                        }
                        ORDER BY ?z
                        """, "OrderByScopeValidationRule"),
                Arguments.of("GROUP BY with out-of-scope variable", """
                        SELECT ?s WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY ?z
                        """, "GroupByScopeValidationRule"),
                Arguments.of("Grouped SELECT projection with ungrouped variable", """
                        SELECT ?s ?o WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY ?s
                        """, "GroupedSelectProjectionValidationRule"),
                Arguments.of("Implicit aggregate projection with ungrouped variable", """
                        SELECT ?s (COUNT(?o) AS ?count) WHERE {
                            ?s ?p ?o
                        }
                        """, "GroupedSelectProjectionValidationRule"),
                Arguments.of("HAVING with out-of-scope variable", """
                        SELECT ?s WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY ?s
                        HAVING (BOUND(?z))
                        """, "HavingScopeValidationRule"),
                Arguments.of("Grouped HAVING with ungrouped variable", """
                        SELECT ?s WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY ?s
                        HAVING (BOUND(?o))
                        """, "GroupedHavingValidationRule"),
                Arguments.of("Grouped ORDER BY with ungrouped variable", """
                        SELECT ?s WHERE {
                            ?s ?p ?o
                        }
                        GROUP BY ?s
                        ORDER BY ?o
                        """, "GroupedOrderByValidationRule")
        );
    }

    @Test
    void validateReturnsGroupedSelectAllSemanticDiagnostic() {
        SparqlParser parser = new SparqlParser();

        QueryValidationResult result = parser.validate("""
                SELECT * WHERE {
                    ?s ?p ?o
                }
                GROUP BY ?s
                """);

        assertFalse(result.isValid());
        assertEquals(1, result.diagnostics().size());
        assertEquals("GroupedSelectProjectionValidationRule", result.diagnostics().getFirst().source());
        assertEquals("SELECT * is not permitted with GROUP BY", result.diagnostics().getFirst().message());
    }

    @Test
    void validateRejectsGroupedSelectExpressionMatchingGroupExpression() {
        SparqlParser parser = new SparqlParser();

        QueryValidationResult result = parser.validate("""
                SELECT (CONCAT(?s, ?o) AS ?key) WHERE {
                    ?s ?p ?o
                }
                GROUP BY CONCAT(?s, ?o)
                """);

        assertFalse(result.isValid());
        assertEquals(2, result.diagnostics().size());
        assertTrue(result.diagnostics().stream()
                .allMatch(diagnostic -> "GroupedSelectProjectionValidationRule".equals(diagnostic.source())));
    }

    @Test
    void validateReturnsDescribeSemanticDiagnostic() {
        SparqlParser parser = new SparqlParser();

        QueryValidationResult result = parser.validate("""
                DESCRIBE ?s
                WHERE {
                    ?s ?p ?o
                }
                ORDER BY ?z
                """);

        assertFalse(result.isValid());
        assertEquals(1, result.diagnostics().size());
        assertEquals(QueryDiagnostic.Kind.SEMANTIC_ERROR, result.diagnostics().getFirst().kind());
        assertEquals("Variable ?z used in ORDER BY is not visible in WHERE clause",
                result.diagnostics().getFirst().message());
    }

    @Test
    void validateSyntaxDiagnosticKeepsLocationWhenAvailable() {
        SparqlParser parser = new SparqlParser();

        QueryValidationResult result = parser.validate("""
                SELECT * WHERE {
                    ?s ?p .
                }
                """);

        assertFalse(result.isValid());
        assertFalse(result.diagnostics().isEmpty());
        assertEquals(QueryDiagnostic.Kind.SYNTAX_ERROR, result.diagnostics().getFirst().kind());
        assertEquals(QueryDiagnostic.Severity.ERROR, result.diagnostics().getFirst().severity());
        assertTrue(result.diagnostics().getFirst().line() >= 1);
        assertTrue(result.diagnostics().getFirst().column() >= 0);
        assertEquals("SparqlParser", result.diagnostics().getFirst().source());
    }

    @Test
    void validateLexerDiagnosticKeepsLocationAndSourceWhenAvailable() {
        SparqlParser parser = new SparqlParser();
        String query = "SELECT * WHERE { ?s ?p ?o \u0001 }";

        QueryValidationResult result = parser.validate(query);

        QueryDiagnostic lexerDiagnostic = result.diagnostics().stream()
                .filter(diagnostic -> diagnostic.kind() == QueryDiagnostic.Kind.LEXER_ERROR)
                .findFirst()
                .orElseThrow();

        assertEquals(QueryDiagnostic.Severity.ERROR, lexerDiagnostic.severity());
        assertTrue(lexerDiagnostic.line() >= 1);
        assertTrue(lexerDiagnostic.column() >= 0);
        assertEquals("SparqlLexer", lexerDiagnostic.source());
    }

    @Test
    void queryValidatorsFactoryReturnsWorkingSparqlValidator() {
        QueryTextValidator validator = SparqlValidators.sparql();

        QueryValidationResult result = validator.validate(VALID_SELECT_QUERY);

        assertTrue(result.isValid());
    }

    private Reader newFailingReader() {
        return new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("read failed");
            }

            @Override
            public void close() {
                // Nothing to close for this in-memory failing reader.
            }
        };
    }
}
