package fr.inria.corese.core.next.query;

import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepositoriesTest {

    @Test
    void defaultRepositoryIsImmediatelyUsableThroughPublicApi() {
        try (Repository repository = Repositories.create()) {
            assertTrue(repository.isOpen());
            assertNotNull(repository.getValueFactory());

            repository.update("INSERT DATA { <urn:s> <urn:p> <urn:o> }");
            assertTrue(repository.ask("ASK { <urn:s> <urn:p> <urn:o> }"));
        }
    }

    @Test
    void closeIsIdempotentAndPreventsNewConnections() {
        Repository repository = Repositories.create();
        RepositoryConnection connection = repository.getConnection();

        repository.close();

        assertFalse(repository.isOpen());
        assertFalse(connection.isOpen());
        assertDoesNotThrow(repository::close);
        assertThrows(RepositoryException.class, repository::getConnection);
        connection.close();
    }

    @Test
    void initializesAndOwnsCallerSuppliedStorage() {
        StorageManager storage = mock(StorageManager.class);
        StorageLifecycle lifecycle = mock(StorageLifecycle.class);
        StorageConfig config = StorageConfig.builder().type("test").build();
        when(storage.lifecycle()).thenReturn(lifecycle);
        when(lifecycle.getState())
                .thenReturn(LifecycleState.NOT_INITIALIZED)
                .thenReturn(LifecycleState.RUNNING)
                .thenReturn(LifecycleState.RUNNING);

        Repository repository = Repositories.create(storage, config);
        assertTrue(repository.isOpen());
        repository.close();

        verify(lifecycle).initialize(config);
        verify(lifecycle).shutdown();
    }

    @Test
    void adoptsAnAlreadyRunningStorageWithoutInitializingItAgain() {
        StorageManager storage = mock(StorageManager.class);
        StorageLifecycle lifecycle = mock(StorageLifecycle.class);
        when(storage.lifecycle()).thenReturn(lifecycle);
        when(lifecycle.getState()).thenReturn(LifecycleState.RUNNING);

        try (Repository repository = Repositories.create(storage)) {
            assertTrue(repository.isOpen());
        }

        verify(lifecycle, never()).initialize(any());
        verify(lifecycle).shutdown();
    }

    @Test
    void rejectsStorageThatCannotBeAdopted() {
        StorageManager storage = mock(StorageManager.class);
        StorageLifecycle lifecycle = mock(StorageLifecycle.class);
        when(storage.lifecycle()).thenReturn(lifecycle);
        when(lifecycle.getState()).thenReturn(LifecycleState.SHUTDOWN);

        assertThrows(RepositoryException.class, () -> Repositories.create(storage));
    }

    @Test
    void reportsPluginSelectionFailuresAsRepositoryErrors() {
        StorageConfig config = StorageConfig.builder().type("missing-plugin").build();

        RepositoryException exception = assertThrows(
                RepositoryException.class,
                () -> Repositories.create(config));

        assertNotNull(exception.getCause());
    }

    @Test
    void rejectsNullCreationArguments() {
        assertThrows(NullPointerException.class, () -> Repositories.create((StorageConfig) null));
        assertThrows(NullPointerException.class, () -> Repositories.create((StorageManager) null));
        assertThrows(NullPointerException.class, () -> Repositories.create(mock(StorageManager.class), null));
    }
}
