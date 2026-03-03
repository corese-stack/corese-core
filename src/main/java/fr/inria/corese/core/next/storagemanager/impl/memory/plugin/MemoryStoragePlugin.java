package fr.inria.corese.core.next.storagemanager.impl.memory.plugin;

import fr.inria.corese.core.next.storagemanager.api.StorageManager;
import fr.inria.corese.core.next.storagemanager.api.plugin.PluginException;
import fr.inria.corese.core.next.storagemanager.api.plugin.StoragePlugin;
import fr.inria.corese.core.next.storagemanager.api.support.config.StorageConfig;
import fr.inria.corese.core.next.storagemanager.impl.memory.MemoryStorageManager;

/**
 * Plugin for MemoryStorageManager
 */
public class MemoryStoragePlugin implements StoragePlugin {

    @Override
    public String getName() {
        return "memory";
    }

    @Override
    public String getDescription() {
        return "In-memory HashMap backend (testing only, no persistence)";
    }


    @Override
    public StorageManager create(StorageConfig config) throws PluginException {
        if (config == null) {
            throw new IllegalArgumentException("StorageConfig must not be null");
        }


        try {
            // Build MemoryStorageManager (no dependencies required)
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
