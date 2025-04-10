package fr.inria.corese.core.next.impl.exception;

import fr.inria.corese.core.next.api.base.exception.CoreseException;

/**
 * Used for any error raised from parsing of a string that should have been in a standard format (i.e RDF/XML, Turtle, JSON-LD, etc.)
 */
public class IncorrectFormatException extends CoreseException {

    private static final long serialVersionUID = -5769394449085722803L;

    public IncorrectFormatException(String message) {
        super(message);
    }

    public IncorrectFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncorrectFormatException(Throwable cause) {
        super(cause);
    }

    public IncorrectFormatException() {
        super();
    }
}
