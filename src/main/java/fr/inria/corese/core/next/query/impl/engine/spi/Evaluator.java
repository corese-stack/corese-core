package fr.inria.corese.core.next.query.impl.engine.spi;

import fr.inria.corese.core.next.query.impl.engine.eval.Eval;

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

    Mode getMode();

    void setMode(Mode mode);

    void setProducer(Producer p);

    void setKGRAM(Eval o);

    void start(Environment env);

    void finish(Environment env);

    void init(Environment env);

    enum Mode {
        KGRAM_MODE, SPARQL_MODE, CACHE_MODE, NO_CACHE_MODE
    }

}
