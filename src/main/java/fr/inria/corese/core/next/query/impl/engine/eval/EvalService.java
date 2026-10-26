package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import static fr.inria.corese.core.next.query.impl.engine.eval.Eval.STOP;

/**
 * Evaluates federated SPARQL {@code SERVICE} pattern operations for {@link Eval}.
 *
 * <p>Delegates remote query invocation to the configured {@link fr.inria.corese.core.next.query.impl.engine.spi.Provider},
 * verifies endpoints, maps returned solutions into memory, and enforces the non-silent error policy.</p>
 */
final class EvalService {

    private final Eval engine;

    EvalService(Eval engine) {
        this.engine = engine;
    }

    int eval(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) {
        Memory env = engine.getMemory();
        Node serv = exp.first().getNode();
        Node node = serv.isVariable() ? env.getNode(serv) : serv;

        if (engine.getProvider() != null) {
            Mappings map = engine.getProvider().service(node, exp, engine.selectQueryMappings(data), engine);
            return processServiceMappings(p, graphNode, stack, n, env, map);
        } else if (exp.isSilent()) {
            return engine.eval(p, graphNode, stack, n + 1);
        } else {
            throw new QueryEvaluationException(
                    "Federated SPARQL SERVICE execution failed: no Provider configured to query endpoint " + node);
        }
    }

    private int processServiceMappings(Producer p, Node graphNode, Stack stack, int n, Memory env, Mappings map) {
        int backtrack = n - 1;
        if (map == null) {
            return backtrack;
        }
        for (Mapping m : map) {
            if (engine.getState().isStopped()) {
                return STOP;
            }
            complete(engine.getQuery(), m, false);

            if (env.push(m, n, false, false)) {
                backtrack = engine.eval(p, graphNode, stack, n + 1);
                env.pop(m, false);
                if (backtrack < n) {
                    return backtrack;
                }
            }
        }
        return backtrack;
    }

    void complete(QueryPlan q, Mapping map, boolean addNode) {
        int i = 0;
        for (Node node : map.getQueryNodes()) {
            Node out = q.getOuterNode(node);
            if (out == null) {
                out = node;
                if (addNode && !q.getSelect().contains(node)) {
                    q.getSelect().add(node);
                }
            }
            map.getQueryNodes()[i] = out;
            i++;
        }
    }
}
