package fr.inria.corese.core.next.impl.exception;

import fr.inria.corese.core.next.api.base.exception.CoreseException;

/**
 * Exception thrown when a file format is not supported.
 */
public class UnsupportedFileFormatException extends CoreseException {

    private static final long serialVersionUID = 7963163989802143570L;

    public UnsupportedFileFormatException(String message) {
        super(message);
    }

    public UnsupportedFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedFileFormatException(Throwable cause) {
        super(cause);
    }

}
