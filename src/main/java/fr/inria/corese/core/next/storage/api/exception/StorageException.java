package fr.inria.corese.core.next.storage.api.exception;

/**
 * Specific exception for StorageManager operations.
 */
public class StorageException extends RuntimeException {

    private final ErrorCode code;

    /**
     * Constructs an exception with a code and message.
     *
     * @param code    Error code
     * @param message Descriptive message
     */
    public StorageException(ErrorCode code, String message) {
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
    public StorageException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * @return the stable category of this storage failure
     */
    public ErrorCode getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "StorageException{" +
                "code=" + code +
                ", message='" + getMessage() + '\'' +
                '}';
    }

}
