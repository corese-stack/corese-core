package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.inria.corese.core.next.query.impl.engine.eval.Eval.STOP;

/**
 * Evaluates SPARQL {@code BIND} and assignment operators for {@link Eval}.
 *
 * <p>Handles standard {@code BIND(expr AS ?var)}, functional/unnesting bindings,
 * constant equality optimizations ({@code OPT_BIND} / {@code cbind}), and binds
 * target variables into memory with error suppression for unbound results.</p>
 */
final class EvalBind {

    private final Eval engine;

    EvalBind(Eval engine) {
        this.engine = engine;
    }

    int eval(Producer p, Node graphNode, Exp exp, Mappings map, Stack stack, int n) {
        if (exp.isFunctional()) {
            return extBind(p, graphNode, exp, stack, n);
        }

        int backtrack = n - 1;
        Memory env = engine.getMemory();
        Node node;
        try {
            node = engine.eval(graphNode, exp.getFilter(), env, p);
        } catch (QueryTypeErrorException error) {
            engine.getVisitor().bind(engine, engine.getGraphNode(graphNode), exp, null);
            return engine.eval(p, graphNode, stack, map, n + 1);
        }

        DatatypeValue dt = node == null ? null : node.getDatatypeValue();
        engine.getVisitor().bind(engine, engine.getGraphNode(graphNode), exp, dt);

        if (node == null) {
            backtrack = engine.eval(p, graphNode, stack, map, n + 1);
        } else if (env.push(exp.getNode(), node, n)) {
            backtrack = engine.eval(p, graphNode, stack, map, n + 1);
            env.pop(exp.getNode());
        }

        return backtrack;
    }

    int extBind(Producer p, Node graphNode, Exp exp, Stack stack, int n) {
        int backtrack = n - 1;
        Memory env = engine.getMemory();
        env.setGraphNode(graphNode);
        Mappings map = engine.eval(exp.getFilter(), env, exp.getNodeList());
        env.setGraphNode(null);
        engine.getVisitor().values(engine, engine.getGraphNode(graphNode), exp, map);

        if (map != null) {
            Map<String, Node> tab = toMap(exp.getNodeList());
            for (Mapping m : map) {
                if (engine.getState().isStopped()) {
                    return STOP;
                }
                if (env.push(tab, m, n)) {
                    backtrack = engine.eval(p, graphNode, stack, n + 1);
                    env.pop(tab, m);
                    if (backtrack < n) {
                        return backtrack;
                    }
                }
            }
        }

        return backtrack;
    }

    Map<String, Node> toMap(List<Node> list) {
        Map<String, Node> m = new HashMap<>();
        for (Node node : list) {
            m.put(node.getLabel(), node);
        }
        return m;
    }

    int optBind(Producer p, Node gNode, Exp exp, Stack stack, int n) {
        Memory env = engine.getMemory();
        if (exp.isBindCst()) {
            return cbind(p, gNode, exp, stack, n);
        }

        int i = 0;
        int j = 1;
        Node node = env.getNode(exp.get(i).getNode());
        if (node == null) {
            i = 1;
            j = 0;
            node = env.getNode(exp.get(i).getNode());
            if (node == null) {
                return engine.eval(p, gNode, stack, n + 1);
            }
        }

        Node qNode = exp.get(j).getNode();
        if (!env.isBound(qNode) && engine.getProducer().isBindable(node)) {
            env.push(qNode, node, env.getIndex(exp.get(i).getNode()));
            int backtrack = engine.eval(p, gNode, stack, n + 1);
            env.pop(qNode);
            return backtrack;
        } else {
            return engine.eval(p, gNode, stack, n + 1);
        }
    }

    int cbind(Producer p, Node gNode, Exp exp, Stack stack, int n) {
        Node qNode = exp.get(0).getNode();
        if (!exp.status() || engine.getMemory().isBound(qNode)) {
            return engine.eval(p, gNode, stack, n + 1);
        }

        if (exp.getNodeList() == null) {
            initBindNodes(exp, engine.getProducer());
        }

        if (exp.getNodeList() != null) {
            return evalBindNodes(p, gNode, exp, stack, n, qNode);
        }
        return engine.eval(p, gNode, stack, n + 1);
    }

    void initBindNodes(Exp exp, Producer prod) {
        for (Object value : exp.getObjectValues()) {
            Expr cst = (Expr) value;
            Node node = prod.getNode(cst.getValue());
            if (node != null && prod.isBindable(node)) {
                exp.addNode(node);
            } else {
                exp.setNodeList(null);
                exp.status(false);
                break;
            }
        }
    }

    int evalBindNodes(Producer p, Node gNode, Exp exp, Stack stack, int n, Node qNode) {
        Memory env = engine.getMemory();
        for (Node node : exp.getNodeList()) {
            env.push(qNode, node, n);
            int backtrack = engine.eval(p, gNode, stack, n + 1);
            env.pop(qNode);
            if (backtrack < n) {
                return backtrack;
            }
        }
        return n - 1;
    }
}
