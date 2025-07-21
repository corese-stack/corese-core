package fr.inria.corese.core.next.impl.exception;

import fr.inria.corese.core.next.api.base.exception.CoreseException;

/**
 * Used to indicate that a literal object has been used incorrectly
 */
public class IncorrectDatatypeException extends CoreseException {

    private static final long serialVersionUID = 787967948679775083L;

    /**
     * Constructor for IncorrectDatatypeException.
     *
     * @param s the error message
     */
    public IncorrectDatatypeException(String s) {
        super(s);
    }
}
