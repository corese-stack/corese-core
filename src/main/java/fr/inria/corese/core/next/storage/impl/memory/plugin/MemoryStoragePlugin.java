package fr.inria.corese.core.next.storage.impl.memory.plugin;

import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.plugin.PluginException;
import fr.inria.corese.core.next.storage.api.plugin.StoragePlugin;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;

/**
 * Plugin for MemoryStorageManager
 */
public class MemoryStoragePlugin implements StoragePlugin {

    private static final String PLUGIN_NAME = "memory";

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getDescription() {
        return "In-memory HashMap backend (testing only, no persistence)";
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
        if (config == null) {
            throw new IllegalArgumentException("StorageConfig must not be null");
        }


        try {
            MemoryStorageManager storage = MemoryStorageManager.builder().build();

            // Initialize lifecycle
            storage.getLifecycle().initialize(config);

            return storage;

        } catch (Exception e) {
            throw new PluginException("Failed to create MemoryStorageManager", e);
        }
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
