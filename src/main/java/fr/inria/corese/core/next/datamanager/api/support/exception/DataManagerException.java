package fr.inria.corese.core.next.datamanager.api.support.exception;

/**
 * Specific exception for DataManager operations.
 */
public class DataManagerException extends Exception {

    private final ErrorCode code;

    /**
     * Constructs an exception with a code and message.
     *
     * @param code    Error code
     * @param message Descriptive message
     */
    public DataManagerException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Constructs an exception with a code, message and cause.
     *
     * @param code    Error code
     * @param message Descriptive message
     * @param cause   Original exception
     */
    public DataManagerException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * Returns the error code.
     *
     * @return Error code
     */
    public ErrorCode getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "DataManagerException{" +
                "code=" + code +
                ", message='" + getMessage() + '\'' +
                '}';
    }

}