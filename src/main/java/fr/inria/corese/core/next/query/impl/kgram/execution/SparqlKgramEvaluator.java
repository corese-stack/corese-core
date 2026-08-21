package fr.inria.corese.core.next.query.impl.kgram.execution;

import fr.inria.corese.core.next.query.impl.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Evaluator;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Producer;
import fr.inria.corese.core.next.query.impl.kgram.core.Eval;

/**
 * KGRAM evaluator for SPARQL query execution.
 *
 * <p>KGRAM requires an {@link Evaluator} even for simple basic graph patterns.
 * This implementation currently covers expression-free graph pattern execution:
 * edge enumeration is delegated to the producer, and RDF term comparison is
 * delegated to the matcher.</p>
 *
 * <p>SPARQL expression features such as FILTER, BIND, and function calls should
 * be added here when they enter the supported execution scope, so expression
 * evaluation remains part of the same KGRAM runtime path.</p>
 */
public final class SparqlKgramEvaluator implements Evaluator {

    private Mode mode = Mode.KGRAM_MODE;

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
        // Expression evaluation state will be initialized here when supported.
    }
}
