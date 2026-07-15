package fr.inria.corese.core.next.data.impl.exception;

import fr.inria.corese.core.next.util.exception.CoreseException;

public class SparqlException extends CoreseException {
    public SparqlException() { }
    public SparqlException(String msg) { super(msg); }
    public SparqlException(String msg, Throwable cause) { super(msg, cause); }
    public SparqlException(Throwable cause) { super(cause); }
}
