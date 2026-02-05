package fr.inria.corese.core.next.data.impl.io.serialization.trig;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.data.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.data.impl.common.literal.XSD;
import fr.inria.corese.core.next.data.impl.exception.SerializationException;
import fr.inria.corese.core.next.data.impl.io.serialization.TestStatementFactory;
import fr.inria.corese.core.next.data.impl.io.serialization.option.LiteralDatatypePolicyEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private TriGSerializerOptions defaultConfig;
    private TestStatementFactory factory;

    @BeforeEach
    void setUp() {
        mockModel = mock(Model.class);
        defaultConfig = TriGSerializerOptions.defaultConfig();
        factory = new TestStatementFactory();
    }

    /**
     * Tests basic TriG serialization of a simple triple.
     * Verifies that the subject, predicate, and object are correctly formatted
     * and that standard prefixes are declared and used.
     *
     * @throws SerializationException if a serialization error occurs.
     */
    @Test
    void testBasicTriGSerialization() throws SerializationException {
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
     */
    @Test
    void testRdfTypeShortcut() throws SerializationException {

        Statement mockStatement = factory.createStatement(
                factory.createIRI("http://example.org/ns/person1"),
                RDF.type.getIRI(),
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
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                
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
     */
    @Test
    void testLiteralWithLanguageTag() throws SerializationException {

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

        TriGSerializerOptions customConfig = new TriGSerializerOptions.Builder()
                .strictMode(false)
                .build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, customConfig);


        triGSerializer.write(writer);


        verify(mockModel, times(2)).stream();
        String expected = """
                @prefix 11: <http://purl.org/dc/elements/1.1/> .
                @prefix data: <http://example.org/data/> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                
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
     */
    @Test
    void testLiteralWithExplicitXsdStringType() throws SerializationException {
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

        TriGSerializerOptions customConfig = new TriGSerializerOptions.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED)
                .build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, customConfig);


        triGSerializer.write(writer);


        verify(mockModel, times(2)).stream();
        String expected = """
                @prefix 11: <http://purl.org/dc/elements/1.1/> .
                @prefix data: <http://example.org/data/> .
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
     */
    @Test
    void testBaseIRI() throws SerializationException {
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

        TriGSerializerOptions configWithBase = new TriGSerializerOptions.Builder()
                .baseIRI("http://example.org/base/")
                .build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, configWithBase);

        triGSerializer.write(writer);

        verify(mockModel, times(2)).stream();
        String expected = """
                @base <http://example.org/base/> .
                @prefix base: <http://example.org/base/> .
                
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
     */
    @Test
    void testEmptyModel() throws SerializationException {

        Model emptyModel = mock(Model.class);
        when(emptyModel.iterator()).thenAnswer(invocation -> Collections.emptyIterator());
        when(emptyModel.stream())
                .thenReturn(Stream.empty())
                .thenReturn(Stream.empty());


        StringWriter writer = new StringWriter();
        TriGSerializer triGSerializer = new TriGSerializer(emptyModel, defaultConfig);


        triGSerializer.write(writer);


        verify(emptyModel, times(2)).stream();

        String expected = "";
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
                factory.createLiteral("invalid", fr.inria.corese.core.next.data.impl.common.literal.RDF.LANGSTRING.getIRI(), null),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();
        TriGSerializerOptions strictConfig = new TriGSerializerOptions.Builder().strictMode(true).build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, strictConfig);


        SerializationException thrown = assertThrows(SerializationException.class, () -> triGSerializer.write(writer));

        assertEquals("TriG", thrown.getFormatName());

        assertEquals("An rdf:langString literal must have a language tag. [Format: TriG]", thrown.getMessage());
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
        TriGSerializerOptions strictConfig = new TriGSerializerOptions.Builder().strictMode(true).validateURIs(true).build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, strictConfig);


        SerializationException thrown = assertThrows(SerializationException.class, () -> triGSerializer.write(writer));

        assertEquals("TriG", thrown.getFormatName());

        assertEquals("IRI contains illegal characters (space, quotes, angle brackets) for the unescaped form : http://example.org/invalid iri [Format: TriG]", thrown.getMessage());
    }

    /**
     * Tests serialization of a literal containing multiple lines.
     * Verifies that the literal is wrapped in triple quotes `"""` when `useMultilineLiterals` is true.
     *
     * @throws SerializationException if a serialization error occurs.
     */
    @Test
    void testMultilineLiteralSerialization() throws SerializationException {

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
        TriGSerializerOptions customConfig = new TriGSerializerOptions.Builder()
                .useMultilineLiterals(true)
                .prettyPrint(true)
                .build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, customConfig);


        triGSerializer.write(writer);


        verify(mockModel, times(2)).stream();

        String expected = """
                @prefix book: <http://example.org/book/> .
                @prefix properties: <http://example.org/properties/> .
                
                book:1 properties:description\s""" + "\"\"\"" + multilineText + "\"\"\"" + " .\n\n";

        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }

    /**
     * Tests basic TriG serialization with a named graph.
     * Verifies that the graph name and graph block are correctly formatted.
     *
     * @throws SerializationException if a serialization error occurs.
     */
    @Test
    void testBasicTrigSerializationWithNamedGraph() throws SerializationException {
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
                
                graph:g1 {
                  data:person1 data:name "Alice" .
                }
                
                """;

        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }


}
