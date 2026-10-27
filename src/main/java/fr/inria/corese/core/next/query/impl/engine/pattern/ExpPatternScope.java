package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates variable scope analysis, in-scope node collection, and filter variable
 * sharing across compiled SPARQL pattern trees ({@link Exp}).
 */
public final class ExpPatternScope {

    private ExpPatternScope() {
        // Utility class
    }

    /**
     * Traverses the given expression pattern and populates the collector with visible nodes.
     */
    public static void getNodes(Exp exp, ExpNodeCollector h) {
        switch (exp.type()) {
            case FILTER -> {
                if (h.isExist()) {
                    getExistNodes(exp.getFilter().getExp(), h, h.getExistNodeList());
                }
            }
            case NODE, ACCEPT -> h.add(exp.getNode());
            case EDGE -> collectEdgeNodes(exp, h);
            case VALUES -> {
                if (exp.getNodeList() != null) {
                    for (Node varNode : exp.getNodeList()) {
                        h.add(varNode);
                    }
                }
            }
            case MINUS -> {
                if (exp.first() != null) {
                    getNodes(exp.first(), h);
                }
            }
            case OPTIONAL -> collectOptionalNodes(exp, h);
            case GRAPH -> {
                h.add(exp.getGraphName());
                if (exp.size() > 1) {
                    getNodes(exp.rest(), h);
                }
            }
            case UNION -> collectUnionNodes(exp, h);
            case BIND -> collectBindNodes(exp, h);
            case QUERY -> queryNodeList(exp, h);
            default -> collectDefaultNodes(exp, h);
        }
    }

    private static void collectEdgeNodes(Exp exp, ExpNodeCollector h) {
        Edge pathEdge = exp.getEdge();
        h.add(pathEdge.getNode(0));
        if (pathEdge.getEdgeVariable() != null) {
            h.add(pathEdge.getEdgeVariable());
        }
        h.add(pathEdge.getNode(1));
        for (int i = 2; i < pathEdge.nbNode(); i++) {
            h.add(pathEdge.getNode(i));
        }
    }

    private static void collectOptionalNodes(Exp exp, ExpNodeCollector h) {
        boolean b = h.isOptional();
        getNodes(exp.first(), h.setOptional(true));
        if (!h.isInSubScope()) {
            getNodes(exp.rest(), h);
        }
        h.setOptional(b);
    }

    private static void collectUnionNodes(Exp exp, ExpNodeCollector h) {
        if (h.isInSubScope()) {
            List<Node> left = getTheNodes(exp.first(), h.copy());
            List<Node> right = getTheNodes(exp.rest(), h.copy());
            for (Node nodeLeft : left) {
                if (right.contains(nodeLeft)) {
                    h.add(nodeLeft);
                }
            }
        } else {
            for (Exp ee : exp) {
                getNodes(ee, h);
            }
        }
    }

    private static void collectBindNodes(Exp exp, ExpNodeCollector h) {
        if (h.isBind()) {
            if (exp.getNodeList() == null) {
                h.add(exp.getNode());
            } else {
                for (Node nodeBind : exp.getNodeList()) {
                    h.add(nodeBind);
                }
            }
        }
    }

    private static void collectDefaultNodes(Exp exp, ExpNodeCollector h) {
        for (Exp ee : exp) {
            getNodes(ee, h);
            if (h.isInSubScopeSample() && ee.isSkipStatement()) {
                break;
            }
        }
    }

    static void queryNodeList(Exp exp, ExpNodeCollector h) {
        List<Node> selectList = h.getSelectNodeList();
        List<Node> subSelectList = exp.getQuery().getSelect();

        if (h.isInSubScope()) {
            List<Node> scopeList = getTheNodes(exp.getQuery().getBody(), h.copy());
            for (Node nodeScope : scopeList) {
                if (subSelectList.contains(nodeScope)) {
                    add(selectList, nodeScope);
                }
            }
        } else {
            for (Node nodeSubSelect : subSelectList) {
                add(selectList, nodeSubSelect);
            }
        }
    }

    static void getExistNodes(Expr expr, ExpNodeCollector h, List<Node> lExistNode) {
        if (expr.oper() == ExprType.EXIST) {
            Exp pat = expr.getPattern();
            List<Node> lNode = getTheNodes(pat, h.copy());
            for (Node exprNode : lNode) {
                add(lExistNode, exprNode);
            }
        } else {
            for (Expr ee : expr.getExpList()) {
                getExistNodes(ee, h, lExistNode);
            }
        }
    }

    public static List<Node> getTheNodes(Exp exp, ExpNodeCollector h) {
        getNodes(exp, h);
        return h.getNodes();
    }

    public static List<Node> getRecordInScopeNodes(Exp exp, boolean bind) {
        if (exp.getInScopeNodeList() == null) {
            exp.setInScopeNodeList(getInScopeNodes(exp, true, bind));
        }
        return exp.getInScopeNodeList();
    }

    public static List<Node> getRecordInScopeNodesWithoutBind(Exp exp) {
        return getRecordInScopeNodes(exp, false);
    }

    public static List<Node> getAllInScopeNodes(Exp exp) {
        return getTheNodes(exp, new ExpNodeCollector(true, true).all());
    }

    public static List<Node> getNodes(Exp exp) {
        return getTheNodes(exp, new ExpNodeCollector(false, true).sample());
    }

    public static List<Node> getInScopeNodes(Exp exp) {
        return getInScopeNodes(exp, true, true);
    }

    public static List<Node> getInScopeNodes(Exp exp, boolean inSubScope, boolean bind) {
        return getTheNodes(exp, new ExpNodeCollector(inSubScope, bind).sample());
    }

    public static void add(List<Node> lNode, Node node) {
        if (node != null && node.isVariable() && !node.isBlank() && !lNode.contains(node)) {
            lNode.add(node);
        }
    }

    public static void share(Exp exp, List<String> filterVar, List<String> expVar) {
        switch (exp.type()) {
            case FILTER, OPT_BIND, OPTION -> {
                // Do not bind
            }
            case OPTIONAL, MINUS -> share(exp.first(), filterVar, expVar);
            case UNION -> shareUnion(exp, filterVar, expVar);
            case QUERY -> shareQuery(exp, filterVar, expVar);
            case BIND -> share(exp.getNode(), filterVar, expVar);
            case EDGE -> {
                for (int i = 0; i < exp.nbNode(); i++) {
                    share(exp.getNode(i), filterVar, expVar);
                }
            }
            case NODE -> share(exp.getNode(), filterVar, expVar);
            default -> {
                for (Exp e : exp) {
                    share(e, filterVar, expVar);
                }
            }
        }
    }

    public static void share(Node node, List<String> fVar, List<String> eVar) {
        if (node != null && node.isVariable()
                && fVar.contains(node.getLabel())
                && !eVar.contains(node.getLabel())) {
            eVar.add(node.getLabel());
        }
    }

    public static void shareUnion(Exp exp, List<String> filterVar, List<String> expVar) {
        List<String> lVar1 = new ArrayList<>();
        List<String> lVar2 = new ArrayList<>();
        share(exp.first(), filterVar, lVar1);
        share(exp.rest(), filterVar, lVar2);
        for (String varString : lVar1) {
            if (lVar2.contains(varString) && !expVar.contains(varString)) {
                expVar.add(varString);
            }
        }
    }

    public static void shareQuery(Exp exp, List<String> filterVar, List<String> expVar) {
        List<String> lVar = new ArrayList<>();
        share(exp.getQuery().getBody(), filterVar, lVar);

        for (Exp child : exp.getQuery().getSelectFun()) {
            String name = child.getNode().getLabel();
            if ((lVar.contains(name) || child.getFilter() != null) && !expVar.contains(name)) {
                expVar.add(name);
            }
        }
    }

    public static boolean bound(List<String> fvec, List<String> evec) {
        for (String varString : fvec) {
            if (!evec.contains(varString)) {
                return false;
            }
        }
        return true;
    }

    public static void inscopeFilter(Exp exp) {
        List<Node> l1 = getRecordInScopeNodesWithoutBind(exp.first());
        List<Node> l2 = getRecordInScopeNodesWithoutBind(exp.rest());

        for (Exp child : exp.rest()) {
            if (child.isFilter() && !child.getFilter().isRecExist()) {
                List<String> lvar = child.getFilter().getVariables();
                if (bind(l1, lvar) && bind(l2, lvar)) {
                    exp.getCreateInscopeFilter().add(child);
                }
            }
        }
    }

    public static boolean bind(List<Node> lnode, List<String> lvar) {
        for (String variable : lvar) {
            boolean suc = false;
            for (Node bindNode : lnode) {
                if (bindNode.getLabel().equals(variable)) {
                    suc = true;
                    break;
                }
            }
            if (!suc) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasUnboundVariables(Exp exp, Filter f) {
        List<String> varList = f.getVariables();
        List<String> nodeList = getNodeVariables(exp);

        for (String variable : varList) {
            if (!nodeList.contains(variable)) {
                return true;
            }
        }

        return false;
    }

    public static List<String> getNodeVariables(Exp exp) {
        List<String> list = new ArrayList<>();
        for (Exp child : exp) {
            switch (child.type()) {
                case EDGE -> getEdgeVariables(child, list);
                case BIND -> getBindVariables(child, list);
                case VALUES -> getValuesVariables(child, list);
                default -> {
                    // other types don't declare direct node variables
                }
            }
        }
        return list;
    }

    public static void getBindVariables(Exp exp, List<String> list) {
        if (exp.getNodeList() == null) {
            addVariable(list, exp.getNode());
        } else {
            getValuesVariables(exp, list);
        }
    }

    public static void getValuesVariables(Exp exp, List<String> list) {
        if (exp.getNodeList() != null) {
            for (Node valuesNode : exp.getNodeList()) {
                addVariable(list, valuesNode);
            }
        }
    }

    public static void getEdgeVariables(Exp exp, List<String> list) {
        addVariable(list, exp.getEdge().getNode(0));
        addVariable(list, exp.getEdge().getNode(1));
        if (!exp.isPath()) {
            addVariable(list, exp.getEdge().getProperty());
        }
    }

    public static void addVariable(List<String> list, Node node) {
        if (node != null && node.isVariable() && !list.contains(node.getLabel())) {
            list.add(node.getLabel());
        }
    }
}
