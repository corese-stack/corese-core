package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.List;

import static fr.inria.corese.core.next.query.impl.engine.eval.Eval.STOP;

/**
 * Evaluates inline SPARQL {@code VALUES} data blocks for {@link Eval}.
 *
 * <p>Iterates across declared solution mappings, binds inline variables into
 * memory, pushes execution onto the stack, and unbinds values on backtrack.</p>
 */
final class EvalValues {

    private final Eval engine;

    EvalValues(Eval engine) {
        this.engine = engine;
    }

    int eval(Producer p, Node graphNode, Exp exp, Stack stack, int n) {
        int backtrack = n - 1;
        engine.getVisitor().values(engine, engine.getGraphNode(graphNode), exp, exp.getMappings());

        for (Mapping map : exp.getMappings()) {
            if (engine.getState().isStopped()) {
                return STOP;
            }
            if (valuesBinding(exp.getNodeList(), map, n)) {
                backtrack = engine.eval(p, graphNode, stack, n + 1);
                free(exp.getNodeList(), map);

                if (backtrack < n) {
                    return backtrack;
                }
            }
        }

        return backtrack;
    }

    boolean valuesBinding(List<Node> varList, Mapping map, int n) {
        int i = 0;
        for (Node qNode : varList) {
            Node node = map.getNode(qNode);
            if (node != null) {
                Node value = engine.getProducer().getNode(node.getValue());
                boolean suc = engine.getMemory().push(qNode, value, n);
                if (!suc) {
                    popBinding(varList, map, i);
                    return false;
                }
            }
            i++;
        }
        return true;
    }

    void popBinding(List<Node> varList, Mapping map, int i) {
        for (int index = 0; index < i; index++) {
            Node variable = varList.get(index);
            if (map.getNode(variable) != null) {
                engine.getMemory().pop(variable);
            }
        }
    }

    void free(List<Node> varList, Mapping map) {
        for (Node qNode : varList) {
            if (map.getNode(qNode) != null) {
                engine.getMemory().pop(qNode);
            }
        }
    }
}
