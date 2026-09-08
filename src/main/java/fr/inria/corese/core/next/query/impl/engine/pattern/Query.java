package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.eval.Eval;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.ExprType;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.path.PathFinder;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.sorter.QuerySorter;

import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.query.impl.engine.filter.Compile;
import fr.inria.corese.core.next.query.impl.engine.eval.Message;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * KGRAM Query
 * also used for subquery
 *
 * @author Olivier Corby, Edelweiss, INRIA 2009
 *
 */
@SuppressWarnings({"java:S1845", "java:S1700", "java:S2387"}) // Legacy KGRAM AST structure
public final class Query extends Exp {


    public static final int QP_T0 = 0; //No QP settings
    public static final int QP_DEFAULT = 1; //Default Corese QP
    public static final int QP_HEURISTICS_BASED = 2;//Heuristics based QP
    public static final int QP_BGP = 3;//BGP based QP

    public static final int STD_PROFILE = -1;
    public static final int COUNT_PROFILE = 1;

    private static final Logger logger = LoggerFactory.getLogger(Query.class);

    public static final String PATHNODE = "pathNode";

    int limit = Integer.MAX_VALUE;
    int offset = 0;
    // if slice > 0 : service gets mappings from previous pattern by slices
    int slice = 20;

    private int number = 0;
    boolean distinct = false;
    int iNode = 0;
    int iEdge = 0;
    List<Node> from;
    List<Node> named;
    List<Node> selectNode;
    // all nodes (on demand)

    // std pattern but minus/exists (without select)
    List<Node> patternNodes;
    // std pattern but minus/exists select nodes
    List<Node> patternSelectNodes;
    // minus/exists (without select)
    List<Node> queryNodes;
    // minus/exists select nodes
    List<Node> querySelectNodes;
    // final query bindings nodes
    List<Node> bindingNodes;
    private List<Node> constructNodes;
    List<Node> relaxEdges;
    List<Exp> selectExp;
    List<Exp> orderBy;
    List<Exp> groupBy;
    List<Filter> failure;
    List<Filter> pathFilter;
    List<Filter> funList;

    List<String> errors;
    List<String> info;
    Exp having;
    Exp construct;
    Exp delete;
	// gNode is a local graph node when subquery has no ?g in its select
    // use case: graph ?g (select where ...)
    Node gNode;
    Node pathNode;
    // outer main query that contains this (when subquery)
    // SPIN graph
    Query query;
    Query outerQuery;
    QueryAst ast;
    Object object;

    // current transformer if any
    private Object transformer;
    // table: transformation -> Transformer
    // shared by templates of Transformer
    HashMap<String, Object> tprinter;

    Compile compiler;
    private final QuerySorter querySorter;

    HashMap<String, Object> pragma;
    // Extended filters: pathNode()
    HashMap<String, Filter> ftable;
	// Extended queries for type check
    // nb occurrences of predicates in where
    HashMap<String, Integer> ptable;
    HashMap<String, Edge> etable;
    // query for class/property definition checking
    HashMap<Edge, Query> table;
    // Extended queries for additional group by
    List<Query> queries;
    private boolean isCompiled = false;
    private boolean datasetSpecified;
    private boolean reportEnabled;

    boolean isCheck = false;
    private boolean isUseBind = true;

    boolean
            isAggregate = false;
    boolean isRelax = false;
    boolean isDistribute = false;
    boolean isOptimize = false;
    boolean isTest = false;
    boolean isNew = true;
    boolean isSort = true;
    boolean isConstruct = false;
    private boolean isInsert = false;
    boolean isDelete = false;
    boolean isUpdate = false;
    boolean isAsk = false;
    boolean isCheckLoop = false;
    boolean isListGroup = false;
    boolean isListPath = true;
    private boolean parallel = true;
    private boolean validate = false;
    private boolean federate = false;
    // store the list of edges of the path
    private boolean isStorePath = true;
    // cache PP result in PathFinder
    private boolean isCachePath = false;
    boolean isCountPath = false;

    boolean
            isCorrect = true;
    boolean isConnect = false;
    boolean isMap = true;
    boolean isRule = false;
    boolean isDetail = true;
    private boolean algebra = false;
    int mode = Matcher.UNDEF;

    int planner = QP_DEFAULT;
    private int queryProfile = STD_PROFILE;

    private boolean isService = false;

    private boolean isSynchronized = false;
    private boolean lock = true;

    private boolean isTemplate = false;
    private boolean isTransformationTemplate = false;

    // member of a set of templates of a pprinter (not a single query that is a template)

    private Exp templateGroup;
    private final List<Node> argList;
    private Mapping mapping;
    private boolean isExtension = false;


    private BgpGenerator bgpGenerator;


    private String service;

	public Query(){
        super(Type.QUERY);
		from 		= new ArrayList<>();
		named 		= new ArrayList<>();
		selectExp 	= new ArrayList<>();
		orderBy 	= new ArrayList<>();
		groupBy 	= new ArrayList<>();
		failure 	= new ArrayList<>();
		pathFilter 	= new ArrayList<>();
		funList 	= new ArrayList<>();

		compiler 	= new Compile(this);
		table 		= new HashMap<>();
		ftable 		= new HashMap<>();
		pragma 		= new HashMap<>();
		tprinter 	= new HashMap<> ();
                ptable          = new HashMap<>();
                etable          = new HashMap<>();
		queries 	= new ArrayList<>();

                selectNode              = new ArrayList<>();
		patternNodes 		= new ArrayList<>();
		queryNodes 		= new ArrayList<>();
		patternSelectNodes 	= new ArrayList<>();
		querySelectNodes 	= new ArrayList<>();
		bindingNodes 		= new ArrayList<>();
		relaxEdges 		= new ArrayList<>();
		argList 		= new ArrayList<>();
                querySorter = new QuerySorter(this);

    }

    Query(Exp e) {
        this();
        add(e);
    }

    public static Query create(Exp e) {
        return new Query(e);
    }

    @SuppressWarnings("java:S1172") // Parameter preserved for legacy factory compatibility
    public static Query create(int type) {
        return new Query();
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


    public Query get(Edge e) {
        return table.get(e);
    }

    public List<Query> getQueries() {
        return queries;
    }

    @Override
    public Object getObject() {
        return object;
    }

    @Override
    public void setObject(Object o) {
        object = o;
    }

    public QueryAst getGlobalAST() {
        return getGlobalQuery().getAST();
    }

    public QueryAst getAST() {
        return ast;
    }

    public void setAST(QueryAst o) {
        ast = o;
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
            //logger.error(str);
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
        args.clear();
        args.add(exp);
    }

    void setGlobalQuery(Query q) {
        query = q;
        inherit(q);
    }

    /**
     * inherit from and from named
     */
    void inherit(Query q) {
        if (!isService()) {
            setFrom(q.getFrom());
            setNamed(q.getNamed());
            setDatasetSpecified(q.isDatasetSpecified());
        }
    }


    public Query getGlobalQuery() {
        if (query != null) {
            return query;
        }
        return this;
    }

    public void setOuterQuery(Query q) {
        outerQuery = q;
    }

    public Query getOuterQuery() {
        if (outerQuery == null) {
            return this;
        }
        return outerQuery;
    }

    public boolean isSubQuery() {
        return query != null;
    }


    public boolean isSelectExpression(){
         for (Exp e : getSelectFun()) {
             if (e.getFilter() != null){
                 return true;
             }
         }
         return false;
    }

    public boolean isCheckLoop() {
        return isCheckLoop;
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

    public Node getPathNode() {
        return pathNode;
    }

    /**
     * constraint in property path: ?x ex:prop @[?this != <John>] ?y
     */
    List<Filter> getPathFilter() {
        return pathFilter;
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

    public List<Node> getPatternNodes() {
        return patternNodes;
    }

    public List<Node> getQueryNodes() {
        return queryNodes;
    }

    public List<Node> getPatternSelectNodes() {
        return patternSelectNodes;
    }

    public List<Node> getBindingNodes() {
        if (getValues() == null) {
            return bindingNodes;
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
        return get(patternNodes, name);
    }

    public Node getQueryNode(String name) {
        return get(queryNodes, name);
    }

    public Node getPatternSelectNode(String name) {
        return get(patternSelectNodes, name);
    }

    public Node getQuerySelectNode(String name) {
        return get(querySelectNodes, name);
    }

    Node get(List<Node> list, String name) {
        for (Node node : list) {
            if (name.equals(node.getLabel())) {
                return node;
            }
        }
        return null;
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
        return distinct;
    }

    public void setDistinct(boolean b) {
        distinct = b;
    }

    public boolean isCheck() {
        return isCheck;
    }

    @Override
    public boolean isAggregate() {
        return isAggregate;
    }

    @Override
    public void setAggregate(boolean b) {
        isAggregate = b;
    }

    public boolean isRelax() {
        return isRelax;
    }

    public void setRelax(boolean b) {
        isRelax = b;
    }

    public boolean isDistribute() {
        if (query != null) {
            return query.isDistribute();
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
        return isGroupBy() || isConnect();
    }

    public boolean isListGroup() {
        return isListGroup;
    }

    public boolean isListPath() {
        return isListPath;
    }

    public boolean isCountPath() {
        return isCountPath;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean b) {
        isCorrect = b;
    }

    public boolean isConnect() {
        return isConnect;
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
        return constructNodes;
    }

    public void setConstructNodes(List<Node> constructNodes) {
        this.constructNodes = constructNodes;
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
        return iNode;
    }

    public int nbEdges() {
        return iEdge;
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

        for (Query q : getQueries()) {
            q.complete(prod);
        }

        getSelect();
    }

    /**
     *
     * index(node) use global query index
     */
    void complete2() {
        for (Filter f : getPathFilter()) {
            index(this, f);
        }
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
        if (getPathNode() != null) {
            index(getPathNode());
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
    public Query getQuery() {
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
        Node node = getPatternNode(name);
        if (node != null) {
            return node;
        }
        if (select) {
            node = getSelectNode(name);
            if (node != null) {
                return node;
            }
        }
        node = getQueryNode(name);
        if (node != null) {
            return node;
        }
        node = getPatternSelectNode(name);
        if (node != null) {
            return node;
        }
        node = getQuerySelectNode(name);
        return node;
    }

    void store(Node node, boolean exist, boolean select) {
        if (select) {
            if (exist) {
                add(querySelectNodes, node);
            } else {
                add(patternSelectNodes, node);
            }
        } else {
            if (exist) {
                add(queryNodes, node);
            } else {
                add(patternNodes, node);
            }
        }
    }

    /**
     * called by compiler transformer
     */
    public void collect() {
        if (getPathNode() != null) {
            /*
             * use case: ?x ex:prop @[?this != <John>] + ?y collect ?this first
             * because it may be within @[exists {?this ?p ?y}} and even worse
             * within @[exists {select ?this where {?this ?p ?y}}]
             */
            store(getPathNode(), false, false);
        }

        for (Exp ee : this) {
            collect(ee, false);
        }

        for (Node node : getBindingNodes()) {
            store(node, false, false);
        }

        for (Node node : getArgList()) {
            store(node, false, false);
        }

        for (Filter ff : getPathFilter()) {
            collectExist(ff.getExp());
        }

        for (Filter f : getFunList()){
             collectExist(f.getExp());
        }
    }

    // exist: inside exists ( exp )
    // or inside    A minus ( exp )
    @SuppressWarnings("java:S3776")
    void collect(Exp exp, boolean exist) {
        switch (exp.type()) {

            case FILTER:
			// get exists () nodes
                // draft
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

            case EDGE, PATH:
                Edge edge = exp.getEdge();
                store(edge.getNode(0), exist, false);
                if (edge.getEdgeVariable() != null) {
                    store(edge.getEdgeVariable(), exist, false);
                }
                store(edge.getNode(1), exist, false);
                for (int i = 2; i < edge.nbNode(); i++) {
                    store(edge.getNode(i), exist, false);
                }
                break;

            case XPATH, EVAL:
                for (int i = 0; i < exp.nbNode(); i++) {
                    Node node = exp.getNode(i);
                    store(node, exist, false);
                }
                break;

            case MINUS:
                // second argument does not bind anything: skip it
                if (exp.first() != null) {
                    collect(exp.first(), exist);
                }
                if (exp.rest() != null) {
                    collect(exp.rest(), true);
                }
                break;

            case QUERY:
                for (Exp ee : exp.getQuery().getSelectFun()) {
                    store(ee.getNode(), exist, true);
                }
                break;

            case BIND:
                collectExist(exp.getFilter().getExp());
                store(exp.getNode(), exist, false); //true);
                if (exp.getNodeList() != null){
                    // values {unnest()} compiled as bind ()
                    for (Node n : exp.getNodeList()){
                        store(n, exist, false); //true);
                    }
                }
                break;

            default:
                for (Exp ee : exp) {
                    collect(ee, exist);
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
    int index(Query query, Exp exp, boolean isExist, int start) {
        int min = switch (exp.type()) {
            case EDGE, PATH, XPATH, EVAL -> indexEdgeExp(query, exp);
            case VALUES -> indexValuesExp(query, exp);
            case NODE -> qIndex(query, exp.getNode());
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

    private int indexEdgeExp(Query query, Exp exp) {
        Edge edge = exp.getEdge();
        edge.setEdgeIndex(iEdge++);
        int min = indexExpEdge(query, exp);
        if (exp.hasPath()) {
            Exp ep = exp.getPath();
            ep.getEdge().setEdgeIndex(edge.getEdgeIndex());
            indexExpEdge(query, ep);
        }
        return min;
    }

    private int indexValuesExp(Query query, Exp exp) {
        int min = Integer.MAX_VALUE;
        for (Node node : exp.getNodeList()) {
            int n = qIndex(query, node);
            min = Math.min(min, n);
        }
        return min;
    }

    private int indexBindExp(Query query, Exp exp, boolean isExist) {
        Node qn = exp.getNode();
        int min = qIndex(query, qn);
        if (exp.getNodeList() != null) {
            for (Node bn : exp.getNodeList()) {
                int ii = qIndex(query, bn);
                min = Math.min(min, ii);
            }
        }
        return indexExpFilter(query, exp, isExist);
    }

    private int indexDefaultExp(Query query, Exp exp, boolean isExist, int start) {
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
    int indexExpFilter(Query query, Exp exp, boolean isExist) {
        int min = Integer.MAX_VALUE;
        boolean hasExist = index(query, exp.getFilter());
        List<String> lVar = exp.getFilter().getVariables(true);

        for (String variable : lVar) {
            Node qNode = query.getProperAndSubSelectNode(variable);
            if (qNode == null) {
                // Note: does not work with filter in exists pattern
                // because getProperAndSubSelectNode does not go into exists pattern
                if (!isTriple(exp, variable)) {
                    // no error message for use case:
                    // variable = ?_bn = <<s p o>>
                    logger.warn(Message.Prefix.UNDEF_VAR.getString(), variable);
                    addError(Message.Prefix.UNDEF_VAR.getString(), variable);
                }
            } else if (!isExist && !hasExist) {
                int n = qIndex(query, qNode);
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

    int indexExpQuery(Query query, Exp exp, boolean isExist) {
        int min = Integer.MAX_VALUE;
        Query qq = exp.getQuery();
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
            int n = qIndex(qq, sNode);
            min = Math.min(min, n);

            if (ee.getFilter() != null) {
                index(qq, ee.getFilter());
            }
        }

        qq.complete2();
        return min;
    }

    int indexExpEdge(Query query, Exp exp) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < exp.nbNode(); i++) {
            int n = qIndex(query, exp.getNode(i));
            min = Math.min(min, n);
        }
        return min;
    }


    @SuppressWarnings("java:S3400") // Placeholder method for future RDF-star variable annotations
    boolean isTriple(Exp exp, String name) {
        // RDF-star variable annotations belonged to the legacy parser expression.
        // The native AST represents triple terms explicitly, so a plain variable
        // reference cannot carry that hidden flag.
        return false;
    }

    /**
     * Generate or retrieve index of node
     * If node is in a sub query, return the
     * index of the outer node corresponding to node and rec.
     */
    @SuppressWarnings("java:S1172") // Query parameter preserved for outer query scoping
    int qIndex(Query query, Node node) {
        return index(node);
    }

    /**
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
        return iNode++;
    }

    int globalNodeIndex() {
        return getOuterQuery().getNodeIndex();
    }

    int getNodeIndex() {
        return iNode;
    }

    /**
     *
     * @return list Exp(node) for select * node list
     */
    public List<Exp> toExp(List<Node> lNode) {
        List<Exp> lExp = new ArrayList<>();
        for (Node node : lNode) {
            lExp.add(Exp.create(Type.NODE, node));
        }
        return lExp;
    }



    public List<Node> getArgList() {
        return argList;
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
    boolean index(Query query, Filter f) {
        return index(query, f.getExp());
    }

    /**
     * Looking for filter(exist {})
     */
    boolean index(Query query, Expr exp) {
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

    public Query orderBy(Node node) {
        if (node != null && doesNotContain(getOrderBy(), node)) {
            addOrderBy(node);
        }
        return this;
    }

    public Query groupBy(Node node) {
        if (node != null && doesNotContain(getGroupBy(), node)) {
            addGroupBy(node);
        }
        return this;
    }

    void setSelect(Query q1, Query q2) {
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

    public void setSynchronized(boolean b) {
        isSynchronized = b;
    }

    public boolean isSynchronized() {
        return isSynchronized;
    }

    public void setTransformer(String p, Object transformer) {
        getGlobalQuery().setPPrinter(p, transformer);
    }

    Object getPPrinter(String p) {
        if (p == null) {
            return transformer;
        }
        return tprinter.get(p);
    }

    void setPPrinter(String p, Object transformer) {
        if (p == null) {
            // next kg:pprint() will use this one
            this.transformer = transformer;
        } else {
            if (this.transformer == null) {
                // next kg:pprint() will use this one
                this.transformer = transformer;
            }
            if (! tprinter.containsKey(p)){
                tprinter.put(p, transformer);
            }
        }
    }

    public void setTemplate(boolean template) {
        isTemplate = template;
    }

    public boolean isTemplate() {
        return isTemplate;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public void setNumber(int number) {
        this.number = number;
    }


    public Exp getTemplateGroup() {
        return templateGroup;
    }


    public int getQueryProfile() {
        return queryProfile;
    }


    public boolean isStorePath() {
        return isStorePath;
    }


    public boolean isCachePath() {
        return isCachePath;
    }

    public void setExtension(boolean b) {
        isExtension = b;
    }

    public boolean isExtension() {
        return isExtension;
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
        return "[Query]";
    }


    public boolean isUseBind() {
        return isUseBind;
    }


    public BgpGenerator getBgpGenerator() {
        return bgpGenerator;
    }

    public void setBgpGenerator(BgpGenerator bgpGenerator) {
        this.bgpGenerator = bgpGenerator;
    }


    public boolean isReportEnabled() {
        return getGlobalQuery().reportEnabled;
    }

    public void setReportEnabled(boolean reportEnabled) {
        getGlobalQuery().reportEnabled = reportEnabled;
    }


    public boolean isTransformationTemplate() {
        return isTransformationTemplate;
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

    public boolean isLock() {
        return lock;
    }


    public void setLock(boolean lock) {
        this.lock = lock;
    }


    public boolean isParallel() {
        return parallel;
    }


    public void setParallel(boolean parallel) {
        this.parallel = parallel;
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


    public boolean isAlgebra() {
        return algebra;
    }


    public void setAlgebra(boolean algebra) {
        this.algebra = algebra;
    }

}
