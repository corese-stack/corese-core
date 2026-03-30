package fr.inria.corese.core.next.query.impl.parser;

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

import org.junit.jupiter.api.Test;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.api.io.parser.QueryOptions;
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

        QueryAst fromString = parser.parse(VALID_SELECT_QUERY);
        QueryAst fromReader = parser.parse(new StringReader(VALID_SELECT_QUERY));

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

        QueryAst ast = parser.parse(new ByteArrayInputStream(bytes));

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
        SparqlParser parser = new SparqlParser((QueryOptions) null);

        QueryAst ast = parser.parse(VALID_SELECT_QUERY);

        assertNotNull(ast);
        assertNotNull(ast.whereClause());
    }

    @Test
    void parserWithWrongConfigTypeParseUsesDefaultOptions() {
        SparqlParser parser = new SparqlParser(new QueryOptions() {});

        QueryAst ast = parser.parse(VALID_SELECT_QUERY);

        assertNotNull(ast);
    }

    @Test
    void parseReaderIoExceptionWrapsInQueryEvaluationException() {
        SparqlParser parser = new SparqlParser(new SparqlParserOptions.Builder().build());
        Reader failingReader = new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("read failed");
            }
            @Override
            public void close() {}
        };

        QueryEvaluationException ex = assertThrows(QueryEvaluationException.class, () ->
                parser.parse(failingReader, null));

        assertTrue(ex.getMessage().contains("Failed to parse") || ex.getCause() instanceof IOException);
    }

    @Test
    void defaultConstructorUsesDefaultOptions() {
        SparqlParser parser = new SparqlParser();

        QueryAst ast = parser.parse(VALID_SELECT_QUERY);

        assertNotNull(ast);
        assertNotNull(parser.getConfig());
        assertInstanceOf(SparqlParserOptions.class, parser.getConfig());
    }
}
