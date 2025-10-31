package fr.inria.corese.core.next.impl.io.serialization.canonical;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.serialization.RDFSerializer;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.impl.io.serialization.DefaultSerializerFactory;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the CanonicalSerializer class.
 * These tests verify that the serializer correctly delegates to an RDFC-1.0 canonicalization
 * component and formats the resulting canonical statements.
 */
class RDFC10SerializerTest {

    @Mock
    private Model mockModel;
    @Mock
    private RDFC10Canonicalizer mockCanonicalizer;
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


    private RDFC10Serializer serializer;
    private RDFC10Options defaultConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        defaultConfig = RDFC10Options.defaultConfig();

        setupBasicMocks();

        serializer = new RDFC10Serializer(mockModel, defaultConfig, mockCanonicalizer) {
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
                new RDFC10Serializer(null, defaultConfig, mockCanonicalizer));
    }


    @Test
    @DisplayName("Constructor with null config should throw NullPointerException")
    void testConstructorNullConfig() {
        assertThrows(NullPointerException.class, () ->
                new RDFC10Serializer(mockModel, null, mockCanonicalizer));
    }

    @Test
    @DisplayName("Constructor with null canonicalizer should throw NullPointerException")
    void testConstructorNullCanonicalizer() {
        assertThrows(NullPointerException.class, () ->
                new RDFC10Serializer(mockModel, defaultConfig, null));
    }

    @Test
    @DisplayName("Constructor with default configuration")
    void testConstructorWithDefaultConfig() {
        RDFC10Serializer defaultSerializer = new RDFC10Serializer(mockModel, defaultConfig, mockCanonicalizer);
        assertNotNull(defaultSerializer);
        assertEquals("RDFC-1.0", defaultSerializer.getFormatName());
    }

    @Test
    @DisplayName("Serialization of an empty model")
    void testSerializeEmptyModel() throws SerializationException {
        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(Collections.emptyList());

        StringWriter writer = new StringWriter();

        serializer.write(writer);

        assertEquals("", writer.toString());
    }

    @Test
    @DisplayName("Serialization with a simple statement without blank nodes")
    void testSerializeSimpleStatement() throws SerializationException {
        Statement simpleStmt = createMockStatement(mockIRI1, mockIRI2, mockLiteral1, null);

        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(Collections.singletonList(simpleStmt));

        StringWriter writer = new StringWriter();

        serializer.write(writer);

        String expectedOutput = "<http://example.org/iri1> <http://example.org/iri2> \"literal1\" .\n";
        assertEquals(expectedOutput, writer.toString());

        verify(mockCanonicalizer).canonicalize(any(Model.class));
    }


    @Test
    @DisplayName("Serialization with context (named graph)")
    void testSerializeWithContext() throws SerializationException {
        Statement stmtWithContext = createMockStatement(mockIRI1, mockIRI2, mockLiteral1, mockIRI1);

        when(mockCanonicalizer.canonicalize(any(Model.class))).thenReturn(Collections.singletonList(stmtWithContext));

        StringWriter writer = new StringWriter();
        serializer.write(writer);

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
    @DisplayName("Test serialization with figure3.ttl")
    void testSerializeFigure3() {
        String canonicalOutput = serializeToRdfCanonical("/canonical/figure3.ttl");

        assertNotNull(canonicalOutput, "Canonical output should not be null");
        assertFalse(canonicalOutput.isEmpty(), "Canonical output should not be empty");
        String actual = canonicalOutput.trim().replace("\r\n", "\n");
        String expected = "<http://example.com/p> <http://example.com/q> _:c14n2 .\n" +
                "<http://example.com/p> <http://example.com/q> _:c14n3 .\n" +
                "_:c14n0 <http://example.com/r> _:c14n1 .\n" +
                "_:c14n2 <http://example.com/p> _:c14n1 .\n" +
                "_:c14n3 <http://example.com/p> _:c14n0 .";

        assertEquals(expected, actual, "Canonical output should match expected format");

    }


    @Test
    @DisplayName("Test serialization with figure2.ttl")
    void testSerializeFigure2() {
        String canonicalOutput = serializeToRdfCanonical("/canonical/figure2.ttl");

        assertNotNull(canonicalOutput, "Canonical output should not be null");
        assertFalse(canonicalOutput.isEmpty(), "Canonical output should not be empty");

        String actual = canonicalOutput.trim().replace("\r\n", "\n");

        String expected = "<http://example.com/p> <http://example.com/q> _:c14n0 .\n" +
                "<http://example.com/p> <http://example.com/r> _:c14n1 .\n" +
                "_:c14n0 <http://example.com/s> <http://example.com/u> .\n" +
                "_:c14n1 <http://example.com/t> <http://example.com/u> .";


        assertEquals(expected, actual, "Canonical output should match RDFC-1.0 specification");
    }

    private String serializeToRdfCanonical(String resourcePath) {
        Model model = new CoreseModel();
        ValueFactory valueFactory = new CoreseAdaptedValueFactory();

        ParserFactory parserFactory = new ParserFactory();
        RDFParser parser = parserFactory.createRDFParser(RDFFormat.TURTLE, model, valueFactory);

        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                fail("Resource not found: " + resourcePath);
            }
            parser.parse(inputStream);
        } catch (IOException e) {
            fail("Failed to parse resource: " + resourcePath + " - " + e.getMessage());
        }

        DefaultSerializerFactory serializerFactory = new DefaultSerializerFactory();
        RDFSerializer serializer = serializerFactory.createSerializer(
                RDFFormat.RDFC_1_0,
                model,
                RDFC10Options.defaultConfig()
        );

        StringWriter writer = new StringWriter();
        serializer.write(writer);
        return writer.toString();
    }
}
