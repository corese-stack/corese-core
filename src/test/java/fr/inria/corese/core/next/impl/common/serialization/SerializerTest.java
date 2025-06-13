package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

import java.io.Writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

class SerializerTest {

    private Serializer serializer;

    @Mock
    private Model mockModel;
    @Mock
    private FormatConfig mockConfig;
    @Mock
    private Writer mockWriter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        serializer = new Serializer(mockModel, mockConfig);
    }

    // --- Tests des constructeurs ---

    @Test
    @DisplayName("Constructor should throw NullPointerException for null model")
    void constructorShouldThrowForNullModel() {
        assertThrows(NullPointerException.class, () -> new Serializer(null), "Model cannot be null");
        assertThrows(NullPointerException.class, () -> new Serializer(null, mockConfig), "Model cannot be null");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null config")
    void constructorShouldThrowForNullConfig() {
        assertThrows(NullPointerException.class, () -> new Serializer(mockModel, null), "FormatConfig cannot be null");
    }

    // --- Tests des arguments de la méthode serialize ---

    @Test
    @DisplayName("serialize should throw NullPointerException for null writer")
    void serializeShouldThrowForNullWriter() {
        assertThrows(NullPointerException.class, () -> serializer.serialize(null, RdfFormat.NTRIPLES), "Writer cannot be null");
    }

    @Test
    @DisplayName("serialize should throw NullPointerException for null format")
    void serializeShouldThrowForNullFormat() {
        assertThrows(NullPointerException.class, () -> serializer.serialize(mockWriter, null), "RdfFormat cannot be null");
    }

    // --- Tests de délégation de sérialisation ---

    @Test
    @DisplayName("serialize should delegate to NTriplesFormat for NTRIPLES format")
    void serializeShouldDelegateToNTriplesFormat() throws SerializationException {
        try (MockedConstruction<NTriplesFormat> mockedNtConstructor = mockConstruction(NTriplesFormat.class)) {
            serializer.serialize(mockWriter, RdfFormat.NTRIPLES);

            assertEquals(1, mockedNtConstructor.constructed().size(), "NTriplesFormat constructor should be called once");

            NTriplesFormat createdNtSerializer = mockedNtConstructor.constructed().get(0);

            verify(createdNtSerializer).write(mockWriter);
        }
    }

    @Test
    @DisplayName("serialize should delegate to NQuadsFormat for NQUADS format")
    void serializeShouldDelegateToNQuadsFormat() throws SerializationException {
        try (MockedConstruction<NQuadsFormat> mockedNqConstructor = mockConstruction(NQuadsFormat.class)) {
            serializer.serialize(mockWriter, RdfFormat.NQUADS);

            assertEquals(1, mockedNqConstructor.constructed().size(), "NQuadsFormat constructor should be called once");
            NQuadsFormat createdNqSerializer = mockedNqConstructor.constructed().get(0);

            verify(createdNqSerializer).write(mockWriter);
        }
    }


}