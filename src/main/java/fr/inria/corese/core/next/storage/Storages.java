package fr.inria.corese.core.next.storage;

import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.config.StorageConfig;
import fr.inria.corese.core.next.storage.api.plugin.PluginException;
import fr.inria.corese.core.next.storage.api.plugin.PluginNotFoundException;
import fr.inria.corese.core.next.storage.api.plugin.StoragePlugin;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/** Public entry point for creating standalone RDF storage backends. */
public final class Storages {

    private Storages() {
    }

    /**
     * Creates an open in-memory storage backend.
     *
     * @return a ready-to-use storage manager
     */
    public static StorageManager create() {
        return create(StorageConfig.memory());
    }

    /**
     * Creates and initializes the backend selected by a storage configuration.
     * The caller owns the returned manager and must close it.
     *
     * @param config backend selection and configuration
     * @return a ready-to-use storage manager
     * @throws NullPointerException if {@code config} is {@code null}
     * @throws PluginNotFoundException if no provider supports {@code config}
     * @throws PluginException if provider discovery or creation fails
     */
    public static StorageManager create(StorageConfig config) {
        StorageConfig checkedConfig = Objects.requireNonNull(config, "config");
        List<StoragePlugin> plugins = discoverPlugins();
        List<StoragePlugin> supported;
        try {
            supported = plugins.stream()
                    .filter(plugin -> plugin.supports(checkedConfig))
                    .sorted(pluginOrder())
                    .toList();
        } catch (RuntimeException exception) {
            throw new PluginException("Storage plugin selection failed", exception);
        }
        if (supported.isEmpty()) {
            String available = plugins.stream()
                    .map(StoragePlugin::getName)
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new PluginNotFoundException(
                    "No storage plugin found for type '"
                            + checkedConfig.getType().orElse("not specified")
                            + "'. Available types: [" + available + ']');
        }

        StoragePlugin plugin = supported.getFirst();
        try {
            StorageManager storage = Objects.requireNonNull(
                    plugin.create(checkedConfig),
                    "Storage plugin returned null: " + plugin.getName());
            if (!storage.isOpen()) {
                throw new PluginException(
                        "Storage plugin returned a backend that is not open: " + plugin.getName());
            }
            return storage;
        } catch (PluginException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PluginException(
                    "Storage plugin '" + plugin.getName() + "' failed", exception);
        }
    }

    private static List<StoragePlugin> discoverPlugins() {
        try {
            List<StoragePlugin> plugins = ServiceLoader.load(StoragePlugin.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .toList();
            var names = new HashSet<String>();
            for (StoragePlugin plugin : plugins) {
                validatePlugin(plugin);
                if (!names.add(plugin.getName())) {
                    throw new PluginException(
                            "Duplicate storage plugin name: " + plugin.getName());
                }
            }
            return plugins;
        } catch (PluginException exception) {
            throw exception;
        } catch (ServiceConfigurationError | RuntimeException exception) {
            throw new PluginException("Storage plugin discovery failed", exception);
        }
    }

    private static void validatePlugin(StoragePlugin plugin) {
        String name = Objects.requireNonNull(plugin.getName(), "plugin name");
        if (name.isBlank()) {
            throw new PluginException("Storage plugin name must not be blank");
        }
    }

    private static Comparator<StoragePlugin> pluginOrder() {
        return Comparator.comparingInt(StoragePlugin::getPriority)
                .reversed()
                .thenComparing(StoragePlugin::getName);
    }
}
