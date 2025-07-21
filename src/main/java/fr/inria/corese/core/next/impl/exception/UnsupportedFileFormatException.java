package fr.inria.corese.core.next.impl.exception;

import fr.inria.corese.core.next.api.base.exception.CoreseException;

/**
 * Exception thrown when a file format is not supported.
 */
public class UnsupportedFileFormatException extends CoreseException {

    private static final long serialVersionUID = 7963163989802143570L;

    /**
     * Creates a new UnsupportedFileFormatException with the specified message.
     * 
     * @param message the detail message for this exception
     */
    public UnsupportedFileFormatException(String message) {
        super(message);
    }

    /**
     * Creates a new UnsupportedFileFormatException with the specified message and
     * cause.
     * 
     * @param message the detail message for this exception
     * @param cause   the cause of this exception
     */
    public UnsupportedFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new UnsupportedFileFormatException with the specified cause.
     * 
     * @param cause the cause of this exception
     */
    public UnsupportedFileFormatException(Throwable cause) {
        super(cause);
    }

}
