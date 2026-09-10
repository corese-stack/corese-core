package fr.inria.corese.core.next.query.impl.engine.solution;

import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;

import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.eval.PointerObject;
import fr.inria.corese.core.next.query.impl.engine.eval.Stack;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.engine.model.BindingContext;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import fr.inria.corese.core.next.query.impl.engine.event.KgramEventDispatcher;
import fr.inria.corese.core.next.query.impl.engine.path.Path;
import fr.inria.corese.core.next.query.impl.engine.eval.ApproximateSearchEnv;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node and Edge binding stacks for KGRAM evaluator
 *
 * @author Olivier Corby, Edelweiss, INRIA 2009
 */
public class Memory extends PointerObject implements Environment {

    private static final String SERVICE_REPORT_ZERO = "?_service_report_0";
    static final Edge[] emptyEdges = new Edge[0];
    static final Edge[] emptyEntities = new Edge[0];
    // number of times nodes are bound by Stack
    // decrease with backtrack
    int[] nbNodes;
    int[] nbEdges;
    // stackIndex[n] = index in Eval Exp stack where nth node is bound first
    // enable to compute where to backjump
    int[] stackIndex;
    Edge[] qEdges;
    Edge[] result;
    Node[] qNodes;
    Node[] nodes;
    Evaluator eval;
    Matcher match;
    Eval kgram;
    Stack stack;
    Exp exp;
    Object object;
    // bnode(label) must return same bnode in same solution, different otherwise.
    // hence must clear bnodes after each solution
    Map<String, DatatypeValue> bnode;
    //  query or sub query
    Query query;
    Node gNode;
    // to evaluate aggregates such as count(?x)
    Mappings results;
    Mappings group;
    Mapping mapping;
    // true when processing aggregate at the end
    boolean isAggregate = false;
    KgramEventDispatcher manager;
    boolean hasEvent = false;
    int nbEdge = 0;
    int nbNode = 0;
    // service evaluation detail report
    private DatatypeValue detail;
    private boolean isEdge;
    private BindingContext bindingContext;
    private ApproximateSearchEnv appxSearchEnv;

    public Memory() {
    }

    public Memory(Matcher m, Evaluator e) {
        match = m;
        eval = e;
        bnode = new HashMap<>();
        this.appxSearchEnv = new ApproximateSearchEnv();
    }

    @Override
    public KgramEventDispatcher getEventManager() {
        if (manager == null) {
            kgram.createManager();
        }
        return manager;
    }

    public void setEventManager(KgramEventDispatcher man) {
        manager = man;
        hasEvent = true;
    }

    @Override
    public Eval getEval() {
        return kgram;
    }

    @Override
    public void setEval(Eval e) {
        kgram = e;
    }

    @Override
    public ProcessVisitor getVisitor() {
        return getEval().getVisitor();
    }

    void setGroup(Mappings lm) {
        group = lm;
    }

    public Memory setResults(Mappings r) {
        results = r;
        return this;
    }

    @Override
    public Query getQuery() {
        return query;
    }

    public Matcher getMatcher() {
        return match;
    }

    @Override
    public Node getGraphNode() {
        return gNode;
    }

    @Override
    public void setGraphNode(Node g) {
        gNode = g;
    }

    public Stack getStack() {
        return stack;
    }


    @Override
    public Exp getExp() {
        return exp;
    }

    @Override
    public void setExp(Exp ee) {
        exp = ee;
    }

    public boolean isAggregate() {
        return isAggregate;
    }

    public void init(Memory memory) {
        setGraphNode(memory.getGraphNode());
        setEval(memory.getEval());
    }

    public Memory init(Query q) {
        // store (sub) query
        query = q;
        if (q.isSubQuery()) {
            // we need the outer query to get the max nb of nodes
            // because index may vary from 0 to max in any sub query
            q = q.getGlobalQuery();
        }
        if (q.isRecordEdge()) {
            // rule record edge
            isEdge = true;
        }
        int nmax = q.nbNodes();
        int emax = q.nbEdges();
        nbNodes = new int[nmax];
        stackIndex = new int[nmax];
        if (!isEdge) {
            emax = 0;
        }
        nbEdges = new int[emax];
        result = new Edge[emax];
        qEdges = new Edge[emax];

        nodes = new Node[nmax];
        qNodes = new Node[nmax];

        start();
        return this;
    }

    void start() {
        nbEdge = 0;
        nbNode = 0;
        for (int i = 0; i < nbNodes.length; i++) {
            qNodes[i] = null;
            nodes[i] = null;
            nbNodes[i] = 0;
            stackIndex[i] = -1;
        }
        if (isEdge) {
            for (int i = 0; i < nbEdges.length; i++) {
                nbEdges[i] = 0;
                result[i] = null;
                qEdges[i] = null;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Node qNode : qNodes) {
            if (qNode != null) {
                if (n++ > 0) {
                    sb.append("\n");
                }
                sb.append("(").append(qNode.getIndex()).append(") ");

                sb.append(qNode).append(" = ").append(getNode(qNode));
            }
        }
        if (getBind() != null && getBind().hasBind()) {
            sb.append("\n").append(getBind());
        }
        return sb.toString();
    }

    Node getNode(String name, List<Node> list) {
        for (Node node : list) {
            if (node.getLabel().equals(name)) {
                return node;
            }
        }
        return null;
    }

    /**
     * mem is a fresh new Memory, init() has been done Copy this memory into mem
     * Use case: exists pattern , sub query Can bind all Memory nodes or bind only
     * subquery select nodes (2 different semantics)
     * Note: let ( .., exists pattern),
     * MUST push BGP solution and then push Bind
     */
    public void copyInto(Query sub, Memory mem, Exp exp) {
        if (sub == null) {
            // exists pattern
            copyInto(mem, exp);
        } else if (eval.getMode() != Evaluator.Mode.SPARQL_MODE) {
            // bind subquery select nodes
            // take only from this memory the nodes
            // that are select nodes of sub query
            // hence sub query memory share only select node bindings
            // with outer query memory
            // use case: ?x :p ?z (select ?z where (?x :q ?z))
            // ?x in sub query is not the same as ?x in outer query (it is not bound here)
            // only ?z is the same
            for (Node subNode : sub.getSelect()) {
                copyInto(subNode, mem);
            }
            mem.share(this);
        }
    }

    /**
     * exists pattern
     * PRAGMA: when exists is in function, this memory is empty
     */
    public void copyInto(Memory mem, Exp exp) {
        if (hasBind()) {
            // bind ldscript variables as sparql
            // pattern matching variables
            mem.copy(getBind(), exp);
        }
        mem.share(this);
        copyInto(mem);
    }

    /**
     * Share global variable, context, etc.
     */
    public void share(BindingContext target, BindingContext source) {
        if (source != null && target != null) {
            target.share(source);
        }
    }

    public void share(Memory source) {
        share(getBind(), source.getBind());
    }

    /**
     * Copy this BindingContext local variable stack into this memory
     */
    void copy(BindingContext bindCtx, Exp exp) {
        List<Node> list = exp.getNodes();
        for (Map.Entry<String, Node> entry : bindCtx.getBindings().entrySet()) {
            String varLabel = entry.getKey();
            Node qn = getNode(varLabel, list);
            if (qn != null) {
                push(qn, entry.getValue());
            }
        }
    }

    void copyInto(Memory mem) {
        // bind all nodes
        // use case: inpath copy the memory
        for (Node qNode : qNodes) {
            copyInto(qNode, mem);
        }
    }

    void copyInto(Node qNode, Memory mem) {
        if (qNode != null) {
            Node tNode = getNode(qNode);
            if (tNode != null) {
                mem.push(qNode, tNode, -1);
            }
        }
    }

    public Mapping store(Query q, Producer p, boolean subEval) {
        return store(q, p, subEval, false);
    }

    /**
     * subEval = true : result of statement such as minus/optional etc
     * in this case: no select exp, no order by, no group by, etc
     * subEval = false: main or nested select query.
     */
    @SuppressWarnings("java:S3776") // Legacy KGRAM solution construction algorithm assembling Mapping results from memory state
    Mapping store(Query q, Producer p, boolean subEval, boolean func) {
        boolean complete = !q.getGlobalQuery().isAlgebra();

        Node detailNode = null;
        if (getReport() != null) {
            // draft: set service report as variable value
            // use case: xt:sparql() return map with report
            // PluginImpl sparql() record report in Environment
            // detailNode is defined by ASTParser with @report metadata
            detailNode = getQuery().getSelectNode(SERVICE_REPORT_ZERO);
            if (detailNode != null) {
                push(detailNode, (Node) getReport());
            }
        }

        int nb = nbNode;
        if (!subEval && complete) {
            // select (exp as var) it may happen that var is already bound in
            // memory (bindings, subquery), so we should not allocate a
            // supplementary cell for var in Mapping node array
            for (Exp selectFunExp : q.getSelectFun()) {
                if (selectFunExp.getFilter() != null && !isBound(selectFunExp.getNode())) {
                    nb++;
                }
            }
        }
        Edge[] qedge = emptyEdges;
        Edge[] tedge = emptyEntities;
        Node[] qnode = new Node[nb];
        Node[] tnode = new Node[nb];
        // order by
        Node[] snode = new Node[q.getOrderBy().size()];
        Node[] gnode = new Node[q.getGroupBy().size()];

        int n = 0;
        int i = 0;
        if (isEdge) {
            qedge = new Edge[nbEdge];
            tedge = new Edge[nbEdge];
            for (Edge edge : qEdges) {
                if (edge != null) {
                    qedge[n] = edge;

                    tedge[n] = p.copy(result[i]);
                    n++;
                }
                i++;
            }
        }

        n = 0;
        i = 0;
        for (Node node : qNodes) {
            if (node != null) {
                qnode[n] = node;
                tnode[n] = nodes[i];
                n++;
            }
            i++;
        }

        Mapping map = null;

        if (complete) {
            if (subEval) {
                // statement e.g. minus/optional/union
                if (func) {
                    orderGroup(q.getOrderBy(), snode, p);
                    orderGroup(q.getGroupBy(), gnode, p);
                }
            } else {
                // main query or nested query
                int count = 0;
                for (Exp e : q.getSelectFun()) {

                    Filter f = e.getFilter();

                    if (f != null) {
                        // select fun(?x) as ?y
                        Node node = null;
                        boolean isBound = isBound(e.getNode());

                        if (!e.isAggregate()) {


                            node = evaluateExpressionNode(f, p);
                            kgram.getVisitor().select(kgram, f.getExp(), node == null ? null : node.getDatatypeValue());
                            // bind fun(?x) as ?y
                            boolean success = push(e.getNode(), node);
                            if (success) {
                                count++;
                            } else {
                                // use case: var was already bound and there is a select (exp as var)
                                // and the two values of var are different
                                // pop previous exp nodes and return null
                                int j = 0;

                                for (Exp ee : q.getSelectFun()) {
                                    // pop previous exp nodes if any
                                    if (j >= count) {
                                        // we have poped all exp nodes
                                        return null;
                                    }

                                    if (ee.getFilter() != null) {
                                        pop(ee.getNode());
                                        j++;
                                    }
                                }
                            }
                        }

                        if (!isBound) {
                            // use case: select (exp as var) where var is already bound
                            qnode[n] = e.getNode();
                            tnode[n] = node;
                            n++;
                        }
                    }
                }

                map = new Mapping(qedge, tedge, qnode, tnode);
                mapping = map;
                map.init();
                // order/group by may access mapping with xt:result()
                orderGroup(q.getOrderBy(), snode, p);
                orderGroup(q.getGroupBy(), gnode, p);

                for (Exp e : q.getSelectFun()) {
                    Filter f = e.getFilter();
                    if (f != null && !e.isAggregate()) {
                        // pop fun(?x) as ?y
                        pop(e.getNode());
                    }
                }
            }
        }

        if (map == null) {
            map = new Mapping(qedge, tedge, qnode, tnode);
            mapping = map;
        }

        if (detailNode != null) {
            pop(detailNode);
            setReport(null);
        }

        map.setOrderBy(snode);
        map.setGroupBy(gnode);
        clear();
        mapping = null;

        return map;
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }

    @Override
    public int size() {
        return nbNode;
    }

    /**
     * BNode table cleared for new solution
     */
    public void clear() {
        bnode.clear();
    }

    @Override
    public Map<String, DatatypeValue> getMap() {
        return bnode;
    }

    void setMap(Map<String, DatatypeValue> m) {
        bnode = m;
    }

    void orderGroup(List<Exp> lExp, Node[] nodes, Producer p) {
        int n = 0;
        for (Exp e : lExp) {
            Node qNode = e.getNode();
            if (qNode != null) {
                nodes[n] = getNode(qNode);
            }
            if (nodes[n] == null) {
                Filter f = e.getFilter();
                if (f != null && !e.isAggregate()) {
                    nodes[n] = evaluateExpressionNode(f, p);
                }

            }
            n++;
        }
    }

    private Node evaluateExpressionNode(Filter filter, Producer producer) {
        try {
            return producer.getNode(kgram.eval(filter, this, producer));
        } catch (QueryTypeErrorException error) {
            // A scalar type error leaves a projected value or sorting key unbound.
            return null;
        }
    }

    boolean pushNodeList(Producer p, Node node, Edge edge, int i) {
        if (node.isMatchCardinality()) {
            return pushCardinality(p, node, edge, i);
        }
        return pushList(p, node, edge, i);
    }

    boolean pushList(Producer p, Node node, Edge edge, int i) {
        ArrayList<Node> list = new ArrayList<>();
        for (int j = i; j < edge.nbNode(); j++) {
            list.add(edge.getNode(j));
        }
        Node target = (p == null) ? node : p.getDatatypeNodeFactory().nodeList(list);
        return push(node, target, i);
    }

    boolean pushCardinality(Producer p, Node node, Edge edge, int i) {
        int n = edge.nbNode() - i;
        Node target = (p == null) ? node : p.getDatatypeNodeFactory().nodeValue(n);
        return push(node, target, i);
    }

    /**
     * pop nodes when fail
     */
    boolean push(Edge q, Edge ent, int n) {
        return push(null, q, ent, n);
    }

    public boolean push(Producer p, Edge q, Edge ent, int n) {
        if (!pushNodes(p, q, ent, n)) {
            return false;
        }
        if (!pushEdgeVariable(q, ent, n)) {
            return false;
        }
        if (isEdge) {
            recordEdge(q, ent);
        }
        return true;
    }

    private boolean pushNodes(Producer p, Edge q, Edge ent, int n) {
        int max = q.nbNode();
        for (int i = 0; i < max; i++) {
            Node node = q.getNode(i);
            if (node != null) {
                boolean success = node.isMatchNodeList() ? pushNodeList(p, node, ent, i) : push(node, ent.getNode(i), n);
                if (!success) {
                    pop(q, i);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean pushEdgeVariable(Edge q, Edge ent, int n) {
        Node pNode = q.getEdgeVariable();
        if (pNode != null) {
            boolean success = push(pNode, ent.getEdgeNode(), n);
            if (!success) {
                pop(q, q.nbNode());
                return false;
            }
        }
        return true;
    }

    private void recordEdge(Edge q, Edge ent) {
        int index = q.getEdgeIndex();
        if (nbEdges[index] == 0) {
            nbEdge++;
        }
        nbEdges[index]++;
        qEdges[index] = q;
        result[index] = ent;
    }

    void pop(Edge q, int length) {
        for (int j = 0; j < length; j++) {
            Node qNode = q.getNode(j);
            if (qNode != null) {
                pop(qNode);
            }
        }
    }

    /**
     * Push a target node in the stack only if the binding is correct: same
     * query/ same target
     */
    public boolean push(Node node, Node target) {
        return push(node, target, -1);
    }

    /**
     * n is the index in Exp stack where Node is bound
     */
    public boolean push(Node node, Node target, int n) {
        if (node.isConstant()) {
            return true;
        }
        int index = node.getIndex();
        if (nodes[index] == null) {
            nodes[index] = target;
            qNodes[index] = node;

            nbNode++;
            nbNodes[index]++;
            // exp stack index where node is bound
            stackIndex[index] = n;
            return true;
        } else if (target == null) {
            // may happen with aggregate or subquery
            return false;
        } else if (match.same(node, nodes[index], target, this)) {
            nbNodes[index]++;
            return true;
        }

        // Query node already bound but target not equal to binding
        // also process use case: ?x ?p ?p
        return false;
    }

    public void pop(Node node) {
        if (node.isVariable()) {
            int index = node.getIndex();
            if (nbNodes[index] > 0) {
                nbNodes[index]--;
                if (nbNodes[index] == 0) {
                    nbNode--;
                    nodes[index] = null;
                    qNodes[index] = null;
                    stackIndex[index] = -1;
                }
            }
        }
    }

    /*
     * max index where edge nodes are bound first
     * hence where to backjump when edge fails
     */
    public int getIndex(Node gNode, Edge edge) {
        int max = -1;
        int length = edge.nbNode();
        for (int i = 0; i < length; i++) {
            Node qNode = edge.getNode(i);
            if (qNode != null) {
                int n = qNode.getIndex();
                if (stackIndex[n] > max) {
                    max = stackIndex[n];
                }
            }
        }
        Node pNode = edge.getEdgeVariable();
        if (pNode != null) {
            int n = pNode.getIndex();
            if (stackIndex[n] > max) {
                max = stackIndex[n];
            }
        }
        if (gNode != null && gNode.isVariable()) {
            int n = gNode.getIndex();
            if (stackIndex[n] > max) {
                max = stackIndex[n];
            }
        }
        return max;
    }

    public int getIndex(List<Node> lNodes) {
        int max = -1;
        for (Node node : lNodes) {
            int index = getIndex(node);
            if (index == -1) {
                return -1;
            }
            max = Math.max(max, index);
        }
        return max;
    }

    public int getIndex(Node node) {
        return stackIndex[node.getIndex()];
    }

    public void pop(Edge q) {
        popNode(q);
        if (isEdge) {
            popEdge(q);
        }
    }

    void popNode(Edge q) {
        if (q != null) {
            int max = q.nbNode();
            for (int i = 0; i < max; i++) {
                Node node = q.getNode(i);
                if (node != null) {
                    pop(node);
                }
            }

            // the edge node if any
            // use case: ?x ?p ?y
            Node pNode = q.getEdgeVariable();
            if (pNode != null) {
                // it was pushed only if it is a variable
                pop(pNode);
            }
        }
    }

    void popEdge(Edge q) {
        int index = q.getEdgeIndex();
        if (nbEdges[index] > 0) {
            nbEdges[index]--;
            if (nbEdges[index] == 0) {
                nbEdge--;
                qEdges[index] = null;
                result[index] = null;
            }
        }
    }

    /**
     * Push elementary result in the memory
     */
    public boolean push(Mapping res, int n) {
        return push(res, n, isEdge);
    }

    /**
     * Bind Mapping in order to compute aggregate on one group Create a fresh
     * new bnode table for the solution of this group use case: select
     * (count(?x) as ?c) (bnode(?c) as ?bn)
     */
    void aggregate(Mapping map) {
        push(map, -1);
        Map<String, DatatypeValue> bnodeMap = map.getMap();
        if (bnodeMap == null) {
            bnodeMap = new HashMap<>();
            map.setMap(bnodeMap);
        }
        setMap(bnodeMap);
    }

    public boolean push(Mapping res, int n, boolean isEdge) {
        return push(res, n, isEdge, true);
    }

    public boolean push(Mapping res, int n, boolean isEdge, boolean isBlank) {
        if (!pushMappingNodes(res, n, isBlank)) {
            return false;
        }
        return !isEdge || pushMappingEdges(res, n);
    }

    private boolean pushMappingNodes(Mapping res, int n, boolean isBlank) {
        int k = 0;
        for (Node qNode : res.getQueryNodes()) {
            if (qNode != null && qNode.getIndex() >= 0 && (!qNode.isBlank() || isBlank)) {
                Node node = res.getNode(k);
                if (!push(qNode, node, n)) {
                    for (int i = 0; i < k; i++) {
                        pop(res.getQueryNode(i));
                    }
                    return false;
                }
            }
            k++;
        }
        return true;
    }

    private boolean pushMappingEdges(Mapping res, int n) {
        int k = 0;
        for (Edge qEdge : res.getQueryEdges()) {
            Edge edge = res.getEdge(k);
            if (!push(qEdge, edge, n)) {
                for (int i = 0; i < k; i++) {
                    pop(res.getQueryEdge(i));
                }
                return false;
            }
            k++;
        }
        return true;
    }

    /**
     * values (?x ?y) { unnest(exp) } exp returns Mappings map push ?x and ?y
     */
    public boolean push(Map<String, Node> list, Mapping map, int n) {
        int k = 0;
        for (Node qNode : map.getQueryNodes()) {
            if (qNode != null) {
                Node tNode = list.get(qNode.getLabel());
                if (tNode != null) {
                    Node node = map.getNodeProtect(k);
                    if (!push(tNode, node, n)) {
                        popPreviousNodes(list, map, k);
                        return false;
                    }
                }
            }
            k++;
        }
        return true;
    }

    private void popPreviousNodes(Map<String, Node> list, Mapping map, int limit) {
        for (int i = 0; i < limit; i++) {
            Node qq = map.getQueryNode(i);
            if (qq != null) {
                Node tt = list.get(qq.getLabel());
                if (tt != null) {
                    pop(tt);
                }
            }
        }
    }

    public void pop(Map<String, Node> list, Mapping map) {
        for (Node qNode : map.getQueryNodes()) {
            if (qNode != null) {
                Node tNode = list.get(qNode.getLabel());
                if (tNode != null) {
                    pop(tNode);
                }
            }
        }
    }

    /**
     * Pop elementary result
     */
    public void pop(Mapping res) {
        pop(res, true);
    }

    public void pop(Mapping res, boolean isEdge) {
        for (Node qNode : res.getQueryNodes()) {
            if (qNode != null && qNode.getIndex() >= 0) {
                pop(qNode);
            }
        }

        if (isEdge) {
            for (Edge qEdge : res.getQueryEdges()) {
                pop(qEdge);
            }
        }
    }


    Node getNode(int n) {
        return nodes[n];
    }

    @Override
    public Node getQueryNode(int n) {
        return qNodes[n];
    }

    @Override
    public Node[] getQueryNodes() {
        return qNodes;
    }

    @Override
    public boolean isBound(Node qNode) {
        return getNode(qNode) != null;
    }

    public Edge getEdge(Edge qEdge) {
        return result[qEdge.getEdgeIndex()];
    }

    @Override
    public Edge[] getEdges() {
        return result;
    }

    @Override
    public Node[] getNodes() {
        return nodes;
    }

    /**
     * The target Node of a query Node in the stack
     */
    @Override
    public Node getNode(Node node) {
        if (node.isConstant()) {
            return node;
        }
        int n = node.getIndex();
        if (n == -1) {
            return null;
        }
        return nodes[n];
    }

    @Override
    public Node getNode(String name) {
        int index = getIndex(name);
        if (index == ExprType.UNBOUND) {
            return null;
        }
        return getNode(index);
    }

    /**
     * Used by aggregate, stack is empty, search in query go also into subquery
     * select because outer query may reference an inner variable in an outer
     * aggregate use case: select count(?x) as ?count where { {select ?x where
     * {...}} }
     */
    @Override
    public Node getQueryNode(String name) {
        return query.getProperAndSubSelectNode(name);
    }

    /**
     * Index of a Node in the stack given its name For filter variable
     * evaluation We start at the end to get the latest bound node use case:
     * graph ?g {{select where {?g ?p ?y}}} outer ?g is the graph node inner ?g
     * is another value gNode = outer ?g
     */
    int getIndex(String name) {
        for (int i = qNodes.length - 1; i >= 0; i--) {
            Node node = qNodes[i];
            if (node != null && node.getLabel().equals(name)) {
                return i;
            }
        }
        return ExprType.UNBOUND;
    }

    @Override
    public Node getNode(Expr varExpr) {
        int index = varExpr.getIndex();
        switch (varExpr.subtype()) {
            // ldscript variable
            // normally we do not get here because ldscript variable is
            // instance of VariableLocal and eval() call Binding directly
            // however, it is not a bug, it is just less efficient to be here
            case ExprType.LOCAL:
                return get(varExpr);

            case ExprType.UNDEF:
                return null;
            // sparql bgp
            case ExprType.GLOBAL:
                index = getIndex(varExpr.getLabel());
                varExpr.setIndex(index);
                if (index == ExprType.UNBOUND) {
                    return null;
                }
                break;

            default:
                break;
        }
        return getNode(index);
    }

    // Filter evaluator
    public Evaluator getEvaluator() {
        return eval;
    }

    /*
     * *************************************
     *
     * Aggregates and system functions
     */
    @Override
    public int count() {
        return current().size();
    }

    /**
     * Current group is set by Mappings aggregate function
     */
    public Mappings current() {
        if (group != null) {
            return group;
        } else {
            return results;
        }
    }

    @Override
    public Mappings getMappings() {
        return current();
    }

    /**
     * Iterate Mappings for aggregate
     *
     */
    @Override
    public Iterable<Mapping> getAggregate() {
        if (current().isFake()) {
            return new ArrayList<>(0);
        }
        return current();
    }

    /**
     * Prepare Mapping for aggregate
     *
     */
    @Override
    public void aggregate(Mapping map, int n) {
        current().prepareAggregate(map, getQuery(), getMap(), n);
    }

    @Override
    public int pathLength(Node qNode) {
        Path path = getPath(qNode);
        if (path == null) {
            return 0;
        }
        return path.length();
    }

    @Override
    public Path getPath(Node qNode) {
        Node node = getNode(qNode);
        if (node == null) {
            return null;
        }
        return node.getPath();
    }


    @Override
    public Object getObject() {
        return object;
    }

    @Override
    public void setObject(Object o) {
        object = o;
    }


    @Override
    public Node get(Expr varExpr) {
        return (Node) bindingContext.get(varExpr);
    }

    public Memory setBinding(BindingContext b) {
        setBind(b);
        return this;
    }

    @Override
    public BindingContext getBind() {
        return bindingContext;
    }

    @Override
    public void setBind(BindingContext b) {
        bindingContext = b;
    }

    @Override
    public boolean hasBind() {
        return bindingContext != null && bindingContext.hasBind();
    }

    @Override
    public ApproximateSearchEnv getAppxSearchEnv() {
        return this.appxSearchEnv;
    }

    public void setAppxSearchEnv(ApproximateSearchEnv appxEnv) {
        this.appxSearchEnv = appxEnv;
    }

    /**
     * List of variable binding
     *
     */
    @Override
    public Iterable<Object> getLoop() {
        return new ArrayList<>(0);
    }

    List<List<DatatypeValue>> getList() {
        ArrayList<List<DatatypeValue>> list = new ArrayList<>();
        int i = 0;
        for (Node n : getQueryNodes()) {
            Node val = getNode(i++);
            if (n != null && val != null) {
                ArrayList<DatatypeValue> l = new ArrayList<>(2);
                l.add(n.getDatatypeValue());
                l.add(val.getDatatypeValue());
                list.add(l);
            }
        }
        return list;
    }

    /**
     * Use case:
     * let (((?var, ?val)) = ?m)
     * let ((?x, ?y) = ?m)
     */
    @Override
    public Object getValue(String varString, int n) {
        if (varString == null) {
            // let (((?var, ?val)) = ?m)  -- ?m : Mapping
            // compiled as: let (?vv = xt:get(?m, 0), (?var, ?val) = ?vv)
            // xt:get(?m, 0) evaluated as xt:gget(?m, null, 0)
            // hence var == null
            return getBinding(n);
        }
        // let ((?x, ?y) = ?m) -- ?m : Mapping
        Node node = getNode(varString);
        if (node == null) {
            return null;
        }
        return node.getDatatypeValue();
    }

    @SuppressWarnings("java:S1168") // Null signals an unbound index in internal evaluation
    List<DatatypeValue> getBinding(int n) {
        List<List<DatatypeValue>> l = getList();
        if (n < l.size()) {
            return l.get(n);
        }
        return null;
    }

    public DatatypeValue getReport() {
        return detail;
    }

    @Override
    public void setReport(DatatypeValue detail) {
        this.detail = detail;
    }

}
