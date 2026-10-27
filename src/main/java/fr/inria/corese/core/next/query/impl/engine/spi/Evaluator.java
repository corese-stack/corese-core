package fr.inria.corese.core.next.query.impl.engine.spi;

import java.time.OffsetDateTime;

/**
 * Interface for the connector that evaluates filters
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public interface Evaluator {

    /**
     * Returns the stable timestamp associated with the current query evaluation.
     *
     * <p>SPARQL requires every invocation of {@code NOW()} in one query to
     * produce the same value.</p>
     *
     * @return query-scoped evaluation timestamp
     */
    OffsetDateTime getQueryEvaluationTime();

    default void start(Environment env) {
    }

    default void finish(Environment env) {
    }

    default void init(Environment env) {
    }
}
