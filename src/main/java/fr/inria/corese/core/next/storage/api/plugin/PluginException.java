package fr.inria.corese.core.next.storage.api.plugin;

import fr.inria.corese.core.next.storage.api.exception.ErrorCode;
import fr.inria.corese.core.next.storage.api.exception.StorageException;

/**
 * Storage plugin discovery, selection, or creation failure.
 */
@SuppressWarnings("java:S110")
public class PluginException extends StorageException {

    public PluginException(String message) {
        super(ErrorCode.PLUGIN_FAILED, message);
    }

    public PluginException(String message, Throwable cause) {
        super(ErrorCode.PLUGIN_FAILED, message, cause);
    }
}
