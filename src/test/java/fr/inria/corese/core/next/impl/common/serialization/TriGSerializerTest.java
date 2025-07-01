package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.common.serialization.config.SerializerConfig;
import fr.inria.corese.core.next.impl.common.serialization.config.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link TriGSerializerTest} using Mockito to verify serialization behavior
 * under various configurations and RDF graph structures.
 */
class TriGSerializerTest {

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
        // Given
        Model mockModel = mock(Model.class);
        Statement mockStatement = mock(Statement.class);
        IRI mockSubject = mock(IRI.class);
        IRI mockPredicate = mock(IRI.class);
        Literal mockObject = mock(Literal.class);


        when(mockSubject.stringValue()).thenReturn("http://example.org/ns/person1");
        when(mockSubject.isIRI()).thenReturn(true);
        when(mockSubject.isResource()).thenReturn(true);
        when(mockSubject.isBNode()).thenReturn(false);
        when(mockSubject.isLiteral()).thenReturn(false);


        when(mockPredicate.stringValue()).thenReturn("http://example.org/ns/hasName");
        when(mockPredicate.isIRI()).thenReturn(true);
        when(mockPredicate.isResource()).thenReturn(true);
        when(mockPredicate.isBNode()).thenReturn(false);
        when(mockPredicate.isLiteral()).thenReturn(false);


        when(mockObject.stringValue()).thenReturn("John Doe");
        when(mockObject.isLiteral()).thenReturn(true);
        when(mockObject.isIRI()).thenReturn(false);
        when(mockObject.isResource()).thenReturn(false);
        when(mockObject.isBNode()).thenReturn(false);
        when(mockObject.getLanguage()).thenReturn(Optional.empty());
        when(mockObject.getDatatype()).thenReturn(null);

        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getObject()).thenReturn(mockObject);
        when(mockStatement.getContext()).thenReturn(null);

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));

        StringWriter writer = new StringWriter();

        TriGSerializer triGSerializer = new TriGSerializer(mockModel, SerializerConfig.trigConfig());


        triGSerializer.write(writer);


        verify(mockModel, times(2)).stream();
        verify(mockSubject, atLeastOnce()).stringValue();
        verify(mockSubject, atLeastOnce()).isIRI();
        verify(mockPredicate, atLeastOnce()).stringValue();
        verify(mockPredicate, atLeastOnce()).isIRI();
        verify(mockObject, atLeastOnce()).stringValue();
        verify(mockObject, atLeastOnce()).isLiteral();


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

        Model mockModel = mock(Model.class);
        Statement mockStatement = mock(Statement.class);
        IRI mockSubject = mock(IRI.class);
        IRI mockPredicate = mock(IRI.class);
        IRI mockObject = mock(IRI.class);

        when(mockSubject.stringValue()).thenReturn("http://example.org/ns/person1");
        when(mockSubject.isIRI()).thenReturn(true);
        when(mockSubject.isResource()).thenReturn(true);
        when(mockSubject.isBNode()).thenReturn(false);
        when(mockSubject.isLiteral()).thenReturn(false);

        when(mockPredicate.stringValue()).thenReturn(SerializationConstants.RDF_TYPE);
        when(mockPredicate.isIRI()).thenReturn(true);
        when(mockPredicate.isResource()).thenReturn(true);
        when(mockPredicate.isBNode()).thenReturn(false);
        when(mockPredicate.isLiteral()).thenReturn(false);

        when(mockObject.stringValue()).thenReturn("http://xmlns.com/foaf/0.1/Person");
        when(mockObject.isIRI()).thenReturn(true);
        when(mockObject.isResource()).thenReturn(true);
        when(mockObject.isBNode()).thenReturn(false);
        when(mockObject.isLiteral()).thenReturn(false);

        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getObject()).thenReturn(mockObject);
        when(mockStatement.getContext()).thenReturn(null);

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));

        StringWriter writer = new StringWriter();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel);


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

        Model mockModel = mock(Model.class);
        Statement mockStatement = mock(Statement.class);
        IRI mockSubject = mock(IRI.class);
        IRI mockPredicate = mock(IRI.class);
        Literal mockObject = mock(Literal.class);


        when(mockSubject.stringValue()).thenReturn("http://example.org/data/book1");
        when(mockSubject.isIRI()).thenReturn(true);
        when(mockSubject.isResource()).thenReturn(true);
        when(mockSubject.isBNode()).thenReturn(false);
        when(mockSubject.isLiteral()).thenReturn(false);


        when(mockPredicate.stringValue()).thenReturn("http://purl.org/dc/elements/1.1/title");
        when(mockPredicate.isIRI()).thenReturn(true);
        when(mockPredicate.isResource()).thenReturn(true);
        when(mockPredicate.isBNode()).thenReturn(false);
        when(mockPredicate.isLiteral()).thenReturn(false);

        when(mockObject.stringValue()).thenReturn("The Odyssey");
        when(mockObject.isLiteral()).thenReturn(true);
        when(mockObject.isIRI()).thenReturn(false);
        when(mockObject.isResource()).thenReturn(false);
        when(mockObject.isBNode()).thenReturn(false);
        when(mockObject.getLanguage()).thenReturn(Optional.of("en"));

        IRI mockRdfLangString = mock(IRI.class);
        when(mockRdfLangString.stringValue()).thenReturn(RDF.LANGSTRING.getIRI().stringValue());
        when(mockRdfLangString.isIRI()).thenReturn(true);
        when(mockRdfLangString.isResource()).thenReturn(true);
        when(mockObject.getDatatype()).thenReturn(mockRdfLangString);

        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getObject()).thenReturn(mockObject);
        when(mockStatement.getContext()).thenReturn(null);

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();

        // Explicitly create SerializerConfig to ensure strictMode is false
        Map<String, String> commonTriGPrefixes = new HashMap<>();
        commonTriGPrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        commonTriGPrefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        commonTriGPrefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        commonTriGPrefixes.put("owl", "http://www.w3.org/2002/07/owl#");

        SerializerConfig config = new SerializerConfig.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.MINIMAL)
                .useRdfTypeShortcut(true)
                .useCollections(true)
                .groupBySubject(true)
                .prettyPrint(true)
                .indent("  ")
                .lineEnding("\n")
                .addCustomPrefixes(commonTriGPrefixes)
                .autoDeclarePrefixes(true)
                .trailingDot(true)
                .strictMode(false)
                .build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, config);


        triGSerializer.write(writer);


        verify(mockModel, times(2)).stream();
        String expected = """
                @prefix 11: <http://purl.org/dc/elements/1.1/> .
                @prefix data: <http://example.org/data/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                  data:book1 11:title \"The Odyssey\"@en .
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
        // Given
        Model mockModel = mock(Model.class);
        Statement mockStatement = mock(Statement.class);
        IRI mockSubject = mock(IRI.class);
        IRI mockPredicate = mock(IRI.class);
        Literal mockObject = mock(Literal.class);
        IRI mockDatatype = mock(IRI.class);

        // Configure mocks
        when(mockSubject.stringValue()).thenReturn("http://example.org/data/book2");
        when(mockSubject.isIRI()).thenReturn(true);
        when(mockSubject.isResource()).thenReturn(true);
        when(mockSubject.isBNode()).thenReturn(false);
        when(mockSubject.isLiteral()).thenReturn(false);


        when(mockPredicate.stringValue()).thenReturn("http://purl.org/dc/elements/1.1/creator");
        when(mockPredicate.isIRI()).thenReturn(true);
        when(mockPredicate.isResource()).thenReturn(true);
        when(mockPredicate.isBNode()).thenReturn(false);
        when(mockPredicate.isLiteral()).thenReturn(false);

        when(mockDatatype.stringValue()).thenReturn(SerializationConstants.XSD_STRING);
        when(mockDatatype.isIRI()).thenReturn(true);
        when(mockDatatype.isResource()).thenReturn(true);

        when(mockObject.stringValue()).thenReturn("Homer");
        when(mockObject.isLiteral()).thenReturn(true);
        when(mockObject.isIRI()).thenReturn(false);
        when(mockObject.isResource()).thenReturn(false);
        when(mockObject.isBNode()).thenReturn(false);
        when(mockObject.getLanguage()).thenReturn(Optional.empty());
        when(mockObject.getDatatype()).thenReturn(mockDatatype);


        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getObject()).thenReturn(mockObject);
        when(mockStatement.getContext()).thenReturn(null);

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();

        Map<String, String> commonTrigPrefixes = new HashMap<>();
        commonTrigPrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        commonTrigPrefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        commonTrigPrefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        commonTrigPrefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        commonTrigPrefixes.put("dc", "http://purl.org/dc/elements/1.1/");

        SerializerConfig config = new SerializerConfig.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED)
                .addCustomPrefixes(commonTrigPrefixes)
                .usePrefixes(true)
                .autoDeclarePrefixes(true)
                .build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, config);


        triGSerializer.write(writer);


        verify(mockModel, times(2)).stream();
        String expected = """ 
                @prefix data: <http://example.org/data/> .
                @prefix dc: <http://purl.org/dc/elements/1.1/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                  data:book2 dc:creator \"Homer\"^^xsd:string .
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
        Model mockModel = mock(Model.class);
        Statement mockStatement = mock(Statement.class);
        IRI mockSubject = mock(IRI.class);
        IRI mockPredicate = mock(IRI.class);
        Literal mockObject = mock(Literal.class);

        when(mockSubject.stringValue()).thenReturn("http://example.org/base/resource1");
        when(mockSubject.isIRI()).thenReturn(true);
        when(mockSubject.isResource()).thenReturn(true);
        when(mockSubject.isBNode()).thenReturn(false);
        when(mockSubject.isLiteral()).thenReturn(false);


        when(mockPredicate.stringValue()).thenReturn("http://example.org/base/prop");
        when(mockPredicate.isIRI()).thenReturn(true);
        when(mockPredicate.isResource()).thenReturn(true);
        when(mockPredicate.isBNode()).thenReturn(false);
        when(mockPredicate.isLiteral()).thenReturn(false);


        when(mockObject.stringValue()).thenReturn("Test");
        when(mockObject.isLiteral()).thenReturn(true);
        when(mockObject.isIRI()).thenReturn(false);
        when(mockObject.isResource()).thenReturn(false);
        when(mockObject.isBNode()).thenReturn(false);
        when(mockObject.getLanguage()).thenReturn(Optional.empty());
        when(mockObject.getDatatype()).thenReturn(null);

        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getObject()).thenReturn(mockObject);
        when(mockStatement.getContext()).thenReturn(null);

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();

        Map<String, String> commonTrigPrefixes = new HashMap<>();
        commonTrigPrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        commonTrigPrefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        commonTrigPrefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        commonTrigPrefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        commonTrigPrefixes.put("base", "http://example.org/base/");

        SerializerConfig configWithBase = new SerializerConfig.Builder()
                .baseIRI("http://example.org/base/")
                .addCustomPrefixes(commonTrigPrefixes)
                .usePrefixes(true)
                .autoDeclarePrefixes(true)
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
        TriGSerializer triGSerializer = new TriGSerializer(emptyModel);


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

        Model mockModel = mock(Model.class);
        Statement mockStatement = mock(Statement.class);
        IRI mockSubject = mock(IRI.class);
        IRI mockPredicate = mock(IRI.class);
        Literal mockObject = mock(Literal.class);

        when(mockSubject.stringValue()).thenReturn("http://example.org/s");
        when(mockSubject.isIRI()).thenReturn(true);
        when(mockSubject.isResource()).thenReturn(true);
        when(mockSubject.isBNode()).thenReturn(false);
        when(mockSubject.isLiteral()).thenReturn(false);


        when(mockPredicate.stringValue()).thenReturn("http://example.org/p");
        when(mockPredicate.isIRI()).thenReturn(true);
        when(mockPredicate.isResource()).thenReturn(true);
        when(mockPredicate.isBNode()).thenReturn(false);
        when(mockPredicate.isLiteral()).thenReturn(false);

        when(mockObject.stringValue()).thenReturn("invalid");
        when(mockObject.isLiteral()).thenReturn(true);
        when(mockObject.isIRI()).thenReturn(false);
        when(mockObject.isResource()).thenReturn(false);
        when(mockObject.isBNode()).thenReturn(false);
        when(mockObject.getLanguage()).thenReturn(Optional.empty());
        IRI mockRdfLangStringDatatype = mock(IRI.class);
        when(mockRdfLangStringDatatype.stringValue()).thenReturn(RDF.LANGSTRING.getIRI().stringValue());
        when(mockRdfLangStringDatatype.isIRI()).thenReturn(true);
        when(mockRdfLangStringDatatype.isResource()).thenReturn(true);
        when(mockObject.getDatatype()).thenReturn(mockRdfLangStringDatatype);


        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getObject()).thenReturn(mockObject);
        when(mockStatement.getContext()).thenReturn(null);

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();
        SerializerConfig strictConfig = new SerializerConfig.Builder().strictMode(true).build();
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

        Model mockModel = mock(Model.class);
        Statement mockStatement = mock(Statement.class);
        IRI mockSubject = mock(IRI.class);
        IRI mockPredicate = mock(IRI.class);
        IRI mockObject = mock(IRI.class);


        when(mockSubject.stringValue()).thenReturn("http://example.org/s");
        when(mockSubject.isIRI()).thenReturn(true);
        when(mockSubject.isResource()).thenReturn(true);
        when(mockSubject.isBNode()).thenReturn(false);
        when(mockSubject.isLiteral()).thenReturn(false);


        when(mockPredicate.stringValue()).thenReturn("http://example.org/p");
        when(mockPredicate.isIRI()).thenReturn(true);
        when(mockPredicate.isResource()).thenReturn(true);
        when(mockPredicate.isBNode()).thenReturn(false);
        when(mockPredicate.isLiteral()).thenReturn(false);


        when(mockObject.stringValue()).thenReturn("http://example.org/invalid iri");
        when(mockObject.isIRI()).thenReturn(true);
        when(mockObject.isResource()).thenReturn(true);
        when(mockObject.isBNode()).thenReturn(false);
        when(mockObject.isLiteral()).thenReturn(false);

        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getObject()).thenReturn(mockObject);
        when(mockStatement.getContext()).thenReturn(null);

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));


        StringWriter writer = new StringWriter();
        SerializerConfig strictConfig = new SerializerConfig.Builder().strictMode(true).validateURIs(true).build();
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
        // Given
        Model mockModel = mock(Model.class);
        Statement mockStatement = mock(Statement.class);
        IRI mockSubject = mock(IRI.class);
        IRI mockPredicate = mock(IRI.class);
        Literal mockObject = mock(Literal.class);


        when(mockSubject.stringValue()).thenReturn("http://example.org/book/1");
        when(mockSubject.isIRI()).thenReturn(true);
        when(mockSubject.isResource()).thenReturn(true);
        when(mockSubject.isBNode()).thenReturn(false);
        when(mockSubject.isLiteral()).thenReturn(false);


        when(mockPredicate.stringValue()).thenReturn("http://example.org/properties/description");
        when(mockPredicate.isIRI()).thenReturn(true);
        when(mockPredicate.isResource()).thenReturn(true);
        when(mockPredicate.isBNode()).thenReturn(false);
        when(mockPredicate.isLiteral()).thenReturn(false);

        String multilineText = "This is the first line.\nThis is the second line.";
        when(mockObject.stringValue()).thenReturn(multilineText);
        when(mockObject.isLiteral()).thenReturn(true);
        when(mockObject.isIRI()).thenReturn(false);
        when(mockObject.isResource()).thenReturn(false);
        when(mockObject.isBNode()).thenReturn(false);
        when(mockObject.getLanguage()).thenReturn(Optional.empty());
        when(mockObject.getDatatype()).thenReturn(null);

        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getObject()).thenReturn(mockObject);
        when(mockStatement.getContext()).thenReturn(null);

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));

        StringWriter writer = new StringWriter();
        SerializerConfig config = new SerializerConfig.Builder()
                .useMultilineLiterals(true)
                .prettyPrint(true)
                .autoDeclarePrefixes(true)
                .build();
        TriGSerializer triGSerializer = new TriGSerializer(mockModel, config);


        triGSerializer.write(writer);


        verify(mockModel, times(2)).stream();
        verify(mockSubject, atLeastOnce()).stringValue();
        verify(mockSubject, atLeastOnce()).isIRI();
        verify(mockPredicate, atLeastOnce()).stringValue();
        verify(mockPredicate, atLeastOnce()).isIRI();
        verify(mockObject, atLeastOnce()).stringValue();
        verify(mockObject, atLeastOnce()).isLiteral();

        String expected = """
                @prefix book: <http://example.org/book/> .
                @prefix properties: <http://example.org/properties/> .
                
                  book:1 properties:description \"\"\"This is the first line.
                This is the second line.\"\"\" .
                """;

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
        // Given
        Model mockModel = mock(Model.class);
        Statement mockStatement = mock(Statement.class);
        IRI mockSubject = mock(IRI.class);
        IRI mockPredicate = mock(IRI.class);
        Literal mockObject = mock(Literal.class);
        IRI mockContext = mock(IRI.class);


        when(mockSubject.stringValue()).thenReturn("http://example.org/data/person1");
        when(mockSubject.isIRI()).thenReturn(true);
        when(mockSubject.isResource()).thenReturn(true);
        when(mockSubject.isBNode()).thenReturn(false);
        when(mockSubject.isLiteral()).thenReturn(false);


        when(mockPredicate.stringValue()).thenReturn("http://example.org/data/name");
        when(mockPredicate.isIRI()).thenReturn(true);
        when(mockPredicate.isResource()).thenReturn(true);
        when(mockPredicate.isBNode()).thenReturn(false);
        when(mockPredicate.isLiteral()).thenReturn(false);


        when(mockObject.stringValue()).thenReturn("Alice");
        when(mockObject.isLiteral()).thenReturn(true);
        when(mockObject.isIRI()).thenReturn(false);
        when(mockObject.isResource()).thenReturn(false);
        when(mockObject.isBNode()).thenReturn(false);
        when(mockObject.getLanguage()).thenReturn(Optional.empty());
        when(mockObject.getDatatype()).thenReturn(null);


        when(mockContext.stringValue()).thenReturn("http://example.org/graph/g1");
        when(mockContext.isIRI()).thenReturn(true);
        when(mockContext.isResource()).thenReturn(true);
        when(mockContext.isBNode()).thenReturn(false);
        when(mockContext.isLiteral()).thenReturn(false);


        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getObject()).thenReturn(mockObject);
        when(mockStatement.getContext()).thenReturn(mockContext);

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));

        StringWriter writer = new StringWriter();

        TriGSerializer triGSerializer = new TriGSerializer(mockModel, SerializerConfig.trigConfig());


        triGSerializer.write(writer);


        verify(mockModel, times(2)).stream();
        verify(mockSubject, atLeastOnce()).stringValue();
        verify(mockSubject, atLeastOnce()).isIRI();
        verify(mockPredicate, atLeastOnce()).stringValue();
        verify(mockPredicate, atLeastOnce()).isIRI();
        verify(mockObject, atLeastOnce()).stringValue();
        verify(mockObject, atLeastOnce()).isLiteral();
        verify(mockContext, atLeastOnce()).stringValue();
        verify(mockContext, atLeastOnce()).isIRI();

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
