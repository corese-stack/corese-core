package fr.inria.corese.core.next.impl.exception;

import fr.inria.corese.core.next.api.base.exception.CoreseException;

/**
 * Used for exception related to the inner workings of the system.
 */
public class InternalException extends CoreseException {
    public InternalException(Exception e) {
        super(e);
    }

    public InternalException(String s) {
        super(s);
    }
}
