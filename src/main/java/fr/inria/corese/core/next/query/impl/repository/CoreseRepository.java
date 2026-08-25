package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.exception.StorageException;

import java.util.Objects;

/**
 * Corese implementation of {@link Repository}.
 *
 * <p>Wraps a {@link StorageManager} and exposes the public query API through
 * {@link RepositoryConnection} instances. Users create connections via
 * {@link #getConnection()} and interact only with the public query API —
 * the parser, AST, bridge, and KGRAM remain invisible.</p>
 *
 * <p>This implementation class is instantiated by the public repository
 * factory. The repository takes ownership of its storage manager and is ready
 * for use when construction returns.</p>
 */
public final class CoreseRepository implements Repository {

    private final StorageManager storage;
    private final StorageLifecycle lifecycle;
    private final ValueFactory valueFactory;
    private volatile boolean open;

    public CoreseRepository(StorageManager storage) {
        this(storage, StorageConfig.builder().build());
    }

    public CoreseRepository(StorageManager storage, StorageConfig config) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.lifecycle = Objects.requireNonNull(storage.getLifecycle(), "storage lifecycle");
        this.valueFactory = new CoreseValueFactory();
        initialize(Objects.requireNonNull(config, "config"));
        this.open = true;
    }

    private void initialize(StorageConfig config) {
        LifecycleState state = lifecycle.getState();
        if (state == LifecycleState.RUNNING) {
            return;
        }
        if (state != LifecycleState.NOT_INITIALIZED) {
            throw new RepositoryException(
                    "Cannot create a repository from storage in state " + state + '.');
        }
        try {
            lifecycle.initialize(config);
        } catch (StorageException | IllegalStateException e) {
            throw new RepositoryException("Failed to initialize repository: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isOpen() {
        return open && lifecycle.getState() == LifecycleState.RUNNING;
    }

    @Override
    public synchronized void close() throws RepositoryException {
        if (!open) {
            return;
        }
        open = false;
        if (lifecycle.getState() != LifecycleState.RUNNING) {
            return;
        }
        try {
            lifecycle.shutdown();
        } catch (StorageException | IllegalStateException e) {
            throw new RepositoryException("Failed to shut down repository: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isWritable() {
        return isOpen();
    }

    @Override
    public RepositoryConnection getConnection() throws RepositoryException {
        if (!isOpen()) {
            throw new RepositoryException("This repository is closed.");
        }
        return new CoreseRepositoryConnection(this, storage);
    }

    @Override
    public ValueFactory getValueFactory() {
        return valueFactory;
    }
}
