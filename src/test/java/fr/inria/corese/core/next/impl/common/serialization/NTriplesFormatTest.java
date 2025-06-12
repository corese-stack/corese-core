package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;

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

class NTriplesFormatTest {

    private Model model;
    private FormatConfig config;
    private NTriplesFormat nTriplesFormat;

    private Resource mockExPerson;
    private IRI mockExName;
    private IRI mockExAge;
    private IRI mockExKnows;
    private Literal mockLiteralJohn;
    private Literal mockLiteral30;
    private Literal mockLiteralHelloEn;
    private Literal mockLiteralTrue;
    private Resource mockBNode1;
    private Resource mockBNode2;

    @BeforeEach
    void setUp() {
        model = mock(Model.class);
        config = new FormatConfig.Builder().build();
        nTriplesFormat = new NTriplesFormat(model, config);


        mockExPerson = createIRI("http://example.org/Person");
        mockExName = createIRI("http://example.org/name");
        mockExAge = createIRI("http://example.org/age");
        mockExKnows = createIRI("http://example.org/knows");

        mockLiteralJohn = createLiteral("John Doe");
        mockLiteral30 = createLiteral("30", "http://www.w3.org/2001/XMLSchema#integer", null);
        mockLiteralHelloEn = createLiteral("Hello", null, "en");
        mockLiteralTrue = createLiteral("true", "http://www.w3.org/2001/XMLSchema#boolean", null);

        mockBNode1 = createBlankNode("b1");
        mockBNode2 = createBlankNode("b2");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null model")
    void constructorShouldThrowForNullModel() {
        assertThrows(NullPointerException.class, () -> new NTriplesFormat(null));
        assertThrows(NullPointerException.class, () -> new NTriplesFormat(null, config));
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null config")
    void constructorShouldThrowForNullConfig() {
        assertThrows(NullPointerException.class, () -> new NTriplesFormat(model, null));
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


        assertEquals("<http://example.org/Person> <http://example.org/name> \"John Doe\" .\n", writer.toString());
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


        assertEquals("<http://example.org/Person> <http://example.org/name> \"John Doe\" .\n", writer.toString());
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


        assertEquals("_:b1 <http://example.org/knows> _:b2 .\n", writer.toString());
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

        assertEquals("genid-b1 <http://example.org/knows> genid-b2 .\n", writer.toString());
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
    @DisplayName("Write should throw SerializationException on null subject value")
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
    @DisplayName("Write should throw SerializationException on null predicate value")
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
    @DisplayName("Write should throw SerializationException on null object value")
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
            "literal with \r carriage return"
    })
    @DisplayName("Write should handle various literal values with proper escaping")
    void writeShouldHandleVariousLiterals(String literalValue) throws SerializationException {


        Literal literalMock = mock(Literal.class);
        when(literalMock.isLiteral()).thenReturn(true);
        when(literalMock.isResource()).thenReturn(false);

        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(escapeNTriplesString(literalValue)).append("\"");
        when(literalMock.stringValue()).thenReturn(sb.toString());

        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                literalMock
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();

        nTriplesFormat.write(writer);

        String expectedEscapedLiteral = escapeNTriplesString(literalValue);
        assertEquals(String.format("<http://example.org/Person> <http://example.org/name> \"%s\" .\n", expectedEscapedLiteral), writer.toString());
    }

    @Test
    @DisplayName("Write should handle multiple statements")
    void writeShouldHandleMultipleStatements() throws SerializationException {

        Statement stmt1 = createStatement(
                mockExPerson,
                mockExName,
                createLiteral("o1")
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

        assertEquals("<http://example.org/Person> <http://example.org/name> \"o1\" .\n" +
                "_:b1 <http://example.org/knows> <http://example.org/Person> .\n", writer.toString());
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

        String expectedOutput = "<http://example.org/Person> <http://example.org/greeting> \"Hello\"@en .\n";

        assertEquals(expectedOutput, writer.toString());
    }

    @Test
    @DisplayName("Should handle literals with datatype IRIs")
    void shouldHandleLiteralsWithDatatypeIRIs() throws SerializationException {

        Statement stmt = createStatement(mockExPerson, mockExAge, mockLiteral30);

        Model currentTestModel = mock(Model.class);
        when(currentTestModel.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer writer = new StringWriter();
        NTriplesFormat serializer = new NTriplesFormat(currentTestModel);
        serializer.write(writer);

        String expectedOutput = "<http://example.org/Person> <http://example.org/age> \"30\"^^<http://www.w3.org/2001/XMLSchema#integer> .\n";

        assertEquals(expectedOutput, writer.toString());
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


    private Literal createLiteral(String lexicalForm) {
        return createLiteral(lexicalForm, null, null);
    }


    private Literal createLiteral(String lexicalForm, String dataType, String langTag) {
        Literal literal = mock(Literal.class);
        when(literal.isLiteral()).thenReturn(true);
        when(literal.isResource()).thenReturn(false);


        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(escapeNTriplesString(lexicalForm)).append("\"");
        if (langTag != null && !langTag.isEmpty()) {
            sb.append("@").append(langTag);
        } else if (dataType != null && !dataType.isEmpty()) {
            sb.append("^^<").append(dataType).append(">");
        }
        when(literal.stringValue()).thenReturn(sb.toString());
        return literal;
    }


    private String escapeNTriplesString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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
