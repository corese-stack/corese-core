package fr.inria.corese.core.next.impl.exception;


import fr.inria.corese.core.next.api.base.exception.CoreseException;

public class ParsingErrorException extends CoreseException {

    private static final long serialVersionUID = -2053549958572141648L;

    public ParsingErrorException(String message) {
        super(message);
    }

    public ParsingErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParsingErrorException(Throwable cause) {
        super(cause);
    }
}
