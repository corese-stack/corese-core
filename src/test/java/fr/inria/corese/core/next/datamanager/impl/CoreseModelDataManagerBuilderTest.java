package fr.inria.corese.core.next.datamanager.impl;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.datamanager.api.ModelDataManager;
import fr.inria.corese.core.next.datamanager.api.transaction.IsolationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CoreseModelDataManagerBuilder.
 */
@DisplayName("CoreseModelDataManagerBuilder Tests")
class CoreseModelDataManagerBuilderTest {

    @Mock
    private Model model;

    private CoreseModelDataManagerBuilder builder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        builder = new CoreseModelDataManagerBuilder();
    }

    @Test
    @DisplayName("Should build DataManager with minimal configuration (only model)")
    void testBuildWithMinimalConfig() {
        ModelDataManager dataManager = builder
                .model(model)
                .build();

        assertNotNull(dataManager);
        assertEquals(model, dataManager.getModel());
    }

    @Test
    @DisplayName("Should build DataManager with full configuration")
    void testBuildWithAllConfig() {
        ModelDataManager dataManager = builder
                .model(model)
                .withTransactions(true)
                .defaultIsolationLevel(IsolationLevel.SERIALIZABLE)
                .build();

        assertNotNull(dataManager);
        assertEquals(model, dataManager.getModel());
        assertTrue(dataManager.getTransactionManager().supportsTransactions());
        assertEquals(IsolationLevel.SERIALIZABLE,
                dataManager.getTransactionManager().getDefaultIsolationLevel());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when building without model")
    void testBuildWithoutModel() {
        assertThrows(IllegalStateException.class,
                () -> builder.build());
    }

    @Test
    @DisplayName("Should disable transaction support by default")
    void testDefaultTransactionSupport() {
        ModelDataManager dataManager = builder
                .model(model)
                .build();

        assertFalse(dataManager.getTransactionManager().supportsTransactions());
    }

    @Test
    @DisplayName("Should use READ_COMMITTED as default isolation level")
    void testDefaultIsolationLevel() {
        ModelDataManager dataManager = builder
                .model(model)
                .build();

        assertEquals(IsolationLevel.READ_COMMITTED,
                dataManager.getTransactionManager().getDefaultIsolationLevel());
    }

    @Test
    @DisplayName("Should support builder method chaining with last value winning")
    void testBuilderChaining() {
        ModelDataManager dataManager = builder
                .model(model)
                .withTransactions(true)
                .withTransactions(false)
                .defaultIsolationLevel(IsolationLevel.SERIALIZABLE)
                .defaultIsolationLevel(IsolationLevel.READ_UNCOMMITTED)
                .build();

        assertNotNull(dataManager);
        assertFalse(dataManager.getTransactionManager().supportsTransactions());
        assertEquals(IsolationLevel.READ_UNCOMMITTED,
                dataManager.getTransactionManager().getDefaultIsolationLevel());
    }

    @Test
    @DisplayName("Should create independent instances on multiple builds")
    void testMultipleBuilds() {
        builder.model(model);

        ModelDataManager dm1 = builder.build();
        ModelDataManager dm2 = builder.build();

        assertNotNull(dm1);
        assertNotNull(dm2);
        assertNotSame(dm1, dm2);
        assertEquals(dm1.getModel(), dm2.getModel());
    }

    @Test
    @DisplayName("Should initialize all operation handlers on build")
    void testAllOperationsAvailable() {
        ModelDataManager dataManager = builder
                .model(model)
                .build();

        assertNotNull(dataManager.getQueryOperations());
        assertNotNull(dataManager.getMutationOperations());
        assertNotNull(dataManager.getMetadataOperations());
        assertNotNull(dataManager.getBulkOperations());
        assertNotNull(dataManager.getTransactionManager());
        assertNotNull(dataManager.getLifecycle());
    }

    @Test
    @DisplayName("Should allow builder reuse with different configurations")
    void testBuilderReuse() {
        builder.model(model).withTransactions(true);

        ModelDataManager dm1 = builder.build();

        builder.withTransactions(false);
        ModelDataManager dm2 = builder.build();

        assertTrue(dm1.getTransactionManager().supportsTransactions());
        assertFalse(dm2.getTransactionManager().supportsTransactions());
    }
}