package fr.inria.corese.core.next.storagemanager.impl.memory;

import fr.inria.corese.core.next.data.api.*;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InMemoryStatementStore.
 */
class InMemoryStatementStoreTest {

    private InMemoryStatementStore adapter;
    private ValueFactory valueFactory;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryStatementStore();
        valueFactory = new CoreseValueFactory();
    }

    @Nested
    @DisplayName("CRUD operations")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should add statement successfully")
        void shouldAddStatementSuccessfully() {
            Statement stmt = mock(Statement.class);

            boolean added = adapter.add(stmt);

            assertTrue(added);
            assertEquals(1, adapter.size());
            assertTrue(adapter.contains(stmt));
        }

        @Test
        @DisplayName("Should not add duplicate statement")
        void shouldNotAddDuplicateStatement() {
            Statement stmt = mock(Statement.class);
            adapter.add(stmt);

            boolean added = adapter.add(stmt);

            assertFalse(added);
            assertEquals(1, adapter.size());
        }

        @Test
        @DisplayName("Should remove statement successfully")
        void shouldRemoveStatementSuccessfully() {
            Statement stmt = mock(Statement.class);
            adapter.add(stmt);

            boolean removed = adapter.remove(stmt);

            assertTrue(removed);
            assertEquals(0, adapter.size());
            assertFalse(adapter.contains(stmt));
        }

        @Test
        @DisplayName("Should return false when removing non-existent statement")
        void shouldReturnFalseWhenRemovingNonExistentStatement() {
            Statement stmt = mock(Statement.class);

            boolean removed = adapter.remove(stmt);

            assertFalse(removed);
        }

        @Test
        @DisplayName("Should check if statement exists")
        void shouldCheckIfStatementExists() {
            Statement stmt = mock(Statement.class);

            assertFalse(adapter.contains(stmt));

            adapter.add(stmt);
            assertTrue(adapter.contains(stmt));
        }

        @Test
        @DisplayName("Should return correct size")
        void shouldReturnCorrectSize() {
            assertEquals(0, adapter.size());

            adapter.add(mock(Statement.class));
            assertEquals(1, adapter.size());

            adapter.add(mock(Statement.class));
            assertEquals(2, adapter.size());
        }

        @Test
        @DisplayName("Should clear all statements")
        void shouldClearAllStatements() {
            adapter.add(mock(Statement.class));
            adapter.add(mock(Statement.class));
            adapter.add(mock(Statement.class));

            adapter.clear();

            assertEquals(0, adapter.size());
        }
    }

    @Nested
    @DisplayName("Pattern matching")
    class PatternMatchingTests {

        @Test
        @DisplayName("Should find all statements with null pattern")
        void shouldFindAllStatementsWithNullPattern() {
            Statement stmt1 = mock(Statement.class);
            Statement stmt2 = mock(Statement.class);
            adapter.add(stmt1);
            adapter.add(stmt2);

            Set<Statement> results = adapter.find(null, null, null, null);

            assertEquals(2, results.size());
            assertTrue(results.contains(stmt1));
            assertTrue(results.contains(stmt2));
        }

        @Test
        @DisplayName("Should find statements by subject")
        void shouldFindStatementsBySubject() {
            Resource subject1 = mock(Resource.class);
            Resource subject2 = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);

            Statement stmt1 = mock(Statement.class);
            when(stmt1.getSubject()).thenReturn(subject1);
            when(stmt1.getPredicate()).thenReturn(predicate);
            when(stmt1.getObject()).thenReturn(object);

            Statement stmt2 = mock(Statement.class);
            when(stmt2.getSubject()).thenReturn(subject2);
            when(stmt2.getPredicate()).thenReturn(predicate);
            when(stmt2.getObject()).thenReturn(object);

            adapter.add(stmt1);
            adapter.add(stmt2);

            Set<Statement> results = adapter.find(subject1, null, null, null);

            assertEquals(1, results.size());
            assertTrue(results.contains(stmt1));
        }

        @Test
        @DisplayName("Should find statements by predicate")
        void shouldFindStatementsByPredicate() {
            Resource subject = mock(Resource.class);
            IRI predicate1 = mock(IRI.class);
            IRI predicate2 = mock(IRI.class);
            Value object = mock(Value.class);

            Statement stmt1 = mock(Statement.class);
            when(stmt1.getSubject()).thenReturn(subject);
            when(stmt1.getPredicate()).thenReturn(predicate1);
            when(stmt1.getObject()).thenReturn(object);

            Statement stmt2 = mock(Statement.class);
            when(stmt2.getSubject()).thenReturn(subject);
            when(stmt2.getPredicate()).thenReturn(predicate2);
            when(stmt2.getObject()).thenReturn(object);

            adapter.add(stmt1);
            adapter.add(stmt2);

            Set<Statement> results = adapter.find(null, predicate1, null, null);

            assertEquals(1, results.size());
            assertTrue(results.contains(stmt1));
        }

        @Test
        @DisplayName("Should find statements by context")
        void shouldFindStatementsByContext() {
            Resource subject = valueFactory.createIRI("http://example.org/s");
            IRI predicate = valueFactory.createIRI("http://example.org/p");
            Value object = valueFactory.createLiteral("value");
            Resource context1 = valueFactory.createIRI("http://example.org/graph1");
            Resource equivalentContext1 = valueFactory.createIRI("http://example.org/graph1");
            Resource context2 = valueFactory.createIRI("http://example.org/graph2");

            Statement stmt1 = valueFactory.createStatement(subject, predicate, object, context1);
            Statement stmt2 = valueFactory.createStatement(subject, predicate, object, context2);

            adapter.add(stmt1);
            adapter.add(stmt2);

            Set<Statement> results = adapter.find(null, null, null, new Resource[]{equivalentContext1});

            assertEquals(1, results.size());
            assertTrue(results.contains(stmt1));
        }

        @Test
        @DisplayName("Should not match IRI and blank node contexts sharing the same string value")
        void shouldNotMatchIriAndBlankNodeContextsWithSameStringValue() {
            Resource subject = valueFactory.createIRI("http://example.org/s");
            IRI predicate = valueFactory.createIRI("http://example.org/p");
            Value object = valueFactory.createLiteral("value");
            BNode blankNodeContext = valueFactory.createBNode("http://example.org/g");
            IRI iriContext = valueFactory.createIRI("http://example.org/g");

            Statement stmt = valueFactory.createStatement(subject, predicate, object, blankNodeContext);
            adapter.add(stmt);

            Set<Statement> results = adapter.find(null, null, null, new Resource[]{iriContext});

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should return empty set when no matches")
        void shouldReturnEmptySetWhenNoMatches() {
            adapter.add(mock(Statement.class));

            Resource unknownSubject = mock(Resource.class);
            Set<Statement> results = adapter.find(unknownSubject, null, null, null);

            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("Context operations")
    class ContextOperationsTests {

        @Test
        @DisplayName("Should clear specific context")
        void shouldClearSpecificContext() {
            Resource context1 = mock(Resource.class);
            Resource context2 = mock(Resource.class);

            Statement stmt1 = mock(Statement.class);
            when(stmt1.getContext()).thenReturn(context1);

            Statement stmt2 = mock(Statement.class);
            when(stmt2.getContext()).thenReturn(context2);

            adapter.add(stmt1);
            adapter.add(stmt2);

            adapter.clearContext(context1);

            assertEquals(1, adapter.size());
            assertFalse(adapter.contains(stmt1));
            assertTrue(adapter.contains(stmt2));
        }

        @Test
        @DisplayName("Should handle clearing non-existent context")
        void shouldHandleClearingNonExistentContext() {
            adapter.add(mock(Statement.class));
            Resource unknownContext = mock(Resource.class);

            adapter.clearContext(unknownContext);

            assertEquals(1, adapter.size());
        }
    }

    @Nested
    @DisplayName("Metadata operations")
    class MetadataOperationsTests {

        @Test
        @DisplayName("Should return all subjects")
        void shouldReturnAllSubjects() {
            Resource subject1 = mock(Resource.class);
            Resource subject2 = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object = mock(Value.class);

            Statement stmt1 = mock(Statement.class);
            when(stmt1.getSubject()).thenReturn(subject1);
            when(stmt1.getPredicate()).thenReturn(predicate);
            when(stmt1.getObject()).thenReturn(object);

            Statement stmt2 = mock(Statement.class);
            when(stmt2.getSubject()).thenReturn(subject2);
            when(stmt2.getPredicate()).thenReturn(predicate);
            when(stmt2.getObject()).thenReturn(object);

            adapter.add(stmt1);
            adapter.add(stmt2);

            Set<Resource> subjects = adapter.getSubjects();

            assertEquals(2, subjects.size());
            assertTrue(subjects.contains(subject1));
            assertTrue(subjects.contains(subject2));
        }

        @Test
        @DisplayName("Should return all predicates")
        void shouldReturnAllPredicates() {
            Resource subject = mock(Resource.class);
            IRI predicate1 = mock(IRI.class);
            IRI predicate2 = mock(IRI.class);
            Value object = mock(Value.class);

            Statement stmt1 = mock(Statement.class);
            when(stmt1.getSubject()).thenReturn(subject);
            when(stmt1.getPredicate()).thenReturn(predicate1);
            when(stmt1.getObject()).thenReturn(object);

            Statement stmt2 = mock(Statement.class);
            when(stmt2.getSubject()).thenReturn(subject);
            when(stmt2.getPredicate()).thenReturn(predicate2);
            when(stmt2.getObject()).thenReturn(object);

            adapter.add(stmt1);
            adapter.add(stmt2);

            Set<IRI> predicates = adapter.getPredicates();

            assertEquals(2, predicates.size());
            assertTrue(predicates.contains(predicate1));
            assertTrue(predicates.contains(predicate2));
        }

        @Test
        @DisplayName("Should return all objects")
        void shouldReturnAllObjects() {
            Resource subject = mock(Resource.class);
            IRI predicate = mock(IRI.class);
            Value object1 = mock(Value.class);
            Value object2 = mock(Value.class);

            Statement stmt1 = mock(Statement.class);
            when(stmt1.getSubject()).thenReturn(subject);
            when(stmt1.getPredicate()).thenReturn(predicate);
            when(stmt1.getObject()).thenReturn(object1);

            Statement stmt2 = mock(Statement.class);
            when(stmt2.getSubject()).thenReturn(subject);
            when(stmt2.getPredicate()).thenReturn(predicate);
            when(stmt2.getObject()).thenReturn(object2);

            adapter.add(stmt1);
            adapter.add(stmt2);

            Set<Value> objects = adapter.getObjects();

            assertEquals(2, objects.size());
            assertTrue(objects.contains(object1));
            assertTrue(objects.contains(object2));
        }

        @Test
        @DisplayName("Should return all contexts excluding null")
        void shouldReturnAllContextsExcludingNull() {
            Resource context1 = mock(Resource.class);
            Resource context2 = mock(Resource.class);

            Statement stmt1 = mock(Statement.class);
            when(stmt1.getContext()).thenReturn(context1);

            Statement stmt2 = mock(Statement.class);
            when(stmt2.getContext()).thenReturn(context2);

            Statement stmt3 = mock(Statement.class);
            when(stmt3.getContext()).thenReturn(null); // Default graph

            adapter.add(stmt1);
            adapter.add(stmt2);
            adapter.add(stmt3);

            Set<Resource> contexts = adapter.getContexts();

            assertEquals(2, contexts.size());
            assertTrue(contexts.contains(context1));
            assertTrue(contexts.contains(context2));
        }
    }
}
