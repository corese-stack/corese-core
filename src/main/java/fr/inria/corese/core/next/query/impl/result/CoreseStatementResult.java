package fr.inria.corese.core.next.query.impl.result;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.query.api.result.StatementResult;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

/** Storage-backed statement result. */
public final class CoreseStatementResult implements StatementResult {

    private final Stream<Statement> stream;
    private final Iterator<Statement> iterator;
    private final Runnable accessGuard;
    private boolean closed;

    public CoreseStatementResult(Stream<Statement> stream, Runnable accessGuard) {
        this.stream = Objects.requireNonNull(stream, "stream");
        this.iterator = stream.iterator();
        this.accessGuard = Objects.requireNonNull(accessGuard, "accessGuard");
    }

    @Override
    public boolean hasNext() {
        checkOpen();
        return iterator.hasNext();
    }

    @Override
    public Statement next() {
        checkOpen();
        return iterator.next();
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            stream.close();
        }
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("This statement result is closed.");
        }
        accessGuard.run();
    }
}
