package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.sparql.options.SparqlAstError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SparqlParserOptions} and its builder.
 */
class SparqlParserOptionsTest {

    @Test
    void buildWithDefaultsReturnsOptionsWithExpectedValues() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder().build();

        assertNotNull(opts.getBaseIRI());
        assertFalse(opts.getBaseIRI().isEmpty());
        assertTrue(opts.isFailFast(), "default failFast should be true");
        assertTrue(opts.isCollectErrors(), "default collectErrors should be true");
        assertFalse(opts.isStrictMode(), "default strictMode should be false");
        assertNotNull(opts.getDiagnostics());
        assertTrue(opts.getDiagnostics().isEmpty());
        assertTrue(opts.getErrors().isEmpty());
    }

    @Test
    void baseIriCustomValueIsReturned() {
        String base = "http://example.org/";
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .baseIRI(base)
                .build();

        assertEquals(base, opts.getBaseIRI());
    }

    @Test
    void failFastTrueIsReturned() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .failFast(true)
                .build();
        assertTrue(opts.isFailFast());
    }

    @Test
    void failFastFalseIsReturned() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .failFast(false)
                .build();
        assertFalse(opts.isFailFast());
    }

    @Test
    void collectErrorsTrueIsReturned() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .collectErrors(true)
                .build();
        assertTrue(opts.isCollectErrors());
    }

    @Test
    void collectErrorsFalseIsReturned() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .collectErrors(false)
                .build();
        assertFalse(opts.isCollectErrors());
    }

    @Test
    void strictModeTrueIsReturned() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .strictMode(true)
                .build();
        assertTrue(opts.isStrictMode());
    }

    @Test
    void strictModeFalseIsReturned() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .strictMode(false)
                .build();
        assertFalse(opts.isStrictMode());
    }

    @Test
    void getDiagnosticsReturnsUnmodifiableList() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .collectErrors(true)
                .build();
        opts.addDiagnostic(new SparqlAstError(
                SparqlAstError.Kind.SYNTAX_ERROR,
                SparqlAstError.Severity.ERROR,
                "err1", 1, 0, null, null));

        List<SparqlAstError> diagnostics = opts.getDiagnostics();
        assertThrows(UnsupportedOperationException.class, () -> diagnostics.add(null));
        assertEquals(1, diagnostics.size());
    }

    @Test
    void addDiagnosticWhenCollectErrorsTrueAddsAndGetErrorsFormatsThem() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .collectErrors(true)
                .build();

        opts.addDiagnostic(new SparqlAstError(
                SparqlAstError.Kind.LEXER_ERROR,
                SparqlAstError.Severity.ERROR,
                "token recognition error", 1, 0, null, null));
        opts.addDiagnostic(new SparqlAstError(
                SparqlAstError.Kind.SYNTAX_ERROR,
                SparqlAstError.Severity.ERROR,
                "mismatched input", 2, 5, "SELECT", null));

        assertEquals(2, opts.getDiagnostics().size());
        assertEquals(SparqlAstError.Kind.LEXER_ERROR, opts.getDiagnostics().get(0).kind());
        assertEquals(1, opts.getDiagnostics().get(0).line());
        assertEquals(SparqlAstError.Kind.SYNTAX_ERROR, opts.getDiagnostics().get(1).kind());
        assertEquals(2, opts.getDiagnostics().get(1).line());
        assertEquals(5, opts.getDiagnostics().get(1).column());
        assertEquals("SELECT", opts.getDiagnostics().get(1).offendingText());

        List<String> errors = opts.getErrors();
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).contains("line 1"));
        assertTrue(errors.get(1).contains("line 2"));
    }

    @Test
    void addDiagnosticWhenCollectErrorsFalseDoesNotAdd() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .collectErrors(false)
                .build();

        opts.addDiagnostic(new SparqlAstError(
                SparqlAstError.Kind.SYNTAX_ERROR,
                SparqlAstError.Severity.ERROR,
                "some error", 1, 0, null, null));

        assertTrue(opts.getDiagnostics().isEmpty());
        assertTrue(opts.getErrors().isEmpty());
    }

    @Test
    void addDiagnosticNullDoesNotAdd() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .collectErrors(true)
                .build();

        opts.addDiagnostic(null);

        assertTrue(opts.getDiagnostics().isEmpty());
        assertTrue(opts.getErrors().isEmpty());
    }

    @Test
    void builderFluentCallsBuildsCorrectOptions() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .baseIRI("http://test.org/")
                .failFast(false)
                .collectErrors(true)
                .strictMode(true)
                .build();

        assertEquals("http://test.org/", opts.getBaseIRI());
        assertFalse(opts.isFailFast());
        assertTrue(opts.isCollectErrors());
        assertTrue(opts.isStrictMode());
    }
}
