package fr.inria.corese.core.next.query.api.result;

import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.query.api.TupleQuery;

import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A set of variable bindings representing a single solution in a SPARQL
 * {@code SELECT} query result.
 * <p>
 * A {@code BindingSet} maps variable names to {@link Value} instances,
 * corresponding to the values produced by evaluating a SPARQL projection.
 * Each {@link Binding} in this set represents one such mapping.
 * </p>
 *
 *
 * @see Binding
 * @see TupleQuery
 */
public interface BindingSet extends Iterable<Binding> {

    /**
     * Returns the names of all variables that are bound in this solution.
     *
     * @return a set of variable names
     */
    Set<String> getBindingNames();

    /**
     * Checks whether a binding exists for a given variable name.
     *
     * @param name the variable to test
     * @return {@code true} if the variable is bound, {@code false} otherwise
     */
    boolean hasBinding(String name);

    /**
     * Retrieves the value bound to a given variable, or {@code null} if
     * the variable is unbound in this solution.
     *
     * @param name the variable name
     * @return the value bound to the variable, or {@code null}
     */
    Value getValue(String name);

    /**
     * Returns a sequential {@link Stream} over the bindings.
     *
     * @return a sequential stream over the bindings in this result
     * @throws IllegalStateException if this result has been closed
     */
    default Stream<Binding> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        iterator(),
                        Spliterator.ORDERED | Spliterator.NONNULL
                ),
                false
        );
    }
}