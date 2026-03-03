package fr.inria.corese.core.next.storagemanager.impl.graph;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.storagemanager.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;
import fr.inria.corese.core.next.storagemanager.api.support.model.StorageStatistics;

import java.util.Collections;
import java.util.Set;

/**
 * Metadata operations implementation for {@link GraphStorageManager}.
 */
public class GraphMetadataOperations implements MetadataOperations {

    private final GraphAdapter adapter;

    /**
     * Constructs a new GraphMetadataOperations.
     *
     * @param adapter the GraphAdapter for Graph access (must not be null)
     * @throws IllegalArgumentException if adapter is null
     */
    public GraphMetadataOperations(GraphAdapter adapter) {
        if (adapter == null) throw new IllegalArgumentException("GraphAdapter cannot be null");
        this.adapter = adapter;
    }

    /**
     * Returns all unique predicate IRIs in the Graph.
     *
     * @return unmodifiable set of all predicates
     * @throws StorageException if the operation fails
     */
    @Override
    public Set<IRI> getPredicates() throws StorageException {
        return Collections.unmodifiableSet(adapter.getPredicates());
    }

    /**
     * Returns all unique subject resources in the Graph.
     *
     * @return unmodifiable set of all subjects
     */
    @Override
    public Set<Resource> getSubjects() {
        return Collections.unmodifiableSet(adapter.getSubjects());
    }

    /**
     * Returns all unique object values in the Graph.
     *
     * @return unmodifiable set of all objects
     */
    @Override
    public Set<Value> getObjects() {
        return Collections.unmodifiableSet(adapter.getObjects());
    }

    /**
     * Returns all unique context (named graph) identifiers in the Graph.
     *
     * @return unmodifiable set of all contexts
     * @throws StorageException if the operation fails
     */
    @Override
    public Set<Resource> getContexts() throws StorageException {
        return Collections.unmodifiableSet(adapter.getContexts());
    }

    /**
     * Returns aggregate statistics about the Graph.
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
