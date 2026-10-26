package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import static fr.inria.corese.core.next.query.impl.engine.eval.Eval.STOP;

/**
 * Evaluates nested SPARQL subqueries for {@link Eval}.
 *
 * <p>Executes the subquery in an isolated child evaluation frame with cloned
 * memory, captures projected results, binds selected outer variables into the
 * parent frame, and unbinds them upon backtracking.</p>
 */
final class EvalSubQuery {

    private final Eval engine;

    EvalSubQuery(Eval engine) {
        this.engine = engine;
    }

    int eval(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) {
        int backtrack = n - 1;
        QueryPlan subQuery = exp.getQuery();
        Memory env = engine.getMemory();
        engine.getVisitor().start(subQuery);

        Eval ev = engine.copy(engine.copyMemory(env, engine.getQuery(), subQuery, null), p, engine.getEvaluator(), false);
        Mappings lMap = ev.eval(graphNode, subQuery, null, engine.selectQueryMappings(data));

        engine.getVisitor().query(engine, engine.getGraphNode(graphNode), exp, lMap);
        engine.getVisitor().finish(lMap);

        for (Mapping map : lMap) {
            if (engine.getState().isStopped()) {
                return STOP;
            }
            boolean bmatch = push(subQuery, map, n);

            if (bmatch) {
                backtrack = engine.eval(p, graphNode, stack, n + 1);
                pop(subQuery, map);
                if (backtrack < n) {
                    return backtrack;
                }
            }
        }

        return backtrack;
    }

    private boolean push(QueryPlan subQuery, Mapping res, int n) {
        int k = 0;
        Memory env = engine.getMemory();
        Matcher mm = engine.getMatcher();
        QueryPlan qq = engine.getQuery();

        for (Exp exp : subQuery.getSelectFun()) {
            Node subNode = exp.getNode();
            Node node = res.getNode(subNode);
            Node outNode;
            if (exp.size() == 0) {
                outNode = qq.getOuterNodeSelf(subNode);
                exp.add(outNode);
            } else {
                outNode = exp.get(0).getNode();
            }

            if (node != null && !(mm.match(outNode, node, env) && env.push(outNode, node, n))) {
                for (int i = 0; i < k; i++) {
                    subNode = subQuery.getSelect().get(i);
                    outNode = qq.getOuterNodeSelf(subNode);
                    Node value = res.getNode(subNode);
                    if (value != null) {
                        env.pop(outNode);
                    }
                }
                return false;
            }
            k++;
        }

        return true;
    }

    private void pop(QueryPlan subQuery, Mapping ans) {
        Memory env = engine.getMemory();
        QueryPlan qq = engine.getQuery();

        for (Node subNode : subQuery.getSelect()) {
            if (ans.isBound(subNode)) {
                Node outNode = qq.getOuterNodeSelf(subNode);
                env.pop(outNode);
            }
        }
    }
}
