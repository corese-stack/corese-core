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
    @DisplayName("createSerializer devrait retourner TurtleSerializer pour le format TURTLE")
    void createSerializer_shouldReturnTurtleSerializer_forTurtleFormat() {
        try (MockedConstruction<TurtleSerializer> mockedConstruction = mockConstruction(TurtleSerializer.class)) {
            RdfSerializer serializer = factory.createSerializer(RdfFormat.TURTLE, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof TurtleSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "Le constructeur de TurtleSerializer devrait être appelé une fois");
        }
    }

    @Test
    @DisplayName("createSerializer devrait retourner NTriplesSerializer pour le format NTRIPLES")
    void createSerializer_shouldReturnNTriplesSerializer_forNTriplesFormat() {
        try (MockedConstruction<NTriplesSerializer> mockedConstruction = mockConstruction(NTriplesSerializer.class)) {
            RdfSerializer serializer = factory.createSerializer(RdfFormat.NTRIPLES, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof NTriplesSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "Le constructeur de NTriplesSerializer devrait être appelé une fois");
        }
    }

    @Test
    @DisplayName("createSerializer devrait retourner NQuadsSerializer pour le format NQUADS")
    void createSerializer_shouldReturnNQuadsSerializer_forNQuadsFormat() {
        try (MockedConstruction<NQuadsSerializer> mockedConstruction = mockConstruction(NQuadsSerializer.class)) {
            RdfSerializer serializer = factory.createSerializer(RdfFormat.NQUADS, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof NQuadsSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "Le constructeur de NQuadsSerializer devrait être appelé une fois");
        }
    }

    @Test
    @DisplayName("createSerializer devrait retourner TriGSerializer pour le format TRIG")
    void createSerializer_shouldReturnTriGSerializer_forTriGFormat() {
        try (MockedConstruction<TriGSerializer> mockedConstruction = mockConstruction(TriGSerializer.class)) {
            RdfSerializer serializer = factory.createSerializer(RdfFormat.TRIG, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof TriGSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "Le constructeur de TriGSerializer devrait être appelé une fois");
        }
    }

    @Test
    @DisplayName("createSerializer devrait retourner XmlSerializer pour le format RDFXML")
    void createSerializer_shouldReturnXmlSerializer_forRdfXmlFormat() {
        try (MockedConstruction<XmlSerializer> mockedConstruction = mockConstruction(XmlSerializer.class)) {
            RdfSerializer serializer = factory.createSerializer(RdfFormat.RDFXML, mockModel, mockConfig);

            assertNotNull(serializer);
            assertTrue(serializer instanceof XmlSerializer);
            assertEquals(1, mockedConstruction.constructed().size(), "Le constructeur de XmlSerializer devrait être appelé une fois");
        }
    }

    @Test
    @DisplayName("createSerializer devrait lever NullPointerException pour un format nul")
    void createSerializer_shouldThrowNPE_forNullFormat() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(null, mockModel, mockConfig),
                "Devrait lever NullPointerException pour RdfFormat nul");
    }

    @Test
    @DisplayName("createSerializer devrait lever NullPointerException pour un modèle nul")
    void createSerializer_shouldThrowNPE_forNullModel() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(RdfFormat.TURTLE, null, mockConfig),
                "Devrait lever NullPointerException pour Model nul");
    }

    @Test
    @DisplayName("createSerializer devrait lever NullPointerException pour une configuration nulle")
    void createSerializer_shouldThrowNPE_forNullConfig() {
        assertThrows(NullPointerException.class,
                () -> factory.createSerializer(RdfFormat.TURTLE, mockModel, null),
                "Devrait lever NullPointerException pour SerializationConfig nulle");
    }
}
