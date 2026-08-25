package fr.inria.corese.core.next.query.api.result;

import fr.inria.corese.core.next.data.api.model.Statement;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Closeable, single-use sequence of RDF statements.
 *
 * <p>Results must be closed, preferably with try-with-resources. Closing the
 * stream returned by {@link #stream()} also closes this result.</p>
 */
public interface StatementResult extends Closeable, Iterable<Statement> {

    /** Returns whether another statement is available. */
    boolean hasNext();

    /**
     * Returns the next statement.
     *
     * @throws NoSuchElementException if the result is exhausted
     * @throws IllegalStateException if the result is closed
     */
    Statement next();

    @Override
    @SuppressWarnings("NullableProblems")
    default Iterator<Statement> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return StatementResult.this.hasNext();
            }

            @Override
            public Statement next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return StatementResult.this.next();
            }
        };
    }

    /**
     * Returns a sequential stream whose close operation closes this result.
     * A terminal stream operation does not close either one automatically.
     */
    default Stream<Statement> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        iterator(), Spliterator.ORDERED | Spliterator.NONNULL),
                false).onClose(this::close);
    }

    /** Closes this result and its underlying storage cursor. */
    @Override
    void close();
}
