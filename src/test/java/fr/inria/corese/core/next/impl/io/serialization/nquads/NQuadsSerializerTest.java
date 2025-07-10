package fr.inria.corese.core.next.impl.io.serialization.nquads;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.io.serialization.TestStatementFactory;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NQuadsSerializerTest {

    private Model model;
    private NQuadsOption config;
    private NQuadsSerializer nQuadsSerializer;
    private TestStatementFactory factory;

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
        config = NQuadsOption.defaultConfig();
        nQuadsSerializer = new NQuadsSerializer(model, config);
        factory = new TestStatementFactory();

        mockExPerson = factory.createIRI("http://example.org/Person");
        mockExName = factory.createIRI("http://example.org/name");
        mockExKnows = factory.createIRI("http://example.org/knows");

        mockLiteralJohn = factory.createLiteral(lexJohn, null, null);
        mockLiteralHelloEn = factory.createLiteral(hello, null, "en");

        mockBNode1 = factory.createBlankNode("b1");
        mockBNode2 = factory.createBlankNode("b2");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null model")
    void constructorShouldThrowForNullModel() {
        assertThrows(NullPointerException.class, () -> new NQuadsSerializer(null), "Model cannot be null");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null configuration")
    void constructorShouldThrowForNullConfig() {
        assertThrows(NullPointerException.class, () -> new NQuadsSerializer(model, null), "Configuration cannot be null");
    }

    @Test
    @DisplayName("Write should serialize a simple statement correctly (default graph)")
    void writeShouldSerializeSimpleStatement() throws SerializationException {
        Statement stmt = factory.createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nQuadsSerializer.write(writer);

        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNQuadsString(lexJohn)) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Write should handle blank nodes with default prefix")
    void writeShouldHandleBlankNodes() throws SerializationException {
        Statement stmt = factory.createStatement(
                mockBNode1,
                mockExKnows,
                mockBNode2
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nQuadsSerializer.write(writer);

        String expected = String.format("_:%s <%s> _:%s",
                mockBNode1.stringValue(),
                mockExKnows.stringValue(),
                mockBNode2.stringValue()) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Write should handle blank nodes in context with default prefix")
    void writeShouldHandleBlankNodesInContext() throws SerializationException {
        Resource blankNodeContext = factory.createBlankNode("b3");
        Statement stmt = factory.createStatement(
                mockBNode1,
                mockExKnows,
                mockExPerson,
                blankNodeContext
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nQuadsSerializer.write(writer);

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
        Statement stmt = factory.createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer faultyWriter = mock(Writer.class);
        doThrow(new IOException("Simulated IO error during write")).when(faultyWriter).write(anyString());
        doThrow(new IOException("Simulated IO error (char array)")).when(faultyWriter).write(any(char[].class), anyInt(), anyInt());
        doThrow(new IOException("Simulated IO error (close)")).when(faultyWriter).close();

        SerializationException thrown = assertThrows(SerializationException.class, () -> nQuadsSerializer.write(faultyWriter));
        assertEquals("N-Quads serialization failed [Format: N-Quads]", thrown.getMessage());
    }

    @Test
    @DisplayName("Write should throw SerializationException for null subject value in strict mode")
    void writeShouldThrowOnNullSubjectValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(null);
        when(stmt.getPredicate()).thenReturn(mockExName);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(stmt.getContext()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nQuadsSerializer.write(writer));
        assertEquals("Invalid N-Quads data: Value cannot be null in N-Quads format when strictMode is enabled. [Format: N-Quads]", thrown.getMessage());
    }

    @Test
    @DisplayName("Write should throw SerializationException for null predicate value in strict mode")
    void writeShouldThrowOnNullPredicateValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(mockExPerson);
        when(stmt.getPredicate()).thenReturn(null);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(stmt.getContext()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nQuadsSerializer.write(writer));
        assertEquals("Invalid N-Quads data: Value cannot be null in N-Quads format when strictMode is enabled. [Format: N-Quads]", thrown.getMessage());
    }

    @Test
    @DisplayName("Write should throw SerializationException for null object value in strict mode")
    void writeShouldThrowOnNullObjectValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(mockExPerson);
        when(stmt.getPredicate()).thenReturn(mockExName);
        when(stmt.getObject()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nQuadsSerializer.write(writer));
        assertEquals("Invalid N-Quads data: Value cannot be null in N-Quads format when strictMode is enabled. [Format: N-Quads]", thrown.getMessage());
    }

    @Test
    @DisplayName("Write should handle null context correctly (default graph)")
    void writeShouldHandleNullContext() throws SerializationException {
        Statement stmt = factory.createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn,
                null
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nQuadsSerializer.write(writer);

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
    @DisplayName("Write should handle various literal values with appropriate escaping (including Unicode)")
    void writeShouldHandleVariousLiterals(String literalValue) throws SerializationException {
        Literal literalMock = factory.createLiteral(literalValue, null, null);

        Statement stmt = factory.createStatement(
                mockExPerson,
                mockExName,
                literalMock
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nQuadsSerializer.write(writer);

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
        Statement stmt = factory.createStatement(mockExPerson, factory.createIRI("http://example.org/greeting"), mockLiteralHelloEn);

        Model currentTestModel = mock(Model.class);
        when(currentTestModel.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer writer = new StringWriter();

        NQuadsSerializer serializer = new NQuadsSerializer(currentTestModel, NQuadsOption.defaultConfig());
        serializer.write(writer);

        String expectedOutput = String.format("<%s> <%s> \"%s\"@%s",
                mockExPerson.stringValue(),
                factory.createIRI("http://example.org/greeting").stringValue(),
                escapeNQuadsString(hello),
                mockLiteralHelloEn.getLanguage().get()) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }

    @Test
    @DisplayName("Should handle literals with custom datatypes")
    void shouldHandleLiteralsWithCustomDatatypes() throws SerializationException {
        IRI customDatatype = factory.createIRI("http://example.org/myDataType");
        Literal customLiteral = factory.createLiteral("123", customDatatype, null);

        Statement stmt = factory.createStatement(mockExPerson, factory.createIRI("http://example.org/value"), customLiteral);

        Model currentTestModel = mock(Model.class);
        when(currentTestModel.iterator()).thenReturn(new MockStatementIterator(stmt));

        NQuadsSerializer serializer = new NQuadsSerializer(currentTestModel, NQuadsOption.defaultConfig());

        StringWriter writer = new StringWriter();
        serializer.write(writer);

        String expectedOutput = String.format("<%s> <%s> \"%s\"^^<%s>",
                mockExPerson.stringValue(),
                factory.createIRI("http://example.org/value").stringValue(),
                escapeNQuadsString("123"),
                customDatatype.stringValue()) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }


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
                    if (c <= 0x1F || c == 0x7F) {
                        sb.append(String.format("\\u%04X", (int) c));
                    } else if (c >= 0x80 && c <= 0xFFFF) {
                        sb.append(String.format("\\u%04X", (int) c));
                    } else if (Character.isHighSurrogate(c)) {
                        int codePoint = s.codePointAt(i);
                        if (Character.isValidCodePoint(codePoint)) {
                            sb.append(String.format("\\U%08X", codePoint));
                            i++;
                        } else {
                            sb.append(c);
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
}
