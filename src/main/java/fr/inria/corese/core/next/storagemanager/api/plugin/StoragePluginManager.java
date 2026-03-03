package fr.inria.corese.core.next.storagemanager.api.plugin;

import fr.inria.corese.core.next.storagemanager.api.StorageManager;
import fr.inria.corese.core.next.storagemanager.api.support.config.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manager for discovering and creating StorageManager plugins.
 */
public class StoragePluginManager {

    private static final Logger logger = LoggerFactory.getLogger(StoragePluginManager.class);

    /**
     * ServiceLoader for discovering plugins
     */
    private static final ServiceLoader<StoragePlugin> serviceLoader =
            ServiceLoader.load(StoragePlugin.class);

    /**
     * Cache of discovered plugins (thread-safe)
     */
    private static volatile List<StoragePlugin> cachedPlugins;

    /**
     * Plugin lookup cache by name (thread-safe)
     */
    private static final Map<String, StoragePlugin> pluginsByName =
            new ConcurrentHashMap<>();

    /**
     * Private constructor - this is a static utility class.
     */
    private StoragePluginManager() {
        throw new AssertionError("StoragePluginManager is a utility class");
    }

    /**
     * Creates a StorageManager instance from the given configuration.
     *
     * @param config the storage configuration (must not be null)
     * @return a configured StorageManager instance
     * @throws IllegalArgumentException if config is null
     * @throws PluginNotFoundException  if no plugin supports the configuration
     * @throws PluginException          if the StorageManager cannot be created
     */
    public static StorageManager create(StorageConfig config) throws PluginException {
        if (config == null) {
            throw new IllegalArgumentException("StorageConfig must not be null");
        }

        // Get all available plugins
        List<StoragePlugin> allPlugins = getAvailablePlugins();

        // Find plugins that support this configuration
        List<StoragePlugin> supportingPlugins = allPlugins.stream()
                .filter(plugin -> plugin.supports(config))
                .sorted(Comparator.comparingInt(StoragePlugin::getPriority).reversed())
                .toList();

        if (supportingPlugins.isEmpty()) {
            String availableTypes = allPlugins.stream()
                    .map(StoragePlugin::getName)
                    .collect(Collectors.joining(", "));

            throw new PluginNotFoundException(
                    String.format("No plugin found for storage type '%s'. Available types: [%s]",
                            config.getType().orElse("not specified"), availableTypes)
            );
        }

        // Select plugin with highest priority
        StoragePlugin selectedPlugin = supportingPlugins.getFirst();

        if (supportingPlugins.size() > 1) {
            String otherPlugins = supportingPlugins.stream()
                    .skip(1)
                    .map(p -> p.getName() + " (priority=" + p.getPriority() + ")")
                    .collect(Collectors.joining(", "));

            logger.warn("Multiple plugins support this configuration. " +
                            "Selected '{}' (priority={}). Ignored: {}",
                    selectedPlugin.getName(),
                    selectedPlugin.getPriority(),
                    otherPlugins);
        }

        try {
            return selectedPlugin.create(config);
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginException(
                    "Failed to create StorageManager with plugin '" +
                            selectedPlugin.getName() + "'", e);
        }
    }

    /**
     * Returns all available StoragePlugin implementations.
     *
     * @return unmodifiable list of available plugins (never null, may be empty)
     */
    public static List<StoragePlugin> getAvailablePlugins() {
        if (cachedPlugins == null) {
            synchronized (StoragePluginManager.class) {
                if (cachedPlugins == null) {
                    List<StoragePlugin> plugins = new ArrayList<>();

                    // Reload ServiceLoader to discover new plugins
                    serviceLoader.reload();

                    // Collect all plugins
                    for (StoragePlugin plugin : serviceLoader) {
                        plugins.add(plugin);
                        pluginsByName.put(plugin.getName(), plugin);
                    }

                    // Sort by priority (highest first)
                    plugins.sort(Comparator.comparingInt(StoragePlugin::getPriority).reversed());

                    // Make immutable
                    cachedPlugins = Collections.unmodifiableList(plugins);
                }
            }
        }

        return cachedPlugins;
    }

    /**
     * Finds a plugin by its name.
     *
     * @param name the plugin name (case-sensitive)
     * @return the plugin with the given name, or empty if not found
     * @throws IllegalArgumentException if name is null
     */
    public static Optional<StoragePlugin> findPlugin(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Plugin name must not be null");
        }

        // Ensure plugins are loaded
        getAvailablePlugins();

        return Optional.ofNullable(pluginsByName.get(name));
    }

    /**
     * Returns the names of all available plugins.
     *
     * @return unmodifiable set of plugin names (never null, may be empty)
     */
    public static Set<String> getPluginNames() {
        return getAvailablePlugins().stream()
                .map(StoragePlugin::getName)
                .collect(Collectors.collectingAndThen(
                        Collectors.toSet(),
                        Collections::unmodifiableSet
                ));
    }

    /**
     * Reloads all plugins from the classpath.
     */
    public static void reload() {
        synchronized (StoragePluginManager.class) {
            cachedPlugins = null;
            pluginsByName.clear();
        }
    }
}