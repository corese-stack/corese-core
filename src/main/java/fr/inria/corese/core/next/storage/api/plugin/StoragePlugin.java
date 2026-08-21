package fr.inria.corese.core.next.storage.api.plugin;

import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;

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
     * <p>When multiple plugins support the same configuration (i.e., their
     * {@link #supports(StorageConfig)} method returns {@code true}), the plugin
     * with the <b>highest priority</b> is selected.
     *
     * <p><b>Priority Semantics:</b>
     * <ul>
     *   <li><b>Higher values = Higher priority:</b> A plugin with priority 100 will be
     *       selected over one with priority 50</li>
     *   <li><b>Default is 0:</b> If this method is not overridden, the plugin has priority 0</li>
     *   <li><b>Negative values are allowed:</b> Use negative priorities for fallback plugins
     *       that should only be used when no other plugin is available</li>
     * </ul>
     *
     *
     * <p><b>Built-in Priorities:</b>
     * <ul>
     *   <li>GraphStoragePlugin: 100</li>
     *   <li>MemoryStoragePlugin: 50</li>
     * </ul>
     *
     *
     * <p><b>Tie-Breaking:</b> If multiple plugins have the same priority and support
     * the same configuration, the selection is non-deterministic. Avoid this by ensuring
     * each plugin has a unique priority for overlapping configurations.
     *
     * @return the plugin priority (higher values = higher priority, default is 0)
     */
    default int getPriority() {
        return 0;
    }
}