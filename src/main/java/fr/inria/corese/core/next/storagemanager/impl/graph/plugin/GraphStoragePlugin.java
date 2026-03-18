package fr.inria.corese.core.next.storagemanager.impl.graph.plugin;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.storagemanager.api.StorageManager;
import fr.inria.corese.core.next.storagemanager.api.plugin.PluginException;
import fr.inria.corese.core.next.storagemanager.api.plugin.StoragePlugin;
import fr.inria.corese.core.next.storagemanager.api.support.config.StorageConfig;
import fr.inria.corese.core.next.storagemanager.impl.graph.GraphStorageManager;

/**
 * Plugin for GraphStorageManager - wraps legacy Corese Graph backend.
 */
public class GraphStoragePlugin implements StoragePlugin {

    @Override
    public String getName() {
        return "graph";
    }

    @Override
    public String getDescription() {
        return "Legacy Corese Graph backend (production-ready, indexed, thread-safe)";
    }

    @Override
    public boolean supports(StorageConfig config) {
        if (config == null) {
            return false;
        }
        return config.getType()
                .map("graph"::equalsIgnoreCase)
                .orElse(false);
    }

    @Override
    public StorageManager create(StorageConfig config) throws PluginException {
        try {
            Graph graph = config.getProperty("graph", Graph.class)
                    .orElseThrow(() -> new PluginException("Graph instance required in config properties"));

            ValueFactory factory = config.getProperty("valueFactory", ValueFactory.class)
                    .orElseThrow(() -> new PluginException("ValueFactory required in config properties"));

            GraphStorageManager storage = GraphStorageManager.builder()
                    .graph(graph)
                    .valueFactory(factory)
                    .build();

            storage.getLifecycle().initialize(config);

            return storage;
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginException("Failed to create GraphStorageManager", e);
        }
    }

    @Override
    public int getPriority() {
        return 100;
    }
}