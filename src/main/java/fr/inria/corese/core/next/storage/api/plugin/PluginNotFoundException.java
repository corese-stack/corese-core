package fr.inria.corese.core.next.storage.api.plugin;

/**
 * Exception thrown when no plugin is found for the requested configuration.
 */
@SuppressWarnings("java:S110")
public class PluginNotFoundException extends PluginException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;


    public PluginNotFoundException(String message) {
        super(message);
    }
}
