package fr.inria.corese.core.next.storagemanager.api.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Utility class for loading StoragePlugin instances from external JAR files.
 */
public class ExternalPluginLoader {

    private static final Logger logger = LoggerFactory.getLogger(ExternalPluginLoader.class);

    /**
     * List of loaded plugin class loaders (to prevent garbage collection).
     */
    private static final List<ClassLoader> loadedClassLoaders = new ArrayList<>();

    /**
     * Private constructor - this is a utility class.
     */
    private ExternalPluginLoader() {
        throw new AssertionError("ExternalPluginLoader is a utility class");
    }

    /**
     * Loads all plugins from a specific JAR file.
     *
     * @param jarFile the JAR file containing plugins
     * @return the number of plugins loaded from this JAR
     * @throws IllegalArgumentException if jarFile is null or doesn't exist
     * @throws Exception                if loading fails
     */
    public static int loadPluginsFromJar(File jarFile) throws Exception {
        if (jarFile == null) {
            throw new IllegalArgumentException("JAR file cannot be null");
        }
        if (!jarFile.exists() || !jarFile.isFile()) {
            throw new IllegalArgumentException("JAR file does not exist: " + jarFile);
        }

        logger.info("Loading plugins from JAR: {} ({} bytes)",
                jarFile.getName(), jarFile.length());

        // Create ClassLoader for the JAR
        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()},
                ExternalPluginLoader.class.getClassLoader()
        );

        // Keep reference to prevent garbage collection
        loadedClassLoaders.add(classLoader);

        // Register ClassLoader with StoragePluginManager
        StoragePluginManager.registerClassLoader(classLoader);

        // Load plugins using ServiceLoader
        ServiceLoader<StoragePlugin> loader = ServiceLoader.load(
                StoragePlugin.class,
                classLoader
        );

        int count = 0;
        for (StoragePlugin plugin : loader) {
            logger.info("Loaded plugin: {} (priority={}, jar={})",
                    plugin.getName(),
                    plugin.getPriority(),
                    jarFile.getName());
            count++;
        }

        // Refresh the plugin cache
        if (count > 0) {
            StoragePluginManager.reload();
            logger.info("Plugin cache refreshed");
        }

        return count;
    }

}