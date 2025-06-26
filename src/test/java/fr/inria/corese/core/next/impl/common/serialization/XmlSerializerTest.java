package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.serialization.config.FormatConfig;
import fr.inria.corese.core.next.impl.common.serialization.config.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.common.serialization.config.PrefixOrderingEnum;
import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the XmlSerializer class.
 */
class XmlSerializerTest {

    @Mock
    private Model mockModel;
    @Mock
    private FormatConfig mockConfig;
    @Mock
    private Resource mockResource;
    @Mock
    private IRI mockIRI;
    @Mock
    private Literal mockLiteral;
    @Mock
    private Statement mockStatement;

    private StringWriter writer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        writer = new StringWriter();


        when(mockConfig.getIndent()).thenReturn("  ");
        when(mockConfig.getLineEnding()).thenReturn("\n");
        when(mockConfig.usePrefixes()).thenReturn(true);
        when(mockConfig.autoDeclarePrefixes()).thenReturn(true);
        when(mockConfig.getCustomPrefixes()).thenReturn(new HashMap<>());
        when(mockConfig.getPrefixOrdering()).thenReturn(PrefixOrderingEnum.CUSTOM);
        when(mockConfig.sortSubjects()).thenReturn(false);
        when(mockConfig.sortPredicates()).thenReturn(false);
        when(mockConfig.getLiteralDatatypePolicy()).thenReturn(LiteralDatatypePolicyEnum.MINIMAL);
        when(mockConfig.stableBlankNodeIds()).thenReturn(true);
    }


    @Test
    @DisplayName("Should serialize a simple IRI triple with auto-declared namespaces")
    void shouldSerializeSimpleIriTriple() throws SerializationException {
        Statement stmt = createMockStatement("http://example.org/subject", "http://xmlns.com/foaf/0.1/name", "http://example.org/object");

        when(mockModel.stream()).thenReturn(Stream.of(stmt));


        Map<String, String> customPrefixes = new HashMap<>();

        customPrefixes.put("http://xmlns.com/foaf/0.1/", "foaf");
        when(mockConfig.getCustomPrefixes()).thenReturn(customPrefixes);

        when(mockConfig.getPrefixOrdering()).thenReturn(PrefixOrderingEnum.ALPHABETICAL);

        XmlSerializer serializer = new XmlSerializer(mockModel, mockConfig);
        serializer.write(writer);


        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:exampleorg="http://example.org/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about="http://example.org/subject">
                    <foaf:name rdf:resource="http://example.org/object"/>
                  </rdf:Description>
                </rdf:RDF>
                """;

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Should handle blank node subject")
    void shouldHandleBlankNodeSubject() throws SerializationException {
        Statement stmt = createMockBlankNodeSubjectStatement("b1", "http://xmlns.com/foaf/0.1/name", "http://example.org/Alice");

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        Map<String, String> customPrefixes = new HashMap<>();
        customPrefixes.put("http://xmlns.com/foaf/0.1/", "foaf");
        when(mockConfig.getCustomPrefixes()).thenReturn(customPrefixes);


        when(mockConfig.stableBlankNodeIds()).thenReturn(true);
        when(mockConfig.getPrefixOrdering()).thenReturn(PrefixOrderingEnum.ALPHABETICAL);

        XmlSerializer serializer = new XmlSerializer(mockModel, mockConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:exampleorg="http://example.org/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:nodeID="b0">
                    <foaf:name rdf:resource="http://example.org/Alice"/>
                  </rdf:Description>
                </rdf:RDF>
                """;


        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Should handle blank node object with correct namespace ordering")
    void shouldHandleBlankNodeObject() throws SerializationException {
        Statement stmt = createMockBlankNodeObjectStatement("http://example.org/book", "http://purl.org/dc/elements/1.1/creator", "b2");

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        Map<String, String> customPrefixes = new HashMap<>();
        customPrefixes.put("http://purl.org/dc/elements/1.1/", "dc");
        when(mockConfig.getCustomPrefixes()).thenReturn(customPrefixes);

        when(mockConfig.stableBlankNodeIds()).thenReturn(true);
        when(mockConfig.getPrefixOrdering()).thenReturn(PrefixOrderingEnum.ALPHABETICAL);

        XmlSerializer serializer = new XmlSerializer(mockModel, mockConfig);
        serializer.write(writer);


        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:exampleorg="http://example.org/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about="http://example.org/book">
                    <dc:creator rdf:nodeID="b0"/>
                  </rdf:Description>
                </rdf:RDF>
                """;

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Should serialize literal with xsd:string datatype (minimal policy)")
    void shouldSerializeLiteralWithStringDatatypeMinimalPolicy() throws SerializationException {
        Statement stmt = createMockLiteralStatement("http://example.org/person", "http://xmlns.com/foaf/0.1/name", "John Doe", SerializationConstants.XSD_STRING,
                null);

        when(mockModel.stream()).thenReturn(Stream.of(stmt));
        when(mockConfig.getLiteralDatatypePolicy()).thenReturn(LiteralDatatypePolicyEnum.MINIMAL);

        Map<String, String> customPrefixes = new HashMap<>();
        customPrefixes.put("http://xmlns.com/foaf/0.1/", "foaf");
        when(mockConfig.getCustomPrefixes()).thenReturn(customPrefixes);
        when(mockConfig.getPrefixOrdering()).thenReturn(PrefixOrderingEnum.ALPHABETICAL);

        XmlSerializer serializer = new XmlSerializer(mockModel, mockConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:exampleorg="http://example.org/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about="http://example.org/person">
                    <foaf:name>John Doe</foaf:name>
                  </rdf:Description>
                </rdf:RDF>
                """;
        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Should serialize literal with custom datatype using minimal policy")
    void shouldSerializeLiteralWithCustomDatatypeMinimalPolicy() throws SerializationException {
        Statement stmt = createMockLiteralStatement("http://example.org/data", "http://example.org/vocabulary/value", "123", SerializationConstants.XSD_INTEGER, null);

        when(mockModel.stream()).thenReturn(Stream.of(stmt));
        when(mockConfig.getLiteralDatatypePolicy()).thenReturn(LiteralDatatypePolicyEnum.MINIMAL);

        Map<String, String> customPrefixes = new HashMap<>();
        customPrefixes.put("http://example.org/vocabulary/", "ex");
        customPrefixes.put("http://www.w3.org/2001/XMLSchema#", "xsd");
        when(mockConfig.getCustomPrefixes()).thenReturn(customPrefixes);
        when(mockConfig.getPrefixOrdering()).thenReturn(PrefixOrderingEnum.ALPHABETICAL);

        XmlSerializer serializer = new XmlSerializer(mockModel, mockConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:ex="http://example.org/vocabulary/" xmlns:exampleorg="http://example.org/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                  <rdf:Description rdf:about="http://example.org/data">
                    <ex:value rdf:datatype="xsd:integer">123</ex:value>
                  </rdf:Description>
                </rdf:RDF>
                """;
        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Should serialize literal with language tag")
    void shouldSerializeLiteralWithLanguage() throws SerializationException {
        Statement stmt = createMockLiteralStatement("http://example.org/book", "http://purl.org/dc/elements/1.1/title", "The Book", null, "en");

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        Map<String, String> customPrefixes = new HashMap<>();
        customPrefixes.put("http://purl.org/dc/elements/1.1/", "dc");
        when(mockConfig.getCustomPrefixes()).thenReturn(customPrefixes);
        when(mockConfig.getPrefixOrdering()).thenReturn(PrefixOrderingEnum.ALPHABETICAL);

        XmlSerializer serializer = new XmlSerializer(mockModel, mockConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:exampleorg="http://example.org/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about="http://example.org/book">
                    <dc:title xml:lang="en">The Book</dc:title>
                  </rdf:Description>
                </rdf:RDF>
                """;

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Should respect alphabetical prefix ordering")
    void shouldRespectPrefixOrderingAlphabetical() throws SerializationException {
        Statement stmt1 = createMockStatement("http://ex.org/s1", "http://ex.org/p1", "http://ex.org/o1");
        Statement stmt2 = createMockStatement("http://ex.com/s2", "http://ex.com/p2", "http://ex.com/o2");

        when(mockModel.stream()).thenReturn(Stream.of(stmt1, stmt2));

        Map<String, String> customPrefixes = new HashMap<>();
        customPrefixes.put("http://ex.org/", "exorg");
        customPrefixes.put("http://ex.com/", "excom");
        when(mockConfig.getCustomPrefixes()).thenReturn(customPrefixes);
        when(mockConfig.getPrefixOrdering()).thenReturn(PrefixOrderingEnum.ALPHABETICAL);

        XmlSerializer serializer = new XmlSerializer(mockModel, mockConfig);
        serializer.write(writer);


        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:excom="http://ex.com/" xmlns:exorg="http://ex.org/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about="http://ex.com/s2">
                    <excom:p2 rdf:resource="http://ex.com/o2"/>
                  </rdf:Description>
                  <rdf:Description rdf:about="http://ex.org/s1">
                    <exorg:p1 rdf:resource="http://ex.org/o1"/>
                  </rdf:Description>
                </rdf:RDF>
                """;

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Should respect default prefix ordering (non-deterministic for subjects)")
    void shouldRespectPrefixOrderingDefault() throws SerializationException {
        Statement stmt1 = createMockStatement("http://ex.org/s1", "http://ex.org/p1", "http://ex.org/o1");
        Statement stmt2 = createMockStatement("http://ex.com/s2", "http://ex.com/p2", "http://ex.com/o2");

        when(mockModel.stream()).thenReturn(Stream.of(stmt1, stmt2));

        Map<String, String> customPrefixes = new HashMap<>();
        customPrefixes.put("http://ex.org/", "exorg");
        customPrefixes.put("http://ex.com/", "excom");
        when(mockConfig.getCustomPrefixes()).thenReturn(customPrefixes);
        when(mockConfig.getPrefixOrdering()).thenReturn(PrefixOrderingEnum.USAGE_ORDER);
        when(mockConfig.sortSubjects()).thenReturn(false);

        XmlSerializer serializer = new XmlSerializer(mockModel, mockConfig);
        serializer.write(writer);

        String actual = writer.toString();

        assertTrue(actual.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<rdf:RDF"));
        assertTrue(actual.endsWith("</rdf:RDF>\n"));

        assertTrue(actual.contains("xmlns:exorg=\"http://ex.org/\""));
        assertTrue(actual.contains("xmlns:excom=\"http://ex.com/\""));
        assertTrue(actual.contains("xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""));

        String desc1 = "  <rdf:Description rdf:about=\"http://ex.org/s1\">\n    <exorg:p1 rdf:resource=\"http://ex.org/o1\"/>\n  </rdf:Description>";
        String desc2 = "  <rdf:Description rdf:about=\"http://ex.com/s2\">\n    <excom:p2 rdf:resource=\"http://ex.com/o2\"/>\n  </rdf:Description>";

        assertTrue(actual.contains(desc1));
        assertTrue(actual.contains(desc2));
    }

    







    /**
     * Helper method to create a mock Statement with IRI subject, predicate, and object.
     * Completes all necessary stubbing for these mocks.
     */
    private Statement createMockStatement(String subjectUri, String predicateUri, String objectUri) {
        Resource subject = mock(Resource.class);
        when(subject.isIRI()).thenReturn(true);
        when(subject.isBNode()).thenReturn(false);
        when(subject.stringValue()).thenReturn(subjectUri);

        IRI predicate = mock(IRI.class);
        when(predicate.isIRI()).thenReturn(true);
        when(predicate.isBNode()).thenReturn(false);
        when(predicate.stringValue()).thenReturn(predicateUri);

        IRI object = mock(IRI.class);
        when(object.isIRI()).thenReturn(true);
        when(object.isBNode()).thenReturn(false);
        when(object.stringValue()).thenReturn(objectUri);

        Statement statement = mock(Statement.class);
        when(statement.getSubject()).thenReturn(subject);
        when(statement.getPredicate()).thenReturn(predicate);
        when(statement.getObject()).thenReturn(object);

        return statement;
    }

    /**
     * Helper method to create a mock Statement with a blank node subject.
     */
    private Statement createMockBlankNodeSubjectStatement(String blankNodeId, String predicateUri, String objectUri) {
        Resource subject = mock(Resource.class);
        when(subject.isIRI()).thenReturn(false);
        when(subject.isBNode()).thenReturn(true);
        when(subject.stringValue()).thenReturn("_:" + blankNodeId);

        IRI predicate = mock(IRI.class);
        when(predicate.isIRI()).thenReturn(true);
        when(predicate.stringValue()).thenReturn(predicateUri);

        IRI object = mock(IRI.class);
        when(object.isIRI()).thenReturn(true);
        when(object.stringValue()).thenReturn(objectUri);

        Statement statement = mock(Statement.class);
        when(statement.getSubject()).thenReturn(subject);
        when(statement.getPredicate()).thenReturn(predicate);
        when(statement.getObject()).thenReturn(object);

        return statement;
    }

    /**
     * Helper method to create a mock Statement with a blank node object.
     */
    private Statement createMockBlankNodeObjectStatement(String subjectUri, String predicateUri, String blankNodeId) {
        Resource subject = mock(Resource.class);
        when(subject.isIRI()).thenReturn(true);
        when(subject.stringValue()).thenReturn(subjectUri);

        IRI predicate = mock(IRI.class);
        when(predicate.isIRI()).thenReturn(true);
        when(predicate.stringValue()).thenReturn(predicateUri);

        Resource object = mock(Resource.class);
        when(object.isIRI()).thenReturn(false);
        when(object.isBNode()).thenReturn(true);
        when(object.stringValue()).thenReturn("_:" + blankNodeId);

        Statement statement = mock(Statement.class);
        when(statement.getSubject()).thenReturn(subject);
        when(statement.getPredicate()).thenReturn(predicate);
        when(statement.getObject()).thenReturn(object);

        return statement;
    }

    /**
     * Helper method to create a mock Statement with a literal object.
     */
    private Statement createMockLiteralStatement(String subjectUri, String predicateUri, String literalValue, String datatypeUri, String langTag) {
        Resource subject = mock(Resource.class);
        when(subject.isIRI()).thenReturn(true);
        when(subject.stringValue()).thenReturn(subjectUri);

        IRI predicate = mock(IRI.class);
        when(predicate.isIRI()).thenReturn(true);
        when(predicate.stringValue()).thenReturn(predicateUri);

        Literal literal = mock(Literal.class);
        when(literal.isIRI()).thenReturn(false);
        when(literal.isBNode()).thenReturn(false);
        when(literal.isLiteral()).thenReturn(true);
        when(literal.stringValue()).thenReturn(literalValue);
        when(literal.getLanguage()).thenReturn(Optional.ofNullable(langTag));
        if (datatypeUri != null) {
            IRI datatype = mock(IRI.class);
            when(datatype.stringValue()).thenReturn(datatypeUri);
            when(literal.getDatatype()).thenReturn(datatype);
        } else {
            when(literal.getDatatype()).thenReturn(null);
        }

        Statement statement = mock(Statement.class);
        when(statement.getSubject()).thenReturn(subject);
        when(statement.getPredicate()).thenReturn(predicate);
        when(statement.getObject()).thenReturn(literal);

        return statement;
    }

}
