package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.serialization.config.FormatConfig;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NQuadsSerializerTest {

    private Model model;
    private FormatConfig config;
    private NQuadsSerializer nQuadsFormat;

    private Resource mockExPerson;
    private IRI mockExName;

    private IRI mockExKnows;

    private final String lexJohn = "John Doe";

    private final String hello = "Hello";

    private Literal mockLiteralJohn;
    private Literal mockLiteralHelloEn;
    private Resource mockBNode1;
    private Resource mockBNode2;

    @BeforeEach
    void setUp() {
        model = mock(Model.class);
        config = FormatConfig.nquadsConfig();
        nQuadsFormat = new NQuadsSerializer(model, config);

        mockExPerson = createIRI("http://example.org/Person");
        mockExName = createIRI("http://example.org/name");

        mockExKnows = createIRI("http://example.org/knows");


        mockLiteralJohn = createLiteral(lexJohn, null, null);
        mockLiteralHelloEn = createLiteral(hello, null, "en");

        mockBNode1 = createBlankNode("b1");
        mockBNode2 = createBlankNode("b2");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null model")
    void constructorShouldThrowForNullModel() {
        assertThrows(NullPointerException.class, () -> new NQuadsSerializer(null), "Model cannot be null");
        assertThrows(NullPointerException.class, () -> new NQuadsSerializer(null, config), "Model cannot be null");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null config")
    void constructorShouldThrowForNullConfig() {
        assertThrows(NullPointerException.class, () -> new NQuadsSerializer(model, null), "Configuration cannot be null");
    }

    @Test
    @DisplayName("Write should serialize simple statement correctly (default graph)")
    void writeShouldSerializeSimpleStatement() throws SerializationException {
        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nQuadsFormat.write(writer);


        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNQuadsString(lexJohn)) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Write should handle blank nodes with default prefix")
    void writeShouldHandleBlankNodes() throws SerializationException {
        Statement stmt = createStatement(
                mockBNode1,
                mockExKnows,
                mockBNode2
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nQuadsFormat.write(writer);

        String expected = String.format("_:%s <%s> _:%s",
                mockBNode1.stringValue(),
                mockExKnows.stringValue(),
                mockBNode2.stringValue()) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Write should handle blank nodes in context with default prefix")
    void writeShouldHandleBlankNodesInContext() throws SerializationException {
        Resource blankNodeContext = createBlankNode("b3");
        Statement stmt = createStatement(
                mockBNode1,
                mockExKnows,
                mockExPerson,
                blankNodeContext
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nQuadsFormat.write(writer);

        String expected = String.format("_:%s <%s> <%s> _:%s",
                mockBNode1.stringValue(),
                mockExKnows.stringValue(),
                mockExPerson.stringValue(),
                blankNodeContext.stringValue()) + " .\n";

        assertEquals(expected, writer.toString());
    }


    @Test
    @DisplayName("Write should throw SerializationException on IO error")
    void writeShouldThrowOnIOException() throws IOException {
        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer faultyWriter = mock(Writer.class);
        doThrow(new IOException("Simulated IO error")).when(faultyWriter).write(anyString());

        assertThrows(SerializationException.class, () -> nQuadsFormat.write(faultyWriter));
    }

    @Test
    @DisplayName("Write should throw SerializationException on null subject value from Statement in strict mode")
    void writeShouldThrowOnNullSubjectValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(null);
        when(stmt.getPredicate()).thenReturn(mockExName);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(stmt.getContext()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nQuadsFormat.write(writer));
        assertEquals("Invalid data: Value cannot be null in N-Quads format when strictMode is enabled. [Format: NQuads]", thrown.getMessage());
    }

    @Test
    @DisplayName("Write should throw SerializationException on null predicate value from Statement in strict mode")
    void writeShouldThrowOnNullPredicateValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(mockExPerson);
        when(stmt.getPredicate()).thenReturn(null);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(stmt.getContext()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nQuadsFormat.write(writer));
        assertEquals("Invalid data: Value cannot be null in N-Quads format when strictMode is enabled. [Format: NQuads]", thrown.getMessage());
    }

    @Test
    @DisplayName("Write should throw SerializationException on null object value from Statement in strict mode")
    void writeShouldThrowOnNullObjectValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(mockExPerson);
        when(stmt.getPredicate()).thenReturn(mockExName);
        when(stmt.getObject()).thenReturn(null);
        when(stmt.getContext()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nQuadsFormat.write(writer));
        assertEquals("Invalid data: Value cannot be null in N-Quads format when strictMode is enabled. [Format: NQuads]", thrown.getMessage());
    }

    @Test
    @DisplayName("Write should correctly handle null context (default graph)")
    void writeShouldHandleNullContext() throws SerializationException {
        // Default config (nquadsConfig) has includeContext = true, but this statement has null context.
        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn,
                null
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nQuadsFormat.write(writer);


        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNQuadsString(lexJohn)) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "simple literal",
            "literal with \"quotes\"",
            "literal with \\ backslash",
            "literal with \n newline",
            "literal with \t tab",
            "literal with \r carriage return",
            "literal with \u0001 (SOH)",
            "literal with \u007F (DEL)"
    })
    @DisplayName("Write should handle various literal values with proper escaping (including Unicode)")
    void writeShouldHandleVariousLiterals(String literalValue) throws SerializationException {
        Literal literalMock = createLiteral(literalValue, null, null);

        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                literalMock
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nQuadsFormat.write(writer);


        String expectedEscapedLiteral = escapeNQuadsString(literalValue);
        String expectedOutput = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                expectedEscapedLiteral) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }


    @Test
    @DisplayName("Should handle literals with language tags")
    void shouldHandleLiteralsWithLanguageTags() throws SerializationException {
        Statement stmt = createStatement(mockExPerson, createIRI("http://example.org/greeting"), mockLiteralHelloEn);

        Model currentTestModel = mock(Model.class);
        when(currentTestModel.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer writer = new StringWriter();

        NQuadsSerializer serializer = new NQuadsSerializer(currentTestModel, FormatConfig.nquadsConfig());
        serializer.write(writer);

        String expectedOutput = String.format("<%s> <%s> \"%s\"@%s",
                mockExPerson.stringValue(),
                createIRI("http://example.org/greeting").stringValue(),
                escapeNQuadsString(hello),
                mockLiteralHelloEn.getLanguage().get()) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }


    /**
     * Creates a mocked Literal object.
     * Important: The `lexicalForm` is the *raw string value* of the literal,
     * without N-Quads specific quotes, lang tags, or datatype URIs.
     * The `NQuadsFormat` class is responsible for adding those.
     *
     * @param lexicalForm The raw string value of the literal (e.g., "hello", "123").
     * @param dataTypeIRI The IRI of the literal's datatype (e.g., XSD.INTEGER.getIRI()), or null for plain/lang-tagged.
     * @param langTag     The language tag (e.g., "en"), or null if not language-tagged.
     * @return A mocked Literal instance.
     */
    private Literal createLiteral(String lexicalForm, IRI dataTypeIRI, String langTag) {
        Literal literal = mock(Literal.class);
        when(literal.isLiteral()).thenReturn(true);
        when(literal.isResource()).thenReturn(false);
        when(literal.stringValue()).thenReturn(lexicalForm);

        if (langTag != null && !langTag.isEmpty()) {
            when(literal.getLanguage()).thenReturn(Optional.of(langTag));
            when(literal.getDatatype()).thenReturn(RDF.langString.getIRI());
        } else {
            when(literal.getLanguage()).thenReturn(Optional.empty());
            when(literal.getDatatype()).thenReturn(dataTypeIRI);
        }
        return literal;
    }

    /**
     * Escapes a string according to N-Quads literal escaping rules.
     * This helper is used in tests to construct the *expected* output strings.
     * It mimics the behavior of NQuadsFormat's internal escapeLiteral method,
     * considering that `nquadsConfig().escapeUnicode()` is `true`.
     *
     * @param s The string to escape.
     * @return The escaped string.
     */
    private String escapeNQuadsString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b': // backspace
                    sb.append("\\b");
                    break;
                case '\f': // form feed
                    sb.append("\\f");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (c <= 0x1F || c == 0x7F) { // Control characters
                        sb.append(String.format("\\u%04X", (int) c));
                    } else if (c >= 0x80 && c <= 0xFFFF) { // Non-ASCII Basic Multilingual Plane characters
                        sb.append(String.format("\\u%04X", (int) c));
                    } else if (Character.isHighSurrogate(c)) { // Supplementary characters
                        int codePoint = s.codePointAt(i);
                        if (Character.isValidCodePoint(codePoint)) {
                            sb.append(String.format("\\U%08X", codePoint));
                            i++; // Skip the low surrogate char
                        } else {
                            sb.append(c); // Append invalid surrogate char directly
                        }
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }


    private static class MockStatementIterator implements Iterator<Statement> {
        private final Statement[] statements;
        private int index = 0;

        MockStatementIterator(Statement... statements) {
            this.statements = statements;
        }

        @Override
        public boolean hasNext() {
            return index < statements.length;
        }

        @Override
        public Statement next() {
            return statements[index++];
        }
    }

    private Statement createStatement(Resource subject, IRI predicate, Value object) {
        return createStatement(subject, predicate, object, null);
    }

    private Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(subject);
        when(stmt.getPredicate()).thenReturn(predicate);
        when(stmt.getObject()).thenReturn(object);
        when(stmt.getContext()).thenReturn(context);
        return stmt;
    }

    private Resource createBlankNode(String id) {
        Resource blankNode = mock(Resource.class);
        when(blankNode.isResource()).thenReturn(true);
        when(blankNode.isBNode()).thenReturn(true);
        when(blankNode.isIRI()).thenReturn(false);
        when(blankNode.stringValue()).thenReturn(id);
        return blankNode;
    }

    private IRI createIRI(String uri) {
        IRI iri = mock(IRI.class);
        when(iri.isResource()).thenReturn(true);
        when(iri.isIRI()).thenReturn(true);
        when(iri.isBNode()).thenReturn(false);
        when(iri.stringValue()).thenReturn(uri);
        return iri;
    }
}