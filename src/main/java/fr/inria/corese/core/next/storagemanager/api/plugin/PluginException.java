package fr.inria.corese.core.next.storagemanager.api.plugin;

import fr.inria.corese.core.next.storagemanager.api.support.exception.ErrorCode;
import fr.inria.corese.core.next.storagemanager.api.support.exception.StorageException;

/**
 * Exception thrown when a plugin fails to create a StorageManager instance.
 */
public class PluginException extends StorageException {
    
    public PluginException(String message) {
        super(ErrorCode.PLUGIN_CREATION_FAILED, message);
    }
    
    public PluginException(String message, Throwable cause) {
        super(ErrorCode.PLUGIN_CREATION_FAILED, message, cause);
    }
}
