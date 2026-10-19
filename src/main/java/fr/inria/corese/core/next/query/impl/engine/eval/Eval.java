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
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.MappingSet;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Matcher;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.query.impl.engine.spi.Provider;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.query.impl.engine.event.Event;
import fr.inria.corese.core.next.query.impl.engine.event.EventImpl;
import fr.inria.corese.core.next.query.impl.engine.event.KgramEventDispatcher;
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

    KgramEventDispatcher manager;
    boolean hasEvent = false;
    // Edge and Node producer
    Producer saveProducer;
    Provider provider;
    // Filter evaluator
    Evaluator evaluator;
    Matcher match;
    // Stacks for binding edges and nodes
    Memory memory;
    Query query;

    Mappings results;
    // initial results to be completed
    Mappings initialResults;
    List<Node> empty = new ArrayList<>(0);
    EvalGraph graphEvaluator;
    EvalJoin join;
    EvalOptional optional;
    int // count number of eval() calls
            nbEdge = 0;
    int nbCall = 0;
    int backjump = -1;
    int maxLevel = -1;
    int limit = Integer.MAX_VALUE;
    // subeval = false: eval query/subquery
    // subeval = true:  eval statement (union, optional, minus, graph, join)
    boolean isSubEval = false;
    // return only select variables in Mapping
    boolean optim = true;
    boolean storeResult = true;
    boolean hasFilter = false;
    private ResultListener listener;
    private ProcessVisitor visitor;
    // Edge and Node producer
    private Producer producer;
    private Stack current;
    private boolean hasListener = false;
    private int nbResult;
    private boolean hasCandidate = false;
    private boolean hasStatement = false;
    private boolean hasProduce = false;
    private volatile boolean stopped = false;
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
    public Mappings query(Query q) {
        return query(null, q, null);
    }

    public Mappings query(Node graphNode, Query q, Mapping m) {
        return queryBasic(graphNode, q, m);
    }

    Mappings queryBasic(Node graphNode, Query q, Mapping m) {
        if (hasEvent) {
            send(Event.BEGIN, q);
        }
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
            if (hasEvent) {
                send(Event.END, q, map);
            }
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
    Mappings eval(Node gNode, Query q, Mapping m, Mappings map) {
        init(q);
        if (q.isValidate()) {
            // just compile and complete query
            return getResults();
        }
        if (q.isCheck()) {
            QueryDefinitionChecker check = QueryDefinitionChecker.create(this);
            check.check(q);
        }

        if (!q.isFail()) {
            query(gNode, q, m, map);

            if (q.getQueryProfile() == Query.COUNT_PROFILE) {
                countProfile();
            } else {
                aggregate();
                // order by
                complete();
            }
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
    @SuppressWarnings("java:S1845") // Query execution method named query by convention
    void query(Node gNode, Query q, Mapping m, Mappings map) {
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
    void queryWithValues(Node gNode, Query q, Mappings map) {
        Exp values = q.getValues();

        if (!values.isPostpone()) {
            for (Mapping m : values.getMappings()) {
                if (stopped) {
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

    void eval(Node gNode, Query q, Mappings map) {
        evalExp(gNode, q.getBody(), map);
    }
    void evalExp(Node gNode, Exp exp, Mappings map) {
        Stack stack = Stack.create(exp);
        set(stack);
        eval(getProducer(), gNode, stack, map, 0);
    }

    /**
     * We just counted number of results: nbResult Just build a Mapping
     */
    void countProfile() {
        Node n = NodeImpl.forValue(Values.factory().createLiteral(nbResult));
        Mapping m = Mapping.create(getQuery().getSelectFun().getFirst().getNode(), n);
        getResults().add(m);
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
    Mappings subEval(Query q, Node gNode, Stack stack, Mappings map, int n) {
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
        if (hasEvent) {
            ev.setEventManager(manager);
        }
        return ev;
    }

    /**
     * copy memory for sub query copy sub query select variables that are
     * already bound in current memory Use case: subquery and exists
     */
    private Memory copyMemory(Memory memory, Query query, Query sub, Exp exp) {
        Memory mem = new Memory(match, evaluator);
        getEvaluator().init(mem);
        if (sub == null) {
            mem.init(query);
        } else {
            mem.init(sub);
        }
        memory.copyInto(sub, mem, exp);
        if (hasEvent) {
            memory.setEventManager(manager);
        }
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

    public Query getQuery() {
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
    public void init(Query q) {
        initMemory(q);
        start(q);
        profile(q);
    }

    void initMemory(Query q) {
        if (memory == null) {
            // when subquery, memory is already assigned
            // assign stack index to EDGE and NODE
            q.complete(getProducer());//service while1 / Query
            memory = new Memory(match, evaluator);
            memory.setEval(this);
            getEvaluator().init(memory);
            // create memory bind stack
            memory.init(q);
            if (hasEvent) {
                memory.setEventManager(manager);
            }
            getProducer().init(q);
            evaluator.start(memory);
        }
    }

    void profile(Query q) {
        // select count(*) as ?c where pattern
        // do not built Mapping, just count them
        if (q.getQueryProfile() == Query.COUNT_PROFILE) {
            storeResult = false;
        }
    }

    // partial init (for global query and subqueries)
    public void start(Query q) {
        limit = q.getLimitOffset();
        // ASK only needs its first solution after OFFSET. Keep the requested
        // Query modifiers intact, and evaluate fully when sorting/grouping needs it.
        if (q.isAsk() && !q.isOrderBy() && !q.hasGroupBy() && !q.isAggregate()) {
            limit = (int) Math.min(limit, (long) q.getOffset() + 1);
        }
        starter(q);
    }

    public void setLimit(int n) {
        limit = n;
    }

    // for sub exp
    private void starter(Query q) {
        query = q;
        // create result holder
        if (initialResults != null) {
            results = initialResults;
        } else {
            results = Mappings.create(query, isSubEval);
        }
        if (hasEvent) {
            results.setEventManager(manager);
        }
        startExtFun();
        // set new results in case of sub query (for aggregates)
        memory.setEval(this);
        memory.setResults(results);
    }

    void startExtFun() {
        hasStatement = getVisitor().statement();
        hasProduce = getVisitor().produce();
        hasCandidate = getVisitor().candidate();
        hasFilter = getVisitor().filter();
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
        if (results.size() >= limit) {
            // backjump to send finish events to listener
            // and perform 'close instructions' if any
            return STOP;
        }
        if (!getVisitor().limit(results)) {
            return STOP;
        }
        if (backjump != -1) {
            if (!isSubEval && optim) {
                // use case: select distinct ?x where
                // backjump where ?x is defined to get a new one
                backtrack = backjump;
                backjump = -1;
            }
        } else if ((query.isDistinct()) && (!isSubEval && optim)) {
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
        boolean isEvent = hasEvent;
        Memory env = getMemory();

        nbCall++;

        if (n >= stack.size()) {
            backtrack = solution(p, null, n);
            return backtrack;
        }

        if (n > maxLevel) {
            maxLevel = n;
        }

        Exp exp = stack.get(n);
        if (hasListener) {
            // rule engine may have a ResultWatcher listener
            exp = getListener().listen(exp, n);
        }

        if (isEvent) {
            send(Event.START, exp, graphNode, stack);
        }

        // A false filter was detected at compile time, or this expression was
        // identified as always failing: no use to evaluate it.
        if (!exp.isFail()) {
            if (hasStatement) {
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
                    backtrack = service(p, graphNode, exp, map, stack, n);
                    break;

                case GRAPH:
                    backtrack =
                            graphEvaluator.eval(p, graphNode, exp, map, stack, n);
                    break;

                case UNION:
                    backtrack = union(p, graphNode, exp, map, stack, n);
                    break;

                case OPTIONAL:
                    backtrack = optional.eval(p, graphNode, exp, map, stack, n);
                    break;
                case MINUS:
                    backtrack = minus(p, graphNode, exp, map, stack, n);
                    break;
                case JOIN:
                    backtrack = join.eval(p, graphNode, exp, map, stack, n);
                    break;
                case QUERY:
                    backtrack = query(p, graphNode, exp, map, stack, n);
                    break;
                case FILTER:
                    backtrack = filter(p, graphNode, exp, stack, n);
                    break;
                case BIND:
                    backtrack = bind(p, graphNode, exp, map, stack, n);
                    break;


                case EDGE:
                    backtrack = edge(p, graphNode, exp, map, stack, n);
                    break;

                case VALUES:
                    backtrack = values(p, graphNode, exp, stack, n);
                    break;

                case OPT_BIND:
                    // Binding constraints introduced by the filter optimizer.
                    backtrack = optBind(p, graphNode, exp, stack, n);
                    break;

                case ACCEPT:
                    // use case: select distinct ?x where
                    // check that ?x is distinct
                    if (optim) {
                        if (getResults().accept(env.getNode(exp.getNode()))) {
                            // backjump here when a mapping will be found with this node
                            // see store()
                            backjump = n - 1;
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

        if (isEvent) {
            send(Event.FINISH, exp, graphNode, stack);
        }

        return backtrack;
    }

    private int minus(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) {
        int backtrack = n - 1;
        Memory env = getMemory();

        Mappings map1 = subEval(p, graphNode, null, exp.first(), exp, data);
        if (stopped) {
            return STOP;
        }
        if (map1.isEmpty()) {
            return backtrack;
        }

        MappingSet set1 = new MappingSet(map1);
        Exp rest = exp.rest();
        Mappings minusMappings = set1.prepareMappingsRest(rest);

        Mappings map2 = subEval(p, graphNode, null, rest, exp, minusMappings);

        getVisitor().minus(this, getGraphNode(graphNode), exp, map1, map2);

        MappingSet set = new MappingSet(exp, set1, new MappingSet(map2));
        set.start();

        for (Mapping map : map1) {
            if (stopped) {
                return STOP;
            }
            boolean ok = !set.minusCompatible(map);
            if ((ok) && (env.push(map, n))) {
                backtrack = eval(p, graphNode, stack, n + 1);
                env.pop(map);
                if (backtrack < n) {
                    return backtrack;
                }
            }
        }
        return backtrack;
    }

    boolean isFederate(Exp exp) {
        if (getQuery().getGlobalQuery().isFederate()) {
            return true;
        }
        return exp.isRecFederate();
    }

    private int union(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) {
        int backtrack = n - 1;
        // join(A, union(B, C)) ; map = eval(A).distinct(inscopenodes())

        Mappings map1 = unionBranch(p, graphNode, exp.first(), exp, data);
        if (stopped) {
            return STOP;
        }
        Mappings map2 = unionBranch(p, graphNode, exp.rest(), exp, data);

        getVisitor().union(this, getGraphNode(graphNode), exp, map1, map2);
        Mappings alternatives = new Mappings();
        alternatives.add(map1);
        alternatives.add(map2);
        return evalUnionMappings(p, graphNode, stack, n, alternatives, backtrack);
    }

    /** Continues the enclosing query once for every solution of a UNION branch. */
    private int evalUnionMappings(
            Producer p, Node graphNode, Stack stack, int n, Mappings mappings, int backtrack) {
        Memory environment = getMemory();
        for (Mapping mapping : mappings) {
            if (stopped) {
                return STOP;
            }
            if (environment.push(mapping, n, false)) {
                backtrack = eval(p, graphNode, stack, n + 1);
                environment.pop(mapping);
                if (backtrack < n) {
                    return backtrack;
                }
            }
        }
        return backtrack;
    }

    /**
     * Eval one exp of union main map is partial solution Mappings resulting
     * from previous statement evaluation, typically eval(A) in join(A, union(B,
     * C)) In federated case (or if exp is itself a union), map is passed to
     * subEval(exp, map), it may be taken into account by service in exp In non
     * federated case, map is included in copy of exp as a values(var, map)
     * clause
     */
    Mappings unionBranch(Producer p, Node graphNode, Exp exp, Exp main, Mappings data) {
        if (exp.isFirstWith()) {
            // union in union: eval inner union with parameter data as is
            return subEval(p, graphNode, null, exp, main, data);
        } else if (isFederate(exp) || exp.isUnion() || isParameterUnionMappings()) {
            Mappings unionData = unionData(exp, data);
            return subEval(p, graphNode, null, exp, main, unionData);
        } else {
            // exp += values(var, map)
            Exp ee = exp.complete(data);
            return subEval(p, graphNode, null, ee, main, null);
        }
    }

    /**
     * exp is a branch of union
     * extract from data relevant mappings for exp in-scope variables
     */
    Mappings unionData(Exp exp, Mappings data) {
        if (data != null) {
            if (data.getNodeList() == null) {
                MappingSet ms = new MappingSet(data);
                return ms.prepareMappingsRest(exp);
            } else if (data.getJoinMappings() != null) {
                // select relevant variables from original join Mappings
                return unionData(exp, data.getJoinMappings());
            }
        }
        return data;
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
            if (stopped) {
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

    private int service(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) {
        int backtrack = n - 1;
        Memory env = getMemory();
        Node serv = exp.first().getNode();
        Node node = serv;

        if (serv.isVariable()) {
            node = env.getNode(serv);
        }

        if (getProvider() != null) {
            // service delegated to provider
            Mappings map = getProvider().service(node, exp, selectQueryMappings(data), this);

            for (Mapping m : map) {
                if (stopped) {
                    return STOP;
                }
                // push each Mapping in memory and continue
                complete(getQuery(), m, false);

                if (env.push(m, n, false, false)) {
                    backtrack = eval(p, graphNode, stack, n + 1);
                    env.pop(m, false);
                    if (backtrack < n) {
                        return backtrack;
                    }
                }
            }
        } else {
            Query q = exp.rest().getQuery();
            return query(p, graphNode, q, data, stack, n);
        }

        return backtrack;
    }

    // process additional variable provided by service
    // such as ?_server_0 in federated mode
    void complete(Query q, Mapping map, boolean addNode) {
        int i = 0;
        for (Node node : map.getQueryNodes()) {
            Node out = q.getOuterNode(node);
            // Preserve auxiliary service bindings absent from the outer projection.
            if (out == null) {
                out = node;
                if (addNode && !q.getSelect().contains(node)) {
                    q.getSelect().add(node);
                }
            }
            map.getQueryNodes()[i] = out;
            i++;
        }
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

    /**
     * bind(exp as var)
     */
    private int bind(Producer p, Node graphNode, Exp exp, Mappings map, Stack stack, int n) {
        if (exp.isFunctional()) {
            return extBind(p, graphNode, exp, stack, n);
        }

        int backtrack = n - 1;
        Memory env = getMemory();
        Node node;
        try {
            node = eval(graphNode, exp.getFilter(), env, p);
        } catch (QueryTypeErrorException error) {
            // SPARQL Extend keeps the input solution when its expression errors;
            // only the target variable is left unbound. Unsupported features and
            // programming failures deliberately still propagate.
            getVisitor().bind(this, getGraphNode(graphNode), exp, null);
            return eval(p, graphNode, stack, map, n + 1);
        }

        getVisitor().bind(this, getGraphNode(graphNode), exp, node == null ? null : node.getDatatypeValue());

        if (node == null) {
            backtrack = eval(p, graphNode, stack, map, n + 1);
        } else if (env.push(exp.getNode(), node, n)) {
            // kgram authorize to bind a variable "again"
            // hence push may fail if value is not the same
            backtrack = eval(p, graphNode, stack, map, n + 1);
            env.pop(exp.getNode());
        }

        return backtrack;
    }

    /**
     * values (?x ?y) { unnest(exp) }
     * compiled as extended bind
     */
    private int extBind(Producer p, Node graphNode, Exp exp, Stack stack, int n) {
        int backtrack = n - 1;
        Memory env = getMemory();
        env.setGraphNode(graphNode);
        Mappings map = eval(exp.getFilter(), env, exp.getNodeList());
        env.setGraphNode(null);
        getVisitor().values(this, getGraphNode(graphNode), exp, map);

        if (map != null) {
            HashMap<String, Node> tab = toMap(exp.getNodeList());
            for (Mapping m : map) {
                if (stopped) {
                    return STOP;
                }
                if (env.push(tab, m, n)) {
                    backtrack = eval(p, graphNode, stack, n + 1);
                    env.pop(tab, m);
                    if (backtrack < n) {
                        return backtrack;
                    }
                }
            }
        }

        return backtrack;
    }

    HashMap<String, Node> toMap(List<Node> list) {
        HashMap<String, Node> m = new HashMap<>();
        for (Node node : list) {
            m.put(node.getLabel(), node);
        }
        return m;
    }

    /**
     *
     */
    private int filter(Producer p, Node graphNode, Exp exp, Stack stack, int n) {
        int backtrack = n - 1;
        Memory env = getMemory();
        boolean success = true;

        if (!exp.isPostpone()) {
            success = test(graphNode, exp.getFilter(), env, p);

            if (hasFilter) {
                success = getVisitor().filter(this, getGraphNode(graphNode), exp.getFilter().getExp(), success);
            }
        }

        if (hasEvent) {
            send(Event.FILTER, exp, success);
        }

        if (success) {
            backtrack = eval(p, graphNode, stack, n + 1);
        }

        return backtrack;
    }

    boolean test(Node graphNode, Filter f, Environment env, Producer p) {
        try {
            env.setGraphNode(graphNode);
            DatatypeValue dt = eval(f, env, p);
            return isTrue(dt);
        } catch (QueryTypeErrorException error) {
            // A SPARQL filter expression error makes that solution fail; it is
            // not an engine failure and must not abort the whole query.
            return false;
        } finally {
            env.setGraphNode(null);
        }
    }

    boolean isTrue(DatatypeValue dt) {
        if (dt == null) {
            return false;
        }
        return dt.isTrue();
    }

    public Node eval(Node graphNode, Filter f, Environment env, Producer p) {
        try {
            env.setGraphNode(graphNode);
            return p.getNode(eval(f.getExp(), env, p));
        } finally {
            env.setGraphNode(null);
        }
    }


    public DatatypeValue eval(Filter f, Environment env, Producer p) {
        return f.getExp().evalWE(getEvaluator(), env.getBind(), env, p);
    }

    public DatatypeValue eval(Expr e, Environment env, Producer p) {
        return e.evalWE(getEvaluator(), env.getBind(), env, p);
    }

    // values var ( unnext(exp) )
    // Note: Producer is not current producer but global producer
    public Mappings eval(Filter f, Environment env, List<Node> nodes) {
        return eval(f, env, getProducer(), nodes);
    }

    public Mappings eval(Filter f, Environment env, Producer p, List<Node> nodes) {

        int n = 1;
        Expr exp = f.getExp();
        if (exp.oper() == UNNEST) {
            if (hasListener) {
                listener.listen(exp);
            }
            if (exp.arity() == 2) {
                // unnest(exp, 2)
                DatatypeValue dt = eval(exp.getExp(1), env, p);
                if (dt == null) {
                    return new Mappings();
                }
                n = dt.intValue();
            }
            exp = exp.getExp(0);
        }
        DatatypeValue res = eval(exp, env, p);
        if (res == null) {
            return new Mappings();
        }
        // use std producer to map in case local p cannot map
        return getProducer().map(nodes, res, n);
    }

    private int values(Producer p, Node graphNode, Exp exp, Stack stack, int n) {
        int backtrack = n - 1;
        getVisitor().values(this, getGraphNode(graphNode), exp, exp.getMappings());

        for (Mapping map : exp.getMappings()) {
            if (stopped) {
                return STOP;
            }
            if (valuesBinding(exp.getNodeList(), map, n)) {
                backtrack = eval(p, graphNode, stack, n + 1);
                free(exp.getNodeList(), map);

                if (backtrack < n) {
                    return backtrack;
                }
            }
        }

        return backtrack;

    }

    boolean valuesBinding(List<Node> varList, Mapping map, int n) {
        int i = 0;
        for (Node qNode : varList) {
            Node node = map.getNode(qNode);
            if (node != null) {
                Node value = getProducer().getNode(node.getValue());
                boolean suc = getMemory().push(qNode, value, n);
                if (!suc) {
                    popBinding(varList, map, i);
                    return false;
                }
            }
            i++;
        }
        return true;
    }

    void popBinding(List<Node> varList, Mapping map, int i) {
        for (int index = 0; index < i; index++) {
            Node variable = varList.get(index);
            if (map.getNode(variable) != null) {
                getMemory().pop(variable);
            }
        }
    }

    /** Releases only bindings contributed by the current VALUES row. */
    void free(List<Node> varList, Mapping map) {
        for (Node qNode : varList) {
            if (map.getNode(qNode) != null) {
                getMemory().pop(qNode);
            }
        }
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
        int evENUM = Event.ENUM;
        boolean isSuccess = false;
        boolean hasGraphNode = graphNode != null;
        boolean isEvent = hasEvent;
        Edge qEdge = exp.getEdge();
        Node graph;
        Memory env = getMemory();
        // used by RuleEngine ResultWatcher ProducerImpl
        env.setExp(exp);
        Query qq = getQuery();
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
        if (hasProduce) {
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

                if (stopped) {
                    return STOP;
                }

                if (entity != null) {
                    nbEdge++;
                    if (hasListener && !listener.listen(exp, qEdge, entity)) {
                        continue;
                    }

                    graph = entity.getGraph();
                    boolean bmatch = match(qEdge, entity, graphNode, graph, env);

                    if (matchNBNode) {
                        bmatch &= (qEdge.nbNode() == entity.nbNode());
                    }

                    if (bmatch) {
                        if (hasCandidate) {
                            DatatypeValue dt = getVisitor().candidate(this, getGraphNode(graphNode), qEdge, entity);
                            if (dt != null) {
                                bmatch = dt.isTrue();
                            }
                        }

                        bmatch &= push(p, qEdge, entity, n);
                    }

                    if (isEvent) {
                        send(evENUM, exp, entity, bmatch);
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
     * subquery
     * select * where {{select * where {?x p ?y}} . ?y q ?z}
     * new eval, new memory, share only sub query select variables
     */
    private int query(Producer p, Node graphNode, Exp exp, Mappings data, Stack stack, int n) {
        int backtrack = n - 1;
        int evENUM = Event.ENUM;
        boolean isEvent = hasEvent;
        Query subQuery = exp.getQuery();
        Memory env = getMemory();
        getVisitor().start(subQuery);

        // copy current Eval,  new stack
        // bind sub query select nodes in new memory
        Eval ev = copy(copyMemory(env, getQuery(), subQuery, null), p, getEvaluator(), false);
        Mappings lMap = ev.eval(graphNode, subQuery, null, selectQueryMappings(data));

        getVisitor().query(this, getGraphNode(graphNode), exp, lMap);
        getVisitor().finish(lMap);

        // enumerate the result of the sub query
        // bind the select nodes into the stack
        for (Mapping map : lMap) {
            if (stopped) {
                return STOP;
            }
            boolean bmatch = push(subQuery, map, n);

            if (isEvent) {
                send(evENUM, exp, map, bmatch);
            }

            if (bmatch) {
                backtrack = eval(p, graphNode, stack, n + 1);
                pop(subQuery, map);
                if (backtrack < n) {
                    return backtrack;
                }
            }
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
     * res is a result of sub query bind the select nodes of sub query into
     * current memory retrieve outer node that correspond to sub node
     */
    private boolean push(Query subQuery, Mapping res, int n) {
        int k = 0;
        Memory env = memory;
        Matcher mm = match;
        Query qq = query;

        for (Exp exp : subQuery.getSelectFun()) {
            Node subNode = exp.getNode();
            Node node = res.getNode(subNode);
            Node outNode;
            if (exp.size() == 0) {
                // store outer node for next time
                outNode = qq.getOuterNodeSelf(subNode);   //ici
                exp.add(outNode);
            } else {
                outNode = exp.get(0).getNode();
            }

            if (node != null && !(mm.match(outNode, node, env) && env.push(outNode, node, n))) {
                // a value may be null because of an optional pattern
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

    /**
     * pop select nodes of sub query
     */
    private void pop(Query subQuery, Mapping ans) {
        Memory env = memory;
        Query qq = query;

        for (Node subNode : subQuery.getSelect()) {
            if (ans.isBound(subNode)) {
                Node outNode = qq.getOuterNodeSelf(subNode);
                env.pop(outNode);
            }
        }
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
            nbResult++;
        }
        if (storeResult && store) {
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
            if (hasEvent) {
                send(Event.RESULT, ans);
            }
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
        return nbEdge;
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


    public void createManager() {
        if (manager == null) {
            setEventManager(new KgramEventDispatcher());
            if (memory != null) {
                memory.setEventManager(manager);
            }
            if (results != null) {
                results.setEventManager(manager);
            }
        }
    }

    public KgramEventDispatcher getEventManager() {
        return manager;
    }

    public void setEventManager(KgramEventDispatcher man) {
        manager = man;
        hasEvent = true;
    }

    void send(int type, Object obj) {
        Event e = EventImpl.create(type, obj);
        manager.send(e);
    }

    void send(int type, Object obj, Object arg) {
        Event e = EventImpl.create(type, obj, arg);
        manager.send(e);
    }

    void send(int type, Object obj, Object arg, Object arg2) {
        Event e = EventImpl.create(type, obj, arg, arg2);
        manager.send(e);
    }

    void set(Stack current) {
        this.current = current;
    }

    public Stack getStack() {
        return current;
    }

    public ProcessVisitor getVisitor() {
        return visitor;
    }

    public void setVisitor(ProcessVisitor visitor) {
        this.visitor = visitor;
    }

    public boolean isStop() {
        return stopped;
    }

    public void setStop(boolean stopped) {
        this.stopped = stopped;
    }




    /**
     * Requests cooperative termination, including active join, graph, and optional
     * evaluation. May be called by the timeout watchdog; remaining evaluation state
     * must only be accessed by the owning execution thread.
     */
    public void finish() {
        setStop(true);
        join.setStop();
        graphEvaluator.setStop();
        optional.setStop(true);
    }

    /**
     * use case:
     * <p>
     * (n) EDGE{?x ?q ?z} (n+1) FILTER{?x = ?y} with BIND {?x := ?y} compiled
     * as:
     * <p>
     * (n) BIND {?x := ?y} (n+1) EDGE{?x ?q ?z} (n+2) FILTER{?x = ?y}
     */
    private int optBind(Producer p, Node gNode, Exp exp, Stack stack, int n) {
        Memory env = getMemory();
        int backtrack;
        if (exp.isBindCst()) {
            backtrack = cbind(p, gNode, exp, stack, n);
            return backtrack;
        } else {

            // ?x = ?y
            int i = 0;
            int j = 1;
            Node node = env.getNode(exp.get(i).getNode());
            if (node == null) {
                i = 1;
                j = 0;
                node = env.getNode(exp.get(i).getNode());
                if (node == null) {
                    // no binding: continue
                    backtrack = eval(p, gNode, stack, n + 1);
                    return backtrack;
                }
            }

            Node qNode = exp.get(j).getNode();
            if (!env.isBound(qNode) && getProducer().isBindable(node)) {
                // bind qNode with same index as other variable
                env.push(qNode, node, env.getIndex(exp.get(i).getNode()));
                if (hasEvent) {
                    send(Event.BIND, exp, qNode, node);
                }
                backtrack = eval(p, gNode, stack, n + 1);
                env.pop(qNode);
            } else {
                backtrack = eval(p, gNode, stack, n + 1);
            }

            return backtrack;
        }
    }

    /**
     * exp : BIND{?x = cst1 || ?x = cst2} Bind ?x with all its values
     */
    private int cbind(Producer p, Node gNode, Exp exp, Stack stack, int n) {
        Node qNode = exp.get(0).getNode();
        if (!exp.status() || memory.isBound(qNode)) {
            return eval(p, gNode, stack, n + 1);
        }

        if (exp.getNodeList() == null) {
            initBindNodes(exp, getProducer());
        }

        if (exp.getNodeList() != null) {
            return evalBindNodes(p, gNode, exp, stack, n, qNode);
        }
        return eval(p, gNode, stack, n + 1);
    }

    private void initBindNodes(Exp exp, Producer prod) {
        for (Object value : exp.getObjectValues()) {
            Expr cst = (Expr) value;
            Node node = prod.getNode(cst.getValue());
            if (node != null && prod.isBindable(node)) {
                exp.addNode(node);
            } else {
                exp.setNodeList(null);
                exp.status(false);
                break;
            }
        }
    }

    private int evalBindNodes(Producer p, Node gNode, Exp exp, Stack stack, int n, Node qNode) {
        Memory env = memory;
        for (Node node : exp.getNodeList()) {
            env.push(qNode, node, n);
            if (hasEvent) {
                send(Event.BIND, exp, qNode, node);
            }
            int backtrack = eval(p, gNode, stack, n + 1);
            env.pop(qNode);
            if (backtrack < n) {
                return backtrack;
            }
        }
        return n - 1;
    }

    /**
     * Draf extension where a Visitor provides Edge iterator
     */
    Iterable<Edge> produce(Node gNode, Edge edge) {
        DatatypeValue res = getVisitor().produce(this, gNode, edge);
        if (res == null) {
            return null;
        }
        if (res instanceof Loopable<?> loopable) {
            Iterable<?> loop = loopable.getLoop();
            if (loop != null) {
                return new IterableEntity(loop);
            }
        }
        return null;
    }

    public boolean isJoinMappings() {
        return joinMappings;
    }

    public ResultListener getListener() {
        return listener;
    }

    public void setListener(ResultListener listener) {
        this.listener = listener;
        this.hasListener = listener != null;
    }

}
