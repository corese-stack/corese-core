package fr.inria.corese.core.next.api.exception;

/**
 * Used to indicate that a literal object has been used incorrectly
 */
public class IncorrectDatatypeException extends CoreseException {

    public IncorrectDatatypeException(String s) {
        super(s);
    }
}
