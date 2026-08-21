package fr.inria.corese.core.next.storage.api.plugin;

/**
 * Exception thrown when no plugin is found for the requested configuration.
 */
public class PluginNotFoundException extends PluginException {

    public PluginNotFoundException(String message) {
        super(message);
    }
}
