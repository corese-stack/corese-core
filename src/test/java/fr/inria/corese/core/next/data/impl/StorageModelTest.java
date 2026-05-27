package fr.inria.corese.core.next.data.impl;

import fr.inria.corese.core.next.data.api.*;
import fr.inria.corese.core.next.data.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.next.storagemanager.api.StorageManager;
import fr.inria.corese.core.next.storagemanager.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storagemanager.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storagemanager.api.operations.MutationOperations;
import fr.inria.corese.core.next.storagemanager.api.operations.QueryOperations;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;
import fr.inria.corese.core.next.storagemanager.api.support.model.MutationResult;
import fr.inria.corese.core.next.storagemanager.api.support.model.StatementPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StorageManagerBasedModel.
 */
class StorageModelTest {

    @Mock
    private StorageManager mockStorage;

    @Mock
    private ValueFactory mockValueFactory;

    @Mock
    private QueryOperations mockQueryOps;

    @Mock
    private MutationOperations mockMutationOps;

    @Mock
    private MetadataOperations mockMetadataOps;

    @Mock
    private StorageLifecycle mockLifecycle;

    private StorageModel model;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock storage to return operations
        when(mockStorage.getQueryOperations()).thenReturn(mockQueryOps);
        when(mockStorage.getMutationOperations()).thenReturn(mockMutationOps);
        when(mockStorage.getMetadataOperations()).thenReturn(mockMetadataOps);
        when(mockStorage.getLifecycle()).thenReturn(mockLifecycle);

        // Build model
        model = StorageModel.builder()
                .storage(mockStorage)
                .valueFactory(mockValueFactory)
                .build();
    }

    @Nested
    @DisplayName("Builder tests")
    class BuilderTests {

        @Test
        @DisplayName("Should throw when storage is null")
        void shouldThrowWhenStorageIsNull() {
            assertThrows(IllegalStateException.class, () -> StorageModel.builder()
                    .valueFactory(mockValueFactory)
                    .build());
        }

        @Test
        @DisplayName("Should throw when valueFactory is null")
        void shouldThrowWhenValueFactoryIsNull() {
            assertThrows(IllegalStateException.class, () -> StorageModel.builder()
                    .storage(mockStorage)
                    .build());
        }

        @Test
        @DisplayName("Should build with initial namespaces")
        void shouldBuildWithInitialNamespaces() {
            Namespace ns = mock(Namespace.class);
            Set<Namespace> namespaces = new HashSet<>(Collections.singletonList(ns));

            StorageModel model = StorageModel.builder()
                    .storage(mockStorage)
                    .valueFactory(mockValueFactory)
                    .namespaces(namespaces)
                    .build();

            assertTrue(model.getNamespaces().contains(ns));
        }
    }

    @Nested
    @DisplayName("Add operations")
    class AddOperationsTests {

        @Test
        @DisplayName("Should add statement to default graph")
        void shouldAddStatementToDefaultGraph() throws StorageException {
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);
            Statement stmt = mock(Statement.class);

            when(mockValueFactory.createStatement(subject, predicate, object)).thenReturn(stmt);
            when(mockMutationOps.insertStatement(stmt)).thenReturn(MutationResult.success(stmt, "Added"));

            boolean added = model.add(subject, predicate, object);

            assertTrue(added);
            verify(mockValueFactory).createStatement(subject, predicate, object);
            verify(mockMutationOps).insertStatement(stmt);
        }

        @Test
        @DisplayName("Should add statement with context")
        void shouldAddStatementWithContext() throws StorageException {
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);
            Resource context = mock(Resource.class);
            Statement stmt = mock(Statement.class);

            when(mockValueFactory.createStatement(subject, predicate, object, context)).thenReturn(stmt);
            when(mockMutationOps.insertStatement(stmt)).thenReturn(MutationResult.success(stmt, "Added"));

            boolean added = model.add(subject, predicate, object, context);

            assertTrue(added);
            verify(mockValueFactory).createStatement(subject, predicate, object, context);
            verify(mockMutationOps).insertStatement(stmt);
        }

        @Test
        @DisplayName("Should add statement to multiple contexts")
        void shouldAddStatementToMultipleContexts() throws StorageException {
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);
            Resource context1 = mock(Resource.class);
            Resource context2 = mock(Resource.class);
            Statement stmt1 = mock(Statement.class);
            Statement stmt2 = mock(Statement.class);

            when(mockValueFactory.createStatement(subject, predicate, object, context1)).thenReturn(stmt1);
            when(mockValueFactory.createStatement(subject, predicate, object, context2)).thenReturn(stmt2);
            when(mockMutationOps.insertStatement(any())).thenReturn(MutationResult.success(null, "Added"));

            boolean added = model.add(subject, predicate, object, context1, context2);

            assertTrue(added);
            verify(mockMutationOps, times(2)).insertStatement(any(Statement.class));
        }

        @Test
        @DisplayName("Should throw when subject is null")
        void shouldThrowWhenSubjectIsNull() {
            assertThrows(NullPointerException.class, () -> model.add(null, mock(IRI.class), mock(Value.class)));
        }

        @Test
        @DisplayName("Should throw when predicate is null")
        void shouldThrowWhenPredicateIsNull() {
            assertThrows(NullPointerException.class, () -> model.add(mock(Resource.class), null, mock(Value.class)));
        }

        @Test
        @DisplayName("Should throw when object is null")
        void shouldThrowWhenObjectIsNull() {
            assertThrows(NullPointerException.class, () -> model.add(mock(Resource.class), mock(IRI.class), null));
        }
    }

    @Nested
    @DisplayName("Contains operations")
    class ContainsOperationsTests {

        @Test
        @DisplayName("Should check if statement exists")
        void shouldCheckIfStatementExists() throws StorageException {
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);

            when(mockQueryOps.exists(any(StatementPattern.class))).thenReturn(true);

            boolean contains = model.contains(subject, predicate, object);

            assertTrue(contains);
            verify(mockQueryOps).exists(any(StatementPattern.class));
        }

        @Test
        @DisplayName("Should check if statement exists with context")
        void shouldCheckIfStatementExistsWithContext() throws StorageException {
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);
            Resource context = mock(Resource.class);

            when(mockQueryOps.exists(any(StatementPattern.class))).thenReturn(false);

            boolean contains = model.contains(subject, predicate, object, context);

            assertFalse(contains);
            verify(mockQueryOps).exists(any(StatementPattern.class));
        }

        @Test
        @DisplayName("Should support wildcard patterns")
        void shouldSupportWildcardPatterns() throws StorageException {
            when(mockQueryOps.exists(any(StatementPattern.class))).thenReturn(true);

            boolean contains = model.contains(null, null, null);

            assertTrue(contains);
        }
    }

    @Nested
    @DisplayName("Remove operations")
    class RemoveOperationsTests {

        @Test
        @DisplayName("Should remove statements")
        void shouldRemoveStatements() throws StorageException {
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);

            when(mockMutationOps.deleteStatements(subject, predicate, object))
                    .thenReturn(MutationResult.bulkBuilder().totalAttempted(1).successCount(1).build());

            boolean removed = model.remove(subject, predicate, object);

            assertTrue(removed);
            verify(mockMutationOps).deleteStatements(subject, predicate, object);
        }


        @Test
        @DisplayName("Should clear specific contexts")
        void shouldClearSpecificContexts() throws StorageException {
            Resource context1 = mock(Resource.class);
            Resource context2 = mock(Resource.class);

            when(mockMutationOps.clear(context1, context2))
                    .thenReturn(MutationResult.success(null, "Cleared"));

            boolean cleared = model.clear(context1, context2);

            assertTrue(cleared);
            verify(mockMutationOps).clear(context1, context2);
        }
    }

    @Nested
    @DisplayName("Query operations")
    class QueryOperationsTests {

        @Test
        @DisplayName("Should get statements")
        void shouldGetStatements() throws StorageException {
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);
            Statement stmt = mock(Statement.class);

            when(mockQueryOps.query(any(StatementPattern.class)))
                    .thenReturn(Stream.of(stmt));

            Iterable<Statement> statements = model.getStatements(subject, predicate, object);

            assertNotNull(statements);
            assertTrue(statements.iterator().hasNext());
            verify(mockQueryOps).query(any(StatementPattern.class));
        }

        @Test
        @DisplayName("Should filter statements")
        void shouldFilterStatements() {
            Resource subject = mock(Resource.class);

            Model filtered = model.filter(subject, null, null);

            assertNotNull(filtered);
            assertNotSame(model, filtered);
        }

        @Test
        @DisplayName("Should iterate over all statements")
        void shouldIterateOverAllStatements() throws StorageException {
            Statement stmt1 = mock(Statement.class);
            Statement stmt2 = mock(Statement.class);

            when(mockQueryOps.query(any(StatementPattern.class)))
                    .thenReturn(Stream.of(stmt1, stmt2));

            Iterator<Statement> iterator = model.iterator();

            assertTrue(iterator.hasNext());
            assertNotNull(iterator.next());
            assertTrue(iterator.hasNext());
            assertNotNull(iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Nested
    @DisplayName("Metadata operations")
    class MetadataOperationsTests {

        @Test
        @DisplayName("Should get subjects")
        void shouldGetSubjects() throws StorageException {
            Resource subject = mock(Resource.class);
            Set<Resource> subjects = new HashSet<>(Collections.singletonList(subject));

            when(mockMetadataOps.getSubjects()).thenReturn(subjects);

            Set<Resource> result = model.subjects();

            assertEquals(1, result.size());
            assertTrue(result.contains(subject));
            verify(mockMetadataOps).getSubjects();
        }

        @Test
        @DisplayName("Should get predicates")
        void shouldGetPredicates() throws StorageException {
            IRI predicate = mock(IRI.class);
            Set<IRI> predicates = new HashSet<>(Collections.singletonList(predicate));

            when(mockMetadataOps.getPredicates()).thenReturn(predicates);

            Set<IRI> result = model.predicates();

            assertEquals(1, result.size());
            assertTrue(result.contains(predicate));
            verify(mockMetadataOps).getPredicates();
        }

        @Test
        @DisplayName("Should get objects")
        void shouldGetObjects() throws StorageException {
            Value object = mock(Value.class);
            Set<Value> objects = new HashSet<>(Collections.singletonList(object));

            when(mockMetadataOps.getObjects()).thenReturn(objects);

            Set<Value> result = model.objects();

            assertEquals(1, result.size());
            assertTrue(result.contains(object));
            verify(mockMetadataOps).getObjects();
        }

        @Test
        @DisplayName("Should get contexts")
        void shouldGetContexts() throws StorageException {
            Resource context = mock(Resource.class);
            Set<Resource> contexts = new HashSet<>(Collections.singletonList(context));

            when(mockMetadataOps.getContexts()).thenReturn(contexts);

            Set<Resource> result = model.contexts();

            assertEquals(1, result.size());
            assertTrue(result.contains(context));
            verify(mockMetadataOps).getContexts();
        }
    }

    @Nested
    @DisplayName("Namespace operations")
    class NamespaceOperationsTests {

        @Test
        @DisplayName("Should get namespaces")
        void shouldGetNamespaces() {
            Set<Namespace> namespaces = model.getNamespaces();

            assertNotNull(namespaces);
            assertTrue(namespaces.isEmpty());
        }

        @Test
        @DisplayName("Should set namespace")
        void shouldSetNamespace() {
            Namespace namespace = mock(Namespace.class);
            when(namespace.getPrefix()).thenReturn("ex");

            model.setNamespace(namespace);

            assertTrue(model.getNamespaces().contains(namespace));
        }

        @Test
        @DisplayName("Should remove namespace")
        void shouldRemoveNamespace() {
            Namespace namespace = mock(Namespace.class);
            when(namespace.getPrefix()).thenReturn("ex");

            model.setNamespace(namespace);
            Optional<Namespace> removed = model.removeNamespace("ex");

            assertTrue(removed.isPresent());
            assertEquals(namespace, removed.get());
            assertFalse(model.getNamespaces().contains(namespace));
        }

        @Test
        @DisplayName("Should return empty when removing non-existent namespace")
        void shouldReturnEmptyWhenRemovingNonExistentNamespace() {
            Optional<Namespace> removed = model.removeNamespace("nonexistent");

            assertFalse(removed.isPresent());
        }

        @Test
        @DisplayName("Should replace namespace with same prefix")
        void shouldReplaceNamespaceWithSamePrefix() {
            Namespace ns1 = mock(Namespace.class);
            Namespace ns2 = mock(Namespace.class);
            when(ns1.getPrefix()).thenReturn("ex");
            when(ns2.getPrefix()).thenReturn("ex");

            model.setNamespace(ns1);
            model.setNamespace(ns2);

            assertEquals(1, model.getNamespaces().size());
            assertTrue(model.getNamespaces().contains(ns2));
            assertFalse(model.getNamespaces().contains(ns1));
        }
    }

    @Nested
    @DisplayName("Size operations")
    class SizeOperationsTests {

        @Test
        @DisplayName("Should return size")
        void shouldReturnSize() throws StorageException {
            when(mockQueryOps.count(any(StatementPattern.class))).thenReturn(42L);

            int size = model.size();

            assertEquals(42, size);
            verify(mockQueryOps).count(any(StatementPattern.class));
        }

        @Test
        @DisplayName("Should return zero for empty model")
        void shouldReturnZeroForEmptyModel() throws StorageException {
            when(mockQueryOps.count(any(StatementPattern.class))).thenReturn(0L);

            int size = model.size();

            assertEquals(0, size);
        }
    }

    @Nested
    @DisplayName("Unmodifiable operations")
    class UnmodifiableOperationsTests {

        @Test
        @DisplayName("Should create unmodifiable view")
        void shouldCreateUnmodifiableView() {
            Model unmodifiable = model.unmodifiable();

            assertNotNull(unmodifiable);
            assertNotSame(model, unmodifiable);
        }

        @Test
        @DisplayName("Should throw when adding to unmodifiable model")
        void shouldThrowWhenAddingToUnmodifiableModel() {
            Model unmodifiable = model.unmodifiable();

            assertThrows(IncorrectOperationException.class, () -> unmodifiable.add(mock(Resource.class), mock(IRI.class), mock(Value.class)));
        }

        @Test
        @DisplayName("Should throw when removing from unmodifiable model")
        void shouldThrowWhenRemovingFromUnmodifiableModel() {
            Model unmodifiable = model.unmodifiable();

            assertThrows(IncorrectOperationException.class, () -> unmodifiable.remove(mock(Resource.class), mock(IRI.class), mock(Value.class)));
        }

        @Test
        @DisplayName("Should throw when clearing unmodifiable model")
        void shouldThrowWhenClearingUnmodifiableModel() {
            Model unmodifiable = model.unmodifiable();

            assertThrows(IncorrectOperationException.class, unmodifiable::clear);
        }

        @Test
        @DisplayName("Should throw when setting namespace on unmodifiable model")
        void shouldThrowWhenSettingNamespaceOnUnmodifiableModel() {
            Model unmodifiable = model.unmodifiable();

            assertThrows(IncorrectOperationException.class, () -> unmodifiable.setNamespace(mock(Namespace.class)));
        }

        @Test
        @DisplayName("Should allow operations on unmodifiable model")
        void shouldAllowQueriesOnUnmodifiableModel() throws StorageException {
            when(mockQueryOps.exists(any(StatementPattern.class))).thenReturn(true);

            Model unmodifiable = model.unmodifiable();
            boolean contains = unmodifiable.contains(null, null, null);

            assertTrue(contains);
        }

        @Test
        @DisplayName("Should return same instance when calling unmodifiable twice")
        void shouldReturnSameInstanceWhenCallingUnmodifiableTwice() {
            Model unmodifiable1 = model.unmodifiable();
            Model unmodifiable2 = unmodifiable1.unmodifiable();

            assertSame(unmodifiable1, unmodifiable2);
        }
    }

}