package fr.inria.corese.core.next.impl.io.parser.jsonld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.api.BNode;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.api.io.serialization.RDFSerializer;
import fr.inria.corese.core.next.api.io.serialization.SerializerFactory;
import fr.inria.corese.core.next.impl.io.option.JSONLDProcessorOptions;
import fr.inria.corese.core.next.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.impl.io.serialization.DefaultSerializerFactory;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Circular tests for JSON-LD parser and serializer integration.
 * These tests verify that data can be correctly serialized to JSON-LD format
 * and then parsed back to an equivalent model (round-trip testing).
 * 
 * The circular testing approach ensures that the parser and serializer
 * are compatible and preserve data integrity across format transformations.
 * 
 * JSON-LD supports both namespaces and named graphs, and has unique features
 * like @context handling, so additional considerations are included.
 */
@DisplayName("JSON-LD Circular Integration Tests")
class JSONLDCircularTest {

    private static final Logger logger = LoggerFactory.getLogger(JSONLDCircularTest.class);

    private ValueFactory valueFactory;
    private SerializerFactory serializerFactory;
    private ParserFactory parserFactory;
    private JSONLDProcessorOptions defaultConfig;

    // Test data constants
    private static final String EXAMPLE_NS = "http://example.org/";
    private static final String SUBJECT_1 = EXAMPLE_NS + "person1";
    private static final String SUBJECT_2 = EXAMPLE_NS + "person2";
    private static final String PREDICATE_NAME = EXAMPLE_NS + "name";
    private static final String PREDICATE_AGE = EXAMPLE_NS + "age";
    private static final String PREDICATE_KNOWS = EXAMPLE_NS + "knows";
    private static final String GRAPH_1 = EXAMPLE_NS + "graph1";
    private static final String GRAPH_2 = EXAMPLE_NS + "graph2";
    private static final String LITERAL_JOHN = "John Doe";
    private static final String LITERAL_JANE = "Jane Smith";
    private static final String LITERAL_AGE_25 = "25";
    private static final String LITERAL_HELLO_EN = "Hello";
    private static final String LANGUAGE_TAG_EN = "en";
    private static final String XSD_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
    private static final String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";

    @BeforeEach
    void setUp() {
        valueFactory = new CoreseAdaptedValueFactory();
        serializerFactory = new DefaultSerializerFactory();
        parserFactory = new ParserFactory();
        defaultConfig = new JSONLDProcessorOptions.Builder()
                .build();
    }

    /**
     * Creates a simple model with basic triples containing IRIs and string
     * literals.
     * 
     * @return A model with two simple triples
     */
    private Model createSimpleTestModel() {
        Model model = new CoreseModel();

        IRI subject1 = valueFactory.createIRI(SUBJECT_1);
        IRI predicateName = valueFactory.createIRI(PREDICATE_NAME);
        Literal objectJohn = valueFactory.createLiteral(LITERAL_JOHN);

        IRI subject2 = valueFactory.createIRI(SUBJECT_2);
        Literal objectJane = valueFactory.createLiteral(LITERAL_JANE);

        model.add(subject1, predicateName, objectJohn);
        model.add(subject2, predicateName, objectJane);

        return model;
    }

    /**
     * Creates a model with named graphs for testing JSON-LD specific functionality.
     * 
     * @return A model with triples in different named graphs
     */
    private Model createNamedGraphsTestModel() {
        Model model = new CoreseModel();

        IRI subject1 = valueFactory.createIRI(SUBJECT_1);
        IRI subject2 = valueFactory.createIRI(SUBJECT_2);
        IRI predicateName = valueFactory.createIRI(PREDICATE_NAME);
        IRI predicateKnows = valueFactory.createIRI(PREDICATE_KNOWS);
        Literal objectJohn = valueFactory.createLiteral(LITERAL_JOHN);
        Literal objectJane = valueFactory.createLiteral(LITERAL_JANE);

        IRI graph1 = valueFactory.createIRI(GRAPH_1);
        IRI graph2 = valueFactory.createIRI(GRAPH_2);

        // Add triples to different named graphs
        model.add(subject1, predicateName, objectJohn, graph1);
        model.add(subject2, predicateName, objectJane, graph2);
        model.add(subject1, predicateKnows, subject2, graph1);

        return model;
    }

    /**
     * Creates a complex model with various RDF value types including
     * typed literals, language-tagged literals, and blank nodes.
     * 
     * @return A model with diverse triple patterns
     */
    private Model createComplexTestModel() {
        Model model = new CoreseModel();

        // Basic IRI and string literal triple
        IRI subject1 = valueFactory.createIRI(SUBJECT_1);
        IRI predicateName = valueFactory.createIRI(PREDICATE_NAME);
        Literal literalJohn = valueFactory.createLiteral(LITERAL_JOHN);
        model.add(subject1, predicateName, literalJohn);

        // Typed literal (integer)
        IRI predicateAge = valueFactory.createIRI(PREDICATE_AGE);
        IRI xsdInteger = valueFactory.createIRI(XSD_INTEGER);
        Literal literalAge = valueFactory.createLiteral(LITERAL_AGE_25, xsdInteger);
        model.add(subject1, predicateAge, literalAge);

        // Language-tagged literal
        Literal literalHelloEn = valueFactory.createLiteral(LITERAL_HELLO_EN, LANGUAGE_TAG_EN);
        IRI predicateGreeting = valueFactory.createIRI(EXAMPLE_NS + "greeting");
        model.add(subject1, predicateGreeting, literalHelloEn);

        // Blank node as subject
        BNode blankNodeSubject = valueFactory.createBNode();
        IRI predicateType = valueFactory.createIRI(EXAMPLE_NS + "type");
        IRI objectPerson = valueFactory.createIRI(EXAMPLE_NS + "Person");
        model.add(blankNodeSubject, predicateType, objectPerson);

        // Blank node as object
        BNode blankNodeObject = valueFactory.createBNode();
        IRI predicateKnows = valueFactory.createIRI(PREDICATE_KNOWS);
        model.add(subject1, predicateKnows, blankNodeObject);

        // IRI to IRI relationship
        IRI subject2 = valueFactory.createIRI(SUBJECT_2);
        model.add(subject1, predicateKnows, subject2);

        return model;
    }

    /**
     * Creates a model with typed literals for testing.
     * 
     * @return A model with integer and string typed literals
     */
    private Model createTypedLiteralsTestModel() {
        Model model = new CoreseModel();

        IRI subject = valueFactory.createIRI(SUBJECT_1);
        IRI predicateAge = valueFactory.createIRI(PREDICATE_AGE);
        IRI predicateName = valueFactory.createIRI(PREDICATE_NAME);

        // Integer literal
        Literal integerLiteral = valueFactory.createLiteral(LITERAL_AGE_25,
                valueFactory.createIRI(XSD_INTEGER));
        model.add(subject, predicateAge, integerLiteral);

        // String literal with explicit datatype
        Literal stringLiteral = valueFactory.createLiteral(LITERAL_JOHN,
                valueFactory.createIRI(XSD_STRING));
        model.add(subject, predicateName, stringLiteral);

        return model;
    }

    /**
     * Creates a model with language-tagged literals for testing.
     * 
     * @return A model with English and French language-tagged literals
     */
    private Model createLanguageTaggedLiteralsTestModel() {
        Model model = new CoreseModel();

        IRI subject = valueFactory.createIRI(SUBJECT_1);
        IRI predicateGreeting = valueFactory.createIRI(EXAMPLE_NS + "greeting");

        // English greeting
        Literal englishGreeting = valueFactory.createLiteral(LITERAL_HELLO_EN, LANGUAGE_TAG_EN);
        model.add(subject, predicateGreeting, englishGreeting);

        // French greeting
        Literal frenchGreeting = valueFactory.createLiteral("Bonjour", "fr");
        model.add(subject, predicateGreeting, frenchGreeting);

        return model;
    }

    /**
     * Creates a model with blank nodes for testing.
     * 
     * @return A model with blank nodes as subject and object
     */
    private Model createBlankNodesTestModel() {
        Model model = new CoreseModel();

        BNode blankSubject = valueFactory.createBNode();
        BNode blankObject = valueFactory.createBNode();
        IRI predicate = valueFactory.createIRI(PREDICATE_KNOWS);

        model.add(blankSubject, predicate, blankObject);

        return model;
    }

    /**
     * Creates a model with special characters and escape sequences for testing.
     * 
     * @return A model with literals containing newlines, quotes, and Unicode
     */
    private Model createSpecialCharactersTestModel() {
        Model model = new CoreseModel();

        IRI subject = valueFactory.createIRI(SUBJECT_1);
        IRI predicateDescription = valueFactory.createIRI(EXAMPLE_NS + "description");
        IRI predicateNote = valueFactory.createIRI(EXAMPLE_NS + "note");

        // Literal with newlines and quotes
        Literal literalWithEscapes = valueFactory.createLiteral("Line 1\nLine 2\tTabbed \"quoted\" text");
        model.add(subject, predicateDescription, literalWithEscapes);

        // Literal with Unicode characters
        Literal literalUnicode = valueFactory.createLiteral("Hello 世界 🌍");
        model.add(subject, predicateNote, literalUnicode);

        return model;
    }

    /**
     * Performs a round-trip serialization and parsing cycle.
     * 
     * @param originalModel The model to serialize and parse back
     * @return The model resulting from parsing the serialized data
     * @throws Exception If serialization or parsing fails
     */
    private Model performRoundTrip(Model originalModel) throws Exception {
        // Serialize to JSON-LD
        RDFSerializer serializer = serializerFactory.createSerializer(
                RDFFormat.JSONLD, originalModel, defaultConfig);

        StringWriter writer = new StringWriter();
        serializer.write(writer);
        String serializedContent = writer.toString();

        // Verify serialization produced content (only check for non-empty models)
        assertNotNull(serializedContent, "Serialized content should not be null");
        if (originalModel.size() > 0) {
            assertTrue(serializedContent.length() > 0, "Serialized content should not be empty for non-empty models");
        }

        // Parse back from JSON-LD
        Model deserializedModel = new CoreseModel();
        RDFParser parser = parserFactory.createRDFParser(
                RDFFormat.JSONLD, deserializedModel, valueFactory);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                serializedContent.getBytes(StandardCharsets.UTF_8));
        parser.parse(inputStream);

        return deserializedModel;
    }

    @Test
    @DisplayName("Round-trip test with simple model containing basic IRIs and literals")
    void testRoundTripWithSimpleModel() throws Exception {
        // Given: A simple model with basic triples
        Model originalModel = createSimpleTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);

        // Then: The deserialized model should be equivalent to the original
        assertEquals(originalModel.size(), deserializedModel.size(),
                "Model sizes should be equal after round-trip");
        assertEquals(originalModel, deserializedModel,
                "Original and deserialized models should be equivalent");
    }

    @Test
    @DisplayName("Round-trip test with model containing named graphs")
    void testRoundTripWithNamedGraphs() throws Exception {
        // Given: A model with triples in different named graphs
        Model originalModel = createNamedGraphsTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);

        // Then: All named graph information should be preserved
        assertEquals(originalModel.size(), deserializedModel.size(),
                "Model sizes should be equal after round-trip");
        assertEquals(originalModel, deserializedModel,
                "Original and deserialized models should be equivalent, preserving named graphs");
    }

    @Test
    @DisplayName("Round-trip test with complex model containing diverse RDF value types")
    void testRoundTripWithComplexModel() throws Exception {
        // Given: A complex model with various RDF constructs
        Model originalModel = createComplexTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);

        // Then: The deserialized model should preserve all data
        assertEquals(originalModel.size(), deserializedModel.size(),
                "Model sizes should be equal after round-trip");
        assertEquals(originalModel, deserializedModel,
                "Original and deserialized models should be equivalent");
    }

    @Test
    @DisplayName("Round-trip test with empty model")
    void testRoundTripWithEmptyModel() throws Exception {
        // Given: An empty model
        Model originalModel = new CoreseModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);

        // Then: The deserialized model should also be empty
        assertEquals(0, originalModel.size(), "Original model should be empty");
        assertEquals(0, deserializedModel.size(), "Deserialized model should be empty");
        assertEquals(originalModel, deserializedModel, "Both models should be equivalent");
    }

    @Test
    @DisplayName("Round-trip test with model containing only typed literals")
    void testRoundTripWithTypedLiterals() throws Exception {
        // Given: A model with various typed literals
        Model originalModel = createTypedLiteralsTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);

        // Then: All typed literals should be preserved correctly
        assertEquals(originalModel.size(), deserializedModel.size(),
                "Model sizes should be equal after round-trip");
        assertEquals(originalModel, deserializedModel,
                "Original and deserialized models should be equivalent");
    }

    @Test
    @DisplayName("Round-trip test with model containing only language-tagged literals")
    void testRoundTripWithLanguageTaggedLiterals() throws Exception {
        // Given: A model with language-tagged literals
        Model originalModel = createLanguageTaggedLiteralsTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);

        // Then: All language tags should be preserved correctly
        assertEquals(originalModel.size(), deserializedModel.size(),
                "Model sizes should be equal after round-trip");
        assertEquals(originalModel, deserializedModel,
                "Original and deserialized models should be equivalent");
    }

    @Test
    @DisplayName("Round-trip test with model containing only blank nodes")
    void testRoundTripWithBlankNodes() throws Exception {
        // Given: A model with blank nodes as subjects and objects
        Model originalModel = createBlankNodesTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);

        // Then: Blank node structure should be preserved (though IDs may differ)
        assertEquals(originalModel.size(), deserializedModel.size(),
                "Model sizes should be equal after round-trip");
        // Note: Blank node equality is based on structure, not IDs
        assertEquals(originalModel, deserializedModel,
                "Original and deserialized models should be structurally equivalent");
    }

    @Test
    @DisplayName("Round-trip test with model containing special characters and escape sequences")
    void testRoundTripWithSpecialCharacters() throws Exception {
        // Given: A model with special characters and escape sequences
        Model originalModel = createSpecialCharactersTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);

        // Then: All special characters should be preserved correctly
        assertEquals(originalModel.size(), deserializedModel.size(),
                "Model sizes should be equal after round-trip");
        assertEquals(originalModel, deserializedModel,
                "Original and deserialized models should be equivalent, preserving special characters");
    }
}
