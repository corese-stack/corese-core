package main.java.fr.inria.corese.core.next.impl.exception;

public class ParsingErrorException extends RuntimeException {

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
