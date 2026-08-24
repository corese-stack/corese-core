package fr.inria.corese.core.next.common.exception;

/**
 * Class for any exception related to the configuration of Corese.
 */
public class ConfigurationException extends CoreseException {

    public ConfigurationException(String msg) {
        super(msg);
    }

    public ConfigurationException(String msg, Throwable t) {
        super(msg, t);
    }
}
