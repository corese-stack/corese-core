package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.query.api.exception.RepositoryException;
import fr.inria.corese.core.next.query.api.repository.Repository;
import fr.inria.corese.core.next.query.api.repository.RepositoryConnection;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.lifecycle.LifecycleState;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.exception.StorageException;


/**
 * Corese implementation of {@link Repository}.
 *
 * <p>Wraps a {@link StorageManager} and exposes the public query API through
 * {@link RepositoryConnection} instances. Users create connections via
 * {@link #getConnection()} and interact only with the public query API —
 * the parser, AST, bridge, and KGRAM remain invisible.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * StorageManager storage = MemoryStorageManager.builder().build();
 * Repository repo = new CoreseRepository(storage);
 * repo.init();
 *
 * try (RepositoryConnection conn = repo.getConnection()) {
 *     TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");
 *     try (TupleQueryResult result = q.evaluate()) {
 *         result.stream().forEach(bs -> System.out.println(bs));
 *     }
 * }
 * repo.shutDown();
 * }</pre>
 */
public final class CoreseRepository implements Repository {

    private final StorageManager storage;
    private final ValueFactory valueFactory;

    public CoreseRepository(StorageManager storage) {
        this.storage = storage;
        this.valueFactory = new CoreseValueFactory();
    }

    @Override
    public void init() throws RepositoryException {
        if (isInitialized()) {
            throw new RepositoryException("Repository is already initialized.");
        }
        try {
            storage.getLifecycle().initialize(StorageConfig.builder().build());
        } catch (StorageException e) {
            throw new RepositoryException("Failed to initialize repository: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isInitialized() {
        return storage.getLifecycle().getState() == LifecycleState.RUNNING;
    }

    @Override
    public void shutDown() throws RepositoryException {
        try {
            storage.getLifecycle().shutdown();
        } catch (StorageException e) {
            throw new RepositoryException("Failed to shut down repository: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isWritable() {
        return isInitialized();
    }

    @Override
    public RepositoryConnection getConnection() throws RepositoryException {
        if (!isInitialized()) {
            throw new RepositoryException("Repository is not initialized. Call init() first.");
        }
        return new CoreseRepositoryConnection(this, storage);
    }

    @Override
    public ValueFactory getValueFactory() {
        return valueFactory;
    }
}
