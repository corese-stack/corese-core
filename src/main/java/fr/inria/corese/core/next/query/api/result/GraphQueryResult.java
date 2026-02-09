package fr.inria.corese.core.next.query.api.result;

import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.query.api.GraphQuery;

import java.io.Closeable;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents the result of evaluating a SPARQL CONSTRUCT or DESCRIBE query.
 *
 * @see Statement
 * @see GraphQuery
 * @see GraphQuery#evaluate()
 */
public interface GraphQueryResult extends Closeable, Iterable<Statement> {

    /**
     * Returns {@code true} if at least one more statement is available from the result.
     *
     * @return {@code true} if another statement can be retrieved, {@code false} otherwise
     * @throws IllegalStateException if this result has been closed
     */
    boolean hasNext();

    /**
     * Returns the next statement in the result sequence.
     *
     * @return the next {@link Statement} in the result (never {@code null})
     * @throws NoSuchElementException if no more statements are available
     *         (i.e., {@link #hasNext()} would return {@code false})
     * @throws IllegalStateException if this result has been closed
     */
    Statement next();

    /**
     * Returns an iterator over the statements in this result.
     *
     * @return an iterator over the statements in this result
     */
    @Override
    default Iterator<Statement> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return GraphQueryResult.this.hasNext();
            }

            @Override
            public Statement next() {
                return GraphQueryResult.this.next();
            }
        };
    }

    /**
     * Returns all remaining statements as a {@link List}.
     *
     * @return a list containing all remaining statements (never {@code null})
     * @throws IllegalStateException if this result has been closed
     */
    default List<Statement> asList() {
        List<Statement> list = new ArrayList<>();
        this.forEach(list::add);
        return list;
    }

    /**
     * Returns a sequential {@link Stream} over the statements in this result.
     *
     * @return a sequential stream over the statements in this result
     * @throws IllegalStateException if this result has been closed
     */
    default Stream<Statement> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        iterator(),
                        Spliterator.ORDERED | Spliterator.NONNULL
                ),
                false
        );
    }

    /**
     * Closes this result and releases any underlying resources such as
     * I/O streams, network connections, or database cursors.
     * <p>
     * Once closed, further iteration is not permitted.
     */
    @Override
    void close();
}