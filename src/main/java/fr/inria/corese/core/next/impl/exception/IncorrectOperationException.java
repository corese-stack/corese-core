package fr.inria.corese.core.next.impl.exception;

import fr.inria.corese.core.next.api.base.exception.CoreseException;

/**
 * Used to indicate that a function should not be used with the current object or with the object in its current state
 */
public class IncorrectOperationException extends CoreseException {

    private static final long serialVersionUID = 5310252146173604452L;
    /**
     * Constructor for IncorrectOperationException.
     *
     * @param s the error message
     */
    public IncorrectOperationException(String s) {
        super(s);
    }

    /**
     * Constructor for IncorrectOperationException with a cause.
     *
     * @param t     the cause of the exception
     */
    public IncorrectOperationException(Throwable t) {
        super(t);
    }
}
