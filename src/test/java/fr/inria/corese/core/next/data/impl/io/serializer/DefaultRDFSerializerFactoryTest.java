package fr.inria.corese.core.next.data.impl.io.serializer;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializerOptions;
import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.data.impl.io.jsonld.JSONLDOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfc10.RDFC10Serializer;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfc10.RDFC10SerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.jsonld.JSONLDSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.nquads.NQuadsSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.nquads.NQuadsSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.ntriples.NTriplesSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.ntriples.NTriplesSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfxml.RDFXMLSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfxml.RDFXMLSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.trig.TriGSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.trig.TriGSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.turtle.TurtleSerializer;
import fr.inria.corese.core.next.data.impl.io.serializer.turtle.TurtleSerializerOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

/**
 * Unit tests for the {@link DefaultRDFSerializerFactory} class.
 * This class verifies that the factory correctly creates instances of
 * various {@link RDFSerializer} implementations based on the provided
 * {@link RDFFormat} and handles null inputs gracefully.
 */
class DefaultRDFSerializerFactoryTest {

    private DefaultRDFSerializerFactory factory;
    private Model mockModel;

    @BeforeEach
    void setUp() {
        factory = new DefaultRDFSerializerFactory();
        mockModel = mock(Model.class);
    }

    @Test
    @DisplayName("createSerializer should return TurtleSerializer for TURTLE format")
    void createSerializer_shouldReturnTurtleSerializer_forTurtleFormat() {
        TurtleSerializerOptions config = TurtleSerializerOptions.defaultConfig();
        try (MockedConstruction<TurtleSerializer> mockedConstruction = mockConstruction(TurtleSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.TURTLE, mockModel, config);

            assertNotNull(serializer);
            assertInstanceOf(TurtleSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "TurtleSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return NTriplesSerializer for NTRIPLES format")
    void createSerializer_shouldReturnNTriplesSerializer_forNTriplesFormat() {
        NTriplesSerializerOptions config = NTriplesSerializerOptions.defaultConfig();
        try (MockedConstruction<NTriplesSerializer> mockedConstruction = mockConstruction(NTriplesSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.NTRIPLES, mockModel, config);

            assertNotNull(serializer);
            assertInstanceOf(NTriplesSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "NTriplesSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return NQuadsSerializer for NQUADS format")
    void createSerializer_shouldReturnNQuadsSerializer_forNQuadsFormat() {
        NQuadsSerializerOptions config = NQuadsSerializerOptions.defaultConfig();
        try (MockedConstruction<NQuadsSerializer> mockedConstruction = mockConstruction(NQuadsSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.NQUADS, mockModel, config);

            assertNotNull(serializer);
            assertInstanceOf(NQuadsSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "NQuadsSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return TriGSerializer for TRIG format")
    void createSerializer_shouldReturnTriGSerializer_forTriGFormat() {
        TriGSerializerOptions config = TriGSerializerOptions.defaultConfig();
        try (MockedConstruction<TriGSerializer> mockedConstruction = mockConstruction(TriGSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.TRIG, mockModel, config);

            assertNotNull(serializer);
            assertInstanceOf(TriGSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "TriGSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return XmlSerializer for RDFXML format")
    void createSerializer_shouldReturnXmlSerializer_forRdfXmlFormat() {
        RDFXMLSerializerOptions config = RDFXMLSerializerOptions.defaultConfig();
        try (MockedConstruction<RDFXMLSerializer> mockedConstruction = mockConstruction(RDFXMLSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.RDFXML, mockModel, config);

            assertNotNull(serializer);
            assertInstanceOf(RDFXMLSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(), "XmlSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return CanonicalSerializer for CANONICAL_RDF format")
    void createSerializer_shouldReturnCanonicalSerializer_forCanonicalRdfFormat() {
        RDFC10SerializerOptions config = RDFC10SerializerOptions.defaultConfig();
        try (MockedConstruction<RDFC10Serializer> mockedConstruction = mockConstruction(RDFC10Serializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.RDFC_1_0, mockModel, config);

            assertNotNull(serializer);
            assertInstanceOf(RDFC10Serializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(), "CanonicalSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return JSONLDSerializer for JSONLD format")
    void createSerializer_shouldReturnJSONLDSerializer_forJSONLDFormat() {
        JSONLDOptions config = new JSONLDOptions.Builder().build();
        try (MockedConstruction<JSONLDSerializer> mockedConstruction = mockConstruction(JSONLDSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.JSONLD, mockModel, config);

            assertNotNull(serializer);
            assertInstanceOf(JSONLDSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "JSONLDSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should throw NullPointerException for a null format")
    void createSerializer_shouldThrowNPE_forNullFormat() {
        TurtleSerializerOptions config = TurtleSerializerOptions.defaultConfig();
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(null, mockModel, config),
                "Should throw NullPointerException for null RDFFormat");
    }

    @Test
    @DisplayName("createSerializer should throw NullPointerException for a null model")
    void createSerializer_shouldThrowNPE_forNullModel() {
        TurtleSerializerOptions config = TurtleSerializerOptions.defaultConfig();
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(RDFFormat.TURTLE, null, config),
                "Should throw NullPointerException for null Model");
    }

    @Test
    @DisplayName("createSerializer should throw NullPointerException for a null config")
    void createSerializer_shouldThrowNPE_forNullConfig() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(RDFFormat.TURTLE, mockModel, null),
                "Should throw NullPointerException for null SerializationConfig");
    }

    @Test
    @DisplayName("createSerializer without config should return TurtleSerializer for TURTLE format")
    void createSerializerWithoutConfig_shouldReturnTurtleSerializer_forTurtleFormat() {
        try (MockedConstruction<TurtleSerializer> mockedConstruction = mockConstruction(TurtleSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.TURTLE, mockModel);

            assertNotNull(serializer);
            assertInstanceOf(TurtleSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "TurtleSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer without config should return NTriplesSerializer for NTRIPLES format")
    void createSerializerWithoutConfig_shouldReturnNTriplesSerializer_forNTriplesFormat() {
        try (MockedConstruction<NTriplesSerializer> mockedConstruction = mockConstruction(NTriplesSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.NTRIPLES, mockModel);

            assertNotNull(serializer);
            assertInstanceOf(NTriplesSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "NTriplesSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer without config should return NQuadsSerializer for NQUADS format")
    void createSerializerWithoutConfig_shouldReturnNQuadsSerializer_forNQuadsFormat() {
        try (MockedConstruction<NQuadsSerializer> mockedConstruction = mockConstruction(NQuadsSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.NQUADS, mockModel);

            assertNotNull(serializer);
            assertInstanceOf(NQuadsSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "NQuadsSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer without config should return TriGSerializer for TRIG format")
    void createSerializerWithoutConfig_shouldReturnTriGSerializer_forTriGFormat() {
        try (MockedConstruction<TriGSerializer> mockedConstruction = mockConstruction(TriGSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.TRIG, mockModel);

            assertNotNull(serializer);
            assertInstanceOf(TriGSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "TriGSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer without config should return XmlSerializer for RDFXML format")
    void createSerializerWithoutConfig_shouldReturnXmlSerializer_forRdfXmlFormat() {
        try (MockedConstruction<RDFXMLSerializer> mockedConstruction = mockConstruction(RDFXMLSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.RDFXML, mockModel);

            assertNotNull(serializer);
            assertInstanceOf(RDFXMLSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "XmlSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer without config should return CanonicalSerializer for CANONICAL_RDF format")
    void createSerializerWithoutConfig_shouldReturnCanonicalSerializer_forCanonicalRdfFormat() {
        try (MockedConstruction<RDFC10Serializer> mockedConstruction = mockConstruction(RDFC10Serializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.RDFC_1_0, mockModel);

            assertNotNull(serializer);
            assertInstanceOf(RDFC10Serializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "CanonicalSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer without config should return JSONLDSerializer for JSONLD format")
    void createSerializerWithoutConfig_shouldReturnJSONLDSerializer_forJSONLDFormat() {
        try (MockedConstruction<JSONLDSerializer> mockedConstruction = mockConstruction(JSONLDSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.JSONLD, mockModel);

            assertNotNull(serializer);
            assertInstanceOf(JSONLDSerializer.class, serializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "JSONLDSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer without config should throw NullPointerException for a null format")
    void createSerializerWithoutConfig_shouldThrowNPE_forNullFormat() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(null, mockModel),
                "Should throw NullPointerException for null RDFFormat");
    }

    @Test
    @DisplayName("createSerializer without config should throw NullPointerException for a null model")
    void createSerializerWithoutConfig_shouldThrowNPE_forNullModel() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(RDFFormat.TURTLE, null),
                "Should throw NullPointerException for null Model");
    }

    @Test
    @DisplayName("Should reject cross-use of format-specific config objects")
    void configCrossUse() {
        JSONLDOptions jsonldOptions = new JSONLDOptions.Builder().build();
        NQuadsSerializerOptions nQuadsSerializerOptions = new NQuadsSerializerOptions.Builder().build();
        NTriplesSerializerOptions nTriplesSerializerOptions = new NTriplesSerializerOptions.Builder().build();
        RDFXMLSerializerOptions rdfxmlSerializerOptions = new RDFXMLSerializerOptions.Builder().build();
        TriGSerializerOptions triGSerializerOptions = new TriGSerializerOptions.Builder().build();
        TurtleSerializerOptions turtleSerializerOptions = new TurtleSerializerOptions.Builder().build();

        assertThrows(SerializationException.class,
                () -> factory.createSerializer(RDFFormat.JSONLD, mockModel, nQuadsSerializerOptions));
        assertThrows(SerializationException.class,
                () -> factory.createSerializer(RDFFormat.NQUADS, mockModel, nTriplesSerializerOptions));
        assertThrows(SerializationException.class,
                () -> factory.createSerializer(RDFFormat.NTRIPLES, mockModel, rdfxmlSerializerOptions));
        assertThrows(SerializationException.class,
                () -> factory.createSerializer(RDFFormat.RDFXML, mockModel, triGSerializerOptions));
        assertThrows(SerializationException.class,
                () -> factory.createSerializer(RDFFormat.TRIG, mockModel, turtleSerializerOptions));
        assertThrows(SerializationException.class,
                () -> factory.createSerializer(RDFFormat.TURTLE, mockModel, jsonldOptions));
    }

    @Test
    void sharedPublicOptionsAreAdaptedToSupportedFormats() {
        RDFSerializerOptions options = RDFSerializerOptions.builder().prettyPrint(false).build();

        assertDoesNotThrow(() -> factory.createSerializer(RDFFormat.TURTLE, mockModel, options));
        assertDoesNotThrow(() -> factory.createSerializer(RDFFormat.TRIG, mockModel, options));
        assertDoesNotThrow(() -> factory.createSerializer(RDFFormat.NTRIPLES, mockModel, options));
        assertDoesNotThrow(() -> factory.createSerializer(RDFFormat.NQUADS, mockModel, options));
        assertDoesNotThrow(() -> factory.createSerializer(RDFFormat.RDFXML, mockModel, options));
        assertDoesNotThrow(() -> factory.createSerializer(RDFFormat.JSONLD, mockModel, options));
        assertThrows(SerializationException.class,
                () -> factory.createSerializer(RDFFormat.RDFC_1_0, mockModel, options));
    }
}
