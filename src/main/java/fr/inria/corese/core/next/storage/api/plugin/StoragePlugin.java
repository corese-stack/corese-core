package fr.inria.corese.core.next.storage.api.plugin;

import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;

/**
 * Service-provider interface for RDF storage backends discovered through
 * {@link java.util.ServiceLoader}.
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
     * @param config storage configuration to check
     * @return {@code true} if this plugin can create a manager for the
     *         configuration; {@code false} when unsupported or {@code null}
     */
    boolean supports(StorageConfig config);

    /**
     * Creates a StorageManager instance from the given configuration.
     *
     * @param config the storage configuration (never null)
     * @return an initialized, open StorageManager instance (never null); ownership
     *         is transferred to the caller
     * @throws PluginException          if the StorageManager cannot be created
     * @throws NullPointerException if {@code config} is {@code null}
     */
    StorageManager create(StorageConfig config) throws PluginException;

    /**
     * Returns the selection priority. Higher values win; equal priorities are
     * ordered by plugin name.
     *
     * @return selection priority, defaulting to zero
     */
    default int getPriority() {
        return 0;
    }
}
