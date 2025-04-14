package fr.inria.corese.core.next.api.base.exception;

public abstract class CoreseException extends RuntimeException {
    public CoreseException() {
        super();
    }

    public CoreseException(String msg) {
        super(msg);
    }

    public CoreseException(Throwable t) {
        super(t);
    }

    public CoreseException(String msg, Throwable t) {
        super(msg, t);
    }
}

