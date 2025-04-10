package fr.inria.corese.core.next.api.exception;

/**
 * CoreseException is the superclass for all exceptions thrown by the Corese framework.
 * It extends RuntimeException, allowing it to be used as an unchecked exception.
 * Its constructors are redirection to the constructors of RuntimeException.
 */
public abstract class CoreseException extends RuntimeException {
    private static final long serialVersionUID = 6828854064356034698L;

    /**
     * Default constructor for CoreseException.
     */
    protected CoreseException() {
        super();
    }

    /**
     * Constructor for CoreseException with a message.
     * @param msg the message to be associated with the exception
     */
    protected CoreseException(String msg) {
        super(msg);
    }

    /**
     * Constructor for CoreseException with a cause.
     * @param t the cause of the exception
     */
    protected CoreseException(Throwable t) {
        super(t);
    }

    /**
     * Constructor for CoreseException with a message and a cause.
     * @param msg the message to be associated with the exception
     * @param t the cause of the exception
     */
    protected CoreseException(String msg, Throwable t) {
        super(msg, t);
    }
}

