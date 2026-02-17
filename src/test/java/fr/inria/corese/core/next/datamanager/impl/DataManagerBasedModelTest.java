package fr.inria.corese.core.next.datamanager.impl;

import fr.inria.corese.core.next.data.api.*;
import fr.inria.corese.core.next.data.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.next.datamanager.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.datamanager.api.support.exception.DataManagerException;
import fr.inria.corese.core.next.datamanager.api.transaction.IsolationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for DataManagerBasedModel.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataManagerBasedModel Tests")
class DataManagerBasedModelTest {

    @Mock
    private Statement stmt1;
    @Mock
    private Statement stmt2;
    @Mock
    private Statement stmt3;
    @Mock
    private IRI predicate;
    @Mock
    private Resource subject;
    @Mock
    private Value object;
    @Mock
    private Resource context;

    private DataManagerBasedModel model;

    @BeforeEach
    void setUp() {
        model = DataManagerBasedModel.builder().build();

        lenient().when(stmt1.getSubject()).thenReturn(subject);
        lenient().when(stmt1.getPredicate()).thenReturn(predicate);
        lenient().when(stmt1.getObject()).thenReturn(object);
        lenient().when(stmt1.getContext()).thenReturn(null);

        lenient().when(stmt2.getSubject()).thenReturn(subject);
        lenient().when(stmt2.getPredicate()).thenReturn(predicate);
        lenient().when(stmt2.getObject()).thenReturn(object);
        lenient().when(stmt2.getContext()).thenReturn(context);

        lenient().when(stmt3.getSubject()).thenReturn(subject);
        lenient().when(stmt3.getPredicate()).thenReturn(predicate);
        lenient().when(stmt3.getObject()).thenReturn(object);
        lenient().when(stmt3.getContext()).thenReturn(null);
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("build() returns a non-null instance")
        void testBuildNotNull() {
            assertNotNull(DataManagerBasedModel.builder().build());
        }

        @Test
        @DisplayName("build() without transactions — supportsTransactions() = false")
        void testBuildNoTransactions() {
            DataManagerBasedModel m = DataManagerBasedModel.builder()
                    .withTransactions(false)
                    .build();
            assertFalse(m.getTransactionManager().supportsTransactions());
        }

        @Test
        @DisplayName("build() with transactions — supportsTransactions() = true")
        void testBuildWithTransactions() {
            DataManagerBasedModel m = DataManagerBasedModel.builder()
                    .withTransactions(true)
                    .build();
            assertTrue(m.getTransactionManager().supportsTransactions());
        }

        @Test
        @DisplayName("build() with SERIALIZABLE isolation level")
        void testBuildWithIsolationLevel() {
            DataManagerBasedModel m = DataManagerBasedModel.builder()
                    .withTransactions(true)
                    .withIsolationLevel(IsolationLevel.SERIALIZABLE)
                    .build();
            assertEquals(IsolationLevel.SERIALIZABLE,
                    m.getTransactionManager().getDefaultIsolationLevel());
        }

        @Test
        @DisplayName("buildAndInit() initializes lifecycle to RUNNING state")
        void testBuildAndInit() throws DataManagerException {
            DataManagerBasedModel m = DataManagerBasedModel.builder().buildAndInit();
            assertEquals(LifecycleState.RUNNING, m.getLifecycle().getState());
        }

        @Test
        @DisplayName("Two instances are independent")
        void testTwoInstancesIndependent() {
            DataManagerBasedModel m1 = DataManagerBasedModel.builder().build();
            DataManagerBasedModel m2 = DataManagerBasedModel.builder().build();
            assertNotSame(m1, m2);
            assertNotSame(m1.getLifecycle(), m2.getLifecycle());
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class LifecycleTests {

        @Test
        @DisplayName("Initial state is NOT_INITIALIZED")
        void testInitialState() {
            assertEquals(LifecycleState.NOT_INITIALIZED, model.getLifecycle().getState());
        }

        @Test
        @DisplayName("isInitialized() returns false before initialize()")
        void testNotInitialized() {
            assertFalse(model.getLifecycle().isInitialized());
        }

        @Test
        @DisplayName("After initialize() — state is RUNNING")
        void testAfterInitialize() throws DataManagerException {
            model.getLifecycle().initialize(
                    fr.inria.corese.core.next.datamanager.api.support.config.DataManagerConfig.builder().build());
            assertEquals(LifecycleState.RUNNING, model.getLifecycle().getState());
        }

        @Test
        @DisplayName("getLifecycle() is not null")
        void testLifecycleNotNull() {
            assertNotNull(model.getLifecycle());
        }
    }


    @Nested
    @DisplayName("Add / contains / remove")
    class MutationTests {

        @Test
        @DisplayName("add(Statement) returns true on first insert")
        void testAddStatement() {
            assertTrue(model.add(stmt1));
        }

        @Test
        @DisplayName("add(Statement) — model contains the statement")
        void testAddThenContains() {
            model.add(stmt1);
            assertTrue(model.contains(stmt1));
        }


        @Test
        @DisplayName("size() after add")
        void testSizeAfterAdd() {
            model.add(stmt1);
            model.add(stmt2);
            assertEquals(2, model.size());
        }

        @Test
        @DisplayName("isEmpty() returns false after add")
        void testIsEmptyAfterAdd() {
            model.add(stmt1);
            assertFalse(model.isEmpty());
        }

        @Test
        @DisplayName("remove(Statement) returns true if present")
        void testRemovePresent() {
            model.add(stmt1);
            assertTrue(model.remove(stmt1));
        }

        @Test
        @DisplayName("remove(Statement) returns false if absent")
        void testRemoveAbsent() {
            assertFalse(model.remove(stmt1));
        }

        @Test
        @DisplayName("remove(Statement) — model no longer contains the statement")
        void testRemoveThenNotContains() {
            model.add(stmt1);
            model.remove(stmt1);
            assertFalse(model.contains(stmt1));
        }


        @Test
        @DisplayName("size() after remove")
        void testSizeAfterRemove() {
            model.add(stmt1);
            model.add(stmt2);
            model.remove(stmt1);
            assertEquals(1, model.size());
        }
    }

    @Nested
    @DisplayName("clear")
    class ClearTests {


        @Test
        @DisplayName("clear() on empty model — no exception")
        void testClearEmpty() {
            assertDoesNotThrow(() -> model.clear());
        }

        @Test
        @DisplayName("clear(context) removes only statements from that context")
        void testClearContext() {
            model.add(stmt1);
            model.add(stmt2);

            model.clear(context);

            assertFalse(model.contains(stmt2));
            assertTrue(model.contains(stmt1));
            assertEquals(1, model.size());
        }
    }

    @Nested
    @DisplayName("getStatements / contains(s,p,o)")
    class QueryTests {

        @Test
        @DisplayName("getStatements with subject — filters correctly")
        void testGetStatementsBySubject() {
            model.add(stmt1);
            Iterable<Statement> result = model.getStatements(subject, null, null);
            assertTrue(result.iterator().hasNext());
        }

        @Test
        @DisplayName("getStatements with unknown subject — returns empty")
        void testGetStatementsUnknownSubject() {
            Resource other = mock(Resource.class);
            model.add(stmt1);
            Iterable<Statement> result = model.getStatements(other, null, null);
            assertFalse(result.iterator().hasNext());
        }

        @Test
        @DisplayName("contains(s,p,o) returns true if present")
        void testContainsSPO() {
            model.add(stmt1);
            assertTrue(model.contains(subject, predicate, object));
        }

        @Test
        @DisplayName("contains(s,p,o) returns false if absent")
        void testContainsSPOAbsent() {
            assertFalse(model.contains(subject, predicate, object));
        }

        @Test
        @DisplayName("contains(null,null,null) returns true if model is not empty")
        void testContainsWildcard() {
            model.add(stmt1);
            assertTrue(model.contains(null, null, null));
        }
    }

    @Nested
    @DisplayName("filter")
    class FilterTests {

        @Test
        @DisplayName("filter() returns a non-null sub-model")
        void testFilterNotNull() {
            model.add(stmt1);
            assertNotNull(model.filter(subject, null, null));
        }

        @Test
        @DisplayName("filter() with existing subject — result is not empty")
        void testFilterBySubject() {
            model.add(stmt1);
            Model filtered = model.filter(subject, null, null);
            assertFalse(filtered.isEmpty());
        }

        @Test
        @DisplayName("filter() with unknown subject — result is empty")
        void testFilterByUnknownSubject() {
            Resource other = mock(Resource.class);
            model.add(stmt1);
            Model filtered = model.filter(other, null, null);
            assertTrue(filtered.isEmpty());
        }

        @Test
        @DisplayName("filter(null,null,null) returns all statements")
        void testFilterAll() {
            model.add(stmt1);
            model.add(stmt2);
            Model filtered = model.filter(null, null, null);
            assertEquals(2, filtered.size());
        }
    }

    @Nested
    @DisplayName("Metadata")
    class MetadataTests {

        @Test
        @DisplayName("subjects() contains the subject after add")
        void testSubjectsAfterAdd() {
            model.add(stmt1);
            assertTrue(model.subjects().contains(subject));
        }

        @Test
        @DisplayName("predicates() contains the predicate after add")
        void testPredicatesAfterAdd() {
            model.add(stmt1);
            assertTrue(model.predicates().contains(predicate));
        }

        @Test
        @DisplayName("objects() contains the object after add")
        void testObjectsAfterAdd() {
            model.add(stmt1);
            assertTrue(model.objects().contains(object));
        }

        @Test
        @DisplayName("contexts() contains the context after add with context")
        void testContextsAfterAdd() {
            model.add(stmt2);
            assertTrue(model.contexts().contains(context));
        }

        @Test
        @DisplayName("subjects() is updated after remove")
        void testSubjectsAfterRemove() {
            model.add(stmt1);
            model.remove(stmt1);
            assertTrue(model.subjects().isEmpty());
        }
    }


    @Nested
    @DisplayName("unmodifiable()")
    class UnmodifiableTests {

        @Test
        @DisplayName("unmodifiable() returns a non-null Model")
        void testUnmodifiableNotNull() {
            assertNotNull(model.unmodifiable());
        }

        @Test
        @DisplayName("unmodifiable() — read operations work (contains)")
        void testUnmodifiableRead() {
            model.add(stmt1);
            Model ro = model.unmodifiable();
            assertTrue(ro.contains(stmt1));
        }

        @Test
        @DisplayName("unmodifiable() — add(Statement) throws IncorrectOperationException")
        void testUnmodifiableAddStatement() {
            Model ro = model.unmodifiable();
            assertThrows(IncorrectOperationException.class, () -> ro.add(stmt1));
        }

        @Test
        @DisplayName("unmodifiable() — remove() throws IncorrectOperationException")
        void testUnmodifiableRemove() {
            model.add(stmt1);
            Model ro = model.unmodifiable();
            assertThrows(IncorrectOperationException.class, () -> ro.remove(stmt1));
        }

        @Test
        @DisplayName("unmodifiable() — clear() throws IncorrectOperationException")
        void testUnmodifiableClear() {
            Model ro = model.unmodifiable();
            assertThrows(IncorrectOperationException.class, ro::clear);
        }

        @Test
        @DisplayName("unmodifiable() — size() returns correct value")
        void testUnmodifiableSize() {
            model.add(stmt1);
            model.add(stmt2);
            assertEquals(2, model.unmodifiable().size());
        }

        @Test
        @DisplayName("unmodifiable().unmodifiable() returns the same object")
        void testUnmodifiableIdempotent() {
            Model ro = model.unmodifiable();
            assertSame(ro, ro.unmodifiable());
        }
    }


    @Nested
    @DisplayName("TransactionManager")
    class TransactionTests {

        @Test
        @DisplayName("getTransactionManager() is not null")
        void testTransactionManagerNotNull() {
            assertNotNull(model.getTransactionManager());
        }

        @Test
        @DisplayName("Without transactions — supportsTransactions() = false")
        void testNoTransactions() {
            assertFalse(model.getTransactionManager().supportsTransactions());
        }

        @Test
        @DisplayName("With transactions — supportsTransactions() = true")
        void testWithTransactions() {
            DataManagerBasedModel m = DataManagerBasedModel.builder()
                    .withTransactions(true)
                    .build();
            assertTrue(m.getTransactionManager().supportsTransactions());
        }
    }


}