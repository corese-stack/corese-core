package fr.inria.corese.core.next.api.exception;

/**
 * Used to indicate that a function should not be used with the current object or with the object in its current state
 */
public class IncorrectOperationException extends CoreseException {

    private static final long serialVersionUID = 5310252146173604452L;

    public IncorrectOperationException(String s) {
        super(s);
    }

    public IncorrectOperationException(Throwable t) {
        super(t);
    }
}
