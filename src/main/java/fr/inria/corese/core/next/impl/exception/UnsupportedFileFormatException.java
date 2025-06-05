package fr.inria.corese.core.next.impl.exception;

public class UnsupportedFileFormatException extends Exception {

    private static final long serialVersionUID = 7963163989802143570L;

    public UnsupportedFileFormatException(String message) {
        super(message);
    }

    public UnsupportedFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedFileFormatException(Throwable cause) {
        super(cause);
    }

}
