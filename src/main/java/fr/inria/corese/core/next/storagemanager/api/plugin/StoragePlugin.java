package fr.inria.corese.core.next.storagemanager.api.plugin;

import fr.inria.corese.core.next.storagemanager.api.StorageManager;
import fr.inria.corese.core.next.storagemanager.api.support.config.StorageConfig;

/**
 * Service Provider Interface (SPI) for StorageManager plugins.
 */
public interface StoragePlugin {

    /**
     * Returns the unique name of this plugin.
     *
     * @return the plugin name (must be unique and non-null)
     */
    String getName();

    /**
     * Returns a human-readable description of this plugin.
     *
     * @return the plugin description (never null)
     */
    default String getDescription() {
        return "StorageManager plugin: " + getName();
    }

    /**
     * Checks if this plugin supports the given configuration.
     *
     *
     * @param config the storage configuration to check (never null)
     * @return {@code true} if this plugin can create a StorageManager for this config
     * @throws IllegalArgumentException if config is null
     */
    boolean supports(StorageConfig config);

    /**
     * Creates a StorageManager instance from the given configuration.
     *
     * @param config the storage configuration (never null)
     * @return a configured StorageManager instance (never null)
     * @throws PluginException          if the StorageManager cannot be created
     * @throws IllegalArgumentException if config is null
     */
    StorageManager create(StorageConfig config) throws PluginException;

    /**
     * Returns the priority of this plugin.
     *
     * @return the plugin priority
     */
    default int getPriority() {
        return 0;
    }
}