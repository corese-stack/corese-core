package fr.inria.corese.core.next.util.exception;

import fr.inria.corese.core.next.data.api.base.exception.CoreseException;

/**
 * Class for any exception related to the configuration of Corese.
 */
public class ConfigurationException extends CoreseException {

    public ConfigurationException() {
        super();
    }

    public ConfigurationException(String msg) {
        super(msg);
    }

    public ConfigurationException(Throwable t) {
        super(t);
    }

    public ConfigurationException(String msg, Throwable t) {
        super(msg, t);
    }
}
