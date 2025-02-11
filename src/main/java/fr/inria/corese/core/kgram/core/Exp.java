package fr.inria.corese.core.kgram.core;

import fr.inria.corese.core.kgram.api.core.*;
import fr.inria.corese.core.kgram.api.query.Producer;
import fr.inria.corese.core.sparql.triple.parser.Expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static fr.inria.corese.core.kgram.api.core.PointerType.STATEMENT;

/**
 * KGRAM/SPARQL expressions: bgp, union, optional, etc.
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public class Exp extends PointerObject
        implements ExpType, ExpPattern, Iterable<Exp> {


    public static final int ANY = -1;
    public static final int SUBJECT = 0;
    public static final int OBJECT = 1;
    public static final int PREDICATE = 2;
    public static final int GRAPH_NAME = 3;

    static final String NL = System.getProperty("line.separator");
    static final String SP = " ";
    // group edge even if there is a disconnected filter
    public static boolean groupEdge = true;
    static Exp empty = new Exp(Title.EMPTY);
    Title type;
    int index = -1;
    // optional success
    boolean // default status must be false (for order by desc())
            status = false;
    boolean skip;
    boolean isFail = false;
    boolean isPath = false;
    boolean isFree = false;
    boolean isAggregate = false;
    boolean isBGP = false;
    boolean isSilent = false;
    VExp args;
    Edge edge;
    Node node;
    List<Node> lNodes;
    // Filter api refer to sparql.triple.parser.Expression
    Filter filter;
    List<Filter> lFilter;
    // min(?l, expGroupBy(?x, ?y))
    List<Exp> expGroupBy;
    // for UNION
    Stack stack;
    // for EXTERN
    Object object;
    Producer producer;
    Regex regex;
    Exp next;
    Mappings map;
    Mappings templateMap;
    HashMap<Node, Mappings> cache;
    int min = -1;
    int max = -1;
    private boolean isPostpone = false;
    private boolean BGPAble = false;
    private boolean isFunctional = false;
    private boolean generated = false;
    private Node arg;
    //  service with several URI:
    private List<Node> nodeSet;
    private List<Node> simpleNodeList;
    private Exp postpone;
    private List<Exp> inscopeFilter;
    private Exp path;
    private Exp bind;
    private Exp values;
    private Query externQuery;
    private int level = -1;
    // service number
    private int number = -1;
    private int num = -1;
    private boolean isSystem = false;
    private boolean mappings = false;


    Exp(Title t) {
        type = t;
        args = new VExp();
        lFilter = new ArrayList<>();
    }

    public static Exp create(Title t) {
        if (t == Title.PATH || t == Title.EDGE) {
            return new ExpEdge(t);
        }
        return new Exp(t);
    }

    public static Exp create(Title t, Exp e1, Exp e2) {
        Exp e = create(t);
        e.add(e1);
        e.add(e2);
        return e;
    }

    public static Exp create(Title t, Exp e1, Exp e2, Exp e3) {
        Exp e = create(t);
        e.add(e1);
        e.add(e2);
        e.add(e3);
        return e;
    }

    public static Exp create(Title t, Exp e1) {
        Exp e = create(t);
        e.add(e1);
        return e;
    }

    public static Exp create(Title t, Node n) {
        Exp exp = create(t);
        exp.setNode(n);
        return exp;
    }

    public static Exp create(Title t, Edge e) {
        Exp exp = create(t);
        exp.setEdge(e);
        return exp;
    }

    public static Exp create(Title t, Filter e) {
        Exp exp = create(t);
        exp.setFilter(e);
        return exp;
    }

    public static Exp createValues(List<Node> list, Mappings map) {
        Exp exp = create(Title.VALUES);
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

    public Exp getPath() {
        return path;
    }

    public boolean hasPath() {
        return path != null;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean b) {
        isSystem = b;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isBGP() {
        return type == Title.BGP;
    }

    public boolean isBGPAnd() {
        return type == Title.AND || type == Title.BGP;
    }

    public boolean isBGPAble() {
        return BGPAble;
    }

    public void setBGPAble(boolean BGPAble) {
        this.BGPAble = BGPAble;
    }

    public Node getCacheNode() {
        return arg;
    }

    public void setCacheNode(Node arg) {
        this.arg = arg;
    }

    public Exp getValues() {
        return values;
    }

    public boolean isPostpone() {
        return isPostpone;
    }

    public List<Node> getNodeSet() {
        return nodeSet;
    }

    public void setNodeSet(List<Node> nodeSet) {
        this.nodeSet = nodeSet;
    }

    // draft for BGP
    public Exp duplicate() {
        Exp exp = Exp.create(type());
        for (Exp e : this) {
            exp.add(e);
        }
        return exp;
    }

    public boolean hasArg() {
        return !args.isEmpty();
    }

    @Override
    public int size() {
        return args.size();
    }

    public void add(Exp e) {
        args.add(e);
    }

    public void add(Edge e) {
        args.add(create(Title.EDGE, e));
    }

    public void add(Node n) {
        args.add(create(Title.NODE, n));
    }

    public void add(Filter f) {
        args.add(create(Title.FILTER, f));
    }

    public void set(int n, Exp e) {
        args.set(n, e);
    }

    @Override
    public Query getQuery() {
        return null;
    }

    public void insert(Exp e) {
        if (type() == Title.AND && e.type() == Title.AND) {
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

        if (type() == Title.VALUES) {
            sb.append(getNodeList());
            sb.append(SP);
        }

        sb.append("{");

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

        if (type() == Title.VALUES) {
            nl(sb, 0);
            sb.append(getMappings().toString(true));
        } else if (type() == Title.WATCH || type() == Title.CONTINUE || type() == Title.BACKJUMP) {
            // skip because loop
        } else {
            if (isOptional() && isPostpone()) {
                sb.append("POSTPONE ");
                getPostpone().toString(sb);
                nl(sb, n);
            }
            int i = 0;
            for (Exp e : this) {
                nl(sb, n);
                e.toString(sb, n + 1).append(SP);
                i++;
            }
        }

        if (type() == Title.VALUES) {
            indent(sb, n);
        }
        sb.append("}");
        return sb;
    }

    StringBuilder nl(StringBuilder sb, int n) {
        sb.append(NL);
        indent(sb, n);
        return sb;
    }

    void indent(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) {
            sb.append(SP).append(SP);
        }
    }

    String title() {
        return this.type.getTitle();
    }

    public void skip(boolean b) {
        skip = b;
    }

    public boolean skip() {
        return skip;
    }

    public void status(boolean b) {
        status = b;
    }

    public boolean status() {
        return status;
    }

    public Title type() {
        return type;
    }

    public boolean isStatement() {
        switch (type()) {
            case BGP:
            case JOIN:
            case UNION:
            case OPTIONAL:
            case MINUS:
            case GRAPH:
            case QUERY:
                return true;

            default:
                return false;
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int n) {
        index = n;
    }

    public boolean isBGPFilter() {
        return isBGP() && isOnly(Title.FILTER);
    }

    boolean isOnly(Title type) {
        for (Exp exp : this) {
            if (exp.type() != type) {
                return false;
            }
        }
        return true;
    }

    public boolean isFilter() {
        return type == Title.FILTER;
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
        return type == Title.NODE;
    }

    public boolean isEdge() {
        return type == Title.EDGE;
    }

    public boolean isEdgePath() {
        return isEdge() || isPath();
    }

    public boolean isOption() {
        return type == Title.OPTION;
    }

    public boolean isOptional() {
        return type == Title.OPTIONAL;
    }

    public boolean isJoin() {
        return type == Title.JOIN;
    }

    public boolean isAndJoin() {
        return isJoin() || (isBGPAnd() && size() == 1 && get(0).isJoin());
    }

    public boolean isAnd() {
        return type == Title.AND;
    }

    public boolean isBinary() {
        return isUnion() || isOptional() || isMinus() || isJoin();
    }

    public boolean isGraph() {
        return type == Title.GRAPH;
    }

    public boolean isUnion() {
        return type == Title.UNION;
    }

    public boolean isMinus() {
        return type == Title.MINUS;
    }

    public boolean isQuery() {
        return type == Title.QUERY;
    }

    public boolean isService() {
        return type == Title.SERVICE;
    }

    public boolean isAtomic() {
        return type == Title.FILTER || type == Title.EDGE || type == Title.NODE
                || type == Title.ACCEPT;
    }

    public void setType(Title n) {
        type = n;
    }

    Exp getNext() {
        return next;
    }

    void setNext(Exp e) {
        next = e;
    }

    public List<Exp> getExpList() {
        return args;
    }

    void getEdgeList(List<Edge> list) {
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
            return args.get(0);
        } else {
            return empty;
        }
    }

    public Exp rest() {
        if (args.size() > 1) {
            return args.get(1);
        } else {
            return null;
        }
    }

    public Exp last() {
        if (!args.isEmpty()) {
            return args.get(args.size() - 1);
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

    @Override
    public Edge getEdge() {
        return edge;
    }

    public void setEdge(Edge e) {
        edge = e;
    }

    public Regex getRegex() {
        return regex;
    }

    public void setRegex(Regex f) {
        regex = f;
    }

    @Override
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

    public Expression getFilterExpression() {
        return getFilter().getFilterExpression();
    }

    public void addFilter(Filter f) {
        lFilter.add(f);
    }

    public List<Filter> getFilters() {
        return lFilter;
    }

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

    public void setExpGroupBy(List<Exp> l) {
        expGroupBy = l;
    }

    public boolean isFail() {
        return isFail;
    }

    public void setFail(boolean b) {
        isFail = b;
    }

    public boolean isPath() {
        return type == Title.PATH;
    }

    public void setPath(Exp path) {
        this.path = path;
    }

    public void setPath(boolean b) {
        isPath = b;
    }

    public boolean isValues() {
        return type == Title.VALUES;
    }

    public void setValues(Exp values) {
        this.values = values;
    }

    public boolean isBind() {
        return type == Title.BIND;
    }

    public void setBind(Exp bind) {
        this.bind = bind;
    }

    public Node getGraphName() {
        return first().first().getNode();
    }

    public Node getServiceNode() {
        return first().getNode();
    }

    public Node getGraphNode() {
        return node;
    }

    @Override
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
        return object;
    }

    public void setObject(Object o) {
        object = o;
    }

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer p) {
        producer = p;
    }

    public List<Object> getObjectValues() {
        if (object instanceof List) {
            return (List<Object>) object;
        } else {
            return new ArrayList<>();
        }
    }

    public int getMin() {
        return min;
    }

    public void setMin(int i) {
        min = i;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int i) {
        max = i;
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
        Title title = type();
        if (title == Title.AND || title == Title.BGP) {
            for (int i = 0; i < size(); i++) {
                Exp exp = get(i);
                switch (exp.type()) {
                    case EDGE:
                    case PATH:
                    case XPATH:
                    case EVAL:
                        if (exp.contains(qNode)) {
                            add(i + 1, Exp.create(Title.ACCEPT, qNode));
                            return true;
                        }
                        break;

                    case AND:
                        if (exp.distinct(qNode)) {
                            return true;
                        }
                        break;
                }
            }
        }

        return false;
    }

    boolean isSortable() {
        return isEdge() || isPath() || isGraph() || type == Title.OPT_BIND;
    }

    boolean isSimple() {
        switch (type) {
            case EDGE:
            case PATH:
            case EVAL:
            case OPT_BIND:
                return true;
            default:
                return false;
        }
    }

    /**
     * Does edge e have a node bound by map (bindings)
     */
    boolean bind(Mapping map) {
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

    /**
     * check special case: e1: ?x path ?y e2: graph path {} e2 cannot be moved
     * before e1
     */
    boolean isGraphPath(Exp e1, Exp e2) {
        if (e1.isPath() && e2.isGraph()) {
            Node var1 = e1.getEdge().getEdgeVariable();
            Node var2 = e2.getGraphName();
            return var1 != null && var2.isVariable() && var1.same(var2);
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
     * use case: Sorter
     */
    void bind(List<Node> lNode) {
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

    void addBind(List<String> lVar) {
        Title title = type();
        if (title == Title.EDGE || title == Title.PATH) {
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
            case PATH:
            case EVAL:
                if (edge.getEdgeVariable() == null) {
                    return edge.nbNode();
                } else {
                    return edge.nbNode() + 1;
                }

            case OPT_BIND:
                return size();
        }

        return 0;
    }

    /**
     * for EDGE exp nodes + edgeNode
     */
    Node getNode(int n) {
        switch (type) {

            case EDGE:
            case PATH:
            case EVAL:

                if (n < edge.nbNode()) {
                    return edge.getNode(n);
                } else {
                    return edge.getEdgeVariable();
                }

            case OPT_BIND:
                return get(n).getNode();
        }
        return null;
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
        switch (type()) {

            case FILTER:
            case OPT_BIND:
                break;

            case OPTION:
                break;

            case OPTIONAL:
            case MINUS:
                first().share(filterVar, expVar);
                break;

            case UNION:
                // must be bound in both branches
                ArrayList<String> lVar1 = new ArrayList<>();
                ArrayList<String> lVar2 = new ArrayList<>();
                first().share(filterVar, lVar1);
                rest().share(filterVar, lVar2);
                for (String varString : lVar1) {
                    if (lVar2.contains(varString) && !expVar.contains(varString)) {
                        expVar.add(varString);
                    }
                }

                break;

            case QUERY:
                ArrayList<String> lVar = new ArrayList<>();
                getQuery().getBody().share(filterVar, lVar);

                for (Exp exp : getQuery().getSelectFun()) {
                    String name = exp.getNode().getLabel();
                    if ((lVar.contains(name) || exp.getFilter() != null) && !expVar.contains(name)) {
                        expVar.add(name);
                    }
                }
                break;

            case BIND:
                // bind may not bind the variable (in case of error)
                // hence variable cannot be considered as bound for filter
                break;

            case EDGE:
            case PATH:
                for (int i = 0; i < nbNode(); i++) {
                    Node nodePath = getNode(i);
                    share(nodePath, filterVar, expVar);
                }
                break;

            case NODE:
                share(getNode(), filterVar, expVar);
                break;

            default:
                for (Exp exp : this) {
                    exp.share(filterVar, expVar);
                }

        }

    }

    void share(Node node, List<String> fVar, List<String> eVar) {
        if (node != null && node.isVariable()
                && fVar.contains(node.getLabel())
                && !eVar.contains(node.getLabel())) {
            eVar.add(node.getLabel());
        }
    }

    public boolean bound(List<String> fvec, List<String> evec) {
        for (String varString : fvec) {
            if (!evec.contains(varString)) {
                return false;
            }
        }
        return true;
    }

    public boolean bind(Filter f) {
        List<String> lVar = f.getVariables();
        List<String> lVarExp = new ArrayList<>();
        share(lVar, lVarExp);
        return bound(lVar, lVarExp);
    }

    /**
     * Return variable nodes of this exp use case: find the variables for select
     * PRAGMA: subquery : return only the nodes of the select return only
     * variables (no cst, no blanks) minus: return only nodes of first argument
     * inSubScope = true : collect nodes of left of optional and surely bound by union
     * optional = true :  we are inside an optional
     */
    void getNodes(ExpHandler h) {

        switch (type()) {

            case FILTER:
                // get exists {} nodes
                if (h.isExist()) {
                    getExistNodes(getFilter().getExp(), h, h.getExistNodeList());
                }
                break;

            case NODE:
                //use case: join() check connection, need all variables
            case ACCEPT:
                h.add(getNode());
                break;

            case EDGE:
            case PATH:
                Edge pathEdge = getEdge();
                h.add(pathEdge.getNode(0));
                if (pathEdge.getEdgeVariable() != null) {
                    h.add(pathEdge.getEdgeVariable());
                }
                h.add(pathEdge.getNode(1));

                for (int i = 2; i < pathEdge.nbNode(); i++) {
                    h.add(pathEdge.getNode(i));
                }
                break;

            case XPATH:
            case EVAL:
                for (int i = 0; i < nbNode(); i++) {
                    Node nodeEval = getNode(i);
                    h.add(nodeEval);
                }
                break;


            case VALUES:
                for (Node varNode : getNodeList()) {
                    h.add(varNode);
                }
                break;

            case MINUS:
                // second argument does not bind anything: skip it
                if (first() != null) {
                    first().getNodes(h);
                }
                break;

            case OPTIONAL:
                boolean b = h.isOptional();
                first().getNodes(h.setOptional(true));
                if (!h.isInSubScope()) {
                    rest().getNodes(h);
                }
                h.setOptional(b);
                break;

            case GRAPH:
                h.add(getGraphName());
                if (size() > 1) {
                    rest().getNodes(h);
                }
                break;

            case UNION:
                if (h.isInSubScope()) {
                    // in-subscope record nodes that are bound in both branches of union
                    List<Node> left = first().getTheNodes(h.copy());
                    List<Node> right = rest().getTheNodes(h.copy());
                    for (Node nodeLeft : left) {
                        if (right.contains(nodeLeft)) {
                            h.add(nodeLeft);
                        }
                    }
                } else {
                    for (Exp ee : this) {
                        ee.getNodes(h);
                    }
                }
                break;

            case BIND:
                if (h.isBind()) {
                    if (getNodeList() == null) {
                        h.add(getNode());
                    } else {
                        for (Node nodeBind : getNodeList()) {
                            h.add(nodeBind);
                        }
                    }
                }

                break;

            case QUERY:
                queryNodeList(h);
                break;

            default:
                // BGP, service, union, named graph pattern
                for (Exp ee : this) {
                    ee.getNodes(h);
                    if (h.isInSubScopeSample() && ee.isSkipStatement()) {
                        // skip statements after optional/minus/union/.. for in-subscope nodes
                        break;
                    }
                }
        }

    }

    boolean isSkipStatement() {
        switch (type()) {
            case JOIN:
            case UNION:
            case OPTIONAL:
            case MINUS:
            case GRAPH:
            case QUERY:
            case SERVICE:
                return true;

            default:
                return false;
        }
    }

    /**
     * complete handler selectList with this query selectList
     */
    void queryNodeList(ExpHandler h) {
        List<Node> selectList = h.getSelectNodeList();
        List<Node> subSelectList = getQuery().getSelect();

        if (h.isInSubScope()) {
            // focus on left optional etc. in query body
            // because select * includes right optional etc.
            List<Node> scopeList = getQuery().getBody().getTheNodes(h.copy());

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

    /**
     * For modularity reasons, Pattern is stored as ExpPattern interface
     */
    public Exp getPattern(Expr exp) {
        return (Exp) exp.getPattern();
    }

    /**
     * This is a filter get exists{} nodes if any
     */
    void getExistNodes(Expr exp, ExpHandler h, List<Node> lExistNode) {
        if (exp.oper() == ExprType.EXIST) {
            Exp pat = getPattern(exp);
            // @todo: subscope = false|true ?
            List<Node> lNode = pat.getTheNodes(h.copy());
            for (Node exprNode : lNode) {
                add(lExistNode, exprNode);
            }
        } else {
            for (Expr ee : exp.getExpList()) {
                getExistNodes(ee, h, lExistNode);
            }
        }
    }

    /**
     * Compute inscope variables for variable binding for MappingSet
     */

    public List<Node> getRecordInScopeNodes(boolean bind) {
        if (getInScopeNodeList() == null) {
            setInScopeNodeList(getInScopeNodes(bind));
        }
        return getInScopeNodeList();
    }

    public List<Node> getRecordInScopeNodes() {
        return getRecordInScopeNodes(true);
    }

    public List<Node> getRecordInScopeNodesWithoutBind() {
        return getRecordInScopeNodes(false);
    }

    public List<Node> getRecordInScopeNodesForService() {
        return getRecordInScopeNodesWithoutBind();
    }

    /**
     * in subscope + bind
     * setAll(true) ::= for all exp in this exp
     */
    public List<Node> getAllInScopeNodes() {
        return getTheNodes(handler(true, true).all());
    }

    /**
     * in scope + bind
     */
    public List<Node> getNodes() {
        return getTheNodes(handler(false, true).sample());
    }

    /**
     * in subscope + bind
     */
    public List<Node> getInScopeNodes() {
        return getTheNodes(handler(true, true).sample());
    }

    // in subscope with/without bind
    List<Node> getInScopeNodes(boolean bind) {
        return getTheNodes(handler(true, bind).sample());
    }

    public List<Node> getAllNodes() {
        return getTheNodes(handler(false, true).setBlank(true).sample());
    }

    public List<Node> getTheNodes(ExpHandler h) {
        getNodes(h);
        return h.getNodes();
    }

    ExpHandler handler(boolean inSubScope, boolean bind) {
        return new ExpHandler(inSubScope, bind);
    }

    void add(List<Node> lNode, Node node) {
        add(lNode, node, false);
    }

    void add(List<Node> lNode, Node node, boolean blank) {
        if (node != null
                && (blank || (node.isVariable() && !node.isBlank()))
                && !lNode.contains(node)) {
            lNode.add(node);
        }
    }

    boolean contain(List<Exp> lExp, Node node) {
        for (Exp exp : lExp) {
            if (exp.getNode().equals(node)) {
                return true;
            }
        }
        return false;
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
        if (type == Title.FILTER) {
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

    boolean isBindCst() {
        return type() == Title.OPT_BIND && size() == 1;
    }

    boolean isBindVar() {
        return type() == Title.OPT_BIND && size() == 2;
    }

    /**
     * Add BIND ?x = ?y
     */
    List<Exp> varBind() {
        List<Exp> lBind = new ArrayList<>();
        for (int i = 0; i < size(); i++) {
            Exp f = get(i);
            if ((f.type() == Title.VALUES) || (f.isFilter() && f.size() > 0)) {
                Exp bindExp = f.first();
                if ((bindExp.type() == Title.OPT_BIND) && (bindExp.isBindCst())) {
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
    void setBind() {
        for (int i = 1; i < size(); i++) {
            Exp f = get(i);
            if (f.isFilter() && f.size() > 0) {
                Exp bindExp = f.first();
                if (bindExp.type() == Title.OPT_BIND
                        // no bind (?x = ?y) in case of JOIN
                        && (!Query.testJoin || bindExp.isBindCst())) {
                    int j = i - 1;
                    while (j > 0 && get(j).isFilter()) {
                        j--;
                    }
                    if (j >= 0) {
                        Exp g = get(j);
                        if ((g.isEdge() || g.isPath())
                                && (!bindExp.isBindCst() || g.bind(bindExp.first().getNode()))) {
                            bindExp.status(true);
                            g.setBind(bindExp);
                        }
                    }
                }
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

    boolean match(Node node, Filter f) {
        if (!node.isVariable() || f.getExp().isRecExist()) {
            return false;
        }
        List<String> lVar = f.getVariables();
        if (lVar.size() != 1) {
            return false;
        }
        return lVar.get(0).equals(node.getLabel());
    }

    /**
     * this is FILTER with TEST ?x < ?y
     */
    public int oper() {
        return getFilter().getExp().oper();
    }

    boolean check(Exp filter, int index) {
        int oper = filter.oper();
        if (oper == ExprType.LT || oper == ExprType.LE) {
            return index == 0;
        } else if (oper == ExprType.GT || oper == ExprType.GE) {
            return index == 1;
        }
        return false;
    }

    boolean order(Exp filter, int index) {
        int oper = filter.oper();
        if (oper == ExprType.LT || oper == ExprType.LE) {
            return index == 0;
        } else if (oper == ExprType.GT || oper == ExprType.GE) {
            return index == 1;
        }
        return true;
    }

    /**
     * index of Node in Edge
     */
    public int indexNode(Node node) {
        if (!isEdge()) {
            return -1;
        }
        for (int i = 0; i < nbNode(); i++) {
            if (node.same(getNode(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * index of node in FILTER ?x < ?y
     */
    public int indexVar(Node node) {
        Expr ee = getFilter().getExp();
        String name = node.getLabel();
        for (int i = 0; i < 2; i++) {
            if (ee.getExp(i).type() == ExprType.VARIABLE
                    && ee.getExp(i).getLabel().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    boolean isBound(List<String> lvar, List<Node> lnode) {
        for (String varString : lvar) {
            if (!isBound(varString, lnode)) {
                return false;
            }
        }
        return true;
    }

    boolean isBound(String variableString, List<Node> lnode) {
        for (Node boundNode : lnode) {
            if (boundNode.isVariable() && boundNode.getLabel().equals(variableString)) {
                return true;
            }
        }
        return false;
    }

    private boolean intersect(List<Node> nodes, List<Node> list) {
        for (Node intersectNode : nodes) {
            if (list.contains(intersectNode)) {
                return true;
            }
        }
        return false;
    }

    public void cache(Node n) {
        setCacheNode(n);
        cache = new HashMap<>();
    }

    boolean hasCache() {
        return cache != null;
    }

    void cache(Node n, Mappings m) {
        cache.put(n, m);
    }

    Mappings getMappings(Node n) {
        return cache.get(n);
    }

    /**
     * {?x ex:p ?y} optional {?y ex:q ?z  filter(?z != ?x) optional {?z ?p ?x}}
     * filter ?x is not bound in optional ...
     * this postponed filter must be processed at the end of optional after join occurred
     */
    public void optional() {
        Exp p = Exp.create(Title.BGP);
        Exp rest = rest();
        for (Exp exp : rest) {
            if (exp.isFilter() && !rest.simpleBind(exp.getFilter())) {
                p.add(exp);
                exp.setPostpone(true);
            } else if (exp.isOptional() || exp.isMinus()) {
                Exp first = exp.first();
                for (Exp e : first) {
                    if (e.isFilter() && !first.simpleBind(e.getFilter())) {
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
     * BGP1 optional { filter(exp) BGP2 }
     * var(exp) memberOf inscope(BGP1, BGP2)
     * TODO:
     * for safety we skip bind because bind may fail
     * and variable may not be bound whereas we need them to be bound
     * to test in-scope filter
     * FIX: we could check at runtime whether variables are bound in Mapping
     * in BGP1 before testing filter. see EvalOptional
     */
    void inscopeFilter() {
        List<Node> l1 = first().getRecordInScopeNodesWithoutBind();
        List<Node> l2 = rest().getRecordInScopeNodesWithoutBind();

        for (Exp exp : rest()) {
            if (exp.isFilter() && !exp.getFilter().isRecExist()) {
                List<String> lvar = exp.getFilter().getVariables();
                if (bind(l1, lvar) && bind(l2, lvar)) {
                    getCreateInscopeFilter().add(exp);
                }
            }
        }
    }

    boolean bind(List<Node> lnode, List<String> lvar) {
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

    /**
     * Filter variables of f are bound by triple, path, values or bind, locally in this Exp
     */
    boolean simpleBind(Filter f) {
        List<String> varList = f.getVariables();
        List<String> nodeList = getNodeVariables();

        for (String variable : varList) {
            if (!nodeList.contains(variable)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Node variables of edge, path, bind, values
     */
    List<String> getNodeVariables() {
        List<String> list = new ArrayList<>();
        for (Exp exp : this) {
            switch (exp.type()) {
                case EDGE:
                case PATH:
                    exp.getEdgeVariables(list);
                    break;
                case BIND:
                    exp.getBindVariables(list);
                    break;
                case VALUES:
                    exp.getValuesVariables(list);
                    break;
            }
        }
        return list;
    }

    void getBindVariables(List<String> list) {
        if (getNodeList() == null) {
            addVariable(list, getNode());
        } else {
            getValuesVariables(list);
        }
    }

    void getValuesVariables(List<String> list) {
        for (Node valuesNode : getNodeList()) {
            addVariable(list, valuesNode);
        }
    }

    void getEdgeVariables(List<String> list) {
        addVariable(list, getEdge().getNode(0));
        addVariable(list, getEdge().getNode(1));
        if (!isPath()) {
            addVariable(list, getEdge().getProperty());
        }
    }

    void addVariable(List<String> list, Node node) {
        if (node.isVariable() && !list.contains(node.getLabel())) {
            list.add(node.getLabel());
        }
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

    @Override
    public PointerType pointerType() {
        return STATEMENT;
    }

    @Override
    public String getDatatypeLabel() {
        return "[Statement]";
    }

    @Override
    public Exp getStatement() {
        return this;
    }

    @Override
    public Object getValue(String variable, int n) {
        if (n < size()) {
            return get(n);
        }
        return null;
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

    boolean isEvaluableWithMappings() {
        return isAndJoinRec() || isRecFederate();
    }

    /**
     * if exp = and(join(and(edge) service()))
     * then pass Mappings map as parameter
     */
    boolean isAndJoinRec() {
        if (isAnd()) {
            if (size() != 1) {
                return false;
            }
            return get(0).isAndJoinRec();
        } else if (isJoin()) {
            Exp fst = get(0);
            return fst.isAnd() && fst.size() > 0 && fst.get(0).isEdgePath();
        }
        return false;
    }

    /**
     * exp is rest of minus, optional: exp is AND
     * exp is rest of join: AND is not mandatory, it may be a service
     * if exp is, or starts with, a service,
     * pass Mappings map as parameter
     */
    boolean isRecFederate() {
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

    boolean isFirstWith(Title type) {
        return type() == type || (isBGPAnd() && size() > 0 && get(0).type() == type);
    }

    boolean isGraphFirstWith(Title type) {
        return type() == Title.GRAPH && size() > 1 && get(1).isFirstWith(type);
    }

    boolean isJoinFirstWith(Title type) {
        return isFirstWith(type) || isGraphFirstWith(type);
    }

    Exp complete(Mappings map) {
        if (map == null || !map.isNodeList()) {
            return this;
        }
        Exp res = duplicate();
        res.getExpList().add(0, getValues(map));
        return res;
    }

    Exp getValues(Mappings map) {
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

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    public List<Node> getInScopeNodeList() {
        return simpleNodeList;
    }

    public void setInScopeNodeList(List<Node> simpleNodeList) {
        this.simpleNodeList = simpleNodeList;
    }

    public Query getExternQuery() {
        return externQuery;
    }

    public void setExternQuery(Query externQuery) {
        this.externQuery = externQuery;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    /**
     * isAlgebra() only, not used
     * This is a BGP
     * if it contains several statements (union, minus, optional, graph, query, bgp), JOIN them
     * if it contains statement and basic (eg triple/path/filter/values/bind)
     * crate BGP for basics and JOIN them
     * otherwise leave as is
     * called by compiler transformer when algebra = true (default is false)
     */
    public void dispatch() {
        if (size() == 0) {
            return;
        }
        int nb = 0;
        int ns = 0;
        for (Exp exp : this) {
            if (exp.isStatement()) {
                ns++;
            } else {
                nb++;
            }
        }
        if (ns == 0 || (ns == 1 && nb == 0)) {
            return;
        }
        doDispatch();
    }




    /*
     *
     * Alternative interpreter not used
     *
     */

    void doDispatch() {
        Exp join = Exp.create(Title.JOIN);
        Exp basic = Exp.create(Title.BGP);
        for (Exp exp : this) {
            if (exp.isStatement()) {
                if (basic.size() > 0) {
                    join.add(basic);
                    basic = Exp.create(Title.BGP);
                }
                join.add(exp);
            } else {
                basic.add(exp);
            }
        }
        if (basic.size() > 0) {
            join.add(basic);
        }
        Exp body = join.dispatch(0);
        getExpList().clear();
        add(body);
    }

    /**
     * create binary JOIN from nary
     */
    Exp dispatch(int i) {
        if (i == size() - 1) {
            return last();
        } else {
            return Exp.create(Title.JOIN, get(i), dispatch(i + 1));
        }
    }

    /**
     * If content is disconnected, generate join(e1, e2)
     * called by QuerySorter when testJoin=true (default is false)
     * not used.
     */
    Exp join() {
        List<Node> connectedNode = null;
        Exp connectedExp = Exp.create(Title.AND);
        List<Exp> disconnectedExp = new ArrayList<>();
        boolean disconnectedFilter = false;

        for (int i = 0; i < size(); i++) {
            Exp e = get(i);

            if (e.type() == Title.FILTER) {
                Filter f = e.getFilter();
                List<String> lvar = f.getVariables();

                if (connectedNode == null || isBound(lvar, connectedNode)) {
                    // filter is first
                    // or filter is bound by current exp : add it to exp
                    connectedExp.add(e);
                } else {
                    // filter not bound by current exp
                    if (!disconnectedFilter) {
                        add(disconnectedExp, connectedExp);
                        disconnectedFilter = true;
                    }
                    add(disconnectedExp, e);
                }
                continue;
            } else {// TODO: UNION
                List<Node> nodes;
                if (type() == Title.MINUS || type() == Title.OPTIONAL) {
                    nodes = e.first().getAllNodes();
                } else {
                    nodes = e.getAllNodes();
                }

                if (disconnectedFilter) {
                    if (!groupEdge) {
                        connectedExp = Exp.create(Title.AND);
                        connectedNode = null;
                    }
                    disconnectedFilter = false;
                }

                if (connectedNode == null) {
                    connectedNode = nodes;
                } else if (intersect(nodes, connectedNode)) {
                    connectedNode.addAll(nodes);
                } else {
                    add(disconnectedExp, connectedExp);
                    connectedExp = Exp.create(Title.AND);
                    connectedNode = nodes;
                }
            }

            connectedExp.add(e);
        }

        if (connectedExp.size() > 0) {
            add(disconnectedExp, connectedExp);
        }

        if (disconnectedExp.size() <= 1) {
            return this;
        } else {
            return join(disconnectedExp);
        }
    }

    void add(List<Exp> list, Exp exp) {
        if (!list.contains(exp)) {
            list.add(exp);
        }
    }

    /**
     * JOIN the exp of the list, except filters which are in a BGP with
     * preceding exp list = e1 e2 f1 e3 return JOIN(AND(JOIN(e1, e2) f1), e3 ).
     */
    Exp join(List<Exp> list) {
        Exp exp = list.get(0);

        for (int i = 1; i < list.size(); i++) {

            Exp cur = list.get(i);

            if (cur.type() == Title.FILTER || exp.type() == Title.FILTER) {
                // and
                if (exp.type() == Title.AND) {
                    exp.add(cur);
                } else {
                    exp = Exp.create(Title.AND, exp, cur);
                }
            } else {
                // variables that may be bound from environment (e.g. values)
                exp = Exp.create(Title.JOIN, exp, cur);
                exp.bindNodes();
            }
        }

        return exp;
    }

    /**
     * Nodes that may be bound by previous clause or by environment except
     * minus, etc.
     * use case:  join
     */
    void bindNodes() {
        for (Exp exp : getExpList()) {
            exp.setNodeList(exp.getInScopeNodes());
        }
    }

    class VExp extends ArrayList<Exp> {
    }


}
