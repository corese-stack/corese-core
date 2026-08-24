package fr.inria.corese.core.next.query.api.result;

import fr.inria.corese.core.next.query.api.TupleQuery;

import java.io.Closeable;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents the result of evaluating a SPARQL {@code SELECT} query.
 * <p>
 * A {@code TupleQueryResult} is a sequence of solution mappings,
 * where each solution is represented as a {@link BindingSet}.
 * Each binding set contains zero or more variable bindings
 * corresponding to the projection variables of the query.
 * </p>
 *
 * <p>Implementations are not required to be thread-safe.</p>
 *
 * @see TupleQuery
 * @see TupleQuery#evaluate()
 */
public interface TupleQueryResult extends Closeable, Iterable<BindingSet> {

    /**
     * Returns the ordered list of variable names that appear in the
     * projection of this SELECT query.
     *
     * @return a list of variable names, in projection order
     */
    List<String> getBindingNames();

    /**
     * Returns {@code true} if the query result contains at least one more
     * solution. Calling this method does not consume the next solution.
     *
     * @return {@code true} if another binding set is available, {@code false} otherwise
     * @throws IllegalStateException if this result has been closed
     */
    boolean hasNext();

    /**
     * Returns the next solution in the result sequence.
     *
     * @return the next {@link BindingSet} (never {@code null})
     * @throws NoSuchElementException if no more solutions are available
     *         (i.e., {@link #hasNext()} would return {@code false})
     * @throws IllegalStateException if this result has been closed
     */
    BindingSet next();

    /**
     * Returns an iterator over the query results.
     *
     * @return an iterator over the binding sets in this result
     */
    @Override
    @SuppressWarnings("NullableProblems")
    default Iterator<BindingSet> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return TupleQueryResult.this.hasNext();
            }

            @Override
            public BindingSet next() {
                return TupleQueryResult.this.next();
            }
        };
    }

    /**
     * Returns a sequential {@link Stream} over the query results.
     *
     * @return a sequential stream over the binding sets in this result
     * @throws IllegalStateException if this result has been closed
     */
    default Stream<BindingSet> stream() {
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
     * network connections, file handles, or database cursors.
     * <p>
     * After calling {@code close()}, calls to {@code hasNext()} or {@code next()}
     * may result in an exception.
     */
    @Override
    void close();
}