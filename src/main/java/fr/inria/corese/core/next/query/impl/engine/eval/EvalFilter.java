package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.List;

import static fr.inria.corese.core.next.query.impl.engine.model.ExprType.UNNEST;

/**
 * Evaluates SPARQL {@code FILTER} expressions and constraints for {@link Eval}.
 *
 * <p>Handles expression evaluation, three-valued Boolean logic testing,
 * error isolation (SPARQL filter errors evaluate to false rather than query abort),
 * and listener/visitor notifications.</p>
 */
final class EvalFilter {

    private final Eval engine;

    EvalFilter(Eval engine) {
        this.engine = engine;
    }

    int eval(Producer p, Node graphNode, Exp exp, Stack stack, int n) {
        int backtrack = n - 1;
        Memory env = engine.getMemory();
        boolean success = true;

        if (!exp.isPostpone()) {
            success = test(graphNode, exp.getFilter(), env, p);

            if (engine.getVisitorDispatcher().hasFilter()) {
                success = engine.getVisitor().filter(engine, engine.getGraphNode(graphNode), exp.getFilter().getExp(), success);
            }
        }

        if (success) {
            backtrack = engine.eval(p, graphNode, stack, n + 1);
        }

        return backtrack;
    }

    boolean test(Node graphNode, Filter f, Environment env, Producer p) {
        try {
            env.setGraphNode(graphNode);
            DatatypeValue dt = eval(f, env, p);
            return isTrue(dt);
        } catch (QueryTypeErrorException error) {
            return false;
        } finally {
            env.setGraphNode(null);
        }
    }

    boolean isTrue(DatatypeValue dt) {
        if (dt == null) {
            return false;
        }
        return dt.isTrue();
    }

    Node eval(Node graphNode, Filter f, Environment env, Producer p) {
        try {
            env.setGraphNode(graphNode);
            return p.getNode(eval(f.getExp(), env, p));
        } finally {
            env.setGraphNode(null);
        }
    }

    DatatypeValue eval(Filter f, Environment env, Producer p) {
        return f.getExp().evalWE(engine.getEvaluator(), env.getBind(), env, p);
    }

    DatatypeValue eval(Expr e, Environment env, Producer p) {
        return e.evalWE(engine.getEvaluator(), env.getBind(), env, p);
    }

    Mappings eval(Filter f, Environment env, Producer p, List<Node> nodes) {
        int n = 1;
        Expr exp = f.getExp();
        if (exp.oper() == UNNEST) {
            if (engine.getVisitorDispatcher().hasListener()) {
                engine.getVisitorDispatcher().listen(exp);
            }
            if (exp.arity() == 2) {
                DatatypeValue dt = eval(exp.getExp(1), env, p);
                if (dt == null) {
                    return new Mappings();
                }
                n = dt.intValue();
            }
            exp = exp.getExp(0);
        }
        DatatypeValue res = eval(exp, env, p);
        if (res == null) {
            return new Mappings();
        }
        return engine.getProducer().map(nodes, res, n);
    }
}
