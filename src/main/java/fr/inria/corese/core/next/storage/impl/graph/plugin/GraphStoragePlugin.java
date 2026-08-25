package fr.inria.corese.core.next.storage.impl.graph.plugin;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.plugin.PluginException;
import fr.inria.corese.core.next.storage.api.plugin.StoragePlugin;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.impl.graph.GraphStorageManager;

import java.util.Objects;

/**
 * Plugin for GraphStorageManager - wraps legacy Corese Graph backend.
 */
public class GraphStoragePlugin implements StoragePlugin {

    private static final String PLUGIN_NAME = "graph";

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getDescription() {
        return "Compatibility adapter for the legacy Corese Graph backend";
    }

    @Override
    public boolean supports(StorageConfig config) {
        if (config == null) {
            return false;
        }
        return config.getType()
                .map(PLUGIN_NAME::equalsIgnoreCase)
                .orElse(false);
    }

    @Override
    public StorageManager create(StorageConfig config) throws PluginException {
        StorageConfig checkedConfig = Objects.requireNonNull(config, "config");
        try {
            Graph graph = checkedConfig.getProperty(PLUGIN_NAME, Graph.class)
                    .orElseThrow(() -> new PluginException("Graph instance required in config properties"));

            ValueFactory factory = checkedConfig.getProperty("valueFactory", ValueFactory.class)
                    .orElseThrow(() -> new PluginException("ValueFactory required in config properties"));

            GraphStorageManager storage = GraphStorageManager.builder()
                    .graph(graph)
                    .valueFactory(factory)
                    .build();

            storage.lifecycle().initialize(checkedConfig);

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
