package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.MappingSet;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import static fr.inria.corese.core.next.query.impl.engine.eval.Eval.STOP;

/**
 * Evaluates SPARQL {@code UNION} pattern operations for {@link Eval}.
 *
 * <p>Evaluates alternatives sequentially, passing context mappings for federated
 * or nested branches, and feeds solutions into the evaluation continuation.</p>
 */
final class EvalUnion {

    private final Eval engine;

    EvalUnion(Eval engine) {
        this.engine = engine;
    }

    int eval(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) {
        Mappings map1 = unionBranch(p, graphNode, exp.first(), exp, data);
        if (engine.getState().isStopped()) {
            return STOP;
        }
        Mappings map2 = unionBranch(p, graphNode, exp.rest(), exp, data);

        engine.getVisitor().union(engine, engine.getGraphNode(graphNode), exp, map1, map2);
        Mappings alternatives = new Mappings();
        alternatives.add(map1);
        alternatives.add(map2);
        return evalUnionMappings(p, graphNode, stack, n, alternatives, n - 1);
    }

    private int evalUnionMappings(
            Producer p, Node graphNode, Stack stack, int n, Mappings mappings, int backtrack) {
        Memory environment = engine.getMemory();
        int currentBacktrack = backtrack;
        for (Mapping mapping : mappings) {
            if (engine.getState().isStopped()) {
                return STOP;
            }
            if (environment.push(mapping, n, false)) {
                currentBacktrack = engine.eval(p, graphNode, stack, n + 1);
                environment.pop(mapping);
                if (currentBacktrack < n) {
                    return currentBacktrack;
                }
            }
        }
        return currentBacktrack;
    }

    Mappings unionBranch(Producer p, Node graphNode, Exp exp, Exp main, Mappings data) {
        if (exp.isFirstWith()) {
            return engine.subEval(p, graphNode, null, exp, main, data);
        } else if (isFederate(exp) || exp.isUnion() || engine.isParameterUnionMappings()) {
            Mappings unionData = unionData(exp, data);
            return engine.subEval(p, graphNode, null, exp, main, unionData);
        } else {
            Exp ee = exp.complete(data);
            return engine.subEval(p, graphNode, null, ee, main, null);
        }
    }

    Mappings unionData(Exp exp, Mappings data) {
        if (data != null) {
            if (data.getNodeList() == null) {
                MappingSet ms = new MappingSet(data);
                return ms.prepareMappingsRest(exp);
            } else if (data.getJoinMappings() != null) {
                return unionData(exp, data.getJoinMappings());
            }
        }
        return data;
    }

    boolean isFederate(Exp exp) {
        if (engine.getQuery().getGlobalQuery().isFederate()) {
            return true;
        }
        return exp.isRecFederate();
    }
}
