package fr.inria.corese.core.next.storage.impl.graph;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.StorageManagerBuilder;
import fr.inria.corese.core.next.storage.api.lifecycle.StorageLifecycle;
import fr.inria.corese.core.next.storage.api.operations.BulkOperations;
import fr.inria.corese.core.next.storage.api.operations.MetadataOperations;
import fr.inria.corese.core.next.storage.api.operations.MutationOperations;
import fr.inria.corese.core.next.storage.api.operations.QueryOperations;
import fr.inria.corese.core.next.storage.api.transaction.TransactionManager;

/**
 * StorageManager implementation wrapping the legacy {@link Graph} class.
 *
 */
public final class GraphStorageManager implements StorageManager {

    private final CoreseGraphStatementStore adapter;
    private final QueryOperations queryOps;
    private final MutationOperations mutationOps;
    private final MetadataOperations metadataOps;
    private final BulkOperations bulkOps;
    private final TransactionManager txManager;
    private final StorageLifecycle lifecycle;

    private GraphStorageManager(Builder builder) {
        if (builder.graph == null) {
            throw new IllegalArgumentException("Graph cannot be null");
        }
        if (builder.valueFactory == null) {
            throw new IllegalArgumentException("ValueFactory cannot be null");
        }

        this.adapter = new CoreseGraphStatementStore(builder.graph, builder.valueFactory);
        this.queryOps = new GraphQueryOperations(adapter);
        this.mutationOps = new GraphMutationOperations(adapter);
        this.metadataOps = new GraphMetadataOperations(adapter);
        this.bulkOps = new GraphBulkOperations(adapter);
        this.txManager = new GraphTransactionManager();
        this.lifecycle = new GraphLifecycleManager(builder.graph);
    }

    @Override
    public QueryOperations getQueryOperations() {
        return queryOps;
    }

    @Override
    public MutationOperations getMutationOperations() {
        return mutationOps;
    }

    @Override
    public MetadataOperations getMetadataOperations() {
        return metadataOps;
    }

    @Override
    public BulkOperations getBulkOperations() {
        return bulkOps;
    }

    @Override
    public TransactionManager getTransactionManager() {
        return txManager;
    }

    @Override
    public StorageLifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * Returns the underlying legacy Graph instance.
     *
     * @return the wrapped Graph
     */
    public Graph getGraph() {
        return adapter.graph();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for GraphStorageManager.
     */
    public static final class Builder implements StorageManagerBuilder {
        private Graph graph;
        private ValueFactory valueFactory;

        /**
         * Sets the legacy Graph instance to wrap.
         *
         * @param graph the Graph instance (must not be null)
         * @return this builder
         */
        public Builder graph(Graph graph) {
            this.graph = graph;
            return this;
        }

        /**
         * Sets the ValueFactory for creating Storage API objects.
         *
         * @param valueFactory the factory (must not be null)
         * @return this builder
         */
        public Builder valueFactory(ValueFactory valueFactory) {
            this.valueFactory = valueFactory;
            return this;
        }

        @Override
        public GraphStorageManager build() {
            if (graph == null) {
                throw new IllegalStateException("Graph must be set before building");
            }
            if (valueFactory == null) {
                throw new IllegalStateException("ValueFactory must be set before building");
            }
            return new GraphStorageManager(this);
        }
    }

    @Override
    public String toString() {
        return "GraphStorageManager{size=" + adapter.size() + "}";
    }
}
