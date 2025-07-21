package fr.inria.corese.core.next.impl.io.serialization.trig;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.io.serialization.TestStatementFactory;
import fr.inria.corese.core.next.impl.io.serialization.option.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link TriGSerializer} using Mockito to verify serialization behavior
 * under various configurations and RDF graph structures.
 */
class TriGSerializerTest {

    private Model mockModel;
    private TriGOption defaultConfig;
    private TestStatementFactory factory;

    @BeforeEach
    void setUp() {
        mockModel = mock(Model.class);
        defaultConfig = TriGOption.defaultConfig();
        factory = new TestStatementFactory();
    }

    /**
     * Tests basic TriG serialization of a simple triple.
     * Verifies that the subject, predicate, and object are correctly formatted
     * and that standard prefixes are declared and used.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    void testBasicTriGSerialization() throws SerializationException, IOException {
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

        TriGSerializer triGSerializer = new TriGSerializer(mockModel, defaultConfig);


        triGSerializer.write(writer);

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
                factory.createIRI(SerializationConstants.RDF_TYPE),
                factory.createIRI("http://xmlns.com/foaf/0.1/Person"),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));

        StringWriter writer = new StringWriter();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, defaultConfig);


        triGSerializer.write(writer);


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

        TriGOption customConfig = new TriGOption.Builder()
                .strictMode(false)
                .build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, customConfig);


        triGSerializer.write(writer);


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
    void testLiteralWithExplicitXsdStringType() throws SerializationException, IOException {
        IRI mockDatatype = factory.createIRI(SerializationConstants.XSD_STRING);
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

        TriGOption customConfig = new TriGOption.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED)
                .build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, customConfig);


        triGSerializer.write(writer);


        verify(mockModel, times(2)).stream();
        String expected = """
                @prefix 11: <http://purl.org/dc/elements/1.1/> .
                @prefix data: <http://example.org/data/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                data:book2 11:creator "Homer"^^xsd:string .
                
                """;
        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
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

        TriGOption configWithBase = new TriGOption.Builder()
                .baseIRI("http://example.org/base/")
                .build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, configWithBase);

        triGSerializer.write(writer);

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
        TriGSerializer triGSerializer = new TriGSerializer(emptyModel, defaultConfig);


        triGSerializer.write(writer);


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
        TriGOption strictConfig = new TriGOption.Builder().strictMode(true).build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, strictConfig);


        SerializationException thrown = assertThrows(SerializationException.class, () -> {
            triGSerializer.write(writer);
        });

        assertEquals("TriG", thrown.getFormatName());

        assertEquals("Invalid data for format TriG: An rdf:langString literal must have a language tag. [Format: TriG]", thrown.getMessage());
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
        TriGOption strictConfig = new TriGOption.Builder().strictMode(true).validateURIs(true).build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, strictConfig);


        SerializationException thrown = assertThrows(SerializationException.class, () -> {
            triGSerializer.write(writer);
        });

        assertEquals("TriG", thrown.getFormatName());

        assertEquals("Invalid data for format TriG: IRI contains illegal characters (space, quotes, angle brackets) for the unescaped form of TriG: http://example.org/invalid iri [Format: TriG]", thrown.getMessage());
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
        TriGOption customConfig = new TriGOption.Builder()
                .useMultilineLiterals(true)
                .prettyPrint(true)
                .build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, customConfig);


        triGSerializer.write(writer);


        verify(mockModel, times(2)).stream();

        String expected = """
                @prefix book: <http://example.org/book/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix properties: <http://example.org/properties/> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                book:1 properties:description\s""" + "\"\"\"" + multilineText + "\"\"\"" + " .\n\n";

        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }

    /**
     * Tests basic TriG serialization with a named graph.
     * Verifies that the graph name and graph block are correctly formatted.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    void testBasicTrigSerializationWithNamedGraph() throws SerializationException, IOException {
        Statement mockStatement = factory.createStatement(
                factory.createIRI("http://example.org/data/person1"),
                factory.createIRI("http://example.org/data/name"),
                factory.createLiteral("Alice", null, null),
                factory.createIRI("http://example.org/graph/g1")
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));
        StringWriter writer = new StringWriter();

        TriGSerializer triGSerializer = new TriGSerializer(mockModel, defaultConfig);

        triGSerializer.write(writer);

        verify(mockModel, times(2)).stream();
        String expected = """
                @prefix data: <http://example.org/data/> .
                @prefix graph: <http://example.org/graph/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                graph:g1 {
                  data:person1 data:name "Alice" .
                } .
                
                """;

        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }


}
