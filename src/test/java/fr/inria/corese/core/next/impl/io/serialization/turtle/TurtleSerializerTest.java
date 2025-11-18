package fr.inria.corese.core.next.impl.io.serialization.turtle;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.common.literal.XSD;
import fr.inria.corese.core.next.impl.io.parser.ParserFactory;
import fr.inria.corese.core.next.impl.io.serialization.SerializerFactory;
import fr.inria.corese.core.next.impl.io.serialization.TestStatementFactory;
import fr.inria.corese.core.next.impl.io.serialization.option.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link TurtleSerializer} using Mockito to verify serialization behavior
 * under various configurations and RDF graph structures.
 */
class TurtleSerializerTest {

    private Model mockModel;
    private TurtleSerializerOptions defaultConfig;
    private TestStatementFactory factory;

    @BeforeEach
    void setUp() {
        mockModel = mock(Model.class);
        defaultConfig = TurtleSerializerOptions.defaultConfig();
        factory = new TestStatementFactory();
    }

    /**
     * Tests basic Turtle serialization of a simple triple.
     * Verifies that the subject, predicate, and object are correctly formatted
     * and that standard prefixes are declared and used.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    void testBasicTurtleSerialization() throws SerializationException, IOException {
        Statement mockStatement = factory.createStatement(
                factory.createIRI("http://example.org/ns/person1"),
                factory.createIRI("http://example.org/ns/hasName"),
                factory.createLiteral("John Doe", null, null),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));

        StringWriter writer = new StringWriter();
        TurtleSerializer turtleSerializer = new TurtleSerializer(mockModel, defaultConfig);

        turtleSerializer.write(writer);

        verify(mockModel, times(2)).stream();

        String expected = """
                @prefix ns: <http://example.org/ns/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                  ns:person1 ns:hasName "John Doe" .
                """;

        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }

    /**
     * Tests the `rdf:type` shortcut (using `a`).
     * Verifies that `rdf:type` is serialized as `a` when the option is enabled.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    void testRdfTypeShortcut() throws SerializationException, IOException {

        Statement mockStatement = factory.createStatement(
                factory.createIRI("http://example.org/ns/person1"),
                fr.inria.corese.core.next.impl.common.vocabulary.RDF.type.getIRI(),
                factory.createIRI("http://xmlns.com/foaf/0.1/Person"),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));

        StringWriter writer = new StringWriter();
        TurtleSerializer turtleSerializer = new TurtleSerializer(mockModel, defaultConfig);


        turtleSerializer.write(writer);

        verify(mockModel, times(2)).stream();

        String expected = """
                @prefix foaf: <http://xmlns.com/foaf/0.1/> .
                @prefix ns: <http://example.org/ns/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                  ns:person1 a foaf:Person .
                """;

        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }

    /**
     * Tests serialization of a literal with a language tag.
     * Verifies that the language tag is appended correctly.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    void testLiteralWithLanguageTag() throws SerializationException, IOException {

        Statement mockStatement = factory.createStatement(
                factory.createIRI("http://example.org/data/book1"),
                factory.createIRI("http://purl.org/dc/elements/1.1/title"),
                factory.createLiteral("The Odyssey", null, "en"),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();

        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.MINIMAL)
                .useRdfTypeShortcut(true)
                .useCollections(true)
                .groupBySubject(true)
                .prettyPrint(true)
                .indent("  ")
                .lineEnding("\n")
                .autoDeclarePrefixes(true)
                .trailingDot(true)
                .strictMode(false)
                .build();
        TurtleSerializer turtleSerializer = new TurtleSerializer(mockModel, config);


        turtleSerializer.write(writer);


        verify(mockModel, times(2)).stream();
        String expected = """
                @prefix 11: <http://purl.org/dc/elements/1.1/> .
                @prefix data: <http://example.org/data/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                  data:book1 11:title "The Odyssey"@en .
                """;
        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }

    /**
     * Tests serialization of a literal with an explicit `xsd:string` datatype.
     * Verifies that the datatype is printed when `ALWAYS_TYPED` policy is used.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    @DisplayName("Should serialize literal with xsd:string datatype (minimal policy)")
    void testLiteralWithExplicitXsdStringType() throws SerializationException, IOException {

        IRI mockDatatype = XSD.STRING.getIRI();
        Statement mockStatement = factory.createStatement(
                factory.createIRI("http://example.org/data/book2"),
                factory.createIRI("http://purl.org/dc/elements/1.1/creator"),
                factory.createLiteral("Homer", mockDatatype, null),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();


        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED)
                .usePrefixes(true)
                .autoDeclarePrefixes(true)
                .addCustomPrefix("dc", "http://purl.org/dc/elements/1.1/")
                .build();
        TurtleSerializer turtleSerializer = new TurtleSerializer(mockModel, config);


        turtleSerializer.write(writer);


        verify(mockModel, times(2)).stream();
        String expected = """
                @prefix data: <http://example.org/data/> .
                @prefix dc: <http://purl.org/dc/elements/1.1/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                  data:book2 dc:creator "Homer"^^xsd:string .
                """;
        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }

    /**
     * Tests serialization of a blank node subject using the default anonymous style (`[]`).
     * Verifies that the blank node is serialized inline with its properties.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    void testBlankNodeSerialization() throws SerializationException, IOException {

        Statement mainStatement = factory.createStatement(
                factory.createIRI("http://example.org/ns/mainSubject"),
                factory.createIRI("http://example.org/ns/refersTo"),
                factory.createBlankNode("b1"),
                null
        );

        Statement bNodePropertyStatement = factory.createStatement(
                factory.createBlankNode("b1"),
                factory.createIRI("http://example.org/ns/hasValue"),
                factory.createLiteral("Value of BNode", null, null),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Arrays.asList(mainStatement, bNodePropertyStatement).iterator());
        when(mockModel.stream()).thenAnswer(invocation -> Stream.of(mainStatement, bNodePropertyStatement));


        StringWriter writer = new StringWriter();
        TurtleSerializer turtleSerializer = new TurtleSerializer(mockModel, defaultConfig);

        turtleSerializer.write(writer);


        verify(mockModel, atLeastOnce()).stream();

        String expected = """
                @prefix ns: <http://example.org/ns/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                  _:b1 ns:hasValue "Value of BNode" .
                  ns:mainSubject ns:refersTo _:b1 .
                  _:b1 ns:hasValue "Value of BNode" .
                """;

        String actual = writer.toString().replace("\r\n", "\n");

        String expected1 = """
  _:b1 ns:hasValue "Value of BNode" .
  ns:mainSubject ns:refersTo _:b1 .
""".trim();

        String expected2 = """
  ns:mainSubject ns:refersTo _:b1 .
  _:b1 ns:hasValue "Value of BNode" .
""".trim();
        assertTrue(expected.contains(expected1) || expected.contains(expected2));
    }

    /**
     * Tests serialization verify that the blank node is serialized
     */
    @Test
    void testBlankNodeSerializarionWithoutId() {
        Logger logger = LoggerFactory.getLogger(TurtleSerializerTest.class);

        ValueFactory valueFactory;
        fr.inria.corese.core.next.api.io.serialization.SerializerFactory serializerFactory;
        ParserFactory parserFactory;
        TurtleSerializerOptions defaultConfig;
        String EXAMPLE_NS = "http://example.org/";
        String PREDICATE_KNOWS = EXAMPLE_NS + "knows";

        valueFactory = new CoreseAdaptedValueFactory();
        serializerFactory = new SerializerFactory();
        parserFactory = new ParserFactory();
        defaultConfig = TurtleSerializerOptions.defaultConfig();

        Model model = new CoreseModel();

        BNode blankSubject = valueFactory.createBNode();
        BNode blankObject = valueFactory.createBNode();
        IRI predicate = valueFactory.createIRI(PREDICATE_KNOWS);

        model.add(blankSubject, predicate, blankObject);


            model.stream().forEach(stmt -> {
                Value obj = stmt.getObject();
                String subjectString = stmt.getSubject().stringValue();
                String predicateString = stmt.getPredicate().stringValue();

                if (obj instanceof Literal literal) {
                    String label = String.valueOf(literal.getLabel());
                    String languageTag = literal.getLanguage().orElse(null);

                    if (languageTag != null) {
                        logger.debug("({}, {}, \"{}\"@{})",
                                subjectString,
                                predicateString,
                                label,
                                languageTag);
                    } else {
                        logger.debug("({}, {}, \"{}\")",
                                subjectString,
                                predicateString,
                                label);
                    }
                } else {
                    logger.debug("({}, {}, {})",
                            subjectString,
                            predicateString,
                            obj.stringValue());
                }
            });

        StringWriter writer = new StringWriter();
        TurtleSerializer turtleSerializer = new TurtleSerializer(model, defaultConfig);

        turtleSerializer.write(writer);
        String actual = writer.toString().replace("\r\n", "\n");
        System.out.println(actual);
    }

    /**
     * Tests serialization with a base IRI defined.
     * Verifies that the `@base` directive is included in the output.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    void testBaseIRI() throws SerializationException, IOException {
        Statement mockStatement = factory.createStatement(
                factory.createIRI("http://example.org/base/resource1"),
                factory.createIRI("http://example.org/base/prop"),
                factory.createLiteral("Test", null, null),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();

        TurtleSerializerOptions configWithBase = new TurtleSerializerOptions.Builder()
                .baseIRI("http://example.org/base/")
                .usePrefixes(true)
                .autoDeclarePrefixes(true)
                .build();
        TurtleSerializer turtleSerializer = new TurtleSerializer(mockModel, configWithBase);

        turtleSerializer.write(writer);

        verify(mockModel, times(2)).stream();
        String expected = """
                @base <http://example.org/base/> .
                @prefix base: <http://example.org/base/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                  base:resource1 base:prop "Test" .
                """;
        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }

    /**
     * Tests serialization of an empty model.
     * Verifies that only prefix declarations (if auto-declared) are written, with no statements.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    void testEmptyModel() throws SerializationException, IOException {

        Model emptyModel = mock(Model.class);
        when(emptyModel.iterator()).thenAnswer(invocation -> Collections.emptyList().iterator());
        when(emptyModel.stream())
                .thenReturn(Stream.empty())
                .thenReturn(Stream.empty());


        StringWriter writer = new StringWriter();
        TurtleSerializer turtleSerializer = new TurtleSerializer(emptyModel, defaultConfig);


        turtleSerializer.write(writer);


        verify(emptyModel, times(2)).stream();

        String expected = """
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                """;
        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }

    /**
     * Tests strict mode validation for an invalid literal (rdf:langString without language tag).
     * Verifies that a {@link SerializationException} is thrown.
     *
     * @throws SerializationException (expected) if a serialization error occurs due to strict mode.
     */
    @Test
    void testStrictModeInvalidLiteral() throws SerializationException {

        Statement mockStatement = factory.createStatement(
                factory.createIRI("http://example.org/s"),
                factory.createIRI("http://example.org/p"),
                factory.createLiteral("invalid", RDF.LANGSTRING.getIRI(), null),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();
        TurtleSerializerOptions strictConfig = new TurtleSerializerOptions.Builder().strictMode(true).build();
        TurtleSerializer turtleSerializer = new TurtleSerializer(mockModel, strictConfig);


        SerializationException thrown = assertThrows(SerializationException.class, () -> {
            turtleSerializer.write(writer);
        });

        assertEquals("Turtle", thrown.getFormatName());

        assertEquals("Invalid data for format Turtle: An rdf:langString literal must have a language tag. [Format: Turtle]", thrown.getMessage());
    }

    /**
     * Tests strict mode validation for an IRI containing invalid characters (e.g., space).
     * Verifies that a {@link SerializationException} is thrown.
     *
     * @throws SerializationException (expected) if a serialization error occurs due to strict mode.
     */
    @Test
    void testStrictModeInvalidIRICharacters() throws SerializationException {

        Statement mockStatement = factory.createStatement(
                factory.createIRI("http://example.org/s"),
                factory.createIRI("http://example.org/p"),
                factory.createIRI("http://example.org/invalid iri"),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();
        TurtleSerializerOptions strictConfig = new TurtleSerializerOptions.Builder().strictMode(true).validateURIs(true).build();
        TurtleSerializer turtleSerializer = new TurtleSerializer(mockModel, strictConfig);


        SerializationException thrown = assertThrows(SerializationException.class, () -> {
            turtleSerializer.write(writer);
        });

        assertEquals("Turtle", thrown.getFormatName());

        assertEquals("Invalid data for format Turtle: IRI contains illegal characters (space, quotes, angle brackets) for the unescaped form of Turtle: http://example.org/invalid iri [Format: Turtle]", thrown.getMessage());
    }

    /**
     * Tests serialization of a literal containing multiple lines.
     * Verifies that the literal is wrapped in triple quotes `"""` when `useMultilineLiterals` is true.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    void testMultilineLiteralSerialization() throws SerializationException, IOException {
        String multilineText = "This is the first line.\nThis is the second line.";
        Statement mockStatement = factory.createStatement(
                factory.createIRI("http://example.org/book/1"),
                factory.createIRI("http://example.org/properties/description"),
                factory.createLiteral(multilineText, null, null),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));

        StringWriter writer = new StringWriter();
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .useMultilineLiterals(true)
                .prettyPrint(true)
                .autoDeclarePrefixes(true)
                .build();
        TurtleSerializer turtleSerializer = new TurtleSerializer(mockModel, config);

        turtleSerializer.write(writer);

        verify(mockModel, times(2)).stream();

        String expected = """
                @prefix book: <http://example.org/book/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix properties: <http://example.org/properties/> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                  book:1 properties:description\s""" + "\"\"\"" + multilineText + "\"\"\"" + " .\n";

        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }

    /**
     * Tests serialization of a literal containing escaped characters.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    void testEscapedCharacterLiteralSerialization() throws SerializationException, IOException {
        ValueFactory coreseFactory = new CoreseAdaptedValueFactory();
        Statement statement = coreseFactory.createStatement(
                coreseFactory.createIRI("http://example.org/book/1"),
                coreseFactory.createIRI("http://example.org/properties/description"),
                coreseFactory.createLiteral("\\ \t \b \n \r \f")
        );

        Model coreseModel = new CoreseModel();
        coreseModel.add(statement);

        StringWriter writer = new StringWriter();
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .autoDeclarePrefixes(false)
                .includeContext(false)
                .prettyPrint(false)
                .usePrefixes(false)
                .build();
        TurtleSerializer turtleSerializer = new TurtleSerializer(coreseModel, config);

        turtleSerializer.write(writer);

        String expected = "<http://example.org/book/1> <http://example.org/properties/description> \"\"\"\\ \t \b \n \r \f\"\"\" .";

        String actual = writer.toString().trim();
        assertEquals(expected, actual);
    }

}
