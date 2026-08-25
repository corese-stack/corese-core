package fr.inria.corese.core.next.storage.impl.model;

import fr.inria.corese.core.next.data.api.term.*;
import fr.inria.corese.core.next.data.api.model.*;
import fr.inria.corese.core.next.data.api.namespace.*;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storage.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
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
        when(mockStorage.queries()).thenReturn(mockQueryOps);
        when(mockStorage.mutations()).thenReturn(mockMutationOps);
        when(mockStorage.metadata()).thenReturn(mockMetadataOps);
        when(mockStorage.lifecycle()).thenReturn(mockLifecycle);

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
            StorageModel.Builder builder = StorageModel.builder().valueFactory(mockValueFactory);
            assertThrows(IllegalStateException.class, builder::build);
        }

        @Test
        @DisplayName("Should throw when valueFactory is null")
        void shouldThrowWhenValueFactoryIsNull() {
            StorageModel.Builder builder = StorageModel.builder().storage(mockStorage);
            assertThrows(IllegalStateException.class, builder::build);
        }

        @Test
        @DisplayName("Should build with initial namespaces")
        void shouldBuildWithInitialNamespaces() {
            Namespace ns = mock(Namespace.class);
            Set<Namespace> namespaces = new HashSet<>(Collections.singletonList(ns));

            StorageModel builtModel = StorageModel.builder()
                    .storage(mockStorage)
                    .valueFactory(mockValueFactory)
                    .namespaces(namespaces)
                    .build();

            assertTrue(builtModel.getNamespaces().contains(ns));
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
            when(mockMutationOps.add(stmt)).thenReturn(true);

            boolean added = model.add(subject, predicate, object);

            assertTrue(added);
            verify(mockValueFactory).createStatement(subject, predicate, object);
            verify(mockMutationOps).add(stmt);
        }

        @Test
        @DisplayName("Should return false when the statement already exists")
        void shouldReportUnchangedDuplicateInsert() throws StorageException {
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);
            Statement stmt = mock(Statement.class);

            when(mockValueFactory.createStatement(subject, predicate, object)).thenReturn(stmt);
            when(mockMutationOps.add(stmt)).thenReturn(false);

            assertFalse(model.add(subject, predicate, object));
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
            when(mockMutationOps.add(stmt)).thenReturn(true);

            boolean added = model.add(subject, predicate, object, context);

            assertTrue(added);
            verify(mockValueFactory).createStatement(subject, predicate, object, context);
            verify(mockMutationOps).add(stmt);
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
            when(mockMutationOps.add(any())).thenReturn(true, true);

            boolean added = model.add(subject, predicate, object, context1, context2);

            assertTrue(added);
            verify(mockMutationOps, times(2)).add(any(Statement.class));
        }

        @Test
        @DisplayName("Should throw when subject is null")
        void shouldThrowWhenSubjectIsNull() {
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);
            assertThrows(NullPointerException.class, () -> model.add(null, predicate, object));
        }

        @Test
        @DisplayName("Should throw when predicate is null")
        void shouldThrowWhenPredicateIsNull() {
            Resource subject = mock(Resource.class);
            Value object = mock(Value.class);
            assertThrows(NullPointerException.class, () -> model.add(subject, null, object));
        }

        @Test
        @DisplayName("Should throw when object is null")
        void shouldThrowWhenObjectIsNull() {
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            assertThrows(NullPointerException.class, () -> model.add(subject, predicate, null));
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

            when(mockQueryOps.contains(any(StatementPattern.class))).thenReturn(true);

            boolean contains = model.contains(subject, predicate, object);

            assertTrue(contains);
            verify(mockQueryOps).contains(any(StatementPattern.class));
        }

        @Test
        @DisplayName("Should check if statement exists with context")
        void shouldCheckIfStatementExistsWithContext() throws StorageException {
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);
            Resource context = mock(Resource.class);

            when(mockQueryOps.contains(any(StatementPattern.class))).thenReturn(false);

            boolean contains = model.contains(subject, predicate, object, context);

            assertFalse(contains);
            verify(mockQueryOps).contains(any(StatementPattern.class));
        }

        @Test
        @DisplayName("Should support wildcard patterns")
        void shouldSupportWildcardPatterns() throws StorageException {
            when(mockQueryOps.contains(any(StatementPattern.class))).thenReturn(true);

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

            when(mockMutationOps.remove(any(StatementPattern.class))).thenReturn(1L);

            boolean removed = model.remove(subject, predicate, object);

            assertTrue(removed);
            verify(mockMutationOps).remove(any(StatementPattern.class));
        }


        @Test
        @DisplayName("Should clear specific contexts")
        void shouldClearSpecificContexts() throws StorageException {
            Resource context1 = mock(Resource.class);
            Resource context2 = mock(Resource.class);

            when(mockMutationOps.clear(context1, context2)).thenReturn(1L);

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

            when(mockQueryOps.find(any(StatementPattern.class)))
                    .thenReturn(Stream.of(stmt));

            Iterable<Statement> statements = model.getStatements(subject, predicate, object);

            assertNotNull(statements);
            assertTrue(statements.iterator().hasNext());
            verify(mockQueryOps).find(any(StatementPattern.class));
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

            when(mockQueryOps.find(any(StatementPattern.class)))
                    .thenReturn(Stream.of(stmt1, stmt2));

            Iterator<Statement> iterator = model.iterator();

            assertTrue(iterator.hasNext());
            assertNotNull(iterator.next());
            assertTrue(iterator.hasNext());
            assertNotNull(iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    @DisplayName("Term views are derived from statements and remain live")
    void termViewsAreDerivedFromStatementsAndRemainLive() throws StorageException {
        Resource subject = mock(Resource.class);
        IRI predicate = mock(IRI.class);
        Value object = mock(Value.class);
        Resource context = mock(Resource.class);
        Statement statement = mock(Statement.class);
        when(statement.getSubject()).thenReturn(subject);
        when(statement.getPredicate()).thenReturn(predicate);
        when(statement.getObject()).thenReturn(object);
        when(statement.getContext()).thenReturn(context);
        when(mockQueryOps.find(any(StatementPattern.class)))
                .thenAnswer(ignored -> Stream.of(statement));
        when(mockMutationOps.remove(any(StatementPattern.class))).thenReturn(1L);

        assertIterableEquals(List.of(subject), model.subjects());
        assertIterableEquals(List.of(predicate), model.predicates());
        assertIterableEquals(List.of(object), model.objects());
        assertIterableEquals(List.of(context), model.contexts());
        assertTrue(model.subjects().remove(subject));
        verify(mockMutationOps).remove(any(StatementPattern.class));
        verifyNoInteractions(mockMetadataOps);
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
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);

            assertThrows(UnsupportedOperationException.class,
                    () -> unmodifiable.add(subject, predicate, object));
        }

        @Test
        @DisplayName("Should throw when removing from unmodifiable model")
        void shouldThrowWhenRemovingFromUnmodifiableModel() {
            Model unmodifiable = model.unmodifiable();
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);

            assertThrows(UnsupportedOperationException.class,
                    () -> unmodifiable.remove(subject, predicate, object));
        }

        @Test
        @DisplayName("Should throw when clearing unmodifiable model")
        void shouldThrowWhenClearingUnmodifiableModel() {
            Model unmodifiable = model.unmodifiable();

            assertThrows(UnsupportedOperationException.class, unmodifiable::clear);
        }

        @Test
        @DisplayName("Should throw when setting namespace on unmodifiable model")
        void shouldThrowWhenSettingNamespaceOnUnmodifiableModel() {
            Model unmodifiable = model.unmodifiable();
            Namespace ns = mock(Namespace.class);

            assertThrows(UnsupportedOperationException.class,
                    () -> unmodifiable.setNamespace(ns));
        }

        @Test
        @DisplayName("Should allow queries on unmodifiable model")
        void shouldAllowQueriesOnUnmodifiableModel() throws StorageException {
            when(mockQueryOps.contains(any(StatementPattern.class))).thenReturn(true);

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
