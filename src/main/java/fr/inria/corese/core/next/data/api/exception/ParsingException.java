package fr.inria.corese.core.next.data.api.exception;

import fr.inria.corese.core.next.common.exception.CoreseException;

import java.io.Serial;

/**
 * Exception thrown when RDF input cannot be parsed.
 */
public class ParsingException extends CoreseException {

    @Serial
    private static final long serialVersionUID = -2053549958572141648L;

    /**
     * Creates a new ParsingException with the specified message.
     *
     * @param message the detail message for this exception
     */
    public ParsingException(String message) {
        super(message);
    }

    /**
     * Creates a new ParsingException with the specified message and cause.
     *
     * @param message the detail message for this exception
     * @param cause   the cause of this exception
     */
    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new ParsingException with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public ParsingException(Throwable cause) {
        super(cause);
    }
}
