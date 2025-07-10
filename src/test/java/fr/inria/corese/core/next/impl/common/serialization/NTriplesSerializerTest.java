package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.serialization.TestStatementFactory;
import fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesOption;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NTriplesSerializerTest {

    private Model model;
    private NTriplesOption config;
    private NTriplesSerializer nTriplesSerializer;
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
        config = NTriplesOption.defaultConfig();
        nTriplesSerializer = new NTriplesSerializer(model, config);
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
        assertThrows(NullPointerException.class, () -> new NTriplesSerializer(null), "Model cannot be null");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null configuration")
    void constructorShouldThrowForNullConfig() {
        assertThrows(NullPointerException.class, () -> new NTriplesSerializer(model, null), "Configuration cannot be null");
    }

    @Test
    @DisplayName("Write should serialize a simple statement correctly (default graph)")
    void writeShouldSerializeSimpleStatement() throws SerializationException {
        Statement stmt = factory.createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn
        );
        Mockito.when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesSerializer.write(writer);

        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNTriplesString(lexJohn)) + " .\n";

        Assertions.assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Write should serialize a statement with context but ignore it (N-Triples)")
    void writeShouldSerializeStatementWithContext() throws SerializationException {
        IRI mockContext = factory.createIRI("http://example.org/ctx");
        Statement stmt = factory.createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn,
                mockContext
        );
        Mockito.when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesSerializer.write(writer);

        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNTriplesString(lexJohn)) + " .\n";

        Assertions.assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Write should handle blank nodes with default N-Triples prefix (_:)")
    void writeShouldHandleBlankNodes() throws SerializationException {
        Statement stmt = factory.createStatement(
                mockBNode1,
                mockExKnows,
                mockBNode2
        );
        Mockito.when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesSerializer.write(writer);

        String expected = String.format("_:%s <%s> _:%s",
                mockBNode1.stringValue(),
                mockExKnows.stringValue(),
                mockBNode2.stringValue()) + " .\n";

        Assertions.assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Write should throw SerializationException on IO error")
    void writeShouldThrowOnIOException() throws IOException {
        Statement stmt = factory.createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn
        );
        Mockito.when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer faultyWriter = Mockito.mock(Writer.class);

        doThrow(new IOException("Simulated IO error during write")).when(faultyWriter).write(anyString());
        doThrow(new IOException("Simulated IO error (char array)")).when(faultyWriter).write(any(char[].class), anyInt(), anyInt());
        doThrow(new IOException("Simulated IO error (close)")).when(faultyWriter).close();

        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(faultyWriter));

        assertEquals("N-Triples serialization failed [Format: N-Triples]", thrown.getMessage());
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
        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(writer));

        assertEquals("Invalid N-Triples data: Value cannot be null in N-Triples format when strictMode is enabled. [Format: N-Triples]", thrown.getMessage());
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
        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(writer));
        assertEquals("Invalid N-Triples data: Value cannot be null in N-Triples format when strictMode is enabled. [Format: N-Triples]", thrown.getMessage());
    }

    @Test
    @DisplayName("Write should throw SerializationException for null object value in strict mode")
    void writeShouldThrowOnNullObjectValue() {
        Statement stmt = Mockito.mock(Statement.class);
        Mockito.when(stmt.getSubject()).thenReturn(mockExPerson);
        Mockito.when(stmt.getPredicate()).thenReturn(mockExName);
        Mockito.when(stmt.getObject()).thenReturn(null);
        Mockito.when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(writer));
        assertEquals("Invalid N-Triples data: Value cannot be null in N-Triples format when strictMode is enabled. [Format: N-Triples]", thrown.getMessage());
    }


    @Test
    @DisplayName("Should handle null context correctly (default graph)")
    void writeShouldHandleNullContext() throws SerializationException {
        Statement stmt = factory.createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn,
                null
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesSerializer.write(writer);


        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNTriplesString(lexJohn)) + " .\n";

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
        Mockito.when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesSerializer.write(writer);


        String expectedEscapedLiteral = escapeNTriplesString(literalValue);
        String expectedOutput = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                expectedEscapedLiteral) + " .\n";

        Assertions.assertEquals(expectedOutput, writer.toString());
    }

    @Test
    @DisplayName("Should handle literals with language tags")
    void shouldHandleLiteralsWithLanguageTags() throws SerializationException {
        Statement stmt = factory.createStatement(mockExPerson, factory.createIRI("http://example.org/greeting"), mockLiteralHelloEn);

        Model currentTestModel = Mockito.mock(Model.class);
        Mockito.when(currentTestModel.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer writer = new StringWriter();

        NTriplesSerializer serializer = new NTriplesSerializer(currentTestModel, NTriplesOption.defaultConfig());
        serializer.write(writer);

        String expectedOutput = String.format("<%s> <%s> \"%s\"@%s",
                mockExPerson.stringValue(),
                factory.createIRI("http://example.org/greeting").stringValue(),
                escapeNTriplesString(hello),
                mockLiteralHelloEn.getLanguage().get()) + " .\n";

        Assertions.assertEquals(expectedOutput, writer.toString());
    }


    @Test
    @DisplayName("Should handle literals with custom datatypes")
    void shouldHandleLiteralsWithCustomDatatypes() throws SerializationException {
        IRI customDatatype = factory.createIRI("http://example.org/myDataType");
        Literal customLiteral = factory.createLiteral("123", customDatatype, null);

        Statement stmt = factory.createStatement(mockExPerson, factory.createIRI("http://example.org/value"), customLiteral);

        Model currentTestModel = mock(Model.class);
        when(currentTestModel.iterator()).thenReturn(new MockStatementIterator(stmt));

        NTriplesSerializer serializer = new NTriplesSerializer(currentTestModel, NTriplesOption.defaultConfig());

        StringWriter writer = new StringWriter();
        serializer.write(writer);

        String expectedOutput = String.format("<%s> <%s> \"%s\"^^<%s>",
                mockExPerson.stringValue(),
                factory.createIRI("http://example.org/value").stringValue(),
                escapeNTriplesString("123"),
                customDatatype.stringValue()) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }


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
                    if (c <= 0x1F || c == 0x7F) {
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
}
