package fr.inria.corese.core.next.impl.exception;

import fr.inria.corese.core.next.api.base.exception.CoreseException;

/**
 * Exception thrown when an error occurs during parsing
 */
public class ParsingErrorException extends CoreseException {

    private static final long serialVersionUID = -2053549958572141648L;

    /**
     * Creates a new ParsingErrorException with the specified message.
     * 
     * @param message the detail message for this exception
     */
    public ParsingErrorException(String message) {
        super(message);
    }

    /**
     * Creates a new ParsingErrorException with the specified message and cause.
     * 
     * @param message the detail message for this exception
     * @param cause   the cause of this exception
     */
    public ParsingErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new ParsingErrorException with the specified cause.
     * 
     * @param cause the cause of this exception
     */
    public ParsingErrorException(Throwable cause) {
        super(cause);
    }
}
