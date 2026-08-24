package fr.inria.corese.core.next.query.impl.result;

import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;

import java.util.Iterator;
import java.util.Objects;

/**
 * Basic implementation backed by a pre-built list of statements.
 */
public final class CoreseGraphQueryResult implements GraphQueryResult {

    private final Iterator<Statement> iterator;
    private boolean closed;

    public CoreseGraphQueryResult(Iterator<Statement> iterator) {
        this.iterator = Objects.requireNonNull(iterator, "iterator");
    }

    @Override
    public boolean hasNext() {
        checkOpen();
        return this.iterator.hasNext();
    }

    @Override
    public Statement next() {
        checkOpen();
        return this.iterator.next();
    }

    @Override
    public void close() {
        closed = true;
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("This graph query result is closed.");
        }
    }
}
