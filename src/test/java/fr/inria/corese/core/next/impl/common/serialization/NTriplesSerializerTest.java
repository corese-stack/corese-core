package fr.inria.corese.core.next.impl.io.serialization;

import fr.inria.corese.core.next.api.*;
<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
========
import fr.inria.corese.core.next.impl.common.serialization.config.NTriplesConfig;
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesFormat;
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

<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
class NTriplesFormatTest {

    private Model model;
    private fr.inria.corese.core.next.impl.io.serialization.FormatConfig config;
    private fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesFormat nTriplesFormat;
========
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NTriplesSerializerTest {

    private Model model;
    private NTriplesConfig config;
    private NTriplesSerializer nTriplesSerializer;
    private TestStatementFactory factory;
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java

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
<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
        model = Mockito.mock(Model.class);
        config = new fr.inria.corese.core.next.impl.io.serialization.FormatConfig.Builder().build();
        nTriplesFormat = new fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesFormat(model, config);
========
        model = mock(Model.class);
        config = NTriplesConfig.defaultConfig();
        nTriplesSerializer = new NTriplesSerializer(model, config);
        factory = new TestStatementFactory();
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java

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
<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
        Assertions.assertThrows(NullPointerException.class, () -> new fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesFormat(null), "Model cannot be null");
        Assertions.assertThrows(NullPointerException.class, () -> new fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesFormat(null, config), "Model cannot be null");
========
        assertThrows(NullPointerException.class, () -> new NTriplesSerializer(null), "Model cannot be null");
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null configuration")
    void constructorShouldThrowForNullConfig() {
<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
        Assertions.assertThrows(NullPointerException.class, () -> new fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesFormat(model, null), "Configuration cannot be null");
========
        assertThrows(NullPointerException.class, () -> new NTriplesSerializer(model, null), "Configuration cannot be null");
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java
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
<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
    @DisplayName("Write should handle blank nodes with custom prefix")
    void writeShouldHandleBlankNodesWithCustomPrefix() throws SerializationException {
        fr.inria.corese.core.next.impl.io.serialization.FormatConfig customConfig = new FormatConfig.Builder().blankNodePrefix("genid-").build();
        fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesFormat customSerializer = new fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesFormat(model, customConfig);

        Statement stmt = createStatement(
                mockBNode1,
                mockExKnows,
                mockBNode2
        );
        Mockito.when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        customSerializer.write(writer);

        String expected = String.format("genid-%s <%s> genid-%s",
                mockBNode1.stringValue(),
                mockExKnows.stringValue(),
                mockBNode2.stringValue()) + " .\n";

        Assertions.assertEquals(expected, writer.toString());
    }

    @Test
========
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java
    @DisplayName("Write should throw SerializationException on IO error")
    void writeShouldThrowOnIOException() throws IOException {
        Statement stmt = factory.createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn
        );
        Mockito.when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer faultyWriter = Mockito.mock(Writer.class);

<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
        Mockito.doThrow(new IOException("Simulated IO error")).when(faultyWriter).write(ArgumentMatchers.anyString());

        Assertions.assertThrows(SerializationException.class, () -> nTriplesFormat.write(faultyWriter));
========
        doThrow(new IOException("Simulated IO error during write")).when(faultyWriter).write(anyString());
        doThrow(new IOException("Simulated IO error (char array)")).when(faultyWriter).write(any(char[].class), anyInt(), anyInt());
        doThrow(new IOException("Simulated IO error (close)")).when(faultyWriter).close();

        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(faultyWriter));

        assertEquals("N-Triples serialization failed [Format: N-Triples]", thrown.getMessage());
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java
    }


    @Test
    @DisplayName("Write should throw SerializationException for null subject value in strict mode")
    void writeShouldThrowOnNullSubjectValue() {
<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
        Statement stmt = Mockito.mock(Statement.class);
        Mockito.when(stmt.getSubject()).thenReturn(null);
        Mockito.when(stmt.getPredicate()).thenReturn(mockExName);
        Mockito.when(stmt.getObject()).thenReturn(mockLiteralJohn);
        Mockito.when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        Assertions.assertThrows(SerializationException.class, () -> nTriplesFormat.write(writer));
========
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(null);
        when(stmt.getPredicate()).thenReturn(mockExName);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(stmt.getContext()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(writer));

        assertEquals("Invalid N-Triples data: Value cannot be null in N-Triples format when strictMode is enabled. [Format: N-Triples]", thrown.getMessage());
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java
    }

    @Test
    @DisplayName("Write should throw SerializationException for null predicate value in strict mode")
    void writeShouldThrowOnNullPredicateValue() {
<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
        Statement stmt = Mockito.mock(Statement.class);
        Mockito.when(stmt.getSubject()).thenReturn(mockExPerson);
        Mockito.when(stmt.getPredicate()).thenReturn(null);
        Mockito.when(stmt.getObject()).thenReturn(mockLiteralJohn);
        Mockito.when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        Assertions.assertThrows(SerializationException.class, () -> nTriplesFormat.write(writer));
========
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(mockExPerson);
        when(stmt.getPredicate()).thenReturn(null);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(stmt.getContext()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(writer));
        assertEquals("Invalid N-Triples data: Value cannot be null in N-Triples format when strictMode is enabled. [Format: N-Triples]", thrown.getMessage());
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java
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
<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
        Assertions.assertThrows(SerializationException.class, () -> nTriplesFormat.write(writer));
========
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
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java
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

<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
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
        Mockito.when(model.iterator()).thenReturn(new MockStatementIterator(stmt1, stmt2));

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

        Assertions.assertEquals(expectedOutput, writer.toString());
    }
========
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java

    @Test
    @DisplayName("Should handle literals with language tags")
    void shouldHandleLiteralsWithLanguageTags() throws SerializationException {
        Statement stmt = factory.createStatement(mockExPerson, factory.createIRI("http://example.org/greeting"), mockLiteralHelloEn);

        Model currentTestModel = Mockito.mock(Model.class);
        Mockito.when(currentTestModel.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer writer = new StringWriter();
<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java
        fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesFormat serializer = new NTriplesFormat(currentTestModel);
========

        NTriplesSerializer serializer = new NTriplesSerializer(currentTestModel, NTriplesConfig.defaultConfig());
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java
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

        NTriplesSerializer serializer = new NTriplesSerializer(currentTestModel, NTriplesConfig.defaultConfig());

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
<<<<<<<< HEAD:src/test/java/fr/inria/corese/core/next/impl/io/serialization/ntriples/NTriplesFormatTest.java


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
        Literal literal = Mockito.mock(Literal.class);
        Mockito.when(literal.isLiteral()).thenReturn(true);
        Mockito.when(literal.isResource()).thenReturn(false);
        Mockito.when(literal.stringValue()).thenReturn(lexicalForm);

        if (langTag != null && !langTag.isEmpty()) {
            Mockito.when(literal.getLanguage()).thenReturn(Optional.of(langTag));


            Mockito.when(literal.getDatatype()).thenReturn(RDF.langString.getIRI());
        } else {
            Mockito.when(literal.getLanguage()).thenReturn(Optional.empty());
            Mockito.when(literal.getDatatype()).thenReturn(dataTypeIRI);
        }
        return literal;
    }

    private Statement createStatement(Resource subject, IRI predicate, Value object) {
        return createStatement(subject, predicate, object, null);
    }

    private Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
        Statement stmt = Mockito.mock(Statement.class);
        Mockito.when(stmt.getSubject()).thenReturn(subject);
        Mockito.when(stmt.getPredicate()).thenReturn(predicate);
        Mockito.when(stmt.getObject()).thenReturn(object);
        Mockito.when(stmt.getContext()).thenReturn(context);
        return stmt;
    }

    private Resource createBlankNode(String id) {
        Resource blankNode = Mockito.mock(Resource.class);
        Mockito.when(blankNode.isResource()).thenReturn(true);
        Mockito.when(blankNode.isBNode()).thenReturn(true);
        Mockito.when(blankNode.isIRI()).thenReturn(false);
        Mockito.when(blankNode.stringValue()).thenReturn(id);
        return blankNode;
    }

    private IRI createIRI(String uri) {
        IRI iri = Mockito.mock(IRI.class);
        Mockito.when(iri.isResource()).thenReturn(true);
        Mockito.when(iri.isIRI()).thenReturn(true);
        Mockito.when(iri.isBNode()).thenReturn(false);
        Mockito.when(iri.stringValue()).thenReturn(uri);
        return iri;
    }
}
========
}
>>>>>>>> feature/corese-next:src/test/java/fr/inria/corese/core/next/impl/common/serialization/NTriplesSerializerTest.java
