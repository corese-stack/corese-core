package fr.inria.corese.core.next.data.impl.io.parser.rdfxml;

import fr.inria.corese.core.next.data.api.*;
import fr.inria.corese.core.next.data.api.base.io.RDFFormat;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.data.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.data.impl.io.serialization.DataSerializerFactory;
import fr.inria.corese.core.next.data.impl.io.serialization.rdfxml.RDFXMLSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.util.ParserTestBase;
import fr.inria.corese.core.next.data.impl.temp.CoreseValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Circular tests for RDF/XML parser and serializer integration.
 * These tests verify that data can be correctly serialized to RDF/XML format
 * and then parsed back to an equivalent model (round-trip testing).
 * The circular testing approach ensures that the parser and serializer
 * are compatible and preserve data integrity across format transformations.
 * RDF/XML supports namespaces, so additional tests are included for prefix
 * handling.
 * NOTE: These tests are currently disabled because they cannot work yet.
 * We need to wait for the RDF/XML parser implementation from PR #176:
 * Once the parser is implemented, these tests can be enabled to verify
 * the round-trip functionality between the parser and serializer.
 */
@DisplayName("RDF/XML Circular Integration Tests")
class RDFXMLCircularTest extends ParserTestBase {

    private ValueFactory valueFactory;
    private fr.inria.corese.core.next.data.api.io.serializer.SerializerFactory serializerFactory;
    private ParserFactory parserFactory;
    private RDFXMLSerializerOptions defaultConfig;

    // Test data constants
    private static final String EXAMPLE_NS = "http://example.org/";
    private static final String SUBJECT_1 = EXAMPLE_NS + "person1";
    private static final String SUBJECT_2 = EXAMPLE_NS + "person2";
    private static final String PREDICATE_NAME = EXAMPLE_NS + "name";
    private static final String PREDICATE_AGE = EXAMPLE_NS + "age";
    private static final String PREDICATE_KNOWS = EXAMPLE_NS + "knows";
    private static final String LITERAL_JOHN = "John Doe";
    private static final String LITERAL_JANE = "Jane Smith";
    private static final String LITERAL_AGE_25 = "25";
    private static final String LITERAL_HELLO_EN = "Hello";
    private static final String LANGUAGE_TAG_EN = "en";
    private static final String XSD_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
    private static final String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";

    @BeforeEach
    void setUp() {
        valueFactory = new CoreseValueFactory();
        serializerFactory = new DataSerializerFactory();
        parserFactory = new ParserFactory();
        defaultConfig = RDFXMLSerializerOptions.defaultConfig();
    }

    /**
     * Creates a simple model with basic triples containing IRIs and string
     * literals.
     * 
     * @return A model with two simple triples
     */
    private Model createSimpleTestModel() {
        Model model = createTestModel();

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
     * Creates a complex model with various RDF value types including
     * typed literals, language-tagged literals, and blank nodes.
     * 
     * @return A model with diverse triple patterns
     */
    private Model createComplexTestModel() {
        Model model = createTestModel();

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
        Model model = createTestModel();

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
        Model model = createTestModel();

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
        Model model = createTestModel();

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
        Model model = createTestModel();

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
     */
    private Model performRoundTrip(Model originalModel) {
        // Serialize to RDF/XML
        RDFSerializer serializer = serializerFactory.createSerializer(
                RDFFormat.RDFXML, originalModel, defaultConfig);

        StringWriter writer = new StringWriter();
        serializer.write(writer);
        String serializedContent = writer.toString();

        // Verify serialization produced content (only check for non-empty models)
        assertNotNull(serializedContent, "Serialized content should not be null");
        if (!originalModel.isEmpty()) {
            assertFalse(serializedContent.isEmpty(),
                    "Serialized content should not be empty for non-empty models");
        }

        // Parse back from RDF/XML
        Model deserializedModel = createTestModel();
        RDFParser parser = parserFactory.createRDFParser(
                RDFFormat.RDFXML, deserializedModel, valueFactory);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                serializedContent.getBytes(StandardCharsets.UTF_8));
        parser.parse(inputStream);

        return deserializedModel;
    }

    /**
     * Verifies that two models contain equivalent statements.
     */
    private void verifyModelsEquivalent(Model original, Model deserialized, String message) {
        assertEquals(original.size(), deserialized.size(), "Model sizes should match");

        // Check each original statement has an equivalent in deserialized
        for (Statement origStmt : original) {
            if (!hasEquivalentStatement(origStmt, deserialized)) {
                fail(message + "\nMissing equivalent for: " + statementToString(origStmt));
            }
        }
    }

    /**
     * Checks if a model contains a statement equivalent to the given one.
     */
    private boolean hasEquivalentStatement(Statement stmt, Model model) {
        for (Statement candidate : model) {
            if (statementsEquivalent(stmt, candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if two statements are equivalent (considering RDF semantics).
     */
    private boolean statementsEquivalent(Statement s1, Statement s2) {
        return valuesEquivalent(s1.getSubject(), s2.getSubject()) &&
                valuesEquivalent(s1.getPredicate(), s2.getPredicate()) &&
                valuesEquivalent(s1.getObject(), s2.getObject());
    }

    /**
     * Checks if two RDF values are equivalent.
     */
    private boolean valuesEquivalent(Value v1, Value v2) {
        // Both blank nodes → always equivalent (IDs may differ)
        if (v1 instanceof BNode && v2 instanceof BNode) {
            return true;
        }

        // Both IRIs → compare string values
        if (v1 instanceof IRI && v2 instanceof IRI) {
            return v1.stringValue().equals(v2.stringValue());
        }

        // Both literals → compare with datatype normalization
        if (v1 instanceof Literal && v2 instanceof Literal) {
            return literalsEquivalent((Literal) v1, (Literal) v2);
        }

        return false;
    }

    /**
     * Checks if two literals are equivalent.
     * Handles xsd:string normalization: "text" ≡ "text"^^xsd:string
     */
    private boolean literalsEquivalent(Literal l1, Literal l2) {
        // Lexical form must match
        if (!l1.getLabel().equals(l2.getLabel())) {
            return false;
        }

        // Language tags must match
        if (l1.getLanguage().isPresent() || l2.getLanguage().isPresent()) {
            return l1.getLanguage().equals(l2.getLanguage());
        }

        // Normalize datatypes: xsd:string ≡ no datatype, and resolve prefixes
        String dt1 = getDatatypeOrNull(l1);
        String dt2 = getDatatypeOrNull(l2);

        // Both null or both xsd:string → equivalent
        if ((dt1 == null || dt1.equals(XSD_STRING)) &&
                (dt2 == null || dt2.equals(XSD_STRING))) {
            return true;
        }

        // Otherwise datatypes must match exactly
        return dt1 != null && dt1.equals(dt2);
    }

    /**
     * Gets the datatype IRI as string, or null if no datatype.
     * Resolves common prefixes (xsd:, rdf:, rdfs:) to full IRIs.
     */
    private String getDatatypeOrNull(Literal lit) {
        if (lit.getDatatype() == null) {
            return null;
        }

        String dt = lit.getDatatype().stringValue();

        // Resolve common prefixes to full IRIs
        if (dt.startsWith("xsd:")) {
            return "http://www.w3.org/2001/XMLSchema#" + dt.substring(4);
        }
        if (dt.startsWith("rdf:")) {
            return "http://www.w3.org/1999/02/22-rdf-syntax-ns#" + dt.substring(4);
        }
        if (dt.startsWith("rdfs:")) {
            return "http://www.w3.org/2000/01/rdf-schema#" + dt.substring(5);
        }

        return dt;
    }

    /**
     * Converts a statement to a debug string.
     */
    private String statementToString(Statement stmt) {
        return "(" + valueToString(stmt.getSubject()) + ", " +
                valueToString(stmt.getPredicate()) + ", " +
                valueToString(stmt.getObject()) + ")";
    }

    /**
     * Converts a value to a debug string.
     */
    private String valueToString(Value v) {
        if (v instanceof BNode) {
            return "_:bnode";
        } else if (v instanceof Literal lit) {
            String result = "\"" + lit.getLabel() + "\"";
            if (lit.getLanguage().isPresent()) {
                result += "@" + lit.getLanguage().get();
            } else if (lit.getDatatype() != null) {
                result += "^^" + lit.getDatatype().stringValue();
            }
            return result;
        } else {
            return v.stringValue();
        }
    }

    @Test
    @DisplayName("Round-trip test with simple model containing basic IRIs and literals")
    void testRoundTripWithSimpleModel()  {
        // Given: A simple model with basic triples
        Model originalModel = createSimpleTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);
        verifyModelsEquivalent(originalModel, deserializedModel,
                "Models should contain equivalent triples");
    }

    @Test
    @DisplayName("Round-trip test with complex model containing diverse RDF value types")
    void testRoundTripWithComplexModel()  {
        // Given: A complex model with various RDF constructs
        Model originalModel = createComplexTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);

        // Then: The deserialized model should preserve all data
        assertEquals(originalModel.size(), deserializedModel.size(),
                "Model sizes should be equal (parser handles all value types)");
    }

    @Test
    @DisplayName("Round-trip test with empty model")
    void testRoundTripWithEmptyModel()  {
        // Given: An empty model
        Model originalModel = createTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);

        // Then: The deserialized model should also be empty
        assertEquals(0, originalModel.size(), "Original model should be empty");
        assertEquals(0, deserializedModel.size(), "Deserialized model should be empty");
    }

    @Test
    @DisplayName("Round-trip test with model containing only typed literals")
    void testRoundTripWithTypedLiterals()  {
        // Given: A model with various typed literals
        Model originalModel = createTypedLiteralsTestModel();
        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);
        verifyModelsEquivalent(originalModel, deserializedModel,
                "Models should contain equivalent typed literals");
    }

    @Test
    @DisplayName("Round-trip test with model containing only language-tagged literals")
    void testRoundTripWithLanguageTaggedLiterals()  {
        // Given: A model with language-tagged literals
        Model originalModel = createLanguageTaggedLiteralsTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);
        verifyModelsEquivalent(originalModel, deserializedModel,
                "Models should contain equivalent language-tagged literals");
    }

    @Test
    @DisplayName("Round-trip test with model containing only blank nodes")
    void testRoundTripWithBlankNodes()  {
        // Given: A model with blank nodes as subjects and objects
        Model originalModel = createBlankNodesTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);

        // Then: Blank node structure should be preserved (though IDs may differ)
        assertEquals(originalModel.size(), deserializedModel.size(),
                "Model sizes should match");
        assertEquals(1, deserializedModel.size(), "Should have exactly one triple");

        Statement stmt = deserializedModel.iterator().next();
        assertInstanceOf(BNode.class, stmt.getSubject(), "Subject should be a blank node");
        assertInstanceOf(BNode.class, stmt.getObject(), "Object should be a blank node");
    }

    @Test
    @DisplayName("Round-trip test with model containing special characters and escape sequences")
    void testRoundTripWithSpecialCharacters()  {
        // Given: A model with special characters and escape sequences
        Model originalModel = createSpecialCharactersTestModel();

        // When: Performing round-trip serialization and parsing
        Model deserializedModel = performRoundTrip(originalModel);
        verifyModelsEquivalent(originalModel, deserializedModel,
                "Models should contain equivalent triples with special characters");
    }
}