package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.eval.Message;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles AST traversal, variable collection, and slot index allocation for {@link QueryPlan}.
 */
public final class QueryExpIndexer {

    private static final Logger logger = LoggerFactory.getLogger(QueryExpIndexer.class);

    private final QueryPlan queryPlan;
    private final VariableLayout layout;

    public QueryExpIndexer(QueryPlan queryPlan, VariableLayout layout) {
        this.queryPlan = queryPlan;
        this.layout = layout;
    }

    /**
     * Traverses query patterns and filters to register visible variables.
     */
    public void collect() {
        for (Exp ee : queryPlan) {
            collect(ee, false);
        }

        for (Node node : layout.getBindingNodes()) {
            layout.store(node, false, false);
        }

        for (Node node : layout.getArgList()) {
            layout.store(node, false, false);
        }

        for (Filter f : queryPlan.getFunList()) {
            collectExist(f.getExp());
        }
    }

    void collect(Exp exp, boolean exist) {
        switch (exp.type()) {
            case FILTER -> collectExist(exp.getFilter().getExp());
            case NODE -> layout.store(exp.getNode(), exist, false);
            case VALUES -> {
                for (Node node : exp.getNodeList()) {
                    layout.store(node, exist, false);
                }
            }
            case EDGE -> collectEdgeNodes(exp.getEdge(), exist);
            case XPATH, EVAL -> {
                for (int i = 0; i < exp.nbNode(); i++) {
                    layout.store(exp.getNode(i), exist, false);
                }
            }
            case MINUS -> collectMinusNodes(exp, exist);
            case QUERY -> {
                for (Exp ee : exp.getQuery().getSelectFun()) {
                    layout.store(ee.getNode(), exist, true);
                }
            }
            case BIND -> collectBindNodes(exp, exist);
            default -> {
                for (Exp ee : exp) {
                    collect(ee, exist);
                }
            }
        }
    }

    private void collectEdgeNodes(Edge edge, boolean insideExistencePattern) {
        layout.store(edge.getNode(0), insideExistencePattern, false);
        if (edge.getEdgeVariable() != null) {
            layout.store(edge.getEdgeVariable(), insideExistencePattern, false);
        }
        layout.store(edge.getNode(1), insideExistencePattern, false);
        for (int index = 2; index < edge.nbNode(); index++) {
            layout.store(edge.getNode(index), insideExistencePattern, false);
        }
    }

    private void collectMinusNodes(Exp expression, boolean insideExistencePattern) {
        if (expression.first() != null) {
            collect(expression.first(), insideExistencePattern);
        }
        if (expression.rest() != null) {
            collect(expression.rest(), true);
        }
    }

    private void collectBindNodes(Exp expression, boolean insideExistencePattern) {
        collectExist(expression.getFilter().getExp());
        layout.store(expression.getNode(), insideExistencePattern, false);
        if (expression.getNodeList() != null) {
            for (Node node : expression.getNodeList()) {
                layout.store(node, insideExistencePattern, false);
            }
        }
    }

    void collectExist(Expr exp) {
        if (exp.oper() == ExprType.EXIST) {
            Exp pat = exp.getPattern();
            collect(pat, true);
        } else {
            for (Expr ee : exp.getExpList()) {
                collectExist(ee);
            }
        }
    }

    /**
     * Recursively indexes expression nodes and edges.
     */
    public int index(QueryPlan query, Exp exp, boolean isExist, int start) {
        int min = switch (exp.type()) {
            case EDGE, XPATH, EVAL -> indexEdgeExp(exp);
            case VALUES -> indexValuesExp(exp);
            case NODE -> queryPlan.index(exp.getNode());
            case BIND -> indexBindExp(query, exp, isExist);
            case FILTER -> indexExpFilter(query, exp, isExist);
            case QUERY -> indexExpQuery(query, exp, isExist);
            case OPT_BIND, ACCEPT -> Integer.MAX_VALUE;
            default -> indexDefaultExp(query, exp, isExist, start);
        };

        if (exp.getGraphNode() != null) {
            queryPlan.index(exp.getGraphNode());
        }

        return min;
    }

    private int indexEdgeExp(Exp exp) {
        Edge edge = exp.getEdge();
        edge.setEdgeIndex(layout.allocateEdgeIndex());
        return indexExpEdge(exp);
    }

    private int indexValuesExp(Exp exp) {
        int min = Integer.MAX_VALUE;
        for (Node node : exp.getNodeList()) {
            int n = queryPlan.index(node);
            min = Math.min(min, n);
        }
        return min;
    }

    private int indexBindExp(QueryPlan query, Exp exp, boolean isExist) {
        Node qn = exp.getNode();
        int min = queryPlan.index(qn);
        if (exp.getNodeList() != null) {
            for (Node bn : exp.getNodeList()) {
                int ii = queryPlan.index(bn);
                min = Math.min(min, ii);
            }
        }
        return indexExpFilter(query, exp, isExist);
    }

    private int indexDefaultExp(QueryPlan query, Exp exp, boolean isExist, int start) {
        int min = Integer.MAX_VALUE;
        int startIndex = (start >= 0) ? start : queryPlan.globalNodeIndex();
        int ind = exp.isUnion() ? startIndex : -1;
        for (Exp e : exp) {
            int n = index(query, e, isExist, ind);
            min = Math.min(min, n);
        }
        return min;
    }

    int indexExpFilter(QueryPlan query, Exp exp, boolean isExist) {
        int min = Integer.MAX_VALUE;
        boolean hasExist = index(query, exp.getFilter());
        List<String> lVar = exp.getFilter().getVariables(true);

        for (String variable : lVar) {
            Node qNode = query.getProperAndSubSelectNode(variable);
            if (qNode == null) {
                logger.warn("{}{}", Message.Prefix.UNDEF_VAR.getString(), variable);
                queryPlan.addError(Message.Prefix.UNDEF_VAR.getString(), variable);
            } else if (!isExist && !hasExist) {
                int n = queryPlan.index(qNode);
                min = Math.min(min, n);
            }
        }
        if (hasExist) {
            min = -1;
        }
        return min;
    }

    int indexExpQuery(QueryPlan query, Exp exp, boolean isExist) {
        int min = Integer.MAX_VALUE;
        QueryPlan qq = exp.getQuery();
        qq.setCompiled();
        qq.setGlobalQuery(queryPlan);
        qq.setOuterQuery(query);
        qq.setAggregate();

        for (Exp e : exp) {
            index(qq, e, isExist, -1);
        }

        for (Exp ee : qq.getSelectFun()) {
            Node sNode = ee.getNode();
            int n = queryPlan.index(sNode);
            min = Math.min(min, n);

            if (ee.getFilter() != null) {
                index(qq, ee.getFilter());
            }
        }

        qq.complete2();
        return min;
    }

    private int indexExpEdge(Exp exp) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < exp.nbNode(); i++) {
            int n = queryPlan.index(exp.getNode(i));
            min = Math.min(min, n);
        }
        return min;
    }

    public boolean index(QueryPlan query, Filter f) {
        return index(query, f.getExp());
    }

    public boolean index(QueryPlan query, Expr exp) {
        boolean b = false;
        if (exp.oper() == ExprType.EXIST) {
            index(query, exp.getPattern(), true, -1);
            b = true;
        } else {
            for (Expr ee : exp.getExpList()) {
                b = index(query, ee) || b;
            }
        }
        return b;
    }
}
