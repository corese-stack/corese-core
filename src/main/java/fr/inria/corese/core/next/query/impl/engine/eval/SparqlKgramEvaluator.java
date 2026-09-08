package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Graph;

import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.query.impl.engine.eval.Eval;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * KGRAM evaluator for SPARQL query execution.
 *
 * <p>KGRAM requires an {@link Evaluator} even for simple basic graph patterns.
 * This implementation owns the small amount of immutable, query-scoped state
 * required by native expression evaluation. Edge enumeration remains delegated
 * to the producer and RDF term comparison to the matcher.</p>
 */
public final class SparqlKgramEvaluator implements Evaluator {

    private final OffsetDateTime queryEvaluationTime = OffsetDateTime.now(ZoneOffset.UTC);
    private Mode mode = Mode.KGRAM_MODE;

    @Override
    public OffsetDateTime getQueryEvaluationTime() {
        return queryEvaluationTime;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public void setProducer(Producer producer) {
        // Expression-free graph pattern execution does not need producer state here.
    }

    @Override
    public void setKGRAM(Eval eval) {
        // Eval is driven by the caller for the currently supported execution scope.
    }

    @Override
    public void start(Environment environment) {
        // Graph pattern execution currently needs no evaluator-side initialization.
    }

    @Override
    public void finish(Environment environment) {
        // Graph pattern execution currently needs no evaluator-side cleanup.
    }

    @Override
    public void init(Environment environment) {
        // All native expression state is immutable and initialized at construction.
    }
}
