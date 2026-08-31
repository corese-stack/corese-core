package fr.inria.corese.core.next.query.impl.kgram.execution;

import fr.inria.corese.core.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Evaluator;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Producer;
import fr.inria.corese.core.next.query.impl.kgram.core.Eval;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.GraphProcessor;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.api.TransformProcessor;
import fr.inria.corese.core.sparql.api.TransformVisitor;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.Context;
import fr.inria.corese.core.sparql.triple.parser.NSManager;

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
 *
 * <p>Implements {@link Computer} so that {@code AstBackedExpr.evalWE} can delegate
 * FILTER expression evaluation via the legacy {@code Expression.evalWE} path.
 * The {@link Computer} methods beyond the basic eval path are not used by simple
 * FILTER expressions and throw {@link UnsupportedOperationException} if called.</p>
 */
public final class SparqlKgramEvaluator implements Evaluator, Computer {

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

    // --- Computer / ComputerProxy ---
    // Required so that AstBackedExpr.evalWE can pass this evaluator to the legacy
    // Expression.evalWE path for FILTER evaluation. The methods below are not
    // invoked by basic SPARQL 1.0 FILTER expressions (equality, comparison, etc.).

    @Override
    public fr.inria.corese.core.kgram.api.query.Evaluator getEvaluator() {
        return null;
    }

    @Override
    public boolean isCompliant() {
        return false;
    }

    @Override
    public TransformProcessor getTransformer(Binding b,
            fr.inria.corese.core.kgram.api.query.Environment env,
            fr.inria.corese.core.kgram.api.query.Producer p) throws EngineException {
        throw new UnsupportedOperationException("getTransformer not supported by SparqlKgramEvaluator");
    }

    @Override
    public TransformProcessor getTransformer(Binding b,
            fr.inria.corese.core.kgram.api.query.Environment env,
            fr.inria.corese.core.kgram.api.query.Producer p,
            Expr exp, IDatatype uri, IDatatype gname) throws EngineException {
        throw new UnsupportedOperationException("getTransformer not supported by SparqlKgramEvaluator");
    }

    @Override
    public GraphProcessor getGraphProcessor() {
        return null;
    }

    @Override
    public TransformVisitor getVisitor(Binding b,
            fr.inria.corese.core.kgram.api.query.Environment env,
            fr.inria.corese.core.kgram.api.query.Producer p) {
        return null;
    }

    @Override
    public Context getContext(Binding b,
            fr.inria.corese.core.kgram.api.query.Environment env,
            fr.inria.corese.core.kgram.api.query.Producer p) {
        return null;
    }

    @Override
    public NSManager getNSM(Binding b,
            fr.inria.corese.core.kgram.api.query.Environment env,
            fr.inria.corese.core.kgram.api.query.Producer p) {
        return null;
    }
}
