package fr.inria.corese.core.next.data.impl.io.serialization;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.base.io.RDFFormat;
import fr.inria.corese.core.next.data.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.data.impl.io.common.JSONLDOptions;
import fr.inria.corese.core.next.data.impl.io.serialization.SerializerFactory;
import fr.inria.corese.core.next.data.impl.io.serialization.canonical.RDFC10Serializer;
import fr.inria.corese.core.next.data.impl.io.serialization.canonical.RDFC10SerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serialization.jsonld.JSONLDSerializer;
import fr.inria.corese.core.next.data.impl.io.serialization.nquads.NQuadsSerializer;
import fr.inria.corese.core.next.data.impl.io.serialization.nquads.NQuadsSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serialization.ntriples.NTriplesSerializer;
import fr.inria.corese.core.next.data.impl.io.serialization.ntriples.NTriplesSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serialization.rdfxml.RDFXMLSerializer;
import fr.inria.corese.core.next.data.impl.io.serialization.rdfxml.RDFXMLSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serialization.trig.TriGSerializer;
import fr.inria.corese.core.next.data.impl.io.serialization.trig.TriGSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serialization.turtle.TurtleSerializer;
import fr.inria.corese.core.next.data.impl.io.serialization.turtle.TurtleSerializerOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

/**
 * Unit tests for the {@link SerializerFactory} class.
 * This class verifies that the factory correctly creates instances of
 * various {@link RDFSerializer} implementations based on the provided
 * {@link RDFFormat} and handles null inputs gracefully.
 */
class SerializerFactoryTest {

    private SerializerFactory factory;
    private Model mockModel;

    @BeforeEach
    void setUp() {
        factory = new SerializerFactory();
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
    @DisplayName("Should accept cross-use of config objects")
    void configCrossUse() {
        JSONLDOptions jsonldOptions = new JSONLDOptions.Builder().build();
        NQuadsSerializerOptions nQuadsSerializerOptions = new NQuadsSerializerOptions.Builder().build();
        NTriplesSerializerOptions nTriplesSerializerOptions = new NTriplesSerializerOptions.Builder().build();
        RDFXMLSerializerOptions rdfxmlSerializerOptions = new RDFXMLSerializerOptions.Builder().build();
        TriGSerializerOptions triGSerializerOptions = new TriGSerializerOptions.Builder().build();
        TurtleSerializerOptions turtleSerializerOptions = new TurtleSerializerOptions.Builder().build();

        assertDoesNotThrow(() -> {
            // JSONLDOptions -- NQuads
            factory.createSerializer(RDFFormat.JSONLD, mockModel, nQuadsSerializerOptions);
        });
        assertDoesNotThrow(() -> {
            // NQuads -- NTriples
            factory.createSerializer(RDFFormat.NQUADS, mockModel, nTriplesSerializerOptions);
        });
        assertDoesNotThrow(() -> {
            // NTriples -- RDFXML
            factory.createSerializer(RDFFormat.NTRIPLES, mockModel, rdfxmlSerializerOptions);
        });
        assertDoesNotThrow(() -> {
            // RDFXML -- TriG
            factory.createSerializer(RDFFormat.RDFXML, mockModel, triGSerializerOptions);
        });
        assertDoesNotThrow(() -> {
            // TriG -- Turtle
            factory.createSerializer(RDFFormat.TRIG, mockModel, turtleSerializerOptions);
        });
        assertDoesNotThrow(() -> {
            // Turtle -- JSONLD
            factory.createSerializer(RDFFormat.TURTLE, mockModel, jsonldOptions);
        });
    }
}