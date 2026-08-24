package fr.inria.corese.core.next.data.api.exception;

import fr.inria.corese.core.next.common.exception.CoreseException;

import java.io.Serial;

/**
 * Indicates that a literal cannot be converted to the requested datatype.
 */
public class InvalidDatatypeException extends CoreseException {

    @Serial
    private static final long serialVersionUID = 787967948679775083L;

    /**
     * Constructor for InvalidDatatypeException.
     *
     * @param s the error message
     */
    public InvalidDatatypeException(String s) {
        super(s);
    }
}
