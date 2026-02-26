package fr.inria.corese.core.next.query.impl.parser;

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
        assertNotNull(opts.getErrors());
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
    void getErrorsReturnsUnmodifiableList() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .collectErrors(true)
                .build();
        opts.addError("err1");

        List<String> errors = opts.getErrors();
        assertThrows(UnsupportedOperationException.class, () -> errors.add("x"));
        assertEquals(1, errors.size());
    }

    @Test
    void addErrorWhenCollectErrorsTrueAddsMessage() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .collectErrors(true)
                .build();

        opts.addError("line 1:0 token recognition error");
        opts.addError("line 2:5 mismatched input");

        assertEquals(2, opts.getErrors().size());
        assertTrue(opts.getErrors().get(0).contains("line 1"));
        assertTrue(opts.getErrors().get(1).contains("line 2"));
    }

    @Test
    void addErrorWhenCollectErrorsFalseDoesNotAdd() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .collectErrors(false)
                .build();

        opts.addError("some error");

        assertTrue(opts.getErrors().isEmpty());
    }

    @Test
    void addErrorNullMessageDoesNotAdd() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder()
                .collectErrors(true)
                .build();

        opts.addError(null);

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
