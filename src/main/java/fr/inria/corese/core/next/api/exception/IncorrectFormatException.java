package fr.inria.corese.core.next.api.exception;

/**
 * Used for any error raised from parsing of a string in a standard format
 */
public class IncorrectFormatException extends CoreseException {

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
