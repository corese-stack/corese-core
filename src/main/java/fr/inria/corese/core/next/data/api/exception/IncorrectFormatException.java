package fr.inria.corese.core.next.data.api.exception;

import fr.inria.corese.core.next.common.exception.CoreseException;

import java.io.Serial;

/**
 * Used for any error raised from parsing of a string that should have been in a standard format (i.e RDF/XML, Turtle, JSON-LD, etc.)
 */
public class IncorrectFormatException extends CoreseException {

    @Serial
    private static final long serialVersionUID = -5769394449085722803L;

    /**
     * Constructor for IncorrectFormatException.
     *
     * @param message the error message
     */
    public IncorrectFormatException(String message) {
        super(message);
    }

}
