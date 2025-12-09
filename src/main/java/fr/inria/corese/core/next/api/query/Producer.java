package fr.inria.corese.core.next.api.query;

import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.Triple;
import fr.inria.corese.core.next.api.dataset.Dataset;
import fr.inria.corese.core.next.api.result.BindingSet;

import java.util.stream.Stream;

/**
 * High-level data producer for SPARQL evaluation.
 * <p>
 * This interface abstracts over the low-level KGRAM {@code Producer} and provides
 * access to triples in terms of the Corese-next RDF model ({@link Value}, {@link Triple}).
 * </p>
 * <p>
 * A {@code Producer} is responsible for delivering candidate triples that match
 * a basic graph pattern, taking into account the active {@link Dataset} (FROM / FROM NAMED)
 * and the current {@link BindingSet} (initial bindings, join context, filters).
 * </p>
 */
public interface Producer {

    /**
     * Initialize this producer for a given query.
     * Called before evaluation starts.
     *
     * @param query the high-level query being evaluated
     */
    default void init(Query query) {
    }

    /**
     * Retrieve matching triples for a basic graph pattern.
     *
     * @param subject        subject (or {@code null} for an unbound variable)
     * @param predicate        predicate (or {@code null} for an unbound variable)
     * @param object        object (or {@code null} for an unbound variable)
     * @param dataset  dataset / FROM &amp; FROM NAMED applicable for this call
     * @param bindings current variable bindings (for join context and FILTER evaluation)
     *
     * @return a (possibly lazy) stream of matching triples
     */
    Stream<Triple> getTriples(
            Value subject,
            Value predicate,
            Value object,
            Dataset dataset,
            BindingSet bindings
    );

    /**
     * Close any underlying resources held by this producer (connections, cursors, etc.).
     */
    default void close() {}
}
