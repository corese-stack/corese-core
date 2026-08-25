package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.storage.api.exception.StorageException;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MemoryMutationOperationsTest {

    private InMemoryStatementStore store;
    private MemoryMutationOperations mutations;

    @BeforeEach
    void setUp() {
        store = mock(InMemoryStatementStore.class);
        mutations = new MemoryMutationOperations(store);
    }

    @Test
    void rejectsMissingDependenciesAndArguments() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryMutationOperations(null));
        assertThrows(NullPointerException.class, () -> mutations.add(null));
        assertThrows(NullPointerException.class, () -> mutations.remove((Statement) null));
        assertThrows(NullPointerException.class, () -> mutations.remove((StatementPattern) null));
    }

    @Test
    void reportsWhetherSingleStatementMutationsChangedTheStore() {
        Statement statement = mock(Statement.class);
        when(store.add(statement)).thenReturn(true, false);
        when(store.remove(statement)).thenReturn(true, false);

        assertTrue(mutations.add(statement));
        assertFalse(mutations.add(statement));
        assertTrue(mutations.remove(statement));
        assertFalse(mutations.remove(statement));
    }

    @Test
    void wrapsBackendFailures() {
        Statement statement = mock(Statement.class);
        when(store.add(statement)).thenThrow(new IllegalStateException("failed"));

        assertThrows(StorageException.class, () -> mutations.add(statement));
    }

    @Test
    void removesMatchingStatementsAndReturnsTheirCount() {
        Statement first = mock(Statement.class);
        Statement second = mock(Statement.class);
        when(store.find(isNull(), isNull(), isNull(), any(Resource[].class)))
                .thenReturn(Set.of(first, second));

        assertEquals(2, mutations.remove(StatementPattern.matchAll()));
        verify(store).remove(first);
        verify(store).remove(second);
    }

    @Test
    void clearsAllOrSelectedGraphsAndReturnsTheRemovedCount() {
        Resource first = mock(Resource.class);
        Resource second = mock(Resource.class);
        Statement firstMatch = mock(Statement.class);
        Statement secondMatch = mock(Statement.class);
        when(store.size()).thenReturn(10, 0);
        when(store.find(null, null, null, new Resource[]{first, second}))
                .thenReturn(Set.of(firstMatch, secondMatch));

        assertEquals(10, mutations.clear());
        assertEquals(2, mutations.clear(first, second));
        verify(store).clear();
        verify(store).remove(firstMatch);
        verify(store).remove(secondMatch);
    }
}
