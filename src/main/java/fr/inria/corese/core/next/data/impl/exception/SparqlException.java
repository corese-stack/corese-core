package fr.inria.corese.core.next.impl.exception;

import fr.inria.corese.core.next.api.base.exception.CoreseException;

public class SparqlException extends CoreseException {
    public SparqlException() { }
    public SparqlException(String msg) { super(msg); }
    public SparqlException(String msg, Throwable cause) { super(msg, cause); }
    public SparqlException(Throwable cause) { super(cause); }
}
