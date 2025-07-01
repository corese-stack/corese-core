package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.serialization.config.SerializerConfig;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NTriplesSerializerTest {

    private Model model;
    private SerializerConfig config;
    private NTriplesSerializer nTriplesSerializer;

    private Resource mockExPerson;
    private IRI mockExName;
    private IRI mockExKnows;

    private final String lexJohn = "John Doe";

    private Literal mockLiteralJohn;
    private Resource mockBNode1;
    private Resource mockBNode2;

    @BeforeEach
    void setUp() {
        model = mock(Model.class);

        config = SerializerConfig.ntriplesConfig();
        nTriplesSerializer = new NTriplesSerializer(model, config);


        mockExPerson = createIRI("http://example.org/Person");
        mockExName = createIRI("http://example.org/name");
        mockExKnows = createIRI("http://example.org/knows");


        mockBNode1 = createBlankNode("b1");
        mockBNode2 = createBlankNode("b2");

        mockLiteralJohn = createLiteral(lexJohn, null, null);
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null model")
    void constructorShouldThrowForNullModel() {
        assertThrows(NullPointerException.class, () -> new NTriplesSerializer(null), "Model cannot be null");
        assertThrows(NullPointerException.class, () -> new NTriplesSerializer(null, config), "Model cannot be null");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null config")
    void constructorShouldThrowForNullConfig() {
        assertThrows(NullPointerException.class, () -> new NTriplesSerializer(model, null), "Configuration cannot be null");
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
        nTriplesSerializer.write(writer);


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
        nTriplesSerializer.write(writer);

        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNTriplesString(lexJohn)) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Write should handle blank nodes with default N-Triples prefix (_:)")
    void writeShouldHandleBlankNodes() throws SerializationException {
        Statement stmt = createStatement(
                mockBNode1,
                mockExKnows,
                mockBNode2
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesSerializer.write(writer);

        String expected = String.format("_:%s <%s> _:%s",
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

        doThrow(new IOException("Simulated IO error during write")).when(faultyWriter).write(anyString());
        doThrow(new IOException("Simulated IO error (char array)")).when(faultyWriter).write(any(char[].class), anyInt(), anyInt());
        doThrow(new IOException("Simulated IO error (close)")).when(faultyWriter).close();

        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(faultyWriter));

        assertEquals("N-Triples serialization failed [Format: N-Triples]", thrown.getMessage());
    }


    @Test
    @DisplayName("Write should throw SerializationException on null subject value from Statement in strict mode")
    void writeShouldThrowOnNullSubjectValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(null);
        when(stmt.getPredicate()).thenReturn(mockExName);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(writer));

        assertEquals("Invalid N-Triples data: Value cannot be null in N-Triples format when strictMode is enabled. [Format: N-Triples]", thrown.getMessage());
    }

    @Test
    @DisplayName("Write should throw SerializationException on null predicate value from Statement in strict mode")
    void writeShouldThrowOnNullPredicateValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(mockExPerson);
        when(stmt.getPredicate()).thenReturn(null);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(writer));
        assertEquals("Invalid N-Triples data: Value cannot be null in N-Triples format when strictMode is enabled. [Format: N-Triples]", thrown.getMessage());
    }

    @Test
    @DisplayName("Write should throw SerializationException on null object value from Statement in strict mode")
    void writeShouldThrowOnNullObjectValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(mockExPerson);
        when(stmt.getPredicate()).thenReturn(mockExName);
        when(stmt.getObject()).thenReturn(null); // L'objet est null
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(writer));
        assertEquals("Invalid N-Triples data: Value cannot be null in N-Triples format when strictMode is enabled. [Format: N-Triples]", thrown.getMessage());
    }


    @Test
    @DisplayName("Should handle literals with language tags correctly")
    void shouldHandleLiteralsWithLanguageTags() throws SerializationException {
        IRI subject = createIRI("http://example.org/person");
        IRI predicate = createIRI("http://example.org/greeting");
        Literal object = createLiteral("Hello", "en"); // Use the helper that sets rdf:langString

        Statement stmt = createStatement(subject, predicate, object);

        Model modelMock = mock(Model.class);
        when(modelMock.iterator()).thenReturn(new MockStatementIterator(stmt));

        SerializerConfig testConfig = new SerializerConfig.Builder()
                .strictMode(true)  // Keep strict mode enabled to test validation
                .build();

        NTriplesSerializer serializer = new NTriplesSerializer(modelMock, testConfig);

        StringWriter writer = new StringWriter();
        serializer.write(writer);

        String expectedOutput = String.format("<%s> <%s> \"%s\"@%s",
                subject.stringValue(),
                predicate.stringValue(),
                escapeNTriplesString("Hello"),
                "en") + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }


    @Test
    @DisplayName("Should handle literals with custom datatypes")
    void shouldHandleLiteralsWithCustomDatatypes() throws SerializationException {
        IRI customDatatype = createIRI("http://example.org/myDataType");
        Literal customLiteral = createLiteral("123", customDatatype, null);

        Statement stmt = createStatement(mockExPerson, createIRI("http://example.org/value"), customLiteral);

        Model currentTestModel = mock(Model.class);
        when(currentTestModel.iterator()).thenReturn(new MockStatementIterator(stmt));

        NTriplesSerializer serializer = new NTriplesSerializer(currentTestModel, SerializerConfig.ntriplesConfig());

        StringWriter writer = new StringWriter();
        serializer.write(writer);

        String expectedOutput = String.format("<%s> <%s> \"%s\"^^<%s>",
                mockExPerson.stringValue(),
                createIRI("http://example.org/value").stringValue(),
                escapeNTriplesString("123"),
                customDatatype.stringValue()) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }


    /**
     * Escapes a string according to N-Triples literal escaping rules.
     * This helper is used in tests to construct the *expected* output strings.
     * It mimics the behavior of nTriplesSerializer's internal escapeLiteral method,
     * specifically when `escapeUnicode` is true (as per ntriplesConfig() default).
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


    /**
     * Creates a mocked Literal object.
     * Important: The `lexicalForm` is the *raw string value* of the literal,
     * without N-Triples specific quotes, lang tags, or datatype URIs.
     * The `nTriplesSerializer` class is responsible for adding those.
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
            // When a language tag is present, the datatype should be rdf:langString implicitly or explicitly
            when(literal.getDatatype()).thenReturn(RDF.langString.getIRI());
        } else {
            when(literal.getLanguage()).thenReturn(Optional.empty());
            when(literal.getDatatype()).thenReturn(dataTypeIRI); // Can be XSD.STRING.getIRI() or any custom IRI
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

    public static Literal createLiteral(String value, String lang) {
        Literal mockLiteral = mock(Literal.class);
        when(mockLiteral.stringValue()).thenReturn(value); // Removed quotes here
        when(mockLiteral.getLanguage()).thenReturn(Optional.of(lang));
        when(mockLiteral.isLiteral()).thenReturn(true);
        when(mockLiteral.isResource()).thenReturn(false);
        when(mockLiteral.getDatatype()).thenReturn(RDF.langString.getIRI());
        return mockLiteral;
    }
}