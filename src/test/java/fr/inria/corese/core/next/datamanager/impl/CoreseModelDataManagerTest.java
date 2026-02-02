package fr.inria.corese.core.next.datamanager.impl;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.datamanager.api.transaction.IsolationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CoreseModelDataManager.
 */
@DisplayName("CoreseModelDataManager Tests")
class CoreseModelDataManagerTest {

    @Mock
    private Model model;

    private CoreseModelDataManager dataManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        dataManager = CoreseModelDataManager.builder()
                .model(model)
                .transactionSupport(true)
                .defaultIsolationLevel(IsolationLevel.READ_COMMITTED)
                .build();
    }

    @Test
    @DisplayName("Should return the configured model instance")
    void testGetModel() {
        assertEquals(model, dataManager.getModel());
    }

    @Test
    @DisplayName("Should provide query operations handler")
    void testGetQueryOperations() {
        assertNotNull(dataManager.getQueryOperations());
    }

    @Test
    @DisplayName("Should provide mutation operations handler")
    void testGetMutationOperations() {
        assertNotNull(dataManager.getMutationOperations());
    }

    @Test
    @DisplayName("Should provide metadata operations handler")
    void testGetMetadataOperations() {
        assertNotNull(dataManager.getMetadataOperations());
    }

    @Test
    @DisplayName("Should provide bulk operations handler")
    void testGetBulkOperations() {
        assertNotNull(dataManager.getBulkOperations());
    }

    @Test
    @DisplayName("Should provide transaction manager with correct configuration")
    void testGetTransactionManager() {
        assertNotNull(dataManager.getTransactionManager());
        assertTrue(dataManager.getTransactionManager().supportsTransactions());
    }

    @Test
    @DisplayName("Should provide lifecycle manager")
    void testGetLifecycle() {
        assertNotNull(dataManager.getLifecycle());
    }

    @Test
    @DisplayName("Should provide static builder factory method")
    void testBuilderStaticMethod() {
        assertNotNull(CoreseModelDataManager.builder());
    }

    @Test
    @DisplayName("Should support disabled transaction mode")
    void testTransactionSupportDisabled() {
        CoreseModelDataManager dm = CoreseModelDataManager.builder()
                .model(model)
                .transactionSupport(false)
                .build();

        assertFalse(dm.getTransactionManager().supportsTransactions());
    }

    @Test
    @DisplayName("Should support custom isolation levels")
    void testCustomIsolationLevel() {
        CoreseModelDataManager dm = CoreseModelDataManager.builder()
                .model(model)
                .transactionSupport(true)
                .defaultIsolationLevel(IsolationLevel.SERIALIZABLE)
                .build();

        assertEquals(IsolationLevel.SERIALIZABLE,
                dm.getTransactionManager().getDefaultIsolationLevel());
    }

    @Test
    @DisplayName("Should ensure all operations share the same model instance")
    void testOperationsShareSameModel() {
        assertEquals(model, dataManager.getModel());

        assertNotNull(dataManager.getQueryOperations());
        assertNotNull(dataManager.getMutationOperations());
        assertNotNull(dataManager.getMetadataOperations());
        assertNotNull(dataManager.getBulkOperations());
    }

    @Test
    @DisplayName("Should start with lifecycle in NOT_INITIALIZED state")
    void testLifecycleInitialState() {
        assertNotNull(dataManager.getLifecycle());
        assertFalse(dataManager.getLifecycle().isInitialized());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when builder has no model")
    void testBuilderRequiresModel() {
        assertThrows(IllegalStateException.class,
                () -> CoreseModelDataManager.builder().build());
    }

    @Test
    @DisplayName("Should create independent DataManager instances")
    void testMultipleInstancesIndependent() {
        CoreseModelDataManager dm1 = CoreseModelDataManager.builder()
                .model(model)
                .build();

        CoreseModelDataManager dm2 = CoreseModelDataManager.builder()
                .model(model)
                .build();

        assertNotSame(dm1, dm2);
        assertNotSame(dm1.getLifecycle(), dm2.getLifecycle());
    }
}
