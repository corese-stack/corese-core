package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.RdfSerializer;
import fr.inria.corese.core.next.api.SerializationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

/**
 * Unit tests for the {@link DefaultSerializerFactory} class.
 * This class verifies that the factory correctly creates instances of
 * various {@link RdfSerializer} implementations based on the provided
 * {@link RdfFormat} and handles null inputs gracefully.
 */
class DefaultSerializerFactoryTest {

    private DefaultSerializerFactory factory;
    private Model mockModel;
    private SerializationConfig mockConfig;


    @BeforeEach
    void setUp() {
        factory = new DefaultSerializerFactory();
        mockModel = mock(Model.class);
        mockConfig = mock(SerializationConfig.class);
    }

    @Test
    @DisplayName("createSerializer should return TurtleSerializer for TURTLE format")
    void createSerializer_shouldReturnTurtleSerializer_forTurtleFormat() {
        try (MockedConstruction<TurtleSerializer> mockedConstruction = mockConstruction(TurtleSerializer.class)) {
            RdfSerializer serializer = factory.createSerializer(RdfFormat.TURTLE, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof TurtleSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "TurtleSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return NTriplesSerializer for NTRIPLES format")
    void createSerializer_shouldReturnNTriplesSerializer_forNTriplesFormat() {
        try (MockedConstruction<NTriplesSerializer> mockedConstruction = mockConstruction(NTriplesSerializer.class)) {
            RdfSerializer serializer = factory.createSerializer(RdfFormat.NTRIPLES, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof NTriplesSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "NTriplesSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return NQuadsSerializer for NQUADS format")
    void createSerializer_shouldReturnNQuadsSerializer_forNQuadsFormat() {
        try (MockedConstruction<NQuadsSerializer> mockedConstruction = mockConstruction(NQuadsSerializer.class)) {
            RdfSerializer serializer = factory.createSerializer(RdfFormat.NQUADS, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof NQuadsSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "NQuadsSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return TriGSerializer for TRIG format")
    void createSerializer_shouldReturnTriGSerializer_forTriGFormat() {
        try (MockedConstruction<TriGSerializer> mockedConstruction = mockConstruction(TriGSerializer.class)) {
            RdfSerializer serializer = factory.createSerializer(RdfFormat.TRIG, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof TriGSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "TriGSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return XmlSerializer for RDFXML format")
    void createSerializer_shouldReturnXmlSerializer_forRdfXmlFormat() {
        try (MockedConstruction<XmlSerializer> mockedConstruction = mockConstruction(XmlSerializer.class)) {
            RdfSerializer serializer = factory.createSerializer(RdfFormat.RDFXML, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof XmlSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "XmlSerializer constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should throw NullPointerException for a null format")
    void createSerializer_shouldThrowNPE_forNullFormat() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(null, mockModel, mockConfig),
                "Should throw NullPointerException for null RdfFormat");
    }

    @Test
    @DisplayName("createSerializer should throw NullPointerException for a null model")
    void createSerializer_shouldThrowNPE_forNullModel() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(RdfFormat.TURTLE, null, mockConfig),
                "Should throw NullPointerException for null Model");
    }

    @Test
    @DisplayName("createSerializer should throw NullPointerException for a null config")
    void createSerializer_shouldThrowNPE_forNullConfig() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(RdfFormat.TURTLE, mockModel, null),
                "Should throw NullPointerException for null SerializationConfig");
    }
}
