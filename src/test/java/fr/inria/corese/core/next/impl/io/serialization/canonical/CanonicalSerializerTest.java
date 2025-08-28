package fr.inria.corese.core.next.impl.io.serialization.canonical;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.serialization.option.CanonicalOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the CanonicalSerializer class.
 * These tests verify that the serializer correctly delegates to an RDFC-1.0 canonicalization
 * component and formats the resulting canonical statements.
 */
class CanonicalSerializerTest {

    @Mock
    private Model mockModel;
    @Mock
    private ValueFactory mockValueFactory;
    @Mock
    private Rdfc10Canonicalizer mockCanonicalizer;
    @Mock
    private BNode mockBNodeE0;
    @Mock
    private BNode mockBNodeE1;
    @Mock
    private BNode mockBNodeE2;
    @Mock
    private BNode mockBNodeE3;


    @Mock
    private BNode canonicalBNodeC0;
    @Mock
    private BNode canonicalBNodeC1;
    @Mock
    private BNode canonicalBNodeC2;
    @Mock
    private BNode canonicalBNodeC3;

    @Mock
    private BNode actualBNodeB0;
    @Mock
    private BNode actualBNodeB1;
    @Mock
    private BNode actualBNodeB2;
    @Mock
    private BNode actualBNodeB3;


    @Mock
    private IRI mockIRIP;
    @Mock
    private IRI mockIRIQ;
    @Mock
    private IRI mockIRIR;

    @Mock
    private IRI mockIRI1;
    @Mock
    private IRI mockIRI2;
    @Mock
    private Literal mockLiteral1;
    @Mock
    private Literal mockLiteral2;


    private CanonicalSerializer serializer;
    private CanonicalOption defaultConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        defaultConfig = CanonicalOption.defaultConfig();

        setupBasicMocks();

        serializer = new CanonicalSerializer(mockModel, defaultConfig, mockValueFactory, mockCanonicalizer) {
            @Override
            protected void writeValue(Writer w, Value v) throws IOException {
                if (v != null) {
                    w.write(v.stringValue());
                }
            }
        };
    }

    /**
     * Configures the basic stringValue() and isBNode() behavior for all mock RDF elements.
     * This ensures consistency across tests.
     */
    private void setupBasicMocks() {

        when(mockIRI1.stringValue()).thenReturn("<http://example.org/iri1>");
        when(mockIRI2.stringValue()).thenReturn("<http://example.org/iri2>");
        when(mockIRI1.isBNode()).thenReturn(false);
        when(mockIRI2.isBNode()).thenReturn(false);

        when(mockIRIP.stringValue()).thenReturn("<http://example.com/#p>");
        when(mockIRIQ.stringValue()).thenReturn("<http://example.com/#q>");
        when(mockIRIR.stringValue()).thenReturn("<http://example.com/#r>");
        when(mockIRIP.isBNode()).thenReturn(false);
        when(mockIRIQ.isBNode()).thenReturn(false);
        when(mockIRIR.isBNode()).thenReturn(false);


        when(mockLiteral1.stringValue()).thenReturn("\"literal1\"");
        when(mockLiteral2.stringValue()).thenReturn("\"literal2\"");
        when(mockLiteral1.isBNode()).thenReturn(false);
        when(mockLiteral2.isBNode()).thenReturn(false);

        when(mockBNodeE0.stringValue()).thenReturn("_:e0");
        when(mockBNodeE1.stringValue()).thenReturn("_:e1");
        when(mockBNodeE2.stringValue()).thenReturn("_:e2");
        when(mockBNodeE3.stringValue()).thenReturn("_:e3");
        when(mockBNodeE0.isBNode()).thenReturn(true);
        when(mockBNodeE1.isBNode()).thenReturn(true);
        when(mockBNodeE2.isBNode()).thenReturn(true);
        when(mockBNodeE3.isBNode()).thenReturn(true);


        when(canonicalBNodeC0.stringValue()).thenReturn("_:c14n0");
        when(canonicalBNodeC1.stringValue()).thenReturn("_:c14n1");
        when(canonicalBNodeC2.stringValue()).thenReturn("_:c14n2");
        when(canonicalBNodeC3.stringValue()).thenReturn("_:c14n3");
        when(canonicalBNodeC0.isBNode()).thenReturn(true);
        when(canonicalBNodeC1.isBNode()).thenReturn(true);
        when(canonicalBNodeC2.isBNode()).thenReturn(true);
        when(canonicalBNodeC3.isBNode()).thenReturn(true);

        when(actualBNodeB0.stringValue()).thenReturn("_:b0");
        when(actualBNodeB1.stringValue()).thenReturn("_:b1");
        when(actualBNodeB2.stringValue()).thenReturn("_:b2");
        when(actualBNodeB3.stringValue()).thenReturn("_:b3");
        when(actualBNodeB0.isBNode()).thenReturn(true);
        when(actualBNodeB1.isBNode()).thenReturn(true);
        when(actualBNodeB2.isBNode()).thenReturn(true);
        when(actualBNodeB3.isBNode()).thenReturn(true);
    }

    /**
     * Helper method to create a mock Statement with configured subject, predicate, object, and context.
     * It also sets up the toString() behavior which is used by the serializer for blank node fingerprinting.
     *
     * @param subject   The subject of the statement.
     * @param predicate The predicate of the statement.
     * @param object    The object of the statement.
     * @param context   The context (named graph) of the statement, can be null.
     * @return A mocked Statement.
     */
    private Statement createMockStatement(Resource subject, IRI predicate, Value object, Resource context) {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(subject);
        when(stmt.getPredicate()).thenReturn(predicate);
        when(stmt.getObject()).thenReturn(object);
        when(stmt.getContext()).thenReturn(context);

        String contextPart = (context != null) ? " " + context.stringValue() : "";
        String expectedToString = subject.stringValue() + " " + predicate.stringValue() + " " + object.stringValue() + contextPart;

        doReturn(expectedToString).when(stmt).toString();

        return stmt;
    }

    @Test
    @DisplayName("Constructor with valid parameters should create an instance")
    void testConstructorWithValidParameters() {
        assertNotNull(serializer);
        assertEquals("RDFC-1.0", serializer.getFormatName());
    }

    @Test
    @DisplayName("Constructor with null model should throw NullPointerException")
    void testConstructorNullModel() {
        assertThrows(NullPointerException.class, () ->
                new CanonicalSerializer(null, defaultConfig, mockValueFactory, mockCanonicalizer));
    }

    @Test
    @DisplayName("Constructor with null valueFactory should throw NullPointerException")
    void testConstructorNullValueFactory() {
        assertThrows(NullPointerException.class, () ->
                new CanonicalSerializer(mockModel, defaultConfig, null, mockCanonicalizer));
    }

    @Test
    @DisplayName("Constructor with null config should throw NullPointerException")
    void testConstructorNullConfig() {
        assertThrows(NullPointerException.class, () ->
                new CanonicalSerializer(mockModel, null, mockValueFactory, mockCanonicalizer));
    }

    @Test
    @DisplayName("Constructor with null canonicalizer should throw NullPointerException")
    void testConstructorNullCanonicalizer() {
        assertThrows(NullPointerException.class, () ->
                new CanonicalSerializer(mockModel, defaultConfig, mockValueFactory, null));
    }

    @Test
    @DisplayName("Constructor with default configuration")
    void testConstructorWithDefaultConfig() {
        CanonicalSerializer defaultSerializer = new CanonicalSerializer(mockModel, defaultConfig, mockValueFactory, mockCanonicalizer);
        assertNotNull(defaultSerializer);
        assertEquals("RDFC-1.0", defaultSerializer.getFormatName());
    }

    @Test
    @DisplayName("Serialization of an empty model")
    void testSerializeEmptyModel() throws SerializationException {
        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(Collections.emptyList());

        StringWriter writer = new StringWriter();

        serializer.serialize(writer);

        assertEquals("", writer.toString());
    }

    @Test
    @DisplayName("Serialization with a simple statement without blank nodes")
    void testSerializeSimpleStatement() throws SerializationException {
        Statement simpleStmt = createMockStatement(mockIRI1, mockIRI2, mockLiteral1, null);

        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(Collections.singletonList(simpleStmt));

        StringWriter writer = new StringWriter();

        serializer.serialize(writer);

        String expectedOutput = "<http://example.org/iri1> <http://example.org/iri2> \"literal1\" .\n";
        assertEquals(expectedOutput, writer.toString());

        verify(mockCanonicalizer).canonicalize(any(Model.class));
    }

    @Test
    @DisplayName("Serialization with blank nodes - W3C canonicalization and output sorting")
    void testSerializeWithBlankNodesAndOutputVerification() throws SerializationException {

        Statement inputStmt1 = createMockStatement(mockIRIP, mockIRIQ, mockBNodeE0, null);
        Statement inputStmt2 = createMockStatement(mockIRIP, mockIRIQ, mockBNodeE1, null);
        Statement inputStmt3 = createMockStatement(mockBNodeE0, mockIRIP, mockBNodeE2, null);
        Statement inputStmt4 = createMockStatement(mockBNodeE1, mockIRIP, mockBNodeE3, null);
        Statement inputStmt5 = createMockStatement(mockBNodeE2, mockIRIR, mockBNodeE3, null);

        List<Statement> originalStatementsFromModel = Arrays.asList(inputStmt1, inputStmt2, inputStmt3, inputStmt4, inputStmt5);
        Collections.shuffle(originalStatementsFromModel, new Random(0));

        Statement canonicalOutputStmt1 = createMockStatement(mockIRIP, mockIRIQ, canonicalBNodeC2, null);
        Statement canonicalOutputStmt2 = createMockStatement(mockIRIP, mockIRIQ, canonicalBNodeC3, null);
        Statement canonicalOutputStmt3 = createMockStatement(canonicalBNodeC0, mockIRIR, canonicalBNodeC1, null);
        Statement canonicalOutputStmt4 = createMockStatement(canonicalBNodeC2, mockIRIP, canonicalBNodeC1, null);
        Statement canonicalOutputStmt5 = createMockStatement(canonicalBNodeC3, mockIRIP, canonicalBNodeC0, null);

        List<Statement> expectedCanonicalStatementsSorted = Arrays.asList(
                canonicalOutputStmt1,
                canonicalOutputStmt2,
                canonicalOutputStmt3,
                canonicalOutputStmt4,
                canonicalOutputStmt5
        );

        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(expectedCanonicalStatementsSorted);

        StringWriter writer = new StringWriter();

        serializer.serialize(writer);

        String expectedOutput = """
                <http://example.com/#p> <http://example.com/#q> _:c14n2 .
                <http://example.com/#p> <http://example.com/#q> _:c14n3 .
                _:c14n0 <http://example.com/#r> _:c14n1 .
                _:c14n2 <http://example.com/#p> _:c14n1 .
                _:c14n3 <http://example.com/#p> _:c14n0 .
                """;
        assertEquals(expectedOutput, writer.toString());

        verify(mockCanonicalizer).canonicalize(any(Model.class));
    }

    @Test
    @DisplayName("Serialization with context (named graph)")
    void testSerializeWithContext() throws SerializationException {
        Statement stmtWithContext = createMockStatement(mockIRI1, mockIRI2, mockLiteral1, mockIRI1);

        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(Collections.singletonList(stmtWithContext));

        StringWriter writer = new StringWriter();
        serializer.serialize(writer);

        String expectedOutput = "<http://example.org/iri1> <http://example.org/iri2> \"literal1\" <http://example.org/iri1> .\n";
        assertEquals(expectedOutput, writer.toString());

        verify(mockCanonicalizer).canonicalize(any(Model.class));
    }

    @Test
    @DisplayName("Test writeContext with null context")
    void testWriteContextWithNullContext() throws IOException {
        StringWriter writer = new StringWriter();
        Statement stmt = mock(Statement.class);
        when(stmt.getContext()).thenReturn(null);

        serializer.writeContext(writer, stmt);

        assertEquals("", writer.toString());
    }

    @Test
    @DisplayName("Test writeContext with non-null context")
    void testWriteContextWithNonNullContext() throws IOException {
        StringWriter writer = new StringWriter();
        Statement stmt = mock(Statement.class);
        when(stmt.getContext()).thenReturn(mockIRI1);

        serializer.writeContext(writer, stmt);

        String expectedOutput = " <http://example.org/iri1>";
        assertEquals(expectedOutput, writer.toString());
    }


    @Test
    @DisplayName("Serialization with blank nodes in context - canonicalization and sorting")
    void testSerializeWithBlankNodeInContextAndOutputVerification() throws SerializationException {

        Statement canonicalOutputStmt1 = createMockStatement(mockIRI1, mockIRI2, mockLiteral1, canonicalBNodeC0);

        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(Collections.singletonList(canonicalOutputStmt1));

        StringWriter writer = new StringWriter();

        serializer.serialize(writer);

        String expectedOutput = "<http://example.org/iri1> <http://example.org/iri2> \"literal1\" _:c14n0 .\n";
        assertEquals(expectedOutput, writer.toString());

        verify(mockCanonicalizer).canonicalize(any(Model.class));
    }

    @Test
    void testSerializeW3CExampleWithDifferentActualOutput() throws SerializationException {
        Statement inputStmt1 = createMockStatement(mockIRIP, mockIRIQ, mockBNodeE0, null);
        Statement inputStmt2 = createMockStatement(mockIRIP, mockIRIQ, mockBNodeE1, null);
        Statement inputStmt3 = createMockStatement(mockBNodeE0, mockIRIP, mockBNodeE2, null);
        Statement inputStmt4 = createMockStatement(mockBNodeE1, mockIRIP, mockBNodeE3, null);
        Statement inputStmt5 = createMockStatement(mockBNodeE2, mockIRIR, mockBNodeE3, null);

        List<Statement> originalStatementsFromModel = Arrays.asList(inputStmt1, inputStmt2, inputStmt3, inputStmt4, inputStmt5);
        Collections.shuffle(originalStatementsFromModel, new Random(0));

        Statement actualOutputStmt1 = createMockStatement(actualBNodeB0, mockIRIR, actualBNodeB2, null);
        Statement actualOutputStmt2 = createMockStatement(actualBNodeB1, mockIRIP, actualBNodeB0, null);
        Statement actualOutputStmt3 = createMockStatement(actualBNodeB3, mockIRIP, actualBNodeB2, null);
        Statement actualOutputStmt4 = createMockStatement(mockIRIP, mockIRIQ, actualBNodeB1, null);
        Statement actualOutputStmt5 = createMockStatement(mockIRIP, mockIRIQ, actualBNodeB3, null);


        List<Statement> actualCanonicalStatementsSorted = Arrays.asList(
                actualOutputStmt1,
                actualOutputStmt2,
                actualOutputStmt3,
                actualOutputStmt4,
                actualOutputStmt5
        );

        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(actualCanonicalStatementsSorted);

        StringWriter writer = new StringWriter();

        serializer.serialize(writer);

        String expectedOutput = """
                _:b0 <http://example.com/#r> _:b2 .
                _:b1 <http://example.com/#p> _:b0 .
                _:b3 <http://example.com/#p> _:b2 .
                <http://example.com/#p> <http://example.com/#q> _:b1 .
                <http://example.com/#p> <http://example.com/#q> _:b3 .
                """;
        assertEquals(expectedOutput, writer.toString());

        verify(mockCanonicalizer).canonicalize(any(Model.class));
    }

    @Test
    @DisplayName("Serialization without trailing dot")
    void testSerializeNoTrailingDot() throws SerializationException {
        CanonicalOption noDotConfig = CanonicalOption.builder().trailingDot(false).build();
        CanonicalSerializer noDotSerializer = new CanonicalSerializer(mockModel, noDotConfig, mockValueFactory, mockCanonicalizer) {
            @Override
            protected void writeValue(Writer w, Value v) throws IOException {
                if (v != null) {
                    w.write(v.stringValue());
                }
            }
        };

        Statement simpleStmt = createMockStatement(mockIRI1, mockIRI2, mockLiteral1, null);

        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(Collections.singletonList(simpleStmt));

        StringWriter writer = new StringWriter();

        noDotSerializer.serialize(writer);

        String expectedOutput = "<http://example.org/iri1> <http://example.org/iri2> \"literal1\"\n";
        assertEquals(expectedOutput, writer.toString());

        verify(mockCanonicalizer).canonicalize(any(Model.class));
    }

    @Test
    @DisplayName("Serialization with different line ending")
    void testSerializeDifferentLineEnding() throws SerializationException {
        CanonicalOption customLineEndingConfig = CanonicalOption.builder().lineEnding("\r\n").build();
        CanonicalSerializer customLineEndingSerializer = new CanonicalSerializer(mockModel, customLineEndingConfig, mockValueFactory, mockCanonicalizer) {
            @Override
            protected void writeValue(Writer w, Value v) throws IOException {
                if (v != null) {
                    w.write(v.stringValue());
                }
            }
        };

        Statement simpleStmt = createMockStatement(mockIRI1, mockIRI2, mockLiteral1, null);
        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(Collections.singletonList(simpleStmt));

        StringWriter writer = new StringWriter();

        customLineEndingSerializer.serialize(writer);

        String expectedOutput = "<http://example.org/iri1> <http://example.org/iri2> \"literal1\" .\r\n";
        assertEquals(expectedOutput, writer.toString());
    }

    @Test
    @DisplayName("Serialization with a mix of statements (with and without context)")
    void testSerializeMixedStatements() throws SerializationException {
        Statement stmt1 = createMockStatement(mockIRI1, mockIRIP, mockLiteral1, null);
        Statement stmt2 = createMockStatement(mockIRI2, mockIRIQ, mockLiteral2, mockIRI1);
        Statement stmt3 = createMockStatement(mockIRI1, mockIRIR, mockLiteral2, null);

        List<Statement> mixedStatements = Arrays.asList(stmt1, stmt2, stmt3);
        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(mixedStatements);

        StringWriter writer = new StringWriter();
        serializer.serialize(writer);

        String expectedOutput = """
                <http://example.org/iri1> <http://example.com/#p> "literal1" .
                <http://example.org/iri2> <http://example.com/#q> "literal2" <http://example.org/iri1> .
                <http://example.org/iri1> <http://example.com/#r> "literal2" .
                """;
        assertEquals(expectedOutput, writer.toString());
    }

}
