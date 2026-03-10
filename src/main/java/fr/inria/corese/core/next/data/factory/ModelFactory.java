package fr.inria.corese.core.next.data.factory;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.impl.StorageModel;
import fr.inria.corese.core.next.storagemanager.api.plugin.PluginException;
import fr.inria.corese.core.next.storagemanager.api.plugin.StoragePluginManager;
import fr.inria.corese.core.next.storagemanager.api.support.config.StorageConfig;

/**
 * Factory class for creating Model instances with different storage backends.
 *
 * <p>This factory simplifies the creation of models by providing convenient methods
 * to create models backed by different storage implementations (Memory, Graph, etc.)
 * without requiring explicit StorageManager configuration.
 *
 * <h2>Supported Storage Types</h2>
 * <ul>
 *   <li><b>"memory"</b> - In-memory HashMap-based storage (fast, no persistence)</li>
 *   <li><b>"graph"</b> - Legacy Graph-based storage (indexed, persistent)</li>
 * </ul>
 *
 * @see Model
 * @see StorageModel
 * @see StoragePluginManager
 * @see ValueFactory
 */
public record ModelFactory(ValueFactory valueFactory) {

    /**
     * Constructs a new ModelFactory with the specified ValueFactory.
     *
     * @param valueFactory the ValueFactory to use for creating RDF values
     * @throws NullPointerException if valueFactory is null
     */
    public ModelFactory {
        if (valueFactory == null) {
            throw new NullPointerException("ValueFactory cannot be null");
        }
    }

    /**
     * Creates a new Model instance with the specified storage backend type.
     *
     * <p>This method provides a convenient way to create models without manually
     * configuring the StorageManager and StorageConfig. The storage backend is
     * selected based on the provided type string.
     *
     * @param storageType the storage backend type ("memory" or "graph")
     * @return a new Model instance backed by the specified storage type
     * @throws PluginException if the memory storage fails to initialize
     */
    public Model createModel(String storageType) throws PluginException {
        StorageConfig config = switch (storageType) {
            case "memory" -> createMemoryConfig();
            case "graph" -> createGraphConfig();
            default -> throw new IllegalArgumentException(
                    "Unknown storage type: '" + storageType + "'. " +
                            "Supported types: [memory, graph]");
        };

        return StorageModel.builder()
                .storage(StoragePluginManager.create(config))
                .valueFactory(valueFactory)
                .build();
    }

    /**
     * Creates a new memory-based Model instance.
     *
     * <p>Memory storage uses a ConcurrentHashMap backend for fast, in-memory storage.
     * This is ideal for testing, prototyping, and small datasets (&lt; 100K statements).
     *
     * @return a new Model instance backed by memory storage
     * @throws PluginException if the memory storage fails to initialize
     * @see #createGraphModel()
     */
    public Model createMemoryModel() throws PluginException {
        StorageConfig config = createMemoryConfig();

        return StorageModel.builder()
                .storage(StoragePluginManager.create(config))
                .valueFactory(valueFactory)
                .build();
    }

    /**
     * Creates a new graph-based Model instance.
     *
     * <p>Graph storage wraps the legacy Corese Graph implementation, providing
     * indexed access, persistence support, and excellent performance for large datasets.
     * This is the recommended choice for production use.
     *
     * @return a new Model instance backed by a new Graph storage
     * @throws PluginException if the graph storage fails to initialize
     * @see #createGraphModel(Graph)
     * @see #createMemoryModel()
     */
    public Model createGraphModel() throws PluginException {
        StorageConfig config = createGraphConfig();

        return StorageModel.builder()
                .storage(StoragePluginManager.create(config))
                .valueFactory(valueFactory)
                .build();
    }

    /**
     * Creates a new graph-based Model instance backed by the specified Graph.
     *
     * <p>This method allows you to wrap an existing Graph instance with the Model API,
     * enabling you to use legacy Graph-based code with the new Model interface.
     * All operations on the returned Model will delegate to the underlying Graph.
     *
     * @param graph the Graph instance to wrap with the Model API
     * @return a new Model instance backed by the specified Graph
     * @throws NullPointerException if graph is null
     * @throws PluginException      if the graph storage fails to initialize
     * @see #createGraphModel()
     */
    public Model createGraphModel(Graph graph) throws PluginException {
        if (graph == null) {
            throw new NullPointerException("Graph cannot be null");
        }

        StorageConfig config = StorageConfig.builder()
                .property("type", "graph")
                .property("graph", graph)
                .property("valueFactory", valueFactory)
                .build();

        return StorageModel.builder()
                .storage(StoragePluginManager.create(config))
                .valueFactory(valueFactory)
                .build();
    }

    /**
     * Returns the ValueFactory used by this factory.
     *
     * <p>The ValueFactory is used to create RDF values (IRIs, Literals, BNodes)
     * for all models created by this factory.
     *
     * @return the ValueFactory instance
     */
    @Override
    public ValueFactory valueFactory() {
        return valueFactory;
    }

    /**
     * Creates a StorageConfig for memory-based storage.
     *
     * @return a StorageConfig configured for memory storage
     */
    private StorageConfig createMemoryConfig() {
        return StorageConfig.builder()
                .property("type", "memory")
                .build();
    }

    /**
     * Creates a StorageConfig for graph-based storage with a new Graph instance.
     *
     * @return a StorageConfig configured for graph storage
     */
    private StorageConfig createGraphConfig() {
        return StorageConfig.builder()
                .property("type", "graph")
                .property("graph", Graph.create())
                .property("valueFactory", valueFactory)
                .build();
    }
}