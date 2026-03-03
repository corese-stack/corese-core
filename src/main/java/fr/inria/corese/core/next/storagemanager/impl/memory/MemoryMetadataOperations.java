package fr.inria.corese.core.next.storagemanager.impl.memory;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.storagemanager.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;
import fr.inria.corese.core.next.storagemanager.api.support.model.StorageStatistics;

import java.util.Collections;
import java.util.Set;

/**
 * Metadata operations implementation for {@link MemoryStorageManager}.
 */
public class MemoryMetadataOperations implements MetadataOperations {

    private final MemoryAdapter adapter;

    /**
     * Constructs a new MemoryMetadataOperations.
     *
     * @param adapter the MemoryAdapter for data access (must not be null)
     * @throws IllegalArgumentException if adapter is null
     */
    public MemoryMetadataOperations(MemoryAdapter adapter) {
        if (adapter == null) throw new IllegalArgumentException("MemoryAdapter cannot be null");
        this.adapter = adapter;
    }

    /**
     * Returns all unique predicate IRIs in the store.
     *
     * @return unmodifiable set of all predicates (never null)
     * @throws StorageException if the operation fails
     */
    @Override
    public Set<IRI> getPredicates() throws StorageException {
        return Collections.unmodifiableSet(adapter.getPredicates());
    }

    /**
     * Returns all unique subject resources in the store.
     *
     * @return unmodifiable set of all subjects (never null)
     */
    @Override
    public Set<Resource> getSubjects() {
        return Collections.unmodifiableSet(adapter.getSubjects());
    }

    /**
     * Returns all unique object values in the store.
     *
     * @return unmodifiable set of all objects (never null)
     */
    @Override
    public Set<Value> getObjects() {
        return Collections.unmodifiableSet(adapter.getObjects());
    }

    /**
     * Returns all unique context (named graph) identifiers in the store.
     *
     * @return unmodifiable set of all contexts (never null)
     * @throws StorageException if the operation fails
     */
    @Override
    public Set<Resource> getContexts() throws StorageException {
        return Collections.unmodifiableSet(adapter.getContexts());
    }

    /**
     * Returns aggregate statistics about the store.
     *
     * @return a {@link StorageStatistics} record with aggregate counts
     */
    @Override
    public StorageStatistics getStatistics() {
        return new StorageStatistics(
                adapter.size(),
                adapter.getSubjects().size(),
                adapter.getPredicates().size(),
                adapter.getObjects().size(),
                adapter.getContexts().size()
        );
    }
}
