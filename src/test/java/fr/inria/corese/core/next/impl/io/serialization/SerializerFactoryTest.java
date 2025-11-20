package fr.inria.corese.core.next.impl.io.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

import fr.inria.corese.core.next.api.io.IOOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.base.io.RDFFormat;
import fr.inria.corese.core.next.api.io.serialization.RDFSerializer;
import fr.inria.corese.core.next.impl.io.serialization.jsonld.JSONLDSerializer;
import fr.inria.corese.core.next.impl.io.serialization.canonical.RDFC10Serializer;
import fr.inria.corese.core.next.impl.io.serialization.nquads.NQuadsSerializer;
import fr.inria.corese.core.next.impl.io.serialization.ntriples.NTriplesSerializer;
import fr.inria.corese.core.next.impl.io.serialization.rdfxml.RDFXMLSerializer;
import fr.inria.corese.core.next.impl.io.serialization.trig.TriGSerializer;
import fr.inria.corese.core.next.impl.io.serialization.turtle.TurtleSerializer;

/**
 * Unit tests for the {@link SerializerFactory} class.
 * This class verifies that the factory correctly creates instances of
 * various {@link RDFSerializer} implementations based on the provided
 * {@link RDFFormat} and handles null inputs gracefully.
 */
class SerializerFactoryTest {

    private SerializerFactory factory;
    private Model mockModel;
    private IOOptions mockConfig;

    @BeforeEach
    void setUp() {
        factory = new SerializerFactory();
        mockModel = mock(Model.class);
        mockConfig = mock(IOOptions.class);
    }

    @Test
    @DisplayName("createSerializer should return TurtleSerializer for TURTLE format")
    void createSerializer_shouldReturnTurtleSerializer_forTurtleFormat() {
        try (MockedConstruction<TurtleSerializer> mockedConstruction = mockConstruction(TurtleSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.TURTLE, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof TurtleSerializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "TurtleSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return NTriplesSerializer for NTRIPLES format")
    void createSerializer_shouldReturnNTriplesSerializer_forNTriplesFormat() {
        try (MockedConstruction<NTriplesSerializer> mockedConstruction = mockConstruction(NTriplesSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.NTRIPLES, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof NTriplesSerializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "NTriplesSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return NQuadsSerializer for NQUADS format")
    void createSerializer_shouldReturnNQuadsSerializer_forNQuadsFormat() {
        try (MockedConstruction<NQuadsSerializer> mockedConstruction = mockConstruction(NQuadsSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.NQUADS, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof NQuadsSerializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "NQuadsSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return TriGSerializer for TRIG format")
    void createSerializer_shouldReturnTriGSerializer_forTriGFormat() {
        try (MockedConstruction<TriGSerializer> mockedConstruction = mockConstruction(TriGSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.TRIG, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof TriGSerializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "TriGSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return XmlSerializer for RDFXML format")
    void createSerializer_shouldReturnXmlSerializer_forRdfXmlFormat() {
        try (MockedConstruction<RDFXMLSerializer> mockedConstruction = mockConstruction(RDFXMLSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.RDFXML, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof RDFXMLSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "XmlSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return CanonicalSerializer for CANONICAL_RDF format")
    void createSerializer_shouldReturnCanonicalSerializer_forCanonicalRdfFormat() {
        try (MockedConstruction<RDFC10Serializer> mockedConstruction = mockConstruction(RDFC10Serializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.RDFC_1_0, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof RDFC10Serializer);
            assertEquals(1, mockedConstruction.constructed().size(), "CanonicalSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return JSONLDSerializer for JSONLD format")
    void createSerializer_shouldReturnJSONLDSerializer_forJSONLDFormat() {
        try (MockedConstruction<JSONLDSerializer> mockedConstruction = mockConstruction(JSONLDSerializer.class)) {
            RDFSerializer serializer = factory.createSerializer(RDFFormat.JSONLD, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof JSONLDSerializer);
            assertEquals(1, mockedConstruction.constructed().size(),
                    "JSONLDSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should throw NullPointerException for a null format")
    void createSerializer_shouldThrowNPE_forNullFormat() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(null, mockModel, mockConfig),
                "Should throw NullPointerException for null RDFFormat");
    }

    @Test
    @DisplayName("createSerializer should throw NullPointerException for a null model")
    void createSerializer_shouldThrowNPE_forNullModel() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(RDFFormat.TURTLE, null, mockConfig),
                "Should throw NullPointerException for null Model");
    }

    @Test
    @DisplayName("createSerializer should throw NullPointerException for a null config")
    void createSerializer_shouldThrowNPE_forNullConfig() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(RDFFormat.TURTLE, mockModel, null),
                "Should throw NullPointerException for null SerializationConfig");
    }
}
