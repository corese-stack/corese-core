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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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

    private int number = 0;
    private final VariableLayout layout;
    private final QueryClauses clauses;
    private final QueryExpIndexer indexer;

    List<Node> relaxEdges;
    List<Filter> failure;
    List<Filter> funList;

    List<String> errors;
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

    private boolean isUseBind = true;

    boolean isRelax = false;
    boolean isDistribute = false;
    boolean isOptimize = false;
    boolean isTest = false;
    boolean isNew = true;
    private boolean validate = false;
    private boolean federate = false;

    boolean isCorrect = true;
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
        clauses = new QueryClauses();
        indexer = new QueryExpIndexer(this, layout);
        failure = new ArrayList<>();
        funList = new ArrayList<>();
        compiler = new FilterOptimizer(this);
        table = new HashMap<>();
        ftable = new HashMap<>();
        pragma = new HashMap<>();
        ptable = new HashMap<>();
        etable = new HashMap<>();
        queries = new ArrayList<>();
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
        clauses = source.clauses.copyPlan(copies);
        indexer = new QueryExpIndexer(this, layout);
        querySorter = null;
        compiler = null;
        this.number = source.number;
        this.relaxEdges = copies.copy(source.relaxEdges);
        this.failure = copies.copy(source.failure);
        this.funList = copies.copy(source.funList);
        this.errors = copies.copy(source.errors);
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
        this.isUseBind = source.isUseBind;
        this.isRelax = source.isRelax;
        this.isDistribute = source.isDistribute;
        this.isOptimize = source.isOptimize;
        this.isTest = source.isTest;
        this.isNew = source.isNew;
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

    /** Creates an empty query plan. */
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
        return clauses.getFrom(gNode);
    }

    public List<Node> getFrom() {
        return clauses.getFrom();
    }

    public List<Node> getNamed() {
        return clauses.getNamed();
    }

    public void setFrom(List<Node> l) {
        clauses.setFrom(l);
    }

    public void setNamed(List<Node> l) {
        clauses.setNamed(l);
    }

    /**
     * Records whether an active dataset was explicitly supplied, even when one
     * of its graph sets is empty.
     */
    public void setDatasetSpecified(boolean datasetSpecified) {
        clauses.setDatasetSpecified(datasetSpecified);
    }

    /** Returns whether the query has an explicit active dataset. */
    public boolean isDatasetSpecified() {
        return clauses.isDatasetSpecified();
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
        return clauses.getSelect();
    }

    public void setSelect(List<Node> list) {
        clauses.setSelect(list);
    }

    /**
     * select variable node list as Exp(node, exp) where exp may be null
     * computed by compiler transformer
     */
    public List<Exp> getSelectFun() {
        return clauses.getSelectFun();
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
        clauses.setSlice(n);
    }

    public int getSlice() {
        return clauses.getSlice();
    }

    public void setMap(boolean b) {
        isMap = b;
    }

    public boolean isMap() {
        return isMap;
    }

    public void setLimit(int n) {
        clauses.setLimit(n);
    }

    public int getLimit() {
        return clauses.getLimit();
    }

    public int getLimitOffset() {
        return clauses.getLimitOffset();
    }

    public void setOffset(int n) {
        clauses.setOffset(n);
    }

    public int getOffset() {
        return clauses.getOffset();
    }

    public boolean isDistinct() {
        return clauses.isDistinct();
    }

    public void setDistinct(boolean b) {
        clauses.setDistinct(b);
    }

    public boolean isDistinctRedundantWithGroupBy() {
        return clauses.isDistinctRedundantWithGroupBy();
    }

    @Override
    public boolean isAggregate() {
        return clauses.isAggregate();
    }

    @Override
    public void setAggregate(boolean b) {
        clauses.setAggregate(b);
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

    public boolean isSelect() {
        return !(isConstruct() || isUpdate() || isInsert() || isDelete() || isAsk());
    }

    public boolean isConstruct() {
        return clauses.isConstruct();
    }

    public void setConstruct(boolean b) {
        clauses.setConstruct(b);
    }

    public boolean isDelete() {
        return clauses.isDelete();
    }

    public void setDelete(boolean b) {
        clauses.setDelete(b);
    }

    public boolean isUpdate() {
        return clauses.isUpdate();
    }

    public void setUpdate(boolean b) {
        clauses.setUpdate(b);
    }

    public boolean isAsk() {
        return clauses.isAsk();
    }

    public void setAsk(boolean b) {
        clauses.setAsk(b);
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
        clauses.setSelectFun(s);
    }

    public void addSelect(Node node) {
        clauses.addSelect(node);
    }

    public void addOrderBy(Node node) {
        clauses.addOrderBy(node);
    }

    public void addGroupBy(Node node) {
        clauses.addGroupBy(node);
    }

    public void setOrderBy(List<Exp> s) {
        clauses.setOrderBy(s);
    }

    public boolean isOrderBy() {
        return clauses.isOrderBy();
    }

    public List<Exp> getOrderBy() {
        return clauses.getOrderBy();
    }

    public void setGroupBy(List<Exp> s) {
        clauses.setGroupBy(s);
    }

    public List<Exp> getGroupBy() {
        return clauses.getGroupBy();
    }

    public boolean isGroupBy() {
        return clauses.isGroupBy();
    }

    public boolean hasGroupBy() {
        return clauses.hasGroupBy();
    }

    public boolean isListGroup() {
        return clauses.isListGroup();
    }

    public void setListGroup(boolean listGroup) {
        clauses.setListGroup(listGroup);
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean b) {
        isCorrect = b;
    }

    public void setHaving(Exp f) {
        clauses.setHaving(f);
    }

    public Exp getHaving() {
        return clauses.getHaving();
    }

    public void setConstruct(Exp c) {
        clauses.setConstruct(c);
    }

    public Exp getConstruct() {
        return clauses.getConstruct();
    }

    public Exp getInsert() {
        return clauses.getInsert();
    }

    public List<Node> getConstructNodes() {
        return layout.getConstructNodes();
    }

    public void setConstructNodes(List<Node> constructNodes) {
        layout.setConstructNodes(constructNodes);
    }

    public void setDelete(Exp c) {
        clauses.setDelete(c);
    }

    public Exp getDelete() {
        return clauses.getDelete();
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
        return clauses.getSelectExp(label);
    }

    public Node getSelectNode(String label) {
        return clauses.getSelectNode(label);
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
        indexer.collect();
    }

    int index(QueryPlan query, Exp exp, boolean isExist, int start) {
        return indexer.index(query, exp, isExist, start);
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

    public int allocateNodeIndex() {
        return layout.allocateNodeIndex();
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
     */
    boolean index(QueryPlan query, Filter f) {
        return indexer.index(query, f);
    }

    /**
     * Looking for filter(exist {})
     */
    boolean index(QueryPlan query, Expr exp) {
        return indexer.index(query, exp);
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

    public List<Edge> getEdges(){
        ArrayList<Edge> list = new ArrayList<>();
        getBody().getEdgeList(list);
        return list;
    }



    public boolean isUseBind() {
        return isUseBind;
    }

    public Mapping getMapping() {
        return mapping;
    }


    public void setInsert(boolean isInsert) {
        clauses.setInsert(isInsert);
    }

    public boolean isInsert() {
        return clauses.isInsert();
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
