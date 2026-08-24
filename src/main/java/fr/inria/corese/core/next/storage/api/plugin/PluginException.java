package fr.inria.corese.core.next.storage.api.plugin;

import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;

/**
 * Exception thrown when a plugin fails to create a StorageManager instance.
 */
@SuppressWarnings("java:S110")
public class PluginException extends StorageException {

    public PluginException(String message) {
        super(ErrorCode.PLUGIN_CREATION_FAILED, message);
    }

    public PluginException(String message, Throwable cause) {
        super(ErrorCode.PLUGIN_CREATION_FAILED, message, cause);
    }
}
