package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.IRdfSerializer;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.impl.common.serialization.config.FormatConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockConstruction;

class DefaultSerializerFactoryTest {

    private DefaultSerializerFactory factory;
    private Model mockModel;
    private FormatConfig mockConfig;

    @BeforeEach
    void setUp() {
        factory = new DefaultSerializerFactory();

        mockModel = org.mockito.Mockito.mock(Model.class);
        mockConfig = org.mockito.Mockito.mock(FormatConfig.class);
    }

    @Test
    @DisplayName("createSerializer should return TurtleFormat for TURTLE format")
    void createSerializer_shouldReturnTurtleFormat_forTurtleFormat() {
        try (MockedConstruction<TurtleSerializer> mockedConstruction = mockConstruction(TurtleSerializer.class)) {
            IRdfSerializer serializer = factory.createSerializer(RdfFormat.TURTLE, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof TurtleSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "TurtleFormat constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return NTriplesFormat for NTRIPLES format")
    void createSerializer_shouldReturnNTriplesFormat_forNTriplesFormat() {
        try (MockedConstruction<NTriplesSerializer> mockedConstruction = mockConstruction(NTriplesSerializer.class)) {
            IRdfSerializer serializer = factory.createSerializer(RdfFormat.NTRIPLES, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof NTriplesSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "NTriplesFormat constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return NQuadsFormat for NQUADS format")
    void createSerializer_shouldReturnNQuadsFormat_forNQuadsFormat() {
        try (MockedConstruction<NQuadsSerializer> mockedConstruction = mockConstruction(NQuadsSerializer.class)) {
            IRdfSerializer serializer = factory.createSerializer(RdfFormat.NQUADS, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof NQuadsSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "NQuadsFormat constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return TriGFormat for TRIG format")
    void createSerializer_shouldReturnTriGFormat_forTriGFormat() {
        try (MockedConstruction<TriGSerializer> mockedConstruction = mockConstruction(TriGSerializer.class)) {
            IRdfSerializer serializer = factory.createSerializer(RdfFormat.TRIG, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof TriGSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "TriGFormat constructor should be called once");
        }
    }

    @Test
    @DisplayName("createSerializer should return XmlSerializer for RDFXML format")
    void createSerializer_shouldReturnXmlSerializer_forRdfXmlFormat() {
        try (MockedConstruction<XmlSerializer> mockedConstruction = mockConstruction(XmlSerializer.class)) {
            IRdfSerializer serializer = factory.createSerializer(RdfFormat.RDFXML, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof XmlSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "XmlSerializer constructor should be called once");
        }
    }


    @Test
    @DisplayName("createSerializer should throw NullPointerException for null format")
    void createSerializer_shouldThrowNPE_forNullFormat() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(null, mockModel, mockConfig),
                "Should throw NullPointerException for null RdfFormat");
    }

    @Test
    @DisplayName("createSerializer should throw NullPointerException for null model")
    void createSerializer_shouldThrowNPE_forNullModel() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(RdfFormat.TURTLE, null, mockConfig),
                "Should throw NullPointerException for null Model");
    }

    @Test
    @DisplayName("createSerializer should throw NullPointerException for null config")
    void createSerializer_shouldThrowNPE_forNullConfig() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(RdfFormat.TURTLE, mockModel, null),
                "Should throw NullPointerException for null FormatConfig");
    }


}
