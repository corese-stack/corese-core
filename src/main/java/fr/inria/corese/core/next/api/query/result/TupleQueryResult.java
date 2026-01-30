package fr.inria.corese.core.next.api.query.result;

import java.io.Closeable;
import java.util.List;

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
 * @see fr.inria.corese.core.next.api.query.TupleQuery
 */
public interface TupleQueryResult extends Closeable {

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
     * @return {@code true} if another binding set is available
     */
    boolean hasNext();

    /**
     * Returns the next solution in the result sequence.
     * <p>
     * If no next element exists, this method may throw an exception
     * @return the next {@link BindingSet}
     */
    BindingSet next();

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