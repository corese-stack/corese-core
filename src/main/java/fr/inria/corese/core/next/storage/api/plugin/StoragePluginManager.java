package fr.inria.corese.core.next.storage.api.plugin;

import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Manager for discovering and creating StorageManager plugins.
 */
@SuppressWarnings({"java:S3077", "java:S3064", "java:S2629"})
public final class StoragePluginManager {

    private static final Logger logger = LoggerFactory.getLogger(StoragePluginManager.class);

    /**
     * ServiceLoader for discovering plugins
     */
    private static final List<ClassLoader> classLoaders = new CopyOnWriteArrayList<>();

    static {
        // Always include the system ClassLoader for internal plugins
        classLoaders.add(StoragePluginManager.class.getClassLoader());
    }

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
    public static StorageManager create(StorageConfig config) {
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
                    Map<String, StoragePlugin> uniquePlugins = new HashMap<>();

                    // Search all registered ClassLoaders
                    for (ClassLoader classLoader : classLoaders) {
                        ServiceLoader<StoragePlugin> loader = ServiceLoader.load(
                                StoragePlugin.class,
                                classLoader
                        );

                        for (StoragePlugin plugin : loader) {
                            // Keep only the first occurrence of each plugin name
                            // (external plugins can override internal ones if loaded first)
                            uniquePlugins.putIfAbsent(plugin.getName(), plugin);
                        }
                    }

                    // Update name cache
                    pluginsByName.clear();
                    pluginsByName.putAll(uniquePlugins);

                    // Sort by priority (highest first)
                    List<StoragePlugin> plugins = new ArrayList<>(uniquePlugins.values());
                    plugins.sort(Comparator.comparingInt(StoragePlugin::getPriority).reversed());

                    // Make immutable
                    cachedPlugins = Collections.unmodifiableList(plugins);

                    logger.debug("Discovered {} plugin(s): {}",
                            cachedPlugins.size(),
                            cachedPlugins.stream()
                                    .map(p -> p.getName() + "(" + p.getPriority() + ")")
                                    .collect(Collectors.joining(", ")));
                }
            }
        }

        return cachedPlugins;
    }

    /**
     * Registers a ClassLoader to search for plugins.
     *
     * @param classLoader the ClassLoader to register (must not be null)
     * @throws IllegalArgumentException if classLoader is null
     */
    public static void registerClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException("ClassLoader cannot be null");
        }

        synchronized (classLoaders) {
            if (!classLoaders.contains(classLoader)) {
                classLoaders.add(classLoader);
                logger.debug("Registered ClassLoader: {}", classLoader.getClass().getSimpleName());
            }
        }
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
     * Reloads all plugins from the classpath.
     */
    public static void reload() {
        synchronized (StoragePluginManager.class) {
            cachedPlugins = null;
            pluginsByName.clear();
            logger.debug("Plugin cache cleared");
        }
    }
}
