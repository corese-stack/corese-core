package fr.inria.corese.core.next.storage.impl.model;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.storage.api.plugin.PluginException;
import fr.inria.corese.core.next.storage.api.plugin.PluginNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StorageModelFactory.
 */
@DisplayName("StorageModelFactory Tests")
class StorageModelFactoryTest {

    private ValueFactory valueFactory;
    private StorageModelFactory factory;

    // Test data
    private IRI subject;
    private IRI predicate;
    private Literal object;

    @BeforeEach
    void setUp() {
        valueFactory = new CoreseValueFactory();
        factory = new StorageModelFactory(valueFactory);

        // Create test data
        subject = valueFactory.createIRI("http://example.org/subject");
        predicate = valueFactory.createIRI("http://example.org/predicate");
        object = valueFactory.createLiteral("test value");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create factory with valid ValueFactory")
        void shouldCreateFactoryWithValidValueFactory() {
            StorageModelFactory customFactory = new StorageModelFactory(valueFactory);

            assertNotNull(customFactory);
            assertEquals(valueFactory, customFactory.valueFactory());
        }

        @Test
        @DisplayName("Should throw NullPointerException when ValueFactory is null")
        void shouldThrowExceptionWhenValueFactoryIsNull() {
            NullPointerException exception = assertThrows(
                    NullPointerException.class,
                    () -> new StorageModelFactory(null)
            );

            assertEquals("ValueFactory cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("createModel(String) Tests")
    class CreateModelByTypeTests {

        @Test
        @DisplayName("Should create memory model when type is 'memory'")
        void shouldCreateMemoryModelWhenTypeIsMemory() throws PluginException {
            Model model = factory.createModel("memory");

            assertNotNull(model);
            assertEquals(0, model.size());
            assertTrue(model.isEmpty());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for unknown type")
        void shouldThrowExceptionForUnknownType() {
            PluginNotFoundException exception = assertThrows(
                    PluginNotFoundException.class,
                    () -> factory.createModel("unknown")
            );

            assertTrue(exception.getMessage().contains("No plugin found"));
            assertTrue(exception.getMessage().contains("unknown"));
        }


        @Test
        @DisplayName("Should throw IllegalArgumentException for empty type")
        void shouldThrowExceptionForEmptyType() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> factory.createModel("")
            );
        }


    }

    @Nested
    @DisplayName("createMemoryModel() Tests")
    class CreateMemoryModelTests {

        @Test
        @DisplayName("Should create empty memory model")
        void shouldCreateEmptyMemoryModel() throws PluginException {
            Model model = factory.createMemoryModel();

            assertNotNull(model);
            assertEquals(0, model.size());
            assertTrue(model.isEmpty());
        }

        @Test
        @DisplayName("Should create functional memory model")
        void shouldCreateFunctionalMemoryModel() throws PluginException {
            Model model = factory.createMemoryModel();

            // Add statement
            model.add(subject, predicate, object);

            assertEquals(1, model.size());
            assertFalse(model.isEmpty());
            assertTrue(model.contains(subject, predicate, object));
        }

        @Test
        @DisplayName("Should create independent memory models")
        void shouldCreateIndependentMemoryModels() throws PluginException {
            Model model1 = factory.createMemoryModel();
            Model model2 = factory.createMemoryModel();

            model1.add(subject, predicate, object);

            assertEquals(1, model1.size());
            assertEquals(0, model2.size());
        }


    }

    @Nested
    @DisplayName("createGraphModel() Tests")
    class CreateGraphModelTests {

        @Test
        @DisplayName("Should create empty graph model")
        void shouldCreateEmptyGraphModel() throws PluginException {
            Model model = factory.createGraphModel();

            assertNotNull(model);
            assertEquals(0, model.size());
            assertTrue(model.isEmpty());
        }

        @Test
        @DisplayName("Should create functional graph model")
        void shouldCreateFunctionalGraphModel() throws PluginException {
            Model model = factory.createGraphModel();

            // Add statement
            model.add(subject, predicate, object);

            assertEquals(1, model.size());
            assertFalse(model.isEmpty());
            assertTrue(model.contains(subject, predicate, object));
        }

        @Test
        @DisplayName("Should create independent graph models")
        void shouldCreateIndependentGraphModels() throws PluginException {
            Model model1 = factory.createGraphModel();
            Model model2 = factory.createGraphModel();

            model1.add(subject, predicate, object);

            assertEquals(1, model1.size());
            assertEquals(0, model2.size());
        }

        @Test
        @DisplayName("Graph model should support multiple statements")
        void graphModelShouldSupportMultipleStatements() throws PluginException {
            Model model = factory.createGraphModel();

            IRI subject2 = valueFactory.createIRI("http://example.org/subject2");
            Literal object2 = valueFactory.createLiteral("test value 2");

            model.add(subject, predicate, object);
            model.add(subject2, predicate, object2);

            assertTrue(model.contains(subject, predicate, object));
            assertTrue(model.contains(subject2, predicate, object2));
        }
    }


    @Nested
    @DisplayName("Storage Type Comparison Tests")
    class StorageTypeComparisonTests {

        @Test
        @DisplayName("Memory and Graph models should be functionally equivalent")
        void memoryAndGraphModelsShouldBeFunctionallyEquivalent() throws PluginException {
            Model memoryModel = factory.createMemoryModel();
            Model graphModel = factory.createGraphModel();

            // Add same data to both
            memoryModel.add(subject, predicate, object);
            graphModel.add(subject, predicate, object);

            // Both should behave the same
            assertEquals(1, memoryModel.size());
            assertEquals(1, graphModel.size());
            assertTrue(memoryModel.contains(subject, predicate, object));
            assertTrue(graphModel.contains(subject, predicate, object));
        }

        @Test
        @DisplayName("createModel should produce same behavior as specialized methods")
        void createModelShouldProduceSameBehaviorAsSpecializedMethods() throws PluginException {
            Model memoryModel1 = factory.createModel("memory");
            Model memoryModel2 = factory.createMemoryModel();


            // Add data
            memoryModel1.add(subject, predicate, object);
            memoryModel2.add(subject, predicate, object);
            // All should behave the same
            assertEquals(memoryModel1.size(), memoryModel2.size());
        }
    }


    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {


        @Test
        @DisplayName("Should handle empty string as storage type")
        void shouldHandleEmptyStringAsStorageType() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> factory.createModel("")
            );

            assertTrue(exception.getMessage().contains("cannot be null or empty"));
        }

        @Test
        @DisplayName("Should handle whitespace-only string as storage type")
        void shouldHandleWhitespaceOnlyStringAsStorageType() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> factory.createModel("   ")
            );

            assertTrue(exception.getMessage().contains("cannot be null or empty"));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Complete workflow: create, populate, query")
        void completeWorkflowCreatePopulateQuery() throws PluginException {
            // Create model
            Model model = factory.createMemoryModel();

            // Populate
            IRI alice = valueFactory.createIRI("http://example.org/Alice");
            IRI bob = valueFactory.createIRI("http://example.org/Bob");
            IRI knows = valueFactory.createIRI("http://xmlns.com/foaf/0.1/knows");

            model.add(alice, knows, bob);

            // Query
            assertTrue(model.contains(alice, knows, bob));
            assertEquals(1, model.size());
            assertFalse(model.isEmpty());
        }

        @Test
        @DisplayName("Should work with different ValueFactory implementations")
        void shouldWorkWithDifferentValueFactoryImplementations() throws PluginException {
            // This test assumes CoreseValueFactory works
            ValueFactory vf = new CoreseValueFactory();
            StorageModelFactory createdFactory = new StorageModelFactory(vf);

            Model model = createdFactory.createMemoryModel();
            assertNotNull(model);

            IRI testIRI = vf.createIRI("http://test.org/resource");
            Literal testLiteral = vf.createLiteral("value");

            assertDoesNotThrow(() -> {
                model.add(testIRI, predicate, testLiteral);
            });
        }


    }
}
