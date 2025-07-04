package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.common.serialization.config.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.common.serialization.config.PrefixOrderingEnum;
import fr.inria.corese.core.next.impl.common.serialization.config.XmlConfig;
import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.StringWriter;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the XmlSerializer class.
 */
class XmlSerializerTest {

    @Mock
    private Model mockModel;
    XmlConfig mockConfig;
    private TestStatementFactory factory;

    private StringWriter writer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        writer = new StringWriter();
        factory = new TestStatementFactory();

        mockConfig = XmlConfig.defaultConfig();
    }


    @Test
    @DisplayName("Should serialize a simple IRI triple with auto-declared namespaces")
    void shouldSerializeSimpleIriTriple() throws SerializationException {
        Statement stmt = factory.createStatement(
                factory.createIRI("http://example.org/subject"),
                factory.createIRI("http://xmlns.com/foaf/0.1/name"),
                factory.createIRI("http://example.org/object")
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        XmlConfig testConfig = new XmlConfig.Builder()
                .autoDeclarePrefixes(true)
                .usePrefixes(true)
                .addCustomPrefix("foaf", "http://xmlns.com/foaf/0.1/")
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();


        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);


        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:exampleorg="http://example.org/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
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
        Statement stmt = factory.createStatement(
                factory.createBlankNode("b1"),
                factory.createIRI("http://xmlns.com/foaf/0.1/name"),
                factory.createIRI("http://example.org/Alice")
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        XmlConfig testConfig = new XmlConfig.Builder()
                .stableBlankNodeIds(true)
                .addCustomPrefix("foaf", "http://xmlns.com/foaf/0.1/")
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:exampleorg="http://example.org/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
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
        Statement stmt = factory.createStatement(
                factory.createIRI("http://example.org/book"),
                factory.createIRI("http://purl.org/dc/elements/1.1/creator"),
                factory.createBlankNode("b2")
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        XmlConfig testConfig = new XmlConfig.Builder()
                .stableBlankNodeIds(true)
                .addCustomPrefix("dc", "http://purl.org/dc/elements/1.1/")
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);


        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:exampleorg="http://example.org/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
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
        Statement stmt = factory.createStatement(
                factory.createIRI("http://example.org/person"),
                factory.createIRI("http://xmlns.com/foaf/0.1/name"),
                factory.createLiteral("John Doe", factory.createIRI(SerializationConstants.XSD_STRING), null)
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        XmlConfig testConfig = new XmlConfig.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.MINIMAL)
                .addCustomPrefix("foaf", "http://xmlns.com/foaf/0.1/")
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:exampleorg="http://example.org/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
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
        Statement stmt = factory.createStatement(
                factory.createIRI("http://example.org/data"),
                factory.createIRI("http://example.org/vocabulary/value"),
                factory.createLiteral("123", factory.createIRI(SerializationConstants.XSD_INTEGER), null)
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        XmlConfig testConfig = new XmlConfig.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.MINIMAL)
                .addCustomPrefix("ex", "http://example.org/vocabulary/")
                .addCustomPrefix("xsd", "http://www.w3.org/2001/XMLSchema#")
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:ex="http://example.org/vocabulary/" xmlns:exampleorg="http://example.org/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
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
        Statement stmt = factory.createStatement(
                factory.createIRI("http://example.org/book"),
                factory.createIRI("http://purl.org/dc/elements/1.1/title"),
                factory.createLiteral("The Book", null, "en")
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        XmlConfig testConfig = new XmlConfig.Builder()
                .addCustomPrefix("dc", "http://purl.org/dc/elements/1.1/")
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:exampleorg="http://example.org/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
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
        Statement stmt1 = factory.createStatement(
                factory.createIRI("http://ex.org/s1"),
                factory.createIRI("http://ex.org/p1"),
                factory.createIRI("http://ex.org/o1")
        );
        Statement stmt2 = factory.createStatement(
                factory.createIRI("http://ex.com/s2"),
                factory.createIRI("http://ex.com/p2"),
                factory.createIRI("http://ex.com/o2")
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt1, stmt2));

        XmlConfig testConfig = new XmlConfig.Builder()
                .addCustomPrefix("exorg", "http://ex.org/")
                .addCustomPrefix("excom", "http://ex.com/")
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .sortSubjects(false)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);


        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:excom="http://ex.com/" xmlns:exorg="http://ex.org/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
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
        Statement stmt1 = factory.createStatement(
                factory.createIRI("http://ex.org/s1"),
                factory.createIRI("http://ex.org/p1"),
                factory.createIRI("http://ex.org/o1")
        );
        Statement stmt2 = factory.createStatement(
                factory.createIRI("http://ex.com/s2"),
                factory.createIRI("http://ex.com/p2"),
                factory.createIRI("http://ex.com/o2")
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt1, stmt2));

        XmlConfig testConfig = new XmlConfig.Builder()
                .addCustomPrefix("exorg", "http://ex.org/")
                .addCustomPrefix("excom", "http://ex.com/")
                .prefixOrdering(PrefixOrderingEnum.USAGE_ORDER)
                .sortSubjects(false)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
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

    @Test
    @DisplayName("Should sort subjects alphabetically")
    void shouldSortSubjectsAlphabetically() throws SerializationException {
        Statement stmt1 = factory.createStatement(
                factory.createIRI("http://ex.org/B"),
                factory.createIRI("http://ex.org/p"),
                factory.createIRI("http://ex.org/o")
        );
        Statement stmt2 = factory.createStatement(
                factory.createIRI("http://ex.org/A"),
                factory.createIRI("http://ex.org/p"),
                factory.createIRI("http://ex.org/o")
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt1, stmt2));

        XmlConfig testConfig = new XmlConfig.Builder()
                .sortSubjects(true)
                .addCustomPrefix("ex", "http://ex.org/")
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:ex="http://ex.org/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                  <rdf:Description rdf:about="http://ex.org/A">
                    <ex:p rdf:resource="http://ex.org/o"/>
                  </rdf:Description>
                  <rdf:Description rdf:about="http://ex.org/B">
                    <ex:p rdf:resource="http://ex.org/o"/>
                  </rdf:Description>
                </rdf:RDF>
                """;

        assertEquals(expected, writer.toString());
    }


    @Test
    @DisplayName("Should escape XML attribute values")
    void shouldEscapeXmlAttributeValues() throws SerializationException {
        Statement stmt = factory.createStatement(
                factory.createIRI("http://example.org/sub&ject<"),
                factory.createIRI("http://example.org/pred"),
                factory.createIRI("http://example.org/obj\"ect'")
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        XmlConfig testConfig = new XmlConfig.Builder()
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:exampleorg="http://example.org/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                  <rdf:Description rdf:about="http://example.org/sub&amp;ject&lt;">
                    <exampleorg:pred rdf:resource="http://example.org/obj&quot;ect&apos;"/>
                  </rdf:Description>
                </rdf:RDF>
                """;

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Should escape XML content values")
    void shouldEscapeXmlContentValues() throws SerializationException {
        Statement stmt = factory.createStatement(
                factory.createIRI("http://example.org/item"),
                factory.createIRI("http://example.org/prop"),
                factory.createLiteral("Value with <tags> & entities", null, null)
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        XmlConfig testConfig = new XmlConfig.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED)
                .addCustomPrefix("ex", "http://example.org/")
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();


        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:ex="http://example.org/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                  <rdf:Description rdf:about="http://example.org/item">
                    <ex:prop>Value with &lt;tags&gt; &amp; entities</ex:prop>
                  </rdf:Description>
                </rdf:RDF>
                """;

        assertEquals(expected, writer.toString());
    }


    @Test
    @DisplayName("Should not auto-declare prefixes if disabled in configuration")
    void shouldNotAutoDeclarePrefixesIfDisabled() throws SerializationException {
        Statement stmt = factory.createStatement(
                factory.createIRI("http://example.org/subject"),
                factory.createIRI("http://xmlns.com/foaf/0.1/name"),
                factory.createIRI("http://example.org/object")
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        XmlConfig testConfig = new XmlConfig.Builder()
                .autoDeclarePrefixes(false)
                .usePrefixes(true)
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                  <rdf:Description rdf:about="http://example.org/subject">
                    <http://xmlns.com/foaf/0.1/name rdf:resource="http://example.org/object"/>
                  </rdf:Description>
                </rdf:RDF>
                """;

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Should not use prefixes if disabled in configuration")
    void shouldNotUsePrefixesIfDisabled() throws SerializationException {
        Statement stmt = factory.createStatement(
                factory.createIRI("http://example.org/subject"),
                factory.createIRI("http://xmlns.com/foaf/0.1/name"),
                factory.createIRI("http://example.org/object")
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt));

        XmlConfig testConfig = new XmlConfig.Builder()
                .usePrefixes(false)
                .autoDeclarePrefixes(true)
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about="http://example.org/subject">
                    <http://xmlns.com/foaf/0.1/name rdf:resource="http://example.org/object"/>
                  </rdf:Description>
                </rdf:RDF>
                """;

        assertEquals(expected, writer.toString());
    }


    @Test
    @DisplayName("Should not generate stable blank node IDs and sort subjects alphabetically")
    void shouldNotGenerateStableBlankNodeIds() throws SerializationException {
        Statement stmt1 = factory.createStatement(
                factory.createBlankNode("bnode-abc"),
                factory.createIRI("http://example.org/p"),
                factory.createIRI("http://example.org/o")
        );
        Statement stmt2 = factory.createStatement(
                factory.createIRI("http://example.org/s"),
                factory.createIRI("http://example.org/p"),
                factory.createBlankNode("bnode-xyz")
        );

        when(mockModel.stream()).thenReturn(Stream.of(stmt1, stmt2));

        XmlConfig testConfig = new XmlConfig.Builder()
                .stableBlankNodeIds(false)
                .sortSubjects(true)
                .addCustomPrefix("ex", "http://example.org/")
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);


        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:ex="http://example.org/" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                  <rdf:Description rdf:nodeID="bnode-abc">
                    <ex:p rdf:resource="http://example.org/o"/>
                  </rdf:Description>
                  <rdf:Description rdf:about="http://example.org/s">
                    <ex:p rdf:nodeID="bnode-xyz"/>
                  </rdf:Description>
                </rdf:RDF>
                """;

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("Should handle an empty model")
    void shouldHandleEmptyModel() throws SerializationException {
        when(mockModel.stream()).thenReturn(Stream.empty());

        XmlConfig testConfig = new XmlConfig.Builder()
                .prefixOrdering(PrefixOrderingEnum.ALPHABETICAL)
                .build();

        XmlSerializer serializer = new XmlSerializer(mockModel, testConfig);
        serializer.write(writer);

        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rdf:RDF xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#">
                </rdf:RDF>
                """;

        assertEquals(expected, writer.toString());
    }
}
