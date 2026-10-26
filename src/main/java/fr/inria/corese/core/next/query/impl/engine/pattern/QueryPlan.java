package fr.inria.corese.core.next.query.impl.engine.pattern;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.planning.QueryPlanner;

import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.query.impl.engine.filter.FilterOptimizer;
import fr.inria.corese.core.next.query.impl.engine.eval.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compiled query operators, projection, dataset and variable indexes.
 *
 * <p>The compiler completes one mutable graph, including its subqueries, before
 * execution. {@link CompiledQueryPlan} captures a detached compilation image and
 * creates an independent graph for each evaluation. A query and its operators
 * belong to that evaluation thread; they are not shared mutable execution state.</p>
 *
 * @author Olivier Corby, Edelweiss, INRIA 2009
 *
 */
public final class QueryPlan extends Exp {


    public static final int QP_T0 = 0; //No QP settings
    public static final int QP_DEFAULT = 1; //Default Corese QP
    public static final int QP_HEURISTICS_BASED = 2;//Heuristics based QP
    public static final int QP_BGP = 3;//BGP based QP

    private static final Logger logger = LoggerFactory.getLogger(QueryPlan.class);

    int limit = Integer.MAX_VALUE;
    int offset = 0;
    // if slice > 0 : service gets mappings from previous pattern by slices
    int slice = 20;

    private int number = 0;
    private boolean distinctResults;
    private final VariableLayout layout;
    List<Node> from;
    List<Node> named;
    List<Node> selectNode;
    List<Node> relaxEdges;
    List<Exp> selectExp;
    List<Exp> orderBy;
    List<Exp> groupBy;
    List<Filter> failure;
    List<Filter> funList;

    List<String> errors;
    List<String> info;
    Exp having;
    Exp construct;
    Exp delete;
    // gNode is a local graph node when subquery has no ?g in its select
    // use case: graph ?g (select where ...)
    Node gNode;
    // Root query whose dataset is inherited by this subquery.
    private QueryPlan globalQuery;
    QueryPlan outerQuery;
    private Object queryObject;

    FilterOptimizer compiler;
    private final QueryPlanner querySorter;

    HashMap<String, Object> pragma;
    // Extended filters: pathNode()
    HashMap<String, Filter> ftable;
    // Extended queries for type check
    // nb occurrences of predicates in where
    HashMap<String, Integer> ptable;
    HashMap<String, Edge> etable;
    // query for class/property definition checking
    HashMap<Edge, QueryPlan> table;
    // Extended queries for additional group by
    List<QueryPlan> queries;
    private boolean isCompiled = false;
    private boolean datasetSpecified;
    private boolean reportEnabled;

    private boolean isUseBind = true;

    private boolean aggregateQuery;
    boolean isRelax = false;
    boolean isDistribute = false;
    boolean isOptimize = false;
    boolean isTest = false;
    boolean isNew = true;
    boolean isConstruct = false;
    private boolean isInsert = false;
    boolean isDelete = false;
    boolean isUpdate = false;
    boolean isAsk = false;
    boolean isListGroup = false;
    private boolean validate = false;
    private boolean federate = false;

    boolean
            isCorrect = true;
    boolean isMap = true;
    boolean isRule = false;
    boolean isDetail = true;

    int planner = QP_DEFAULT;

    private boolean isService = false;


    private Mapping mapping;

    private String service;

    /** Creates an uncompiled query whose mutable containers belong to its compiler. */
    public QueryPlan() {
        super(Type.QUERY);
        layout = new VariableLayout();
        from = new ArrayList<>();
        named = new ArrayList<>();
        selectExp = new ArrayList<>();
        orderBy = new ArrayList<>();
        groupBy = new ArrayList<>();
        failure = new ArrayList<>();
        funList = new ArrayList<>();
        compiler = new FilterOptimizer(this);
        table = new HashMap<>();
        ftable = new HashMap<>();
        pragma = new HashMap<>();
        ptable = new HashMap<>();
        etable = new HashMap<>();
        queries = new ArrayList<>();
        selectNode = new ArrayList<>();
        relaxEdges = new ArrayList<>();
        querySorter = new QueryPlanner(this);
    }

    /**
     * Constructs a completed execution graph without allocating compilation services.
     * Registration precedes graph links because subqueries refer to their outer query.
     */
    @SuppressWarnings("this-escape") // PlanCopy only records identity; no execution observes the shell before construction finishes.
    private QueryPlan(QueryPlan source, PlanCopy copies) {
        super(Type.QUERY);
        copies.remember(source, this);
        source.copyPlanFields(this, copies);
        layout = source.layout.copyPlan(copies);
        querySorter = null;
        compiler = null;
        this.limit = source.limit;
        this.offset = source.offset;
        this.slice = source.slice;
        this.number = source.number;
        this.distinctResults = source.distinctResults;
        this.from = copies.copy(source.from);
        this.named = copies.copy(source.named);
        this.selectNode = copies.copy(source.selectNode);
        this.relaxEdges = copies.copy(source.relaxEdges);
        this.selectExp = copies.copy(source.selectExp);
        this.orderBy = copies.copy(source.orderBy);
        this.groupBy = copies.copy(source.groupBy);
        this.failure = copies.copy(source.failure);
        this.funList = copies.copy(source.funList);
        this.errors = copies.copy(source.errors);
        this.info = copies.copy(source.info);
        this.having = copies.copy(source.having);
        this.construct = copies.copy(source.construct);
        this.delete = copies.copy(source.delete);
        this.gNode = copies.copy(source.gNode);
        this.globalQuery = copies.copy(source.globalQuery);
        this.outerQuery = copies.copy(source.outerQuery);
        this.queryObject = copies.copy(source.queryObject);
        this.pragma = copies.copy(source.pragma);
        this.ftable = copies.copy(source.ftable);
        this.ptable = copies.copy(source.ptable);
        this.etable = copies.copy(source.etable);
        this.table = copies.copy(source.table);
        this.queries = copies.copy(source.queries);
        this.isCompiled = source.isCompiled;
        this.datasetSpecified = source.datasetSpecified;
        this.reportEnabled = source.reportEnabled;
        this.isUseBind = source.isUseBind;
        this.aggregateQuery = source.aggregateQuery;
        this.isRelax = source.isRelax;
        this.isDistribute = source.isDistribute;
        this.isOptimize = source.isOptimize;
        this.isTest = source.isTest;
        this.isNew = source.isNew;
        this.isConstruct = source.isConstruct;
        this.isInsert = source.isInsert;
        this.isDelete = source.isDelete;
        this.isUpdate = source.isUpdate;
        this.isAsk = source.isAsk;
        this.isListGroup = source.isListGroup;
        this.validate = source.validate;
        this.federate = source.federate;
        this.isCorrect = source.isCorrect;
        this.isMap = source.isMap;
        this.isRule = source.isRule;
        this.isDetail = source.isDetail;
        this.planner = source.planner;
        this.isService = source.isService;
        this.mapping = copies.copy(source.mapping);
        this.service = copies.copy(source.service);
    }

    /** Copies a completed query, with fresh containers and no retained planning producer. */
    @Override
    public QueryPlan copyPlan(PlanCopy copies) {
        if (!isCompiled) {
            throw new IllegalStateException("Only completed queries can form an execution image");
        }
        return new QueryPlan(this, copies);
    }

    QueryPlan(Exp e) {
        this();
        add(e);
    }

    public static QueryPlan create(Exp e) {
        return new QueryPlan(e);
    }

    /** Creates an empty compiled query. */
    public static QueryPlan create() {
        return new QueryPlan();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {

        if (!getSelectFun().isEmpty()) {
            sb.append("select ");
            sb.append(getSelectFun());
            sb.append("\n");
        }

        super.toString(sb);

        if (!getOrderBy().isEmpty()) {
            sb.append("\n");
            sb.append("order by ");
            sb.append(getOrderBy());
        }
        if (!getGroupBy().isEmpty()) {
            sb.append("\n");
            sb.append("group by ");
            sb.append(getGroupBy());
        }
        if (getHaving() != null) {
            sb.append("\n");
            sb.append("having ");
            sb.append(getHaving());
        }
        Exp val = getValues();
        if (val != null && !val.getMappings().isEmpty()) {
            sb.append("\n");
            sb.append("values");
            sb.append(val.getNodeList());
            sb.append("{");
            sb.append(val.getMappings());
            sb.append("}");
        }

        return sb;
    }


    public QueryPlan get(Edge e) {
        return table.get(e);
    }

    public List<QueryPlan> getQueries() {
        return queries;
    }

    @Override
    public Object getObject() {
        return queryObject;
    }

    @Override
    public void setObject(Object o) {
        queryObject = o;
    }

    public int getPlanProfile() {
        return planner;
    }


    public void addError(String mes, Object obj) {
        getGlobalQuery().setError(mes, obj);
    }

    void setError(String mes, Object obj) {
        setError(mes, obj, true);
    }

    void setError(String mes, Object obj, boolean duplicate) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        String str = mes;
        if (obj != null) {
            str += obj;
        }

        if (!errors.contains(str)) {

            if (!duplicate) {
                for (String m : errors) {
                    if (m.startsWith(mes)) {
                        return;
                    }
                }
            }
            errors.add(str);
        }
    }


    public void addInfo(String mes, Object obj) {
        if (info == null) {
            info = new ArrayList<>();
        }
        if (obj != null) {
            mes += obj;
        }
        if (!info.contains(mes)) {
            info.add(mes);
        }
    }

    public Exp getBody() {
        return first();
    }

    public void setBody(Exp exp){
        clear();
        add(exp);
    }

    void setGlobalQuery(QueryPlan q) {
        globalQuery = q;
        inherit(q);
    }

    /**
     * inherit from and from named
     */
    void inherit(QueryPlan q) {
        if (!isService()) {
            setFrom(q.getFrom());
            setNamed(q.getNamed());
            setDatasetSpecified(q.isDatasetSpecified());
        }
    }


    public QueryPlan getGlobalQuery() {
        if (globalQuery != null) {
            return globalQuery;
        }
        return this;
    }

    public void setOuterQuery(QueryPlan q) {
        outerQuery = q;
    }

    public QueryPlan getOuterQuery() {
        if (outerQuery == null) {
            return this;
        }
        return outerQuery;
    }

    public boolean isSubQuery() {
        return globalQuery != null;
    }


    public boolean isSelectExpression(){
         for (Exp e : getSelectFun()) {
             if (e.getFilter() != null){
                 return true;
             }
         }
         return false;
    }

    /**
     * Fake local graph node
     */
    @Override
    public Node getGraphNode() {
        return gNode;
    }

    public void setGraphNode(Node n) {
        gNode = n;
    }

    public List<Filter> getFunList(){
        return funList;
    }


    public List<Node> getFrom(Node gNode) {
        if (gNode == null) {
            return from;
        } else {
            return named;
        }
    }

    public List<Node> getFrom() {
        return from;
    }

    public List<Node> getNamed() {
        return named;
    }

    public void setFrom(List<Node> l) {
        from = l;
    }

    public void setNamed(List<Node> l) {
        named = l;
    }

    /**
     * Records whether an active dataset was explicitly supplied, even when one
     * of its graph sets is empty.
     */
    public void setDatasetSpecified(boolean datasetSpecified) {
        this.datasetSpecified = datasetSpecified;
    }

    /** Returns whether the query has an explicit active dataset. */
    public boolean isDatasetSpecified() {
        return datasetSpecified;
    }

    VariableLayout getLayout() {
        return layout;
    }

    public List<Node> getPatternNodes() {
        return layout.getPatternNodes();
    }

    public List<Node> getQueryNodes() {
        return layout.getQueryNodes();
    }

    public List<Node> getPatternSelectNodes() {
        return layout.getPatternSelectNodes();
    }

    public List<Node> getBindingNodes() {
        if (getValues() == null) {
            return layout.getBindingNodes();
        }
        return getValues().getNodeList();
    }

    /**
     * compute select * node list in body patterns
     * pattern nodes U pattern select nodes
     * the native select node list of this query (if any) is not taken into account here
     * it may be taken onto account by compiler transformer
     */
    public List<Node> selectNodesFromPattern() {
        List<Node> list = new ArrayList<>();
        list.addAll(getPatternNodes());

        for (Node node : getPatternSelectNodes()) {
            if (!list.contains(node)) {
                Node ext = getExtNode(node);
                add(list, ext);
            }
        }
        return list;
    }



    /**
     * select variable node list, including (exp as variable)
     * computed by compiler transformer
     */
    public List<Node> getSelect() {
        return selectNode;
    }

    public void setSelect(List<Node> list) {
        selectNode = list;
    }

    /**
     * select variable node list as Exp(node, exp) where exp may be null
     * computed by compiler transformer
     */
    public List<Exp> getSelectFun() {
        return selectExp;
    }

    public Node getPatternNode(String name) {
        return layout.getPatternNode(name);
    }

    public Node getQueryNode(String name) {
        return layout.getQueryNode(name);
    }

    public Node getPatternSelectNode(String name) {
        return layout.getPatternSelectNode(name);
    }

    public Node getQuerySelectNode(String name) {
        return layout.getQuerySelectNode(name);
    }

    public void setSlice(int n) {
        slice = n;
    }

    public int getSlice() {
        return slice;
    }

    public void setMap(boolean b) {
        isMap = b;
    }

    public boolean isMap() {
        return isMap;
    }

    public void setLimit(int n) {
        limit = n;
    }

    public int getLimit() {
        return limit;
    }

    public int getLimitOffset() {
        // when order by/group by/count(), return all results, group sort agg, and then apply offset/limit
        if (!isConstruct()
                && (isOrderBy() || hasGroupBy() || isAggregate())) {
            return Integer.MAX_VALUE;
        }
        if (limit < Integer.MAX_VALUE - offset) {
            return limit + offset;
        } else {
            return limit;
        }
    }

    public void setOffset(int n) {
        offset = n;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isDistinct() {
        return distinctResults;
    }

    public void setDistinct(boolean b) {
        distinctResults = b;
    }

    /**
     * Determines whether DISTINCT is strictly redundant with GROUP BY.
     *
     * <p>In SPARQL 1.1, if a query uses GROUP BY and all grouping expressions are simple
     * variables directly projected in the SELECT list, then every solution row emitted
     * by the GROUP BY stage already possesses a unique combination of group key values.
     * Consequently, each projected row is guaranteed to be pairwise distinct, making an
     * additional DISTINCT deduplication pass redundant.</p>
     *
     * <p>A counter-example where DISTINCT is NOT redundant:
     * {@code SELECT DISTINCT (COUNT(*) AS ?n) WHERE { ... } GROUP BY ?x}
     * Here, two distinct groups (:a and :b) may yield the same count ?n = 1. If ?x is not
     * projected, removing DISTINCT would erroneously return two rows instead of one.</p>
     *
     * @return true if DISTINCT is guaranteed to be redundant with GROUP BY
     */
    public boolean isDistinctRedundantWithGroupBy() {
        if (!isDistinct() || !hasGroupBy() || isListGroup()) {
            return false;
        }
        List<Exp> groupList = getGroupBy();
        List<Node> selectNodes = getSelect();
        if (groupList == null || groupList.isEmpty() || selectNodes == null || selectNodes.isEmpty()) {
            return false;
        }
        Set<String> selectVarNames = extractSelectVariableNames(selectNodes);
        return areAllGroupKeysProjected(groupList, selectVarNames);
    }

    private static Set<String> extractSelectVariableNames(List<Node> selectNodes) {
        return selectNodes.stream()
                .filter(n -> n != null && n.isVariable())
                .map(Node::getLabel)
                .collect(Collectors.toSet());
    }

    private static boolean areAllGroupKeysProjected(List<Exp> groupList, Set<String> selectVarNames) {
        for (Exp groupExp : groupList) {
            if (groupExp == null || groupExp.getNode() == null || groupExp.getFilter() != null) {
                return false;
            }
            Node groupNode = groupExp.getNode();
            if (!groupNode.isVariable() || !selectVarNames.contains(groupNode.getLabel())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isAggregate() {
        return aggregateQuery;
    }

    @Override
    public void setAggregate(boolean b) {
        aggregateQuery = b;
    }

    public boolean isRelax() {
        return isRelax;
    }

    public void setRelax(boolean b) {
        isRelax = b;
    }

    public boolean isDistribute() {
        if (globalQuery != null) {
            return globalQuery.isDistribute();
        } else {
            return isDistribute;
        }
    }


    public boolean isSelect(){
        return ! (isConstruct() || isUpdate() || isInsert() || isDelete() || isAsk());
    }

    public boolean isConstruct() {
        return isConstruct;
    }

    public void setConstruct(boolean b) {
        isConstruct = b;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public void setDelete(boolean b) {
        isDelete = b;
    }

    public boolean isUpdate() {
        return isUpdate;
    }

    public void setUpdate(boolean b) {
        isUpdate = b;
    }

    public boolean isAsk() {
        return isAsk;
    }

    public void setAsk(boolean b) {
        isAsk = b;
    }

    public boolean isTest() {
        return isTest;
    }

    public void setTest(boolean b) {
        isTest = b;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean b) {
        isNew = b;
    }

    public boolean isOptimize() {
        return isOptimize;
    }

    public void setOptimize(boolean b) {
        isOptimize = b;
    }

    public void setAggregate() {
        for (Exp exp : getSelectFun()) {
            if (exp.getFilter() != null && exp.isAggregate() && !exp.isExpGroupBy()) {
                setAggregate(true);
            }
        }
        for (Exp exp : getOrderBy()) {
            if (exp.getFilter() != null && exp.getFilter().isAggregate()) {
                setAggregate(true);
            }
        }
    }


    public void setSelectFun(List<Exp> s) {
        selectExp = s;
    }

    public void addSelect(Node node) {
        selectExp.add(Exp.create(Type.NODE, node));
    }

    public void addOrderBy(Node node) {
        orderBy.add(Exp.create(Type.NODE, node));
    }

    public void addGroupBy(Node node) {
        groupBy.add(Exp.create(Type.NODE, node));
    }

    public void setOrderBy(List<Exp> s) {
        orderBy = s;
    }

    public boolean isOrderBy() {
        return !orderBy.isEmpty();
    }

    public List<Exp> getOrderBy() {
        return orderBy;
    }

    public void setGroupBy(List<Exp> s) {
        groupBy = s;
    }

    public List<Exp> getGroupBy() {
        return groupBy;
    }

    public boolean isGroupBy() {
        return !groupBy.isEmpty();
    }

    public boolean hasGroupBy() {
        return isGroupBy();
    }

    public boolean isListGroup() {
        return isListGroup;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean b) {
        isCorrect = b;
    }

    public void setHaving(Exp f) {
        having = f;
    }

    public Exp getHaving() {
        return having;
    }

    public void setConstruct(Exp c) {
        construct = c;
    }

    public Exp getConstruct() {
        return construct;
    }

    public Exp getInsert() {
        return construct;
    }

    public List<Node> getConstructNodes() {
        return layout.getConstructNodes();
    }

    public void setConstructNodes(List<Node> constructNodes) {
        layout.setConstructNodes(constructNodes);
    }

    public void setDelete(Exp c) {
        delete = c;
    }

    public Exp getDelete() {
        return delete;
    }

    @Override
    boolean member(Node node, List<Exp> lExp) {
        return member(node.getLabel(), lExp);
    }

    boolean member(String variable, List<Exp> lExp) {
        for (Exp exp : lExp) {
            if (variable.equals(exp.getNode().getLabel())) {
                return true;
            }
        }
        return false;
    }

    public Exp getSelectExp(String label) {
        for (Exp exp : getSelectFun()) {
            Node node = exp.getNode();
            if (node.getLabel().equals(label)) {
                return exp;
            }
        }
        return null;
    }

    public Node getSelectNode(String label) {
        Exp exp = getSelectExp(label);
        if (exp != null) {
            return exp.getNode();
        }
        return null;
    }

    public int nbNodes() {
        return layout.getNodeIndex();
    }

    public int nbEdges() {
        return layout.getEdgeIndex();
    }


    /**
     * Called by Eval before query evaluation
     * Only on global query, not on subquery
     */
    public void complete(Producer prod) {
        synchronized (this) {
            if (!isCompiled()) {
                basicComplete(prod);
                setCompiled();
            }
        }
    }

    void basicComplete(Producer prod) {
        // sort edges according to variable connexity, assign filters
        // recurse on subquery
        querySorter.compile(prod);
        setAggregate();
        // recurse on subquery
        index(this, getBody(), false, -1);

        for (Exp ee : getSelectFun()) {
            // use case: query node created to hold fun result
            Node snode = ee.getNode();
            index(snode);
            // use case: select (exists{?x :p ?y} as ?b)
            if (ee.getFilter() != null) {
                index(this, ee.getFilter());
            }
        }

        complete2();

        for (QueryPlan q : getQueries()) {
            q.complete(prod);
        }

        getSelect();
    }

    /**
     *
     * index(node) use global query index
     */
    void complete2() {
        index(getOrderBy());
        index(getGroupBy());

        if (getHaving() != null) {
            index(this, getHaving().getFilter());
        }

        for (Filter f :getFunList()){
            index(this, f);
        }

        for (Node node : getBindingNodes()) {
            index(node);
        }

        for (Node node : getArgList()) {
            index(node);
        }

        if (getGraphNode() != null) {
            index(getGraphNode());
        }
    }

    void index(List<Exp> list) {
        for (Exp ee : list) {
            // use case: group by (exists(?x :p ?y) as ?b)
            // use case: order by exists(?x :p ?y)
            if (ee.getFilter() != null) {
                index(this, ee.getFilter());
            }
        }
    }

    @Override
    public QueryPlan getQuery() {
        return this;
    }

    @Override
    public QueryPlan getQueryPlan() {
        return this;
    }


    public Node getOuterNode(Node subNode) {
        return getExtNode(subNode.getLabel());
    }

    public Node getOuterNodeSelf(Node subNode) {
        Node n = getExtNode(subNode.getLabel());
        if (n == null) {
            return subNode;
        }
        return n;
    }

    public Node getExtNode(Node qNode) {
        return getExtNode(qNode.getLabel());
    }

    public Node getNode(String name) {
        return getExtNode(name);
    }

    /**
     * get node with going in select sub query
     * go into its own select because
     * order by may reuse a select variable use case: transformer find node for
     * select &amp; group by
     */
    public Node getProperAndSubSelectNode(String name) {
        return getExtNode(name, true);
    }

    public Node getExtNode(String name) {
        return getExtNode(name, false);
    }

    public Node getExtNode(String name, boolean select) {
        return layout.getExtNode(name, select, select ? getSelectNode(name) : null);
    }

    void store(Node node, boolean exist, boolean select) {
        layout.store(node, exist, select);
    }

    /**
     * called by compiler transformer
     */
    public void collect() {
        for (Exp ee : this) {
            collect(ee, false);
        }

        for (Node node : getBindingNodes()) {
            store(node, false, false);
        }

        for (Node node : getArgList()) {
            store(node, false, false);
        }

        for (Filter f : getFunList()){
             collectExist(f.getExp());
        }
    }

    // exist: inside exists ( exp )
    // or inside    A minus ( exp )
    void collect(Exp exp, boolean exist) {
        switch (exp.type()) {

            case FILTER:
                collectExist(exp.getFilter().getExp());
                break;

            case NODE:
                store(exp.getNode(), exist, false);
                break;

            case VALUES:
                for (Node node : exp.getNodeList()) {
                    store(node, exist, false);
                }
                break;

            case EDGE:
                collectEdgeNodes(exp.getEdge(), exist);
                break;

            case XPATH, EVAL:
                for (int i = 0; i < exp.nbNode(); i++) {
                    Node node = exp.getNode(i);
                    store(node, exist, false);
                }
                break;

            case MINUS:
                collectMinusNodes(exp, exist);
                break;

            case QUERY:
                for (Exp ee : exp.getQuery().getSelectFun()) {
                    store(ee.getNode(), exist, true);
                }
                break;

            case BIND:
                collectBindNodes(exp, exist);
                break;

            default:
                for (Exp ee : exp) {
                    collect(ee, exist);
                }
        }

    }

    private void collectEdgeNodes(Edge edge, boolean insideExistencePattern) {
        store(edge.getNode(0), insideExistencePattern, false);
        if (edge.getEdgeVariable() != null) {
            store(edge.getEdgeVariable(), insideExistencePattern, false);
        }
        store(edge.getNode(1), insideExistencePattern, false);
        for (int index = 2; index < edge.nbNode(); index++) {
            store(edge.getNode(index), insideExistencePattern, false);
        }
    }

    private void collectMinusNodes(Exp expression, boolean insideExistencePattern) {
        if (expression.first() != null) {
            collect(expression.first(), insideExistencePattern);
        }
        // The right operand is indexed but does not introduce solution bindings.
        if (expression.rest() != null) {
            collect(expression.rest(), true);
        }
    }

    private void collectBindNodes(Exp expression, boolean insideExistencePattern) {
        collectExist(expression.getFilter().getExp());
        store(expression.getNode(), insideExistencePattern, false);
        if (expression.getNodeList() != null) {
            for (Node node : expression.getNodeList()) {
                store(node, insideExistencePattern, false);
            }
        }
    }

    void collectExist(Expr exp) {
        if (exp.oper() == ExprType.EXIST) {
            Exp pat = getPattern(exp);
            collect(pat, true);
        } else {
            for (Expr ee : exp.getExpList()) {
                collectExist(ee);
            }
        }
    }

    /**
     * set Index for EDGE & NODE
     * check if subquery is aggregate
     * in case of UNION, start is the start index for both branches
     * return the min of index of exp Nodes
     * query is (sub)query this is global query
     */
    int index(QueryPlan query, Exp exp, boolean isExist, int start) {
        int min = switch (exp.type()) {
            case EDGE, XPATH, EVAL -> indexEdgeExp(exp);
            case VALUES -> indexValuesExp(exp);
            case NODE -> index(exp.getNode());
            case BIND -> indexBindExp(query, exp, isExist);
            case FILTER -> indexExpFilter(query, exp, isExist);
            case QUERY -> indexExpQuery(query, exp, isExist);
            case OPT_BIND, ACCEPT -> Integer.MAX_VALUE;
            default -> indexDefaultExp(query, exp, isExist, start);
        };

        if (exp.getGraphNode() != null) {
            index(exp.getGraphNode());
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
            int n = index(node);
            min = Math.min(min, n);
        }
        return min;
    }

    private int indexBindExp(QueryPlan query, Exp exp, boolean isExist) {
        Node qn = exp.getNode();
        int min = index(qn);
        if (exp.getNodeList() != null) {
            for (Node bn : exp.getNodeList()) {
                int ii = index(bn);
                min = Math.min(min, ii);
            }
        }
        return indexExpFilter(query, exp, isExist);
    }

    private int indexDefaultExp(QueryPlan query, Exp exp, boolean isExist, int start) {
        int min = Integer.MAX_VALUE;
        int startIndex = (start >= 0) ? start : globalNodeIndex();
        int ind = exp.isUnion() ? startIndex : -1;
        for (Exp e : exp) {
            int n = index(query, e, isExist, ind);
            min = Math.min(min, n);
        }
        return min;
    }

    // use case: index filter exists (?x ?p ?y)
    int indexExpFilter(QueryPlan query, Exp exp, boolean isExist) {
        int min = Integer.MAX_VALUE;
        boolean hasExist = index(query, exp.getFilter());
        List<String> lVar = exp.getFilter().getVariables(true);

        for (String variable : lVar) {
            Node qNode = query.getProperAndSubSelectNode(variable);
            if (qNode == null) {
                // Note: does not work with filter in exists pattern
                // because getProperAndSubSelectNode does not go into exists pattern
                logger.warn(Message.Prefix.UNDEF_VAR.getString(), variable);
                addError(Message.Prefix.UNDEF_VAR.getString(), variable);
            } else if (!isExist && !hasExist) {
                int n = index(qNode);
                min = Math.min(min, n);
            }
        }
        if (hasExist) {
            // use case:
            // exists (?x p ?y filter(?x != ?z))
            min = -1;
        }
        return min;
    }

    int indexExpQuery(QueryPlan query, Exp exp, boolean isExist) {
        int min = Integer.MAX_VALUE;
        QueryPlan qq = exp.getQuery();
        qq.setCompiled();
        qq.setGlobalQuery(this);
        qq.setOuterQuery(query);
        qq.setAggregate();

        for (Exp e : exp) {
            // for subquery, do not consider index here
            index(qq, e, isExist, -1);
        }

        for (Exp ee : qq.getSelectFun()) {
            // use case: query node created to hold fun result
            // see Transformer compiler.compileSelectFun()
            Node sNode = ee.getNode();
            int n = index(sNode);
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
            int n = index(exp.getNode(i));
            min = Math.min(min, n);
        }
        return min;
    }


    /**
     * Returns a node's existing index or assigns one from the outer query's
     * binding space. Subqueries use the same index allocator as their parent.
     *
     * @param node node to index
     * @return existing or newly assigned binding index
     */
    public int index(Node node) {
        if (node.getIndex() == -1) {
            node.setIndex(newGlobalNodeIndex());
        }
        return node.getIndex();
    }

    /**
     * Use outer query node index for all (sub) queries
     */
    int newGlobalNodeIndex() {
        return getOuterQuery().newNodeIndex();
    }

    int newNodeIndex() {
        return layout.allocateNodeIndex();
    }

    int globalNodeIndex() {
        return getOuterQuery().getNodeIndex();
    }

    int getNodeIndex() {
        return layout.getNodeIndex();
    }

    /**
     *
     * @return list Exp(node) for select * node list
     */
    @SuppressWarnings("java:S9391") // Preserve ordered accumulation without allocating a stream pipeline.
    public List<Exp> toExp(List<Node> lNode) {
        List<Exp> lExp = new ArrayList<>();
        for (Node node : lNode) {
            lExp.add(Exp.create(Type.NODE, node));
        }
        return lExp;
    }

    public List<Node> getArgList() {
        return layout.getArgList();
    }

    /**
     * Compute node list for filter variables use case: Pattern compiler (?x =
     * cst) Note: does not dive into minus (PAT)
     */
    public List<Node> getNodes(Exp exp) {
        return getNodes(exp.getFilter());
    }

    public List<Node> getNodes(Filter f) {
        List<String> lVar = f.getVariables();
        ArrayList<Node> lNode = new ArrayList<>();
        for (String variable : lVar) {
            Node node = getProperAndSubSelectNode(variable);
            if (node != null && !lNode.contains(node)) {
                lNode.add(node);
            }
        }
        return lNode;
    }


    /**
     * Dependency with filter exp for tracking filter(exists {PAT})
     *
     */
    boolean index(QueryPlan query, Filter f) {
        return index(query, f.getExp());
    }

    /**
     * Looking for filter(exist {})
     */
    boolean index(QueryPlan query, Expr exp) {
        boolean b = false;
        if (exp.oper() == ExprType.EXIST) {
            index(query, getPattern(exp), true, -1);
            b = true;
        } else {
            for (Expr ee : exp.getExpList()) {
                b = index(query, ee) || b;
            }
        }
        return b;
    }

    public QueryPlan orderBy(Node node) {
        if (node != null && doesNotContain(getOrderBy(), node)) {
            addOrderBy(node);
        }
        return this;
    }

    public QueryPlan groupBy(Node node) {
        if (node != null && doesNotContain(getGroupBy(), node)) {
            addGroupBy(node);
        }
        return this;
    }

    void setSelect(QueryPlan q1, QueryPlan q2) {
        List<Exp> list = new ArrayList<>();
        list.addAll(q1.getSelectFun());
        for (Exp exp : q2.getSelectFun()) {
            if (doesNotContain(list, exp.getNode())) {
                list.add(exp);
            }
        }
        setSelectFun(list);
    }

    // rec set global query
    void setGlobalQuery(Exp exp) {
        for (Exp q : exp) {
            if (q.isQuery()) {
                q.getQuery().setGlobalQuery(this);
                setGlobalQuery(q);
            }
        }
    }

    public void setService(boolean isService) {
        this.isService = isService;
    }

    @Override
    public boolean isService() {
        return isService;
    }

    public void setService(String serv) {
        this.service = serv;
    }

    public String getService() {
        return service;
    }

    void setCompiled() {
        this.isCompiled = true;
    }

    boolean isCompiled() {
        return isCompiled;
    }

    public Filter getFilter(String name) {
        return ftable.get(name);
    }

    public Filter getGlobalFilter(String name) {
        return getGlobalQuery().getFilter(name);
    }

    public void setRule(boolean rule) {
        isRule = rule;
    }

    public boolean isRule() {
        return isRule;
    }

    public boolean isRecordEdge(){
        return isRule() || isRelax();
    }

    public boolean isDetail() {
        return isDetail;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public Iterable<Object> getLoop() {
        return () -> getEdges().stream().map(Object.class::cast).iterator();
    }

    public List<Edge> getEdges(){
        ArrayList<Edge> list = new ArrayList<>();
        getBody().getEdgeList(list);
        return list;
    }

    @Override
    public String getDatatypeLabel() {
        return "[QueryPlan]";
    }


    public boolean isUseBind() {
        return isUseBind;
    }



    public boolean isReportEnabled() {
        return getGlobalQuery().reportEnabled;
    }

    public void setReportEnabled(boolean reportEnabled) {
        getGlobalQuery().reportEnabled = reportEnabled;
    }




    @Override
    public Mapping getMapping() {
        return mapping;
    }


    public void setInsert(boolean isInsert) {
        this.isInsert = isInsert;
    }

    public boolean isInsert() {
        return isInsert;
    }

    public boolean isFederate() {
        return federate;
    }


    public void setFederate(boolean federate) {
        this.federate = federate;
    }


    public boolean isValidate() {
        return validate;
    }


    public void setValidate(boolean validate) {
        this.validate = validate;
    }



}
