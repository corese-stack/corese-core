package fr.inria.corese.core.next.query.impl.kgram.adapter;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.api.Computer;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.exceptions.EngineException;
import fr.inria.corese.core.sparql.triple.function.term.Binding;
import fr.inria.corese.core.sparql.triple.parser.Expression;

/**
 * Invokes {@link Expression#evalWE} for Corese-next {@link fr.inria.corese.core.next.query.impl.kgram.api.core.Expr}
 */
public final class TripleParserEvalSupport {

    private TripleParserEvalSupport() {
    }

    public static IDatatype evalWE(
            Expression expr, Computer eval, Binding binding, Environment env, Producer producer) {
        fr.inria.corese.core.kgram.api.query.Environment kgramEnv =
                (env instanceof fr.inria.corese.core.kgram.api.query.Environment e)
                        ? e : new LegacyEnvironmentBridge(env);
        fr.inria.corese.core.kgram.api.query.Producer kgramProducer =
                (producer instanceof fr.inria.corese.core.kgram.api.query.Producer p)
                        ? p : LegacyProducerBridge.INSTANCE;
        try {
            return expr.evalWE(eval, binding, kgramEnv, kgramProducer);
        } catch (EngineException e) {
            throw new QueryEvaluationException(e.getMessage(), e);
        }
    }
}
