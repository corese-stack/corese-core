package fr.inria.corese.core.next.api.exception;

/**
 * Used to indicates that a function should not be used with the current object or with the object in its current state
 */
public class IncorrectOperationException extends CoreseException {
    public IncorrectOperationException(String s) {
        super(s);
    }

    public IncorrectOperationException(Throwable t) {
        super(t);
    }
}
