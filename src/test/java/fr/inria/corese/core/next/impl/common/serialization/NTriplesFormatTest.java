package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
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

class NTriplesFormatTest {

    private Model model;
    private FormatConfig config;
    private NTriplesFormat nTriplesFormat;

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
        config = new FormatConfig.Builder().build();
        nTriplesFormat = new NTriplesFormat(model, config);


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
        assertThrows(NullPointerException.class, () -> new NTriplesFormat(null), "Model cannot be null");
        assertThrows(NullPointerException.class, () -> new NTriplesFormat(null, config), "Model cannot be null");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null config")
    void constructorShouldThrowForNullConfig() {
        assertThrows(NullPointerException.class, () -> new NTriplesFormat(model, null), "Configuration cannot be null");
    }

    @Test
    @DisplayName("Write should serialize simple statement correctly")
    void writeShouldSerializeSimpleStatement() throws SerializationException {
        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesFormat.write(writer);


        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNTriplesString(lexJohn)) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Write should serialize statement with context but ignore it (N-Triples)")
    void writeShouldSerializeStatementWithContext() throws SerializationException {
        IRI mockContext = createIRI("http://example.org/ctx");
        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn,
                mockContext
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesFormat.write(writer);

        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNTriplesString(lexJohn)) + " .\n";

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
        nTriplesFormat.write(writer);

        String expected = String.format("_:%s <%s> _:%s",
                mockBNode1.stringValue(),
                mockExKnows.stringValue(),
                mockBNode2.stringValue()) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Write should handle blank nodes with custom prefix")
    void writeShouldHandleBlankNodesWithCustomPrefix() throws SerializationException {
        FormatConfig customConfig = new FormatConfig.Builder().blankNodePrefix("genid-").build();
        NTriplesFormat customSerializer = new NTriplesFormat(model, customConfig);

        Statement stmt = createStatement(
                mockBNode1,
                mockExKnows,
                mockBNode2
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        customSerializer.write(writer);

        String expected = String.format("genid-%s <%s> genid-%s",
                mockBNode1.stringValue(),
                mockExKnows.stringValue(),
                mockBNode2.stringValue()) + " .\n";

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

        assertThrows(SerializationException.class, () -> nTriplesFormat.write(faultyWriter));
    }

    @Test
    @DisplayName("Write should throw SerializationException on null subject value from Statement")
    void writeShouldThrowOnNullSubjectValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(null);
        when(stmt.getPredicate()).thenReturn(mockExName);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        assertThrows(SerializationException.class, () -> nTriplesFormat.write(writer));
    }

    @Test
    @DisplayName("Write should throw SerializationException on null predicate value from Statement")
    void writeShouldThrowOnNullPredicateValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(mockExPerson);
        when(stmt.getPredicate()).thenReturn(null);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        assertThrows(SerializationException.class, () -> nTriplesFormat.write(writer));
    }

    @Test
    @DisplayName("Write should throw SerializationException on null object value from Statement")
    void writeShouldThrowOnNullObjectValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(mockExPerson);
        when(stmt.getPredicate()).thenReturn(mockExName);
        when(stmt.getObject()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        assertThrows(SerializationException.class, () -> nTriplesFormat.write(writer));
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
    @DisplayName("Write should handle various literal values with proper escaping")
    void writeShouldHandleVariousLiterals(String literalValue) throws SerializationException {
        Literal literalMock = createLiteral(literalValue, null, null);

        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                literalMock
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesFormat.write(writer);

        String expectedEscapedLiteral = escapeNTriplesString(literalValue);
        String expectedOutput = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                expectedEscapedLiteral) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }

    @Test
    @DisplayName("Write should handle multiple statements")
    void writeShouldHandleMultipleStatements() throws SerializationException {
        Statement stmt1 = createStatement(
                mockExPerson,
                mockExName,
                createLiteral("o1", null, null)
        );
        Statement stmt2 = createStatement(
                mockBNode1,
                mockExKnows,
                mockExPerson,
                createIRI("http://example.org/ctx")
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt1, stmt2));

        StringWriter writer = new StringWriter();
        nTriplesFormat.write(writer);

        String expectedOutput = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNTriplesString("o1")) + " .\n" +
                String.format("_:%s <%s> <%s>",
                        mockBNode1.stringValue(),
                        mockExKnows.stringValue(),
                        mockExPerson.stringValue()) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }

    @Test
    @DisplayName("Should handle literals with language tags")
    void shouldHandleLiteralsWithLanguageTags() throws SerializationException {
        Statement stmt = createStatement(mockExPerson, createIRI("http://example.org/greeting"), mockLiteralHelloEn);

        Model currentTestModel = mock(Model.class);
        when(currentTestModel.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer writer = new StringWriter();
        NTriplesFormat serializer = new NTriplesFormat(currentTestModel);
        serializer.write(writer);

        String expectedOutput = String.format("<%s> <%s> \"%s\"@%s",
                mockExPerson.stringValue(),
                createIRI("http://example.org/greeting").stringValue(),
                escapeNTriplesString(hello),
                mockLiteralHelloEn.getLanguage().get()) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }


    /**
     * Escapes a string according to N-Triples literal escaping rules.
     * This helper is used in tests to construct the *expected* output strings.
     * It mimics the behavior of NTriplesFormat's internal escapeLiteral method.
     *
     * @param s The string to escape.
     * @return The escaped string.
     */
    private String escapeNTriplesString(String s) {
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
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (c >= '\u0000' && c <= '\u001F' || c == '\u007F') {
                        sb.append(String.format("\\u%04X", (int) c));
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


    /**
     * Creates a mocked Literal object.
     * Important: The `lexicalForm` is the *raw string value* of the literal,
     * without N-Triples specific quotes, lang tags, or datatype URIs.
     * The `NTriplesFormat` class is responsible for adding those.
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