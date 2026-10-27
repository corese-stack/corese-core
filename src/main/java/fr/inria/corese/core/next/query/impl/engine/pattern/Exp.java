package fr.inria.corese.core.next.query.impl.engine.pattern;
import fr.inria.corese.core.next.query.impl.engine.eval.Stack;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.ExpPattern;
import fr.inria.corese.core.next.query.impl.engine.model.ExpType;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.path.PropertyPathEdge;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Mutable operators for compiled SPARQL graph patterns and solution modifiers.
 *
 * <p>Child operators, indexed nodes and attached expressions belong to one
 * compiler or evaluation. {@link #copyPlan(PlanCopy)} detaches mutable links
 * while preserving their identity relationships within the copied graph.
 * An executable graph must not be shared between concurrent evaluations.</p>
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public class Exp
        implements ExpType, ExpPattern, Iterable<Exp>, PlanCopyable<Exp> {

    public static final int SUBJECT = 0;
    public static final int OBJECT = 1;
    public static final int PREDICATE = 2;

    static final String NEWLINE = "\n";
    static final String SP = " ";
    private Type type;
    private int bindingIndex = -1;
    // optional success
    private boolean status = false;
    private boolean isFail = false;
    private boolean isAggregate = false;
    private boolean isSilent = false;
    private VExp args;
    private Edge edge;
    private Node node;
    private List<Node> lNodes;
    // Expression evaluation contract, independent of parser syntax types.
    private Filter filter;
    private List<Filter> lFilter;
    // min(?l, expGroupBy(?x, ?y))
    private List<Exp> expGroupBy;
    // for UNION
    private Stack stack;
    // Optimizer-specific payload, such as candidate values for an equality filter.
    private Object payload;
    private Producer producer;
    private Mappings map;
    private boolean isPostpone = false;
    private boolean isFunctional = false;
    //  service with several URI:
    private List<Node> simpleNodeList;
    private Exp postpone;
    private List<Exp> inscopeFilter;
    private Exp bind;
    private Exp values;
    // service number
    private int number = -1;
    private boolean isSystem = false;
    private boolean mappings = false;

    Exp(Type t) {
        type = t;
        args = new VExp();
        lFilter = new ArrayList<>();
    }

    /** Copies this operator and all mutable links into an independent execution graph. */
    @Override
    public Exp copyPlan(PlanCopy copies) {
        Exp target = Exp.create(type);
        copies.remember(this, target);
        copyPlanFields(target, copies);
        return target;
    }

    /** Copies the common operator fields after the target has been registered. */
    void copyPlanFields(Exp target, PlanCopy copies) {
        target.type = type;
        target.bindingIndex = bindingIndex;
        target.status = status;
        target.isFail = isFail;
        target.isAggregate = isAggregate;
        target.isSilent = isSilent;
        target.edge = copies.copy(edge);
        target.node = copies.copy(node);
        target.lNodes = copies.copy(lNodes);
        target.filter = copies.copy(filter);
        target.lFilter = copies.copy(lFilter);
        target.expGroupBy = copies.copy(expGroupBy);
        target.stack = copies.copy(stack);
        target.payload = copies.copy(payload);
        target.producer = copies.copy(producer);
        target.map = copies.copy(map);
        target.isPostpone = isPostpone;
        target.isFunctional = isFunctional;
        target.simpleNodeList = copies.copy(simpleNodeList);
        target.postpone = copies.copy(postpone);
        target.inscopeFilter = copies.copy(inscopeFilter);
        target.bind = copies.copy(bind);
        target.values = copies.copy(values);
        target.number = number;
        target.isSystem = isSystem;
        target.mappings = mappings;
        for (Exp child : args) target.args.add(copies.copy(child));
    }

    public static Exp create(Type t) {
        if (t == Type.EDGE) {
            return new ExpEdge(t);
        }
        return new Exp(t);
    }

    public static Exp create(Type t, Exp e1, Exp e2) {
        Exp e = create(t);
        e.add(e1);
        e.add(e2);
        return e;
    }

    public static Exp create(Type t, Exp e1, Exp e2, Exp e3) {
        Exp e = create(t);
        e.add(e1);
        e.add(e2);
        e.add(e3);
        return e;
    }

    public static Exp create(Type t, Exp e1) {
        Exp e = create(t);
        e.add(e1);
        return e;
    }

    public static Exp create(Type t, Node n) {
        Exp exp = create(t);
        exp.setNode(n);
        return exp;
    }

    public static Exp create(Type t, Edge e) {
        Exp exp = create(t);
        exp.setEdge(e);
        return exp;
    }

    public static Exp create(Type t, Filter e) {
        Exp exp = create(t);
        exp.setFilter(e);
        return exp;
    }

    public static Exp createValues(List<Node> list, Mappings map) {
        Exp exp = create(Type.VALUES);
        exp.setNodeList(list);
        exp.setMappings(map);
        return exp;
    }

    public Exp getBind() {
        return bind;
    }

    public boolean isFunctional() {
        return isFunctional;
    }

    public void setFunctional(boolean isFunctional) {
        this.isFunctional = isFunctional;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean b) {
        isSystem = b;
    }

    public boolean isBGP() {
        return type == Type.BGP;
    }

    public boolean isBGPAnd() {
        return type == Type.AND || type == Type.BGP;
    }

    public Exp getValues() {
        return values;
    }

    public boolean isPostpone() {
        return isPostpone;
    }

    /** Returns a shallow operator containing the same child references. */
    public Exp duplicate() {
        Exp exp = Exp.create(type());
        for (Exp e : this) {
            exp.add(e);
        }
        return exp;
    }

    public int size() {
        return args.size();
    }

    public QueryPlan getQuery() {
        return null;
    }

    public void add(Exp e) {
        args.add(e);
    }

    public void clear() {
        args.clear();
    }

    public void add(Edge e) {
        args.add(create(Type.EDGE, e));
    }

    public void add(Node n) {
        args.add(create(Type.NODE, n));
    }

    public void add(Filter f) {
        args.add(create(Type.FILTER, f));
    }

    public void set(int n, Exp e) {
        args.set(n, e);
    }

    public void insert(Exp e) {
        if (type() == Type.AND && e.type() == Type.AND) {
            for (Exp ee : e) {
                insert(ee);
            }
        } else {
            args.add(e);
        }
    }

    public void add(int n, Exp e) {
        args.add(n, e);
    }

    public boolean remove(Exp e) {
        return args.remove(e);
    }

    public Exp remove(int n) {
        return args.remove(n);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        return toString(sb, 0);
    }

    StringBuilder toString(StringBuilder sb, int n) {
        sb.append(title()).append(SP);

        if (type() == Type.VALUES) {
            sb.append(getNodeList()).append(SP);
        }

        sb.append("{");

        appendEdgeNodeFilter(sb);

        if (type() == Type.VALUES) {
            nl(sb, 0);
            sb.append(getMappings().toString(true));
            indent(sb, n);
        } else {
            appendChildren(sb, n);
        }

        sb.append("}");
        return sb;
    }

    private void appendEdgeNodeFilter(StringBuilder sb) {
        if (edge != null) {
            sb.append(edge);
            if (size() > 0) {
                sb.append(SP);
            }
            if (getBind() != null) {
                sb.append(SP).append(getBind()).append(SP);
            }
        }
        if (node != null) {
            sb.append(node).append(SP);
            if (size() > 0) {
                sb.append(SP);
            }
        }
        if (filter != null) {
            sb.append(filter);
            if (size() > 0) {
                sb.append(SP);
            }
        }
    }

    private void appendChildren(StringBuilder sb, int n) {
        if (isOptional() && isPostpone()) {
            sb.append("POSTPONE ");
            getPostpone().toString(sb);
            nl(sb, n);
        }
        for (Exp e : this) {
            nl(sb, n);
            e.toString(sb, n + 1).append(SP);
        }
    }

    void nl(StringBuilder sb, int n) {
        sb.append(NEWLINE);
        indent(sb, n);
    }

    void indent(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) {
            sb.append(SP).append(SP);
        }
    }

    String title() {
        return this.type.getTitle();
    }

    public void status(boolean b) {
        status = b;
    }

    public boolean status() {
        return status;
    }

    public Type type() {
        return type;
    }

    public boolean isStatement() {
        return switch (type()) {
            case BGP, JOIN, UNION, OPTIONAL, MINUS, GRAPH, QUERY -> true;
            default -> false;
        };
    }

    public int getIndex() {
        return bindingIndex;
    }

    public void setIndex(int n) {
        bindingIndex = n;
    }

    public boolean isBGPFilter() {
        return isBGP() && isOnly();
    }

    boolean isOnly() {
        for (Exp exp : this) {
            if (exp.type() != Type.FILTER) {
                return false;
            }
        }
        return true;
    }

    public boolean isFilter() {
        return type == Type.FILTER;
    }

    public boolean isAggregate() {
        return isAggregate;
    }

    public void setAggregate(boolean b) {
        isAggregate = b;
    }

    public boolean isSilent() {
        return isSilent;
    }

    public void setSilent(boolean b) {
        isSilent = b;
    }

    public boolean isNode() {
        return type == Type.NODE;
    }

    public boolean isEdge() {
        return type == Type.EDGE;
    }

    public boolean isEdgePath() {
        return isEdge();
    }

    public boolean isOption() {
        return type == Type.OPTION;
    }

    public boolean isOptional() {
        return type == Type.OPTIONAL;
    }

    public boolean isJoin() {
        return type == Type.JOIN;
    }

    public boolean isAnd() {
        return type == Type.AND;
    }

    public boolean isBinary() {
        return isUnion() || isOptional() || isMinus() || isJoin();
    }

    public boolean isGraph() {
        return type == Type.GRAPH;
    }

    public boolean isUnion() {
        return type == Type.UNION;
    }

    public boolean isMinus() {
        return type == Type.MINUS;
    }

    public boolean isQuery() {
        return type == Type.QUERY;
    }

    public boolean isService() {
        return type == Type.SERVICE;
    }

    public void setType(Type n) {
        type = n;
    }

    public List<Exp> getExpList() {
        return args;
    }

    public void getEdgeList(List<Edge> list) {
        for (Exp exp : getExpList()) {
            if (exp.isEdge()) {
                list.add(exp.getEdge());
            } else {
                exp.getEdgeList(list);
            }
        }
    }

    public Exp first() {
        if (!args.isEmpty()) {
            return args.getFirst();
        } else {
            return new Exp(Type.EMPTY);
        }
    }

    public Exp rest() {
        if (args.size() > 1) {
            return args.get(1);
        } else {
            return null;
        }
    }

    @Override
    public Iterator<Exp> iterator() {
        return args.iterator();
    }

    public Exp get(int n) {
        return args.get(n);
    }

    public Edge getEdge() {
        return edge;
    }

    public void setEdge(Edge e) {
        edge = e;
    }

    public Mappings getMappings() {
        return map;
    }

    public Mappings getActualMappings() {
        if (getValues() == null) {
            return null;
        }
        return getValues().getMappings();
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter f) {
        filter = f;
        if (f.isRecAggregate()) {
            setAggregate(true);
        }
    }

    public void addFilter(Filter f) {
        lFilter.add(f);
    }

    public List<Filter> getFilters() {
        return lFilter;
    }

    @SuppressWarnings("java:S1172") // Subclasses like ExpEdge override this method with specific filtering
    public List<Filter> getFilters(int n, int t) {
        return new ArrayList<>(0);
    }

    public boolean isHaving() {
        return getHavingFilter() != null;
    }

    public Filter getHavingFilter() {
        Expr e = getFilter().getExp();
        if (e.arity() >= 3) {
            return e.getExp(2).getFilter();
        }
        return null;
    }

    public boolean isExpGroupBy() {
        return expGroupBy != null;
    }

    public List<Exp> getExpGroupBy() {
        return expGroupBy;
    }

    public boolean isFail() {
        return isFail;
    }

    public void setFail(boolean b) {
        isFail = b;
    }

    public boolean isPath() {
        return isEdge() && getEdge() instanceof PropertyPathEdge;
    }

    public boolean isValues() {
        return type == Type.VALUES;
    }

    public void setValues(Exp values) {
        this.values = values;
    }

    public boolean isBind() {
        return type == Type.BIND;
    }

    public void setBind(Exp bind) {
        this.bind = bind;
    }

    public Node getGraphName() {
        return first().first().getNode();
    }

    public Node getGraphNode() {
        return node;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node n) {
        node = n;
    }

    public List<Node> getNodeList() {
        return lNodes;
    }

    public void setNodeList(List<Node> l) {
        lNodes = l;
    }

    public boolean hasNodeList() {
        return getNodeList() != null && !getNodeList().isEmpty();
    }

    public void addNode(Node n) {
        if (lNodes == null) {
            lNodes = new ArrayList<>();
        }
        lNodes.add(n);
    }

    public Object getObject() {
        return payload;
    }

    public void setObject(Object o) {
        payload = o;
    }

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer p) {
        producer = p;
    }

    public List<Object> getObjectValues() {
        if (payload instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        return new ArrayList<>();
    }


    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack st) {
        stack = st;
    }

    /**
     * use case: select distinct ?x where add an ACCEPT ?x statement to check
     * that ?x is new
     * other exp than those listed here have no distinct processor at runtime
     * in other words, this is only for main body BGP
     */
    boolean distinct(Node qNode) {
        Type title = type();
        if (title == Type.AND || title == Type.BGP) {
            for (int i = 0; i < size(); i++) {
                Exp exp = get(i);
                switch (exp.type()) {
                    case EDGE:
                        if (exp.contains(qNode)) {
                            add(i + 1, Exp.create(Type.ACCEPT, qNode));
                            return true;
                        }
                        break;

                    case AND:
                        if (exp.distinct(qNode)) {
                            return true;
                        }
                        break;

                    default:
                        break;
                }
            }
        }

        return false;
    }

    public boolean isSortable() {
        return isEdge() || isGraph() || type == Type.OPT_BIND;
    }

    boolean isSimple() {
        return switch (type) {
            case EDGE, OPT_BIND -> true;
            default -> false;
        };
    }

    /**
     * Does edge e have a node bound by map (bindings)
     */
    public boolean bind(Mapping map) {
        if (!isEdge()) {
            return false;
        }

        for (int i = 0; i < nbNode(); i++) {
            Node bindNode = getNode(i);
            if (bindNode.isVariable() && map.getNode(bindNode) != null) {
                return true;
            }
        }

        return false;
    }


    public int nBind(List<Node> lNode, List<String> lVar, List<Exp> lBind) {
        if (isSimple()) {
            return count(lNode, lVar, lBind);
        } else {
            return gCount(lNode, lVar, lBind);
        }
    }

    int gCount(List<Node> lNode, List<String> lVar, List<Exp> lBind) {
        int n = 0;
        List<Node> list = getNodes();
        for (Node listNode : list) {
            n += member(listNode, lNode, lVar, lBind);
        }
        return n;
    }

    int count(List<Node> lNode, List<String> lVar, List<Exp> lBind) {
        int n = 0;
        for (int i = 0; i < nbNode(); i++) {
            n += member(getNode(i), lNode, lVar, lBind);
        }
        return n;
    }

    int member(Node node, List<Node> lNode, List<String> lVar, List<Exp> lBind) {
        if (node.isConstant()) {
            return 1;
        }
        if (member(node, lBind)) {
            return 1;
        }
        if (lNode.contains(node) || lVar.contains(node.getLabel())) {
            return 2;
        }
        return 0;
    }

    boolean member(Node node, List<Exp> lBind) {
        for (Exp exp : lBind) {
            if (node.same(exp.first().getNode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * list of nodes that are bound by this exp no minus and no exists
     * use case: ConnectedPatternOrdering
     */
    public void bind(List<Node> lNode) {
        if (isSimple()) {
            for (int i = 0; i < nbNode(); i++) {
                Node bindNode = getNode(i);
                if (bindNode != null) {
                    bind(bindNode, lNode);
                }
            }
        } else {
            List<Node> list = getNodes();
            for (Node bindNode : list) {
                bind(bindNode, lNode);
            }
        }
    }

    void bind(Node node, List<Node> lNode) {
        if (!lNode.contains(node)) {
            lNode.add(node);
        }
    }

    public void addBind(List<String> lVar) {
        Type title = type();
        if (title == Type.EDGE) {
            for (int i = 0; i < nbNode(); i++) {
                Node bindNode = getNode(i);
                addBind(bindNode, lVar);
            }
        }
    }

    void addBind(Node node, List<String> lVar) {
        if (node.isVariable() && !lVar.contains(node.getLabel())) {
            lVar.add(node.getLabel());
        }
    }

    /**
     * for EDGE exp nodes + edgeNode
     */
    int nbNode() {
        switch (type) {
            case EDGE:
                if (edge.getEdgeVariable() == null) {
                    return edge.nbNode();
                } else {
                    return edge.nbNode() + 1;
                }

            case OPT_BIND:
                return size();

            default:
                return 0;
        }
    }

    /**
     * for EDGE exp nodes + edgeNode
     */
    Node getNode(int n) {
        switch (type) {
            case EDGE:
                if (n < edge.nbNode()) {
                    return edge.getNode(n);
                } else {
                    return edge.getEdgeVariable();
                }

            case OPT_BIND:
                return get(n).getNode();

            default:
                return null;
        }
    }

    /**
     * for EDGE exp nodes + edgeNode
     */
    public boolean contains(Node node) {
        if (getEdge().contains(node)) {
            return true;
        }
        Node pNode = getEdge().getEdgeVariable();
        if (pNode == null) {
            return false;
        }
        return pNode == node;
    }

    /**
     * @param filterVar: variables of a filter
     * @param expVar:    list of variables bound by expressions Add in expVar the
     *                   variables bound by this expression that are in filterVar bound means no
     *                   optional, no union
     */
    public void share(List<String> filterVar, List<String> expVar) {
        ExpPatternScope.share(this, filterVar, expVar);
    }

    void share(Node node, List<String> fVar, List<String> eVar) {
        ExpPatternScope.share(node, fVar, eVar);
    }

    public boolean bound(List<String> fvec, List<String> evec) {
        return ExpPatternScope.bound(fvec, evec);
    }

    /**
     * Return variable nodes of this exp use case: find the variables for select
     * PRAGMA: subquery : return only the nodes of the select return only
     * variables (no cst, no blanks) minus: return only nodes of first argument
     * inSubScope = true : collect nodes of left of optional and surely bound by union
     * optional = true :  we are inside an optional
     */
    void getNodes(ExpNodeCollector h) {
        ExpPatternScope.getNodes(this, h);
    }

    boolean isSkipStatement() {
        return switch (type()) {
            case JOIN, UNION, OPTIONAL, MINUS, GRAPH, QUERY, SERVICE -> true;
            default -> false;
        };
    }

    /**
     * For modularity reasons, Pattern is stored as ExpPattern interface
     */
    public Exp getPattern(Expr exp) {
        return exp.getPattern();
    }

    /**
     * Compute inscope variables for variable binding for MappingSet
     */
    public List<Node> getRecordInScopeNodes(boolean bind) {
        return ExpPatternScope.getRecordInScopeNodes(this, bind);
    }

    public List<Node> getRecordInScopeNodesWithoutBind() {
        return ExpPatternScope.getRecordInScopeNodesWithoutBind(this);
    }

    /**
     * in subscope + bind
     * setAll(true) ::= for all exp in this exp
     */
    public List<Node> getAllInScopeNodes() {
        return ExpPatternScope.getAllInScopeNodes(this);
    }

    /**
     * in scope + bind
     */
    public List<Node> getNodes() {
        return ExpPatternScope.getNodes(this);
    }

    /**
     * in subscope + bind
     */
    public List<Node> getInScopeNodes() {
        return ExpPatternScope.getInScopeNodes(this);
    }

    // in subscope with/without bind
    List<Node> getInScopeNodes(boolean bind) {
        return ExpPatternScope.getInScopeNodes(this, true, bind);
    }

    public List<Node> getTheNodes(ExpNodeCollector h) {
        return ExpPatternScope.getTheNodes(this, h);
    }

    void add(List<Node> lNode, Node node) {
        ExpPatternScope.add(lNode, node);
    }

    boolean doesNotContain(List<Exp> lExp, Node node) {
        for (Exp exp : lExp) {
            if (exp.getNode().equals(node)) {
                return false;
            }
        }
        return true;
    }

    /**
     * compute the variable list use case: filter(exists {?x ?p ?y}) no minus
     */
    @Override
    public void getVariables(List<String> list) {
        getVariables(list, false);
    }

    @Override
    public void getVariables(List<String> list, boolean excludeLocal) {
        List<Node> lNode = getNodes();
        for (Node varNode : lNode) {
            String name = varNode.getLabel();
            if (!list.contains(name)) {
                list.add(name);
            }
        }
        // go into filters if any
        getFilterVar(list, excludeLocal);
    }

    public void getFilterVar(List<String> list, boolean excludeLocal) {
        if (type == Type.FILTER) {
            List<String> lVar = getFilter().getVariables(excludeLocal);
            for (String varString : lVar) {
                if (!list.contains(varString)) {
                    list.add(varString);
                }
            }
        } else {
            for (Exp exp : getExpList()) {
                exp.getFilterVar(list, excludeLocal);
            }
        }
    }

    public boolean isBindCst() {
        return type() == Type.OPT_BIND && size() == 1;
    }

    /**
     * Add BIND ?x = ?y
     */
    public List<Exp> varBind() {
        List<Exp> lBind = new ArrayList<>();
        for (Exp f : this) {
            if ((f.type() == Type.VALUES) || (f.isFilter() && f.size() > 0)) {
                Exp bindExp = f.first();
                if ((bindExp.type() == Type.OPT_BIND) && (bindExp.isBindCst())) {
                    // ?x = cst
                    lBind.add(bindExp);
                }
            }
        }
        return lBind;
    }

    /**
     * If a filter carry a bind, set the bind into its edge or path
     */
    public void setBind() {
        for (int i = 1; i < size(); i++) {
            Exp f = get(i);
            if (f.isFilter() && f.size() > 0) {
                Exp bindExp = f.first();
                if (bindExp.type() == Type.OPT_BIND) {
                    processBind(i, bindExp);
                }
            }
        }
    }

    private void processBind(int index, Exp bindExp) {
        int j = index - 1;
        while (j > 0 && get(j).isFilter()) {
            j--;
        }
        if (j >= 0) {
            Exp g = get(j);
            if (g.isEdge()
                    && (!bindExp.isBindCst() || g.bind(bindExp.first().getNode()))) {
                bindExp.status(true);
                g.setBind(bindExp);
            }
        }
    }

    /**
     * Edge bind node
     */
    boolean bind(Node node) {
        for (int i = 0; i < nbNode(); i++) {
            if (getNode(i).equals(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {?x ex:p ?y} optional {?y ex:q ?z  filter(?z != ?x) optional {?z ?p ?x}}
     * filter ?x is not bound in optional ...
     * this postponed filter must be processed at the end of optional after join occurred
     */
    public void optional() {
        Exp p = Exp.create(Type.BGP);
        Exp rest = rest();
        for (Exp exp : rest) {
            if (exp.isFilter() && rest.hasUnboundVariables(exp.getFilter())) {
                p.add(exp);
                exp.setPostpone(true);
            } else if (exp.isOptional() || exp.isMinus()) {
                Exp first = exp.first();
                for (Exp e : first) {
                    if (e.isFilter() && first.hasUnboundVariables(e.getFilter())) {
                        p.add(e);
                        e.setPostpone(true);
                    }
                }
            }
        }
        if (p.size() > 0) {
            setPostpone(p);
            setPostpone(true);
        }
        inscopeFilter();
    }

    /**
     * BGP1 optional ( filter(exp) BGP2 )
     * var(exp) memberOf inscope(BGP1, BGP2)
     * Note:
     * for safety we skip bind because bind may fail
     * and variable may not be bound whereas we need them to be bound
     * to test in-scope filter
     * FIX: we could check at runtime whether variables are bound in Mapping
     * in BGP1 before testing filter. see EvalOptional
     */
    void inscopeFilter() {
        ExpPatternScope.inscopeFilter(this);
    }

    boolean bind(List<Node> lnode, List<String> lvar) {
        return ExpPatternScope.bind(lnode, lvar);
    }

    /**
     * Check if filter variables are NOT bound by triple, path, values or bind, locally in this Exp
     */
    boolean hasUnboundVariables(Filter f) {
        return ExpPatternScope.hasUnboundVariables(this, f);
    }

    /**
     * Node variables of edge, path, bind, values
     */
    List<String> getNodeVariables() {
        return ExpPatternScope.getNodeVariables(this);
    }

    void getBindVariables(List<String> list) {
        ExpPatternScope.getBindVariables(this, list);
    }

    void getValuesVariables(List<String> list) {
        ExpPatternScope.getValuesVariables(this, list);
    }

    void getEdgeVariables(List<String> list) {
        ExpPatternScope.getEdgeVariables(this, list);
    }

    void addVariable(List<String> list, Node node) {
        ExpPatternScope.addVariable(list, node);
    }

    // optional postponed filters
    public Exp getPostpone() {
        return postpone;
    }

    public void setPostpone(boolean postpone) {
        this.isPostpone = postpone;
    }

    public void setPostpone(Exp postpone) {
        this.postpone = postpone;
    }


    public List<Exp> getInscopeFilter() {
        return inscopeFilter;
    }

    public void setInscopeFilter(List<Exp> inscopeFilter) {
        this.inscopeFilter = inscopeFilter;
    }

    public List<Exp> getCreateInscopeFilter() {
        if (getInscopeFilter() == null) {
            setInscopeFilter(new ArrayList<>());
        }
        return getInscopeFilter();
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * exp is rest of minus, optional: exp is AND
     * exp is rest of join: AND is not mandatory, it may be a service
     * if exp is, or starts with, a service,
     * pass Mappings map as parameter
     */
    public boolean isRecFederate() {
        if (isService()) {
            return true;
        }
        if (size() == 1) {
            return get(0).isRecService();
        } else if (isBGPAnd() && size() > 0) {
            return get(0).isRecService();
        } else {
            return false;
        }
    }

    boolean isRecService() {
        return isService() || (isBinary() && isFederate2());
    }

    // binary such as union
    boolean isFederate2() {
        return size() == 2 &&
                get(0).isRecFederate() &&
                get(1).isRecFederate();
    }

    public boolean isFirstWith() {
        return type() == Type.UNION || (isBGPAnd() && size() > 0 && get(0).type() == Type.UNION);
    }

    public Exp complete(Mappings map) {
        if (map == null || !map.isNodeList()) {
            return this;
        }
        Exp res = duplicate();
        res.getExpList().addFirst(getValues(map));
        return res;
    }

    public Exp getValues(Mappings map) {
        return createValues(map.getNodeList(), map);
    }

    public boolean isMappings() {
        return mappings;
    }

    public void setMappings(Mappings m) {
        map = m;
    }

    public void setMappings(boolean mappings) {
        this.mappings = mappings;
    }

    public List<Node> getInScopeNodeList() {
        return simpleNodeList;
    }

    public void setInScopeNodeList(List<Node> simpleNodeList) {
        this.simpleNodeList = simpleNodeList;
    }

    /**
     * Nodes that may be bound by previous clause or by environment except
     * minus, etc.
     * use case:  join
     */
    public void bindNodes() {
        for (Exp exp : getExpList()) {
            exp.setNodeList(exp.getInScopeNodes());
        }
    }

    static final class VExp extends ArrayList<Exp> {

        private static final long serialVersionUID = 1L;
    }
}
