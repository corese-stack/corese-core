package fr.inria.corese.core.next.storage.api.plugin;

/**
 * Exception thrown when no plugin is found for the requested configuration.
 */
@SuppressWarnings("java:S110")
public class PluginNotFoundException extends PluginException {

    public PluginNotFoundException(String message) {
        super(message);
    }
}
