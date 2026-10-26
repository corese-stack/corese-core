package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitor;
import fr.inria.corese.core.next.query.impl.engine.event.ProcessVisitorDefault;
import fr.inria.corese.core.next.query.impl.engine.model.BindingContext;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.ExpType;
import fr.inria.corese.core.next.query.impl.engine.model.Expr;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Loopable;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.MappingSet;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.query.impl.engine.spi.Provider;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.query.impl.engine.event.ResultListener;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.engine.model.ExprType.UNNEST;

/**
 * Backtracking evaluator for compiled SPARQL operator graphs.
 *
 * <p>{@link Stack} holds the pending operators, {@link Memory} owns their node
 * and edge bindings, and {@link Producer} enumerates candidates. Expression
 * evaluation and RDF-term compatibility use the evaluator and matcher contracts.
 * Producer traversals are closed when exhausted or abandoned.</p>
 *
 * <p>Instances and their mutable binding state are confined to the evaluation thread.
 * A timeout watchdog may invoke {@link #finish()} concurrently; cancellation is
 * published through volatile flags in this evaluator and its operator helpers.</p>
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 */
public final class Eval implements ExpType {

    static final int STOP = -2;
    static final Logger logger = LoggerFactory.getLogger(Eval.class);

    // Propagate candidate mappings when evaluating edges.
    private boolean pushEdgeMappings = true;
    // Restrict graph evaluation using incoming solution mappings.
    private boolean parameterGraphMappings = true;
    // Supply incoming mappings independently to both UNION branches.
    private boolean parameterUnionMappings = true;

    private final EvaluationState state = new EvaluationState();
    private final EvalVisitorDispatcher visitors = new EvalVisitorDispatcher();

    // Edge and Node producer
    Producer saveProducer;
    Provider provider;
    // Filter evaluator
    Evaluator evaluator;
    Matcher match;
    // Stacks for binding edges and nodes
    Memory memory;
    QueryPlan query;

    Mappings results;
    // initial results to be completed
    Mappings initialResults;
    List<Node> empty = new ArrayList<>(0);
    EvalGraph graphEvaluator;
    EvalJoin join;
    EvalOptional optional;
    EvalMinus minusEvaluator;
    EvalUnion unionEvaluator;
    EvalService serviceEvaluator;
    EvalValues valuesEvaluator;
    EvalFilter filterEvaluator;
    EvalBind bindEvaluator;
    EvalSubQuery subQueryEvaluator;
    // subeval = false: eval query/subquery
    // subeval = true:  eval statement (union, optional, minus, graph, join)
    boolean isSubEval = false;
    // return only select variables in Mapping
    boolean optim = true;
    // Edge and Node producer
    private Producer producer;
    private Stack current;
    private boolean joinMappings = true;

    /**
     * @param p edge and node producer
     * @param e filter evaluator, given an environment (access to variable
     *          binding)
     */
    public Eval(Producer p, Evaluator e, Matcher m) {
        producer = p;
        saveProducer = producer;
        evaluator = e;
        match = m;
        setVisitor(new ProcessVisitorDefault());
        e.setKGRAM(this);
        graphEvaluator = new EvalGraph(this);
        join = new EvalJoin(this);
        optional = new EvalOptional(this);
        minusEvaluator = new EvalMinus(this);
        unionEvaluator = new EvalUnion(this);
        serviceEvaluator = new EvalService(this);
        valuesEvaluator = new EvalValues(this);
        filterEvaluator = new EvalFilter(this);
        bindEvaluator = new EvalBind(this);
        subQueryEvaluator = new EvalSubQuery(this);

    }

    public static Eval create(Producer p, Evaluator e, Matcher m) {
        return new Eval(p, e, m);
    }

    public boolean isPushEdgeMappings() {
        return pushEdgeMappings;
    }

    public void setPushEdgeMappings(boolean aPushEdgeMappings) {
        pushEdgeMappings = aPushEdgeMappings;
    }

    public boolean isParameterGraphMappings() {
        return parameterGraphMappings;
    }

    public void setParameterGraphMappings(boolean aParameterGraphMappings) {
        parameterGraphMappings = aParameterGraphMappings;
    }

    public boolean isParameterUnionMappings() {
        return parameterUnionMappings;
    }

    public void setParameterUnionMappings(boolean aParameterUnionMappings) {
        parameterUnionMappings = aParameterUnionMappings;
    }

    public void setNewMappingsVersion(boolean b) {
        setPushEdgeMappings(b);
        setParameterGraphMappings(b);
        setParameterUnionMappings(b);
    }

    public void set(Provider p) {
        provider = p;
    }

    public void set(Producer p) {
        setProducer(p);
    }

    public void set(Matcher m) {
        match = m;
    }

    public void set(Evaluator e) {
        evaluator = e;
    }

    /**
     * Main eval function
     */
    public Mappings query(QueryPlan q) {
        return query(null, q, null);
    }

    public Mappings query(Node graphNode, QueryPlan q, Mapping m) {
        return queryBasic(graphNode, q, m);
    }

    Mappings queryBasic(Node graphNode, QueryPlan q, Mapping m) {
        initMemory(q);
        share(m);
        getProducer().start(q);
        try {
            getVisitor().init(q);
            share(getVisitor());
            getVisitor().before(q);
            Mappings map = eval(graphNode, q, m, null);
            getVisitor().orderby(map);
            getVisitor().after(map);
            return map;
        } finally {
            getProducer().finish(q);
        }
    }

    // share global variables and ProcessVisitor
    void share(Mapping m) {
        if (m != null && m.getBind() != null) {
            if (getBind() != null) {
                getBind().share(m.getBind());
            }
            if (m.getBind().getVisitor() != null) {
                // use case: let (?g = construct where)
                // see Interpreter exist() getMapping()
                setVisitor(m.getBind().getVisitor());
            }
        }
    }

    // Propagate a shareable visitor to nested evaluations through their bindings.
    void share(ProcessVisitor vis) {
        if (vis.isShareable() && getBind().getVisitor() == null) {
            getBind().setVisitor(vis);
        }
    }


    /**
     * Mapping m is binding parameter, possibly null
     * Supplied bindings are applied before the main query or subquery is evaluated.
     * Mappings map is results or previous statement, possibly null
     * use case: optional(A, B) map = relevant subset of results of A
     * for main query and subquery
     */
    Mappings eval(Node gNode, QueryPlan q, Mapping m, Mappings map) {
        init(q);
        if (q.isValidate()) {
            // just compile and complete query
            return getResults();
        }

        if (!q.isFail()) {
            query(gNode, q, m, map);
            aggregate();
            // order by
            complete();
        }

        getEvaluator().finish(getMemory());
        return getResults();
    }

    /**
     * Mapping m is binding parameter, possibly null
     * Supplied bindings are applied before the main query or subquery is evaluated.
     * Mappings map is results or previous statement, possibly null
     * use case: optional(A, B) map = relevant subset of results of A
     */
    @SuppressWarnings("java:S1845") // QueryPlan execution method named query by convention
    void query(Node gNode, QueryPlan q, Mapping m, Mappings map) {
        if (m != null) {
            // bind mapping variables into memory
            bind(m);
        }
        if (q.getValues() == null) {
            // no external values
            eval(gNode, q, map);
        } else {
            // there is binding parameter (m and/or map)
            // Mapping m is bound in memory, keep it, mappings map is passed as eval parameter
            // bind external values one by one in memory and eval one by one
            queryWithValues(gNode, q, map);
        }
    }

    /**
     * Bind external values one by one in memory and eval one by one
     */
    void queryWithValues(Node gNode, QueryPlan q, Mappings map) {
        Exp values = q.getValues();

        if (!values.isPostpone()) {
            for (Mapping m : values.getMappings()) {
                if (state.isStopped()) {
                    return;
                }
                if (valuesBinding(values.getNodeList(), m, -1)) {
                    eval(gNode, q, map);
                    free(values.getNodeList(), m);
                }
            }
            return;
        }
        eval(gNode, q, map);
    }

    void eval(Node gNode, QueryPlan q, Mappings map) {
        evalExp(gNode, q.getBody(), map);
    }
    void evalExp(Node gNode, Exp exp, Mappings map) {
        Stack stack = Stack.create(exp);
        set(stack);
        eval(getProducer(), gNode, stack, map, 0);
    }

    public void setMappings(Mappings lMap) {
        initialResults = lMap;
    }

    /**
     * main subEval function
     * evaluates exp in new Eval with new Memory
     * for optional, minus, union, join, graph
     * not for subquery which has its own processing
     */
    Mappings subEval(QueryPlan q, Node gNode, Stack stack, Mappings map, int n) {
        setSubEval(true);
        starter(q);
        eval(getProducer(), gNode, stack, map, n);

        return getResults();
    }

    /**
     * Eval exp in a fresh new Memory where exp is part of main expression
     * use case: main = optional, minus, union, join
     * Node gNode : actual graph node
     * Node queryNode : exp query graph node
     * use case: union
     */

    // use case: optional, join, minus
    public Mappings subEval(Producer p, Node gNode, Node queryNode, Exp exp, Exp main, Mappings map) {
        return subEval(p, gNode, queryNode, exp, main, map, null, false, false);
    }

    /**
     * Evaluates an EXISTS graph pattern against the bindings of the current solution.
     * The nested evaluation receives a fresh memory while copying every currently
     * bound query node from the active environment, so correlated variables retain
     * SPARQL EXISTS semantics even when evaluated inside an OPTIONAL or aggregate context.
     */
    public boolean exists(Producer producer, Node graphNode, Environment env, Exp pattern) {
        Memory nestedMemory = new Memory(match, evaluator);
        evaluator.init(nestedMemory);
        nestedMemory.init(getQuery());
        switch (env) {
            case Mapping map -> nestedMemory.copy(map, pattern);
            case Memory mem -> mem.copyInto(nestedMemory, pattern);
            case null, default -> getMemory().copyInto(nestedMemory, pattern);
        }

        Eval nested = copy(nestedMemory, producer);
        Mappings mappings = nested.subEval(
                getQuery(), graphNode, Stack.create(pattern), null, 0);
        return !mappings.isEmpty();
    }

    /**
     * external = false : graphNode is named graph URI or null, queryGraphNode is meaningless
     * external = true :  graphNode is external graph,
     * queryGraphNode: named graph variable if any or null
     * graphNode: if ext=true & queryNode=null -> graphNode can be null
     * external graph: external graph in GraphStore, PPath or Node graph pointer
     * When external=true: Producer p is new Producer(externalGraph)
     * exp is current exp to evaluate in new Eval with new Memory
     * main is embedding statement of exp (main = A optional B, exp = A | exp = B)
     * map and m are possible bindings stemming from previous statement evaluation
     */
    @SuppressWarnings("java:S107") // Internal KGRAM sub-evaluation method requires 9 execution parameters
    Mappings subEval(Producer p, Node graphNode, Node queryGraphNode, Exp exp, Exp main,
                     Mappings map, Mapping m, boolean bind, boolean external) {
        Memory mem = new Memory(match, getEvaluator());
        getEvaluator().init(mem);
        // share Binding (global variable, context, etc.)
        mem.share(getMemory());
        mem.init(getQuery());
        Eval eval = copy(mem, p);
        if (external) {
            if (queryGraphNode != null) {
                // graph ?queryNode { } -> bind ?queryNode = graphNode for further use
                // usually we do not bind named graph variable before
                // statement evaluation to preserve sparql semantics
                // external graph is an extension to sparql
                mem.push(queryGraphNode, graphNode, -1);
            }
            // named graph is external graph, not URI ->
            // do not pass graphNode as named graph URI
            // Producer p is bound to external named graph
            graphNode = null;
        }
        bind(mem, exp, main, m, bind);
        return eval.subEval(getQuery(), graphNode, Stack.create(exp), map, 0);
    }

    /**
     * subEval with bind parameters
     * freshMemory inherits data to evaluate exp
     */
    void bind(Memory freshMemory, Exp exp, Exp main, Mapping m, boolean bind) {
        if (m != null) {
            freshMemory.push(m, -1);
        }

        // QueryPlanner may have computed exp node list candidate for binding
        if (main.isGraph() && main.getNodeList() != null) {
            bindExpNodeList(freshMemory, main, main.getGraphName());
        } else if ((bind || main.isBinary()) && exp.getNodeList() != null) {
            // A optional B
            // bind variables of A from environment
            bindExpNodeList(freshMemory, exp, null);
        }

        bindService(freshMemory, exp, main);
    }

    /**
     * Bind relevant nodes of exp from memory to fresh memory
     * except may be a graphNode: do not bind it here
     * because it is bound somewhere else by subEval
     * getNodeList() is computer by QueryPlanner
     */
    void bindExpNodeList(Memory freshMemory, Exp exp, Node except) {
        for (Node qnode : exp.getNodeList()) {
            // getOuterNodeSelf use case: join(subquery, exp)  -- federated query use case
            if (except == null || qnode != except) {
                Node myqnode = getMemory().getQuery().getOuterNodeSelf(qnode);
                Node node = getMemory().getNode(myqnode);
                if (node != null) {
                    freshMemory.push(qnode, node, -1);
                }
            }
        }
    }

    /**
     * Bind service variable if any
     */
    void bindService(Memory freshMemory, Exp exp, Exp main) {
        if (main.type() == Type.JOIN) {
            service(exp, freshMemory);
        }
    }

    /**
     * special case for service variable
     * JOIN(service ?s {}, exp) if ?s is bound in main memory,
     * bind it in subeval memory
     */
    void service(Exp exp, Memory freshMemory) {
        if (exp.type() == Type.SERVICE) {
            bindService(exp, freshMemory);
        } else {
            for (Exp ee : exp.getExpList()) {
                Type type = ee.type();
                if (type == Type.SERVICE) {
                    bindService(ee, freshMemory);
                } else if (type == Type.AND || type == Type.BGP || type == Type.JOIN) {
                    service(ee, freshMemory);
                }
            }
        }
    }

    // bind service variable for further use
    void bindService(Exp exp, Memory freshMemory) {
        Node serv = exp.first().getNode();
        if (serv.isVariable() && getMemory().isBound(serv)) {
            freshMemory.push(serv, getMemory().getNode(serv));
        }
    }

    /**
     * Copy current evaluator to eval subquery same memory (share bindings) new
     * exp stack
     */
    public Eval copy(Memory m, Producer p) {
        return copy(m, p, getEvaluator(), false);
    }

    // extern = true if the statement to evaluate is LDScript query:
    // let (select where)
    public Eval copy(Memory m, Producer p, boolean extern) {
        return copy(m, p, getEvaluator(), extern);
    }

    Eval copy(Memory m, Producer p, Evaluator e, boolean extern) {
        Eval ev = create(p, e, getMatcher());
        ev.pushEdgeMappings = pushEdgeMappings;
        ev.parameterGraphMappings = parameterGraphMappings;
        ev.parameterUnionMappings = parameterUnionMappings;
        ev.joinMappings = joinMappings;
        ev.setMemory(m);
        ev.set(getProvider());
        if (!extern || getVisitor().isShareable()) {
            ev.setVisitor(getVisitor());
        }
        ev.startExtFun();
        return ev;
    }

    /**
     * copy memory for sub query copy sub query select variables that are
     * already bound in current memory Use case: subquery and exists
     */
    Memory copyMemory(Memory memory, QueryPlan query, QueryPlan sub, Exp exp) {
        Memory mem = new Memory(match, evaluator);
        getEvaluator().init(mem);
        if (sub == null) {
            mem.init(query);
        } else {
            mem.init(sub);
        }
        memory.copyInto(sub, mem, exp);
        return mem;
    }


    public void setSubEval(boolean b) {
        isSubEval = b;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory mem) {
        memory = mem;
    }

    public BindingContext getBinding() {
        return getEnvironment().getBind();
    }

    public BindingContext getBind() {
        return getEnvironment().getBind();
    }

    public QueryPlan getQuery() {
        return query;
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    public Matcher getMatcher() {
        return match;
    }

    public void setMatcher(Matcher m) {
        match = m;
    }

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    public Provider getProvider() {
        return provider;
    }

    public Environment getEnvironment() {
        return memory;
    }

    public Mappings getResults() {
        return results;
    }

    // total init (for global query)
    public void init(QueryPlan q) {
        initMemory(q);
        start(q);
    }

    void initMemory(QueryPlan q) {
        if (memory == null) {
            // when subquery, memory is already assigned
            // assign stack index to EDGE and NODE
            q.complete(getProducer());//service while1 / QueryPlan
            memory = new Memory(match, evaluator);
            memory.setEval(this);
            getEvaluator().init(memory);
            // create memory bind stack
            memory.init(q);
            getProducer().init(q);
            evaluator.start(memory);
        }
    }

    // partial init (for global query and subqueries)
    public void start(QueryPlan q) {
        state.setLimit(q.getLimitOffset());
        // ASK only needs its first solution after OFFSET. Keep the requested
        // QueryPlan modifiers intact, and evaluate fully when sorting/grouping needs it.
        if (q.isAsk() && !q.isOrderBy() && !q.hasGroupBy() && !q.isAggregate()) {
            state.setLimit((int) Math.min(state.getLimit(), (long) q.getOffset() + 1));
        }
        starter(q);
    }

    public void setLimit(int n) {
        state.setLimit(n);
    }

    public int getLimit() {
        return state.getLimit();
    }

    // for sub exp
    private void starter(QueryPlan q) {
        query = q;
        // create result holder
        if (initialResults != null) {
            results = initialResults;
        } else {
            results = Mappings.create(query, isSubEval);
        }
        startExtFun();
        // set new results in case of sub query (for aggregates)
        memory.setEval(this);
        memory.setResults(results);
    }

    void startExtFun() {
        visitors.updateVisitorFlags();
    }

    private void complete() {
        results.complete(this);
    }

    private void aggregate() {
        results.aggregate(evaluator, memory, getProducer());
    }

    /**
     * We can bind nodes before processing query
     */
    void bind(Node qnode, Node node) {
        memory.push(qnode, node, -1);
    }

    /**
     * Bind select nodes of Mapping to [select] nodes of query
     */
    void bind(Mapping map) {
        for (Node qNode : map.getSelectQueryNodes()) {
            Node qqNode = query.getOuterNode(qNode);
            if (qqNode != null) {
                Node node = map.getNode(qNode);
                if (node != null) {
                    bind(qqNode, node);
                }
            }
        }
    }

    private int solution(Producer p, Mapping m, int n) {
        int backtrack = n - 1;
        int status = store(p, m);
        if (status == STOP) {
            return STOP;
        }
        if (results.size() >= state.getLimit()) {
            // backjump to send finish events to listener
            // and perform 'close instructions' if any
            return STOP;
        }
        if (!getVisitor().limit(results)) {
            return STOP;
        }
        if (state.hasBackjump()) {
            if (!isSubEval && optim) {
                // use case: select distinct ?x where
                // backjump where ?x is defined to get a new one
                backtrack = state.getBackjump();
                state.clearBackjump();
            }
        } else if (query.isDistinct() && !query.isDistinctRedundantWithGroupBy() && !isSubEval && optim) {
            int index = memory.getIndex(query.getSelect());
            if (index != -1) {
                backtrack = index;
            }
        }
        return backtrack;
    }

    /**
     * gNode is a graph name URI if any, may be null
     */
    int eval(Producer p, Node gNode, Stack stack, int n) {
        return eval(p, gNode, stack, null, n);
    }

    /**
     * Mappings map, possibly null, is result of previous expression that may be used to evaluate current exp
     * optional(s p o, o q r)
     * map = eval(s p o); eval(o q r, map) -> can use o in map
     * map is relevant subset of result, projected on relevant subset of variables of right expression
     * special cases:
     * 1) when there is no relevant subset of results wrt variables, map=full result in case exp is a union
     * 2) relevant subset of result contains full result in case exp is a union
     * case union: each branch of union select its own relevant subset of results from full result
     * Mappings map is recursively passed as parameter until one exp can use it
     * It can be passed recursively through several statements: join(A, optional(union(B, C), D))
     * Eventually, and() edge() path() transform Mappings map into values clause
     */
    @SuppressWarnings("java:S3776") // Core KGRAM evaluation dispatch loop coordinating stack traversal and backtracking
    int eval(Producer p, Node graphNode, Stack stack, Mappings map, int n) {
        int backtrack = n - 1;
        Memory env = getMemory();

        state.incrementCall();

        if (n >= stack.size()) {
            backtrack = solution(p, null, n);
            return backtrack;
        }

        state.updateMaxLevel(n);

        Exp exp = stack.get(n);
        if (visitors.hasListener()) {
            exp = visitors.listen(exp, n);
        }

        // A false filter was detected at compile time, or this expression was
        // identified as always failing: no use to evaluate it.
        if (!exp.isFail()) {
            if (visitors.hasStatement()) {
                getVisitor().statement(this, getGraphNode(graphNode), exp);
            }

            if (!isJoinMappings()) {
                // for testing and debug
                map = null;
            }

            switch (exp.type()) {

                case EMPTY:

                    eval(p, graphNode, stack, n + 1);
                    break;

                case AND:
                    backtrack = and(p, graphNode, exp, stack, map, n);
                    break;

                case BGP:
                    backtrack = p.evaluatesBGPWithEdges()
                            ? and(p, graphNode, exp, stack, map, n)
                            : bgp(p, graphNode, exp, stack, n);
                    break;

                case SERVICE:
                    // @note: map processing is not optimal for service with union
                    // we pass mappings only for variables that are in-scope in
                    // both branches of the union
                    // it can be bypassed with values var (undef)
                    backtrack = serviceEvaluator.eval(p, graphNode, exp, map, stack, n);
                    break;

                case GRAPH:
                    backtrack =
                            graphEvaluator.eval(p, graphNode, exp, map, stack, n);
                    break;

                case UNION:
                    backtrack = unionEvaluator.eval(p, graphNode, exp, map, stack, n);
                    break;

                case OPTIONAL:
                    backtrack = optional.eval(p, graphNode, exp, map, stack, n);
                    break;
                case MINUS:
                    backtrack = minusEvaluator.eval(p, graphNode, exp, map, stack, n);
                    break;
                case JOIN:
                    backtrack = join.eval(p, graphNode, exp, map, stack, n);
                    break;
                case QUERY:
                    backtrack = subQueryEvaluator.eval(p, graphNode, exp, map, stack, n);
                    break;
                case FILTER:
                    backtrack = filterEvaluator.eval(p, graphNode, exp, stack, n);
                    break;
                case BIND:
                    backtrack = bindEvaluator.eval(p, graphNode, exp, map, stack, n);
                    break;


                case EDGE:
                    backtrack = edge(p, graphNode, exp, map, stack, n);
                    break;

                case VALUES:
                    backtrack = valuesEvaluator.eval(p, graphNode, exp, stack, n);
                    break;

                case OPT_BIND:
                    // Binding constraints introduced by the filter optimizer.
                    backtrack = bindEvaluator.optBind(p, graphNode, exp, stack, n);
                    break;

                case ACCEPT:
                    // use case: select distinct ?x where
                    // check that ?x is distinct
                    if (optim) {
                        if (getResults().accept(env.getNode(exp.getNode()))) {
                            // backjump here when a mapping will be found with this node
                            // see store()
                            state.setBackjump(n - 1);
                            backtrack = eval(p, graphNode, stack, n + 1);
                        }
                    } else {
                        backtrack = eval(p, graphNode, stack, n + 1);
                    }
                    break;

                default:
                    break;
            }
        }

        return backtrack;
    }

    private int and(Producer p, Node graphNode, Exp exp, Stack stack, Mappings data, int n) {
        getVisitor().bgp(this, getGraphNode(graphNode), exp, null);

        if (data != null && exp.size() > 0 && exp.get(0).isEdgePath()) {
            // pass Mappings data as values clause
            exp = exp.complete(data);
            stack = stack.and(exp, n);
            // Mappings data is in stack, not in parameter
            return eval(p, graphNode, stack, n);
        } else {
            stack = stack.and(exp, n);
            // pass Mappings data as parameter
            return eval(p, graphNode, stack, data, n);
        }
    }

    private int bgp(Producer p, Node graphNode, Exp exp, Stack stack, int n) {
        int backtrack = n - 1;
        List<Node> from = getQuery().getFrom(graphNode);
        Mappings map = p.getMappings(graphNode, from, exp, getMemory());

        for (Mapping m : map) {
            if (state.isStopped()) {
                return STOP;
            }
            m.fixQueryNodes(getQuery());
            boolean b = getMemory().push(m, n, false);
            if (b) {
                int back = eval(p, graphNode, stack, n + 1);
                getMemory().pop(m);
                if (back < n) {
                    return back;
                }
            }
        }
        return backtrack;
    }

    Node getNode(Producer p, Node gNode) {
        if (gNode.isConstant()) {
            return p.getNode(gNode);
        }
        return getMemory().getNode(gNode);
    }

    Node getGraphNode(Node node) {
        return node;
    }

    boolean isFederate(Exp exp) {
        return unionEvaluator.isFederate(exp);
    }

    boolean valuesBinding(List<Node> varList, Mapping map, int n) {
        return valuesEvaluator.valuesBinding(varList, map, n);
    }

    void free(List<Node> varList, Mapping map) {
        valuesEvaluator.free(varList, map);
    }

    boolean test(Node graphNode, Filter f, Environment env, Producer p) {
        return filterEvaluator.test(graphNode, f, env, p);
    }

    public Node eval(Node graphNode, Filter f, Environment env, Producer p) {
        return filterEvaluator.eval(graphNode, f, env, p);
    }

    public DatatypeValue eval(Filter f, Environment env, Producer p) {
        return filterEvaluator.eval(f, env, p);
    }

    public DatatypeValue eval(Expr e, Environment env, Producer p) {
        return filterEvaluator.eval(e, env, p);
    }

    public Mappings eval(Filter f, Environment env, List<Node> nodes) {
        return filterEvaluator.eval(f, env, getProducer(), nodes);
    }

    public Mappings eval(Filter f, Environment env, Producer p, List<Node> nodes) {
        return filterEvaluator.eval(f, env, p, nodes);
    }

    /**
     * Enumerate candidate edges
     * exp = query edge
     * graphNode = null for default graph triple pattern
     * graphNode = URI of named graph for named graph triple pattern
     * graph ?g { exp } evaluates exp for each value of ?g = g_i
     * in this case graphNode = g_i and from is useless here
     * from named has been used in named graph pattern evaluator
     * data is possible relevant bindings coming from preceding statement evaluation
     * data may be null
     */
    @SuppressWarnings("java:S3776") // Core KGRAM edge evaluation algorithm managing graph matching and backjump optimization
    private int edge(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) {
        int backtrack = n - 1;
        boolean isSuccess = false;
        boolean hasGraphNode = graphNode != null;
        Edge qEdge = exp.getEdge();
        Node graph;
        Memory env = getMemory();
        // used by RuleEngine ResultWatcher ProducerImpl
        env.setExp(exp);
        QueryPlan qq = getQuery();
        // for default graph triple pattern, consider select from if any
        // for named graph triple pattern, from named is useless here because graphNode = URI of one named graph
        List<Node> graphNodeList = qq.getFrom(graphNode);
        // Graph binding used to determine the backtracking level.
        Node backtrackGraphNode = graphNode;
        boolean matchNBNode = qEdge.isMatchArity();

        if (data != null && data.getNodeList() != null && isPushEdgeMappings()) {
            // push values(data) before edge in stack
            return eval(p, graphNode, stack.addCopy(n, exp.getValues(data)), n);
        }

        if (p.getMode() == Producer.EXTENSION) {
            // Producer is Extension only for the query that created it
            // use case: templates may share same Producer
            if (p.getQuery() == env.getQuery()) {
                // special case graph ?g { exp } where ?g is external graph
                // forget from and from named if any
                graphNodeList = empty;
                backtrackGraphNode = p.getGraphNode();
            } else {
                backtrackGraphNode = null;
            }
        }

        Iterable<Edge> entities;
        if (visitors.hasProduce()) {
            // A query visitor may provide candidates before the producer is consulted.
            entities = produce(graphNode, qEdge);
            if (entities == null) {
                entities = p.getEdges(graphNode, graphNodeList, qEdge, env);
            }
        } else {
            entities = p.getEdges(graphNode, graphNodeList, qEdge, env);
        }

        try {
            for (Edge entity : entities) {

                if (state.isStopped()) {
                    return STOP;
                }

                if (entity != null) {
                    state.incrementEdge();
                    if (visitors.hasListener() && !visitors.listen(exp, qEdge, entity)) {
                        continue;
                    }

                    graph = entity.getGraph();
                    boolean bmatch = match(qEdge, entity, graphNode, graph, env);

                    if (matchNBNode) {
                        bmatch &= (qEdge.nbNode() == entity.nbNode());
                    }

                    if (bmatch) {
                        if (visitors.hasCandidate()) {
                            DatatypeValue dt = getVisitor().candidate(this, getGraphNode(graphNode), qEdge, entity);
                            if (dt != null) {
                                bmatch = dt.isTrue();
                            }
                        }

                        bmatch &= push(p, qEdge, entity, n);
                    }

                    if (bmatch) {
                        isSuccess = true;
                        backtrack = eval(p, graphNode, stack, n + 1);

                        env.pop(qEdge);
                        if (hasGraphNode) {
                            env.pop(graphNode);
                        }

                        if (backtrack < n) {
                            return backtrack;
                        }
                    }
                }
            }
        } finally {
            p.closeEdges(entities);
        }

        if (!isSuccess && optim) {
            // compute index in the stack of exp where to backtrack
            // backtrack index is the highest index in the stack among edge node index
            // we backtrack as much as possible in the exp stack
            backtrack = env.getIndex(backtrackGraphNode, qEdge);
        }

        return backtrack;
    }

    /**
     * Skip Mappings with null nodeList for subquery and service
     */
    Mappings selectQueryMappings(Mappings data) {
        if (data != null) {
            if (data.getNodeList() == null) {
                // no variable in-scope wrt select clause
                return null;
            } else {
                // forget original join Mappings (in case of union in body)
                // because original Mappings may not fit the select clause
                data.setJoinMappings(null);
            }
        }
        return data;
    }

    /**
     * Store a new result
     */
    private int store(Producer p, Mapping m) {
        boolean store = true;
        if (getListener() != null) {
            store = getListener().process(getMemory());
        }
        if (store) {
            state.incrementResult();
        }
        if (store) {
            Mapping ans = m;
            if (m == null) {
                ans = getMemory().store(getQuery(), p, isSubEval);
            }
            store(ans);
        }
        return -1;
    }

    void store(Mapping ans) {
        if (ans != null && acceptable(ans)) {
            boolean b = true;
            if (!isSubEval) {
                b = getVisitor().distinct(this, getQuery(), ans);
                if (b) {
                    b = getVisitor().result(this, getResults(), ans);
                }
            }
            if (b) {
                getResults().add(ans);
            }
        }
    }

    boolean acceptable(Mapping m) {
        return getResults().acceptable(m);
    }


    public int getCount() {
        return state.getNbEdge();
    }

    private boolean match(Edge qEdge, Edge edge, Node gNode, Node graphNode, Memory memory) {
        if (!getMatcher().match(qEdge, edge, memory)) {
            return false;
        }
        if (gNode == null || graphNode == null) {
            return true;
        }
        return getMatcher().match(gNode, graphNode, memory);
    }

    private boolean push(Producer p, Edge qEdge, Edge ent, int n) {
        Memory env = getMemory();
        return env.push(p, qEdge, ent, n);
    }


    void set(Stack current) {
        this.current = current;
    }

    public Stack getStack() {
        return current;
    }

    public ProcessVisitor getVisitor() {
        return visitors.getVisitor();
    }

    public void setVisitor(ProcessVisitor visitor) {
        visitors.setVisitor(visitor);
    }

    public boolean isStop() {
        return state.isStopped();
    }

    public void setStop(boolean stopped) {
        state.setStopped(stopped);
    }




    /**
     * Requests cooperative termination, including active join, graph, and optional
     * evaluation. May be called by the timeout watchdog; remaining evaluation state
     * must only be accessed by the owning execution thread.
     */
    public void finish() {
        state.stop();
        join.setStop();
        graphEvaluator.setStop();
        optional.setStop(true);
    }

    /**
     * Draft extension where a Visitor provides Edge iterator
     */
    Iterable<Edge> produce(Node gNode, Edge edge) {
        return visitors.produce(this, gNode, edge);
    }

    public boolean hasFilter() {
        return visitors.hasFilter();
    }

    public boolean isJoinMappings() {
        return joinMappings;
    }

    public ResultListener getListener() {
        return visitors.getListener();
    }

    public void setListener(ResultListener listener) {
        visitors.setListener(listener);
    }

    public EvaluationState getState() {
        return state;
    }

    public EvalVisitorDispatcher getVisitorDispatcher() {
        return visitors;
    }

}
