package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.common.serialization.config.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.common.serialization.config.TriGConfig;
import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Optional;
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
    private TriGConfig defaultConfig;

    @BeforeEach
    void setUp() {
        mockModel = mock(Model.class);
        defaultConfig = TriGConfig.defaultConfig();
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
        Statement mockStatement = createStatement(
                createIRI("http://example.org/ns/person1"),
                createIRI("http://example.org/ns/hasName"),
                createLiteral("John Doe", null, null),
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

        Statement mockStatement = createStatement(
                createIRI("http://example.org/ns/person1"),
                createIRI(SerializationConstants.RDF_TYPE),
                createIRI("http://xmlns.com/foaf/0.1/Person"),
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

        Statement mockStatement = createStatement(
                createIRI("http://example.org/data/book1"),
                createIRI("http://purl.org/dc/elements/1.1/title"),
                createLiteral("The Odyssey", null, "en"),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();

        TriGConfig customConfig = new TriGConfig.Builder()
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
        IRI mockDatatype = createIRI(SerializationConstants.XSD_STRING);
        Statement mockStatement = createStatement(
                createIRI("http://example.org/data/book2"),
                createIRI("http://purl.org/dc/elements/1.1/creator"),
                createLiteral("Homer", mockDatatype, null),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();

        TriGConfig customConfig = new TriGConfig.Builder()
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
        Statement mockStatement = createStatement(
                createIRI("http://example.org/base/resource1"),
                createIRI("http://example.org/base/prop"),
                createLiteral("Test", null, null),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();

        TriGConfig configWithBase = new TriGConfig.Builder()
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

        Statement mockStatement = createStatement(
                createIRI("http://example.org/s"),
                createIRI("http://example.org/p"),
                createLiteral("invalid", RDF.LANGSTRING.getIRI(), null),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();
        TriGConfig strictConfig = new TriGConfig.Builder().strictMode(true).build();
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

        Statement mockStatement = createStatement(
                createIRI("http://example.org/s"),
                createIRI("http://example.org/p"),
                createIRI("http://example.org/invalid iri"),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();
        TriGConfig strictConfig = new TriGConfig.Builder().strictMode(true).validateURIs(true).build();
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
        Statement mockStatement = createStatement(
                createIRI("http://example.org/book/1"),
                createIRI("http://example.org/properties/description"),
                createLiteral(multilineText, null, null),
                null
        );

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));

        StringWriter writer = new StringWriter();
        // Create a TriGConfig with useMultilineLiterals enabled
        TriGConfig customConfig = new TriGConfig.Builder()
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
        Statement mockStatement = createStatement(
                createIRI("http://example.org/data/person1"),
                createIRI("http://example.org/data/name"),
                createLiteral("Alice", null, null),
                createIRI("http://example.org/graph/g1")
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


    private Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(subject);
        when(stmt.getPredicate()).thenReturn(predicate);
        when(stmt.getObject()).thenReturn(object);
        when(stmt.getContext()).thenReturn(context);
        return stmt;
    }


    private IRI createIRI(String uri) {
        IRI iri = mock(IRI.class);
        when(iri.isResource()).thenReturn(true);
        when(iri.isIRI()).thenReturn(true);
        when(iri.isBNode()).thenReturn(false);
        when(iri.stringValue()).thenReturn(uri);
        return iri;
    }

    /**
     * Creates a mocked Literal object.
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
            when(literal.getDatatype()).thenReturn(RDF.LANGSTRING.getIRI());
        } else {
            when(literal.getLanguage()).thenReturn(Optional.empty());
            when(literal.getDatatype()).thenReturn(dataTypeIRI);
        }
        return literal;
    }
}
