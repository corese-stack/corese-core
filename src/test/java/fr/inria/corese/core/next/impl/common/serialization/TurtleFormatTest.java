package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.serialization.config.FormatConfig;
import fr.inria.corese.core.next.impl.common.serialization.config.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.common.util.SerializationConstants;
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
 * Test class for {@link TurtleFormat} using Mockito to verify serialization behavior
 * under various configurations and RDF graph structures.
 */
class TurtleFormatTest {

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
        // Given
        Model mockModel = mock(Model.class);
        Statement mockStatement = mock(Statement.class);
        IRI mockSubject = mock(IRI.class);
        IRI mockPredicate = mock(IRI.class);
        Literal mockObject = mock(Literal.class);

        // Configure mocks for the subject
        when(mockSubject.stringValue()).thenReturn("http://example.org/ns/person1");
        when(mockSubject.isIRI()).thenReturn(true);
        when(mockSubject.isResource()).thenReturn(true);
        when(mockSubject.isBNode()).thenReturn(false);
        when(mockSubject.isLiteral()).thenReturn(false);

        // Configure mocks for the predicate
        when(mockPredicate.stringValue()).thenReturn("http://example.org/ns/hasName");
        when(mockPredicate.isIRI()).thenReturn(true);
        when(mockPredicate.isResource()).thenReturn(true);
        when(mockPredicate.isBNode()).thenReturn(false);
        when(mockPredicate.isLiteral()).thenReturn(false);

        // Configure mocks for the object (literal)
        when(mockObject.stringValue()).thenReturn("John Doe");
        when(mockObject.isLiteral()).thenReturn(true);
        when(mockObject.isIRI()).thenReturn(false);
        when(mockObject.isResource()).thenReturn(false);
        when(mockObject.isBNode()).thenReturn(false);
        when(mockObject.getLanguage()).thenReturn(Optional.empty());
        when(mockObject.getDatatype()).thenReturn(null);

        // Configure the statement
        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getObject()).thenReturn(mockObject);
        when(mockStatement.getContext()).thenReturn(null);

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));

        StringWriter writer = new StringWriter();
        TurtleFormat turtleFormat = new TurtleFormat(mockModel, FormatConfig.turtleConfig());

        // When
        turtleFormat.write(writer);

        // Then
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
        // Given
        Model mockModel = mock(Model.class);
        Statement mockStatement = mock(Statement.class);
        IRI mockSubject = mock(IRI.class);
        IRI mockPredicate = mock(IRI.class);
        IRI mockObject = mock(IRI.class);

        // Configure mocks
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
        TurtleFormat turtleFormat = new TurtleFormat(mockModel);

        // When
        turtleFormat.write(writer);

        // Then
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
        when(mockRdfLangString.stringValue()).thenReturn(SerializationConstants.RDF_LANGSTRING);
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

        // Explicitly create FormatConfig to ensure strictMode is false
        Map<String, String> commonTurtlePrefixes = new HashMap<>();
        commonTurtlePrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        commonTurtlePrefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        commonTurtlePrefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        commonTurtlePrefixes.put("owl", "http://www.w3.org/2002/07/owl#");

        FormatConfig config = new FormatConfig.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.MINIMAL)
                .useRdfTypeShortcut(true)
                .useCollections(true)
                .groupBySubject(true)
                .prettyPrint(true)
                .indent("  ")
                .lineEnding("\n")
                .addCustomPrefixes(commonTurtlePrefixes)
                .autoDeclarePrefixes(true)
                .trailingDot(true)
                .strictMode(false)
                .build();
        TurtleFormat turtleFormat = new TurtleFormat(mockModel, config);

        // When
        turtleFormat.write(writer);

        // Then
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

        Map<String, String> commonTurtlePrefixes = new HashMap<>();
        commonTurtlePrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        commonTurtlePrefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        commonTurtlePrefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        commonTurtlePrefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        commonTurtlePrefixes.put("dc", "http://purl.org/dc/elements/1.1/");

        FormatConfig config = new FormatConfig.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED)
                .addCustomPrefixes(commonTurtlePrefixes)
                .usePrefixes(true)
                .autoDeclarePrefixes(true)
                .build();
        TurtleFormat turtleFormat = new TurtleFormat(mockModel, config);

        // When
        turtleFormat.write(writer);

        // Then
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
     * Tests serialization of a blank node subject using the default anonymous style (`[]`).
     * Verifies that the blank node is serialized inline with its properties.
     *
     * @throws SerializationException if a serialization error occurs.
     * @throws IOException            if an I/O error occurs during writing.
     */
    @Test
    void testBlankNodeSerialization() throws SerializationException, IOException {
        Model mockModel = mock(Model.class);

        Resource mockBNode = mock(Resource.class);
        IRI mockBNodeProperty = mock(IRI.class);
        Literal mockBNodeObject = mock(Literal.class);


        IRI mockMainSubject = mock(IRI.class);
        IRI mockMainPredicate = mock(IRI.class);
        Statement mainStatement = mock(Statement.class);


        Statement bNodePropertyStatement = mock(Statement.class);


        when(mockBNode.stringValue()).thenReturn("b1");
        when(mockBNode.isBNode()).thenReturn(true);
        when(mockBNode.isResource()).thenReturn(true);
        when(mockBNode.isIRI()).thenReturn(false);
        when(mockBNode.isLiteral()).thenReturn(false);

        when(mockBNodeProperty.stringValue()).thenReturn("http://example.org/ns/hasValue");
        when(mockBNodeProperty.isIRI()).thenReturn(true);
        when(mockBNodeProperty.isResource()).thenReturn(true);
        when(mockBNodeProperty.isBNode()).thenReturn(false);
        when(mockBNodeProperty.isLiteral()).thenReturn(false);

        when(mockBNodeObject.stringValue()).thenReturn("Value of BNode");
        when(mockBNodeObject.isLiteral()).thenReturn(true);
        when(mockBNodeObject.isIRI()).thenReturn(false);
        when(mockBNodeObject.isResource()).thenReturn(false);
        when(mockBNodeObject.isBNode()).thenReturn(false);
        when(mockBNodeObject.getLanguage()).thenReturn(Optional.empty());
        when(mockBNodeObject.getDatatype()).thenReturn(null);


        when(bNodePropertyStatement.getSubject()).thenReturn(mockBNode);
        when(bNodePropertyStatement.getPredicate()).thenReturn(mockBNodeProperty);
        when(bNodePropertyStatement.getObject()).thenReturn(mockBNodeObject);
        when(bNodePropertyStatement.getContext()).thenReturn(null);


        when(mockMainSubject.stringValue()).thenReturn("http://example.org/ns/mainSubject");
        when(mockMainSubject.isIRI()).thenReturn(true);
        when(mockMainSubject.isResource()).thenReturn(true);
        when(mockMainSubject.isBNode()).thenReturn(false);
        when(mockMainSubject.isLiteral()).thenReturn(false);

        when(mockMainPredicate.stringValue()).thenReturn("http://example.org/ns/refersTo");
        when(mockMainPredicate.isIRI()).thenReturn(true);
        when(mockMainPredicate.isResource()).thenReturn(true);
        when(mockMainPredicate.isBNode()).thenReturn(false);
        when(mockMainPredicate.isLiteral()).thenReturn(false);

        when(mainStatement.getSubject()).thenReturn(mockMainSubject);
        when(mainStatement.getPredicate()).thenReturn(mockMainPredicate);
        when(mainStatement.getObject()).thenReturn(mockBNode);
        when(mainStatement.getContext()).thenReturn(null);


        when(mockModel.iterator()).thenAnswer(invocation -> Arrays.asList(mainStatement, bNodePropertyStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mainStatement, bNodePropertyStatement))
                .thenReturn(Stream.of(mainStatement, bNodePropertyStatement))
                .thenReturn(Stream.of(mainStatement, bNodePropertyStatement))
                .thenReturn(Stream.of(mainStatement, bNodePropertyStatement))
                .thenReturn(Stream.of(mainStatement, bNodePropertyStatement));


        StringWriter writer = new StringWriter();
        TurtleFormat turtleFormat = new TurtleFormat(mockModel, FormatConfig.turtleConfig());

        turtleFormat.write(writer);


        verify(mockModel, times(5)).stream();

        String expected = """
                @prefix ns: <http://example.org/ns/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                
                  ns:mainSubject ns:refersTo [
                    ns:hasValue "Value of BNode"
                  ] .
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

        Map<String, String> commonTurtlePrefixes = new HashMap<>();
        commonTurtlePrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        commonTurtlePrefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        commonTurtlePrefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        commonTurtlePrefixes.put("owl", "http://www.w3.org/2002/07/owl#");
        commonTurtlePrefixes.put("base", "http://example.org/base/");

        FormatConfig configWithBase = new FormatConfig.Builder()
                .baseIRI("http://example.org/base/")
                .addCustomPrefixes(commonTurtlePrefixes)
                .usePrefixes(true)
                .autoDeclarePrefixes(true)
                .build();
        TurtleFormat turtleFormat = new TurtleFormat(mockModel, configWithBase);

        turtleFormat.write(writer);

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
        TurtleFormat turtleFormat = new TurtleFormat(emptyModel);


        turtleFormat.write(writer);


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
        when(mockRdfLangStringDatatype.stringValue()).thenReturn(SerializationConstants.RDF_LANGSTRING);
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
        FormatConfig strictConfig = new FormatConfig.Builder().strictMode(true).build();
        TurtleFormat turtleFormat = new TurtleFormat(mockModel, strictConfig);


        SerializationException thrown = assertThrows(SerializationException.class, () -> {
            turtleFormat.write(writer);
        });

        assertEquals("Turtle", thrown.getFormatName());

        assertEquals("Invalid data for Turtle format: An rdf:langString literal must have a language tag. [Format: Turtle]", thrown.getMessage());
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
        IRI mockObject = mock(IRI.class); // Invalid IRI


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


        when(mockObject.stringValue()).thenReturn("http://example.org/invalid iri"); // Contains a space
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
        FormatConfig strictConfig = new FormatConfig.Builder().strictMode(true).validateURIs(true).build();
        TurtleFormat turtleFormat = new TurtleFormat(mockModel, strictConfig);


        SerializationException thrown = assertThrows(SerializationException.class, () -> {
            turtleFormat.write(writer);
        });

        assertEquals("Turtle", thrown.getFormatName());

        assertEquals("Invalid data for Turtle format: IRI contains illegal characters (space, quotes, angle brackets) for unescaped Turtle form: http://example.org/invalid iri [Format: Turtle]", thrown.getMessage());
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
        when(mockObject.getDatatype()).thenReturn(null); // Assuming no specific datatype for simplicity

        // Configure the statement
        when(mockStatement.getSubject()).thenReturn(mockSubject);
        when(mockStatement.getPredicate()).thenReturn(mockPredicate);
        when(mockStatement.getObject()).thenReturn(mockObject);
        when(mockStatement.getContext()).thenReturn(null);

        when(mockModel.iterator()).thenAnswer(invocation -> Collections.singletonList(mockStatement).iterator());
        when(mockModel.stream())
                .thenReturn(Stream.of(mockStatement))
                .thenReturn(Stream.of(mockStatement));

        StringWriter writer = new StringWriter();
        // Configure FormatConfig to enable multiline literals
        FormatConfig config = new FormatConfig.Builder()
                .useMultilineLiterals(true)
                .prettyPrint(true)
                .autoDeclarePrefixes(true)
                .build();
        TurtleFormat turtleFormat = new TurtleFormat(mockModel, config);

        // When
        turtleFormat.write(writer);

        // Then
        verify(mockModel, times(2)).stream();
        verify(mockSubject, atLeastOnce()).stringValue();
        verify(mockSubject, atLeastOnce()).isIRI();
        verify(mockPredicate, atLeastOnce()).stringValue();
        verify(mockPredicate, atLeastOnce()).isIRI();
        verify(mockObject, atLeastOnce()).stringValue();
        verify(mockObject, atLeastOnce()).isLiteral();

        // Corrected expected string
        String expected = """
                @prefix book: <http://example.org/book/> .
                @prefix properties: <http://example.org/properties/> .
                
                  book:1 properties:description \"\"\"This is the first line.
                This is the second line.\"\"\" .
                """;

        String actual = writer.toString().replace("\r\n", "\n");
        assertEquals(expected, actual);
    }
}
