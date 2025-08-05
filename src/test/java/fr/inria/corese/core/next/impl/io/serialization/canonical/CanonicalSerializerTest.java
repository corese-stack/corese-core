package fr.inria.corese.core.next.impl.io.serialization.canonical;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.exception.SerializationException;
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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the CanonicalSerializer class.
 * These tests verify the canonicalization of blank nodes, the sorting of statements,
 * and the correct serialization of the RDF model.
 */
class CanonicalSerializerTest {

    @Mock
    private Model mockModel;
    @Mock
    private ValueFactory mockValueFactory;

    @Mock
    private BNode mockBNode1;
    @Mock
    private BNode mockBNode2;

    @Mock
    private BNode canonicalBNode1;
    @Mock
    private BNode canonicalBNode2;

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


        serializer = new CanonicalSerializer(mockModel, defaultConfig, mockValueFactory) {
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

        when(mockLiteral1.stringValue()).thenReturn("\"literal1\"");
        when(mockLiteral2.stringValue()).thenReturn("\"literal2\"");

        when(mockBNode1.stringValue()).thenReturn("_:originalBNode1");
        when(mockBNode2.stringValue()).thenReturn("_:originalBNode2");

        when(canonicalBNode1.stringValue()).thenReturn("_:b0");
        when(canonicalBNode2.stringValue()).thenReturn("_:b1");

        when(mockIRI1.isBNode()).thenReturn(false);
        when(mockIRI2.isBNode()).thenReturn(false);
        when(mockLiteral1.isBNode()).thenReturn(false);
        when(mockLiteral2.isBNode()).thenReturn(false);
        when(mockBNode1.isBNode()).thenReturn(true);
        when(mockBNode2.isBNode()).thenReturn(true);
        when(canonicalBNode1.isBNode()).thenReturn(true);
        when(canonicalBNode2.isBNode()).thenReturn(true);
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
        assertEquals("Canonical RDF", serializer.getFormatName());
    }

    @Test
    @DisplayName("Constructor with null model should throw NullPointerException")
    void testConstructorNullModel() {
        assertThrows(NullPointerException.class, () ->
                new CanonicalSerializer(null, defaultConfig, mockValueFactory));
    }

    @Test
    @DisplayName("Constructor with null valueFactory should throw NullPointerException")
    void testConstructorNullValueFactory() {
        assertThrows(NullPointerException.class, () ->
                new CanonicalSerializer(mockModel, defaultConfig, null));
    }

    @Test
    @DisplayName("Constructor with null config should throw NullPointerException")
    void testConstructorNullConfig() {
        assertThrows(NullPointerException.class, () ->
                new CanonicalSerializer(mockModel, null, mockValueFactory));
    }

    @Test
    @DisplayName("Constructor with default configuration")
    void testConstructorWithDefaultConfig() {
        CanonicalSerializer defaultSerializer = new CanonicalSerializer(mockModel, mockValueFactory);
        assertNotNull(defaultSerializer);
        assertEquals("Canonical RDF", defaultSerializer.getFormatName());
    }

    @Test
    @DisplayName("Serialization of an empty model")
    void testSerializeEmptyModel() throws SerializationException {

        doAnswer(invocation -> {
            return null;
        }).when(mockModel).forEach(any());

        StringWriter writer = new StringWriter();

        serializer.write(writer);

        assertEquals("", writer.toString());
    }

    @Test
    @DisplayName("Serialization with a simple statement without blank nodes")
    void testSerializeSimpleStatement() throws SerializationException {
        Statement simpleStmt = createMockStatement(mockIRI1, mockIRI2, mockLiteral1, null);

        doAnswer(invocation -> {
            ((Consumer<Statement>) invocation.getArgument(0)).accept(simpleStmt);
            return null;
        }).when(mockModel).forEach(any());


        when(mockValueFactory.createStatement(mockIRI1, mockIRI2, mockLiteral1, null))
                .thenReturn(simpleStmt);

        StringWriter writer = new StringWriter();

        serializer.write(writer);

        String expectedOutput = "<http://example.org/iri1> <http://example.org/iri2> \"literal1\" .\n";
        assertEquals(expectedOutput, writer.toString());
    }

    @Test
    @DisplayName("Serialization with blank nodes - canonicalization and output sorting")
    void testSerializeWithBlankNodesAndOutputVerification() throws SerializationException {

        Statement originalStmt1 = createMockStatement(mockBNode1, mockIRI1, mockLiteral1, null);
        Statement originalStmt2 = createMockStatement(mockIRI2, mockIRI1, mockBNode2, null);
        Statement originalStmt3 = createMockStatement(mockBNode1, mockIRI2, mockIRI1, null);

        List<Statement> statementsInModel = Arrays.asList(originalStmt3, originalStmt1, originalStmt2);
        Collections.shuffle(statementsInModel, new Random(0));

        doAnswer(invocation -> {
            ((Consumer<Statement>) invocation.getArgument(0)).accept(statementsInModel.get(0));
            ((Consumer<Statement>) invocation.getArgument(0)).accept(statementsInModel.get(1));
            ((Consumer<Statement>) invocation.getArgument(0)).accept(statementsInModel.get(2));
            return null;
        }).when(mockModel).forEach(any());


        when(mockValueFactory.createBNode("b0")).thenReturn(canonicalBNode1);
        when(mockValueFactory.createBNode("b1")).thenReturn(canonicalBNode2);


        Statement canonicalStmtA = createMockStatement(canonicalBNode1, mockIRI1, mockLiteral1, null);
        Statement canonicalStmtB = createMockStatement(mockIRI2, mockIRI1, canonicalBNode2, null);
        Statement canonicalStmtC = createMockStatement(canonicalBNode1, mockIRI2, mockIRI1, null);


        when(mockValueFactory.createStatement(any(), any(), any(), any()))
                .thenReturn(canonicalStmtB, canonicalStmtA, canonicalStmtC);


        StringWriter writer = new StringWriter();

        serializer.write(writer);

        String expectedOutput = """
                <http://example.org/iri2> <http://example.org/iri1> _:b1 .
                _:b0 <http://example.org/iri1> "literal1" .
                _:b0 <http://example.org/iri2> <http://example.org/iri1> .
                """;
        assertEquals(expectedOutput, writer.toString());

        verify(mockValueFactory).createBNode("b0");
        verify(mockValueFactory).createBNode("b1");
        verify(mockValueFactory, times(3)).createStatement(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Serialization with context (named graph)")
    void testSerializeWithContext() throws SerializationException {
        Statement stmtWithContext = createMockStatement(mockIRI1, mockIRI2, mockLiteral1, mockIRI1);

        doAnswer(invocation -> {
            ((Consumer<Statement>) invocation.getArgument(0)).accept(stmtWithContext);
            return null;
        }).when(mockModel).forEach(any());


        when(mockValueFactory.createStatement(mockIRI1, mockIRI2, mockLiteral1, mockIRI1))
                .thenReturn(stmtWithContext);


        StringWriter writer = new StringWriter();

        serializer.write(writer);

        String expectedOutput = "<http://example.org/iri1> <http://example.org/iri2> \"literal1\" <http://example.org/iri1> .\n";
        assertEquals(expectedOutput, writer.toString());
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

        Statement originalStmt1 = createMockStatement(mockIRI1, mockIRI2, mockLiteral1, mockBNode1);

        List<Statement> statementsInModel = Collections.singletonList(originalStmt1);
        doAnswer(invocation -> {
            ((Consumer<Statement>) invocation.getArgument(0)).accept(statementsInModel.get(0));
            return null;
        }).when(mockModel).forEach(any());

        when(mockValueFactory.createBNode("b0")).thenReturn(canonicalBNode1);

        Statement canonicalStmt1 = createMockStatement(mockIRI1, mockIRI2, mockLiteral1, canonicalBNode1);

        when(mockValueFactory.createStatement(any(), any(), any(), any()))
                .thenReturn(canonicalStmt1);

        StringWriter writer = new StringWriter();


        serializer.write(writer);

        String expectedOutput = "<http://example.org/iri1> <http://example.org/iri2> \"literal1\" _:b0 .\n";
        assertEquals(expectedOutput, writer.toString());

        verify(mockValueFactory).createBNode("b0");
        verify(mockValueFactory, times(1)).createStatement(any(), any(), any(), any());
    }
}
