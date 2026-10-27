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
 * Evaluates SPARQL {@code MINUS} pattern operations for {@link Eval}.
 *
 * <p>Evaluates the left branch, prepares compatibility constraints for the
 * right branch, evaluates the right branch, and filters out incompatible solutions
 * before continuing backtracking.</p>
 */
final class EvalMinus {

    private final Eval engine;

    EvalMinus(Eval engine) {
        this.engine = engine;
    }

    int eval(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) {
        int backtrack = n - 1;
        Memory env = engine.getMemory();

        Mappings map1 = engine.subEval(p, graphNode, null, exp.first(), exp, data);
        if (engine.getState().isStopped()) {
            return STOP;
        }
        if (map1.isEmpty()) {
            return backtrack;
        }

        MappingSet set1 = new MappingSet(map1);
        Exp rest = exp.rest();
        Mappings minusMappings = set1.prepareMappingsRest(rest);

        Mappings map2 = engine.subEval(p, graphNode, null, rest, exp, minusMappings);

        engine.getVisitor().minus(engine, engine.getGraphNode(graphNode), exp, map1, map2);

        MappingSet set = new MappingSet(exp, set1, new MappingSet(map2));
        set.start();

        for (Mapping map : map1) {
            if (engine.getState().isStopped()) {
                return STOP;
            }
            boolean ok = !set.minusCompatible(map);
            if (ok && env.push(map, n)) {
                backtrack = engine.eval(p, graphNode, stack, n + 1);
                env.pop(map);
                if (backtrack < n) {
                    return backtrack;
                }
            }
        }
        return backtrack;
    }
}
