package fr.inria.corese.core.next.query.kgram.adapter;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.Expression;

/**
 * Invokes {@link Expression#evalWE} for Corese-next {@link fr.inria.corese.core.next.query.kgram.api.core.Expr}
 * wrappers built on the SPARQL triple.parser tree. The interpreter is still compiled against
 * {@link fr.inria.corese.core.kgram.api.query.Environment} and
 * {@link fr.inria.corese.core.kgram.api.query.Producer}; concrete values must implement those
 * types in addition to the
 * {@link fr.inria.corese.core.next.query.kgram.api.query} interfaces used on
 * {@link fr.inria.corese.core.next.query.kgram.api.core.Expr#evalWE}.
 */
public final class TripleParserEvalSupport {

    private TripleParserEvalSupport() {
    }

    public static IDatatype evalWE(
            Expression expr, Computer eval, Binding binding, Environment env, Producer producer) {
        if (!(env instanceof fr.inria.corese.core.kgram.api.query.Environment kgramEnv)) {
            throw new QueryEvaluationException(
                    "Environment must implement fr.inria.corese.core.kgram.api.query.Environment"
                            + " (required by sparql.triple.parser.Expression#evalWE)");
        }
        if (!(producer instanceof fr.inria.corese.core.kgram.api.query.Producer kgramProducer)) {
            throw new QueryEvaluationException(
                    "Producer must implement fr.inria.corese.core.kgram.api.query.Producer"
                            + " (required by sparql.triple.parser.Expression#evalWE)");
        }
        try {
            return expr.evalWE(eval, binding, kgramEnv, kgramProducer);
        } catch (EngineException e) {
            throw new QueryEvaluationException(e.getMessage(), e);
        }
    }
}
