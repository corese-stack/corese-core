package fr.inria.corese.core.next.query.impl.kgram.path;

import fr.inria.corese.core.next.query.impl.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Filter;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Regex;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Evaluator;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Matcher;
import fr.inria.corese.core.next.query.impl.kgram.api.query.Producer;
import fr.inria.corese.core.next.query.api.exception.QueryException;
import fr.inria.corese.core.next.query.impl.kgram.core.*;
import fr.inria.corese.core.next.query.impl.kgram.event.ResultListener;
import fr.inria.corese.core.next.query.impl.kgram.tool.EdgeInv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * ********************************************************
 * ?x rdf:resf * /rdf:first
 * write paths in a synchronized buffer
 * ?x ^(p/q) ?y ::= ?y p/q ?x -> ?x inv(q)/inv(p) ?y
 * ?x p/q uri path is computed backward from uri to ?x with index=1 other=0
 * isReverse=true sequence is eval as q/p, all other exp are the same
 * ?x ^(p/q) uri ::= uri p/q ?x -> ?x inv(q)/inv(p) uri
 * PP Extensions
 * Path Variable: ?x exp :: $path ?y
 * Path Length: pathLength($path)
 * Path Enumeration: ?x exp :: $path ?y graph $path {?a ?p ?b}
 * Shortest path: ?x distinct short exp ?y ; ?x short exp ?y -- Note: complete
 * short to get only shortest
 * Path weight: ?x (rdf:first@2 / rdf:rest@1* / ^rdf:first@2) * ?y
 * Constaint: ?x exp
 * @ {?this a foaf:Person} ?y ?x exp
 * @ [a foaf:Person] ?y
 * Parallel Path: ?x (foaf:knows || ^rdfs:seeAlso) + ?y
 * Check Loop: pf.setCheckLoop(true) => exp+ exp[n,m] without loop
 * exec.setPathLoop(false) pragma {kg:path kg:loop false}
 *
 * @author Olivier Corby, Edelweiss, INRIA 2010
 * @ thanx Corentin Follenfant for the idea of property weight into the regex
 *
 ********************************************************
 */
public class PathFinder {

    private static final Logger logger = LoggerFactory.getLogger(PathFinder.class);

    // synchronized buffer between this and projection
    private PathMappingBuffer mbuffer;
    private Environment memory;
    private ResultListener listener;
    private Eval kgram;
    private final Producer producer;
    private final Matcher matcher;
    private final Evaluator evaluator;
    private final Query query;
    private Mappings lMap;
    private final HashMap<Integer, Mappings> store;
    private Filter filter;
    private Memory mem;
    private Edge edge;
    private Node gNode;
    private Node targetNode;
    private Node regexNode;
    private Node varNode;
    private List<Node> from;
    private Node[] qNodes;

    // index of node in edge that is the start of the path
    private int index = 0;
    // the inverse of the index (i.e. the other arg)
    private int other;
    private boolean isStop = false;
    private boolean hasListener = false;
    private boolean isDistinct = false;

    private boolean isReverse;
    private boolean isShort;
    private boolean isOne;

    // if true: return list of path instead of thread buffer: 50% faster but enumerate all path
    private boolean isList = false;
    private boolean checkLoop = false;
    private boolean isCountPath = false;
    private boolean isCache = false;
    private boolean isStorePath = true;

    private static final int MAX_LENGTH = Integer.MAX_VALUE;
    private int min = 0;
    private int max = MAX_LENGTH;

    private Regex regexp;

    /**
     * @param isStorePath the isStorePath to set
     */
    public void setStorePath(boolean isStorePath) {
        this.isStorePath = isStorePath;
    }

    /**
     * @return the isCache
     */
    public boolean isCache() {
        return isCache;
    }

    /**
     * @param isCache the isCache to set
     */
    public void setCache(boolean isCache) {
        this.isCache = isCache;
    }

    public PathFinder(Producer p, Matcher match, Evaluator eval, Query q) {
        query = q;
        producer = p;
        matcher = match;
        evaluator = eval;
        lMap = new Mappings();
        store = new HashMap<>();
    }

    public static PathFinder create(Eval eval, Producer p, Query q) {
        PathFinder pf = new PathFinder(p, eval.getMatcher(), eval.getEvaluator(), q);
        pf.setEval(eval);
        return pf;
    }

    void setEval(Eval ev) {
        kgram = ev;
    }

    public void set(ResultListener rl) {
        listener = rl;
        hasListener = rl != null;
    }

    public void setCheckLoop(boolean b) {
        checkLoop = b;
    }

    public void setCountPath(boolean b) {
        isCountPath = b;
        if (b) {
            isDistinct = false;
        }
    }

    public void setList(boolean b) {
        isList = b;
    }

    /**
     * Start/init computation of a new list of path
     */
    public void start(Edge edge, Node node, Memory env, Filter f) {
        regexNode = node;
        List<String> lVar = null;
        if (f != null) {
            lVar = f.getVariables();
        }
        lMap = new Mappings();
        this.edge = edge;
        int n = index(edge, env, lVar);
        start(n);
        index = n;
        targetNode = env.getNode(edge.getNode(other));
        varNode = edge.getEdgeVariable();
        if (f != null && match(edge, lVar, index)) {
            filter = f;
            init(env);
        }
        if (mem == null && node != null) {
            init(env);
        }
    }

    void init(Memory env) {
        mem = new Memory(matcher, evaluator);
        mem.init(env.getQuery());
        mem.init(env);
        evaluator.init(mem);
        mem.share(mem.getBind(), env.getBind());
        mem.setEval(kgram);
    }

    boolean match(Edge edge, List<String> lVar, int index) {
        return lVar.size() == 1
                && edge.getNode(index).isVariable()
                && edge.getNode(index).getLabel().equals(lVar.getFirst());
    }

    /**
     * Compute the index of node of edge which will be start of the path If one
     * of the nodes is bound, this is it If one of the nodes is a constant, this
     * is it If there is a filter on a node, this is it Otherwise it is node at
     * index 0
     */
    int index(Edge edge, Environment mem, List<String> lVar) {
        // which arg is bound if any ?
        for (int i = 0; i < 2; i++) {
            if (mem.isBound(edge.getNode(i))) {
                return i;
            }
        }
        for (int i = 0; i < 2; i++) {
            if (edge.getNode(i).isConstant()) {
                return i;
            }
        }
        if (lVar != null) {
            for (int i = 0; i < 2; i++) {
                if (match(edge, lVar, i)) {
                    return i;
                }
            }
        }
        return 0;
    }

    /**
     * Enumerate all path, return a list of path 50% faster that thread but
     * enumerate *all* path
     */
    public Iterable<Mapping> candidate2(Node gNode, List<Node> from, Environment mem) {
        this.gNode = gNode;
        this.from = from;
        this.memory = mem;
        Node cstart = get(memory, index);

        Mappings map = getMappings(cstart);
        if (map != null) {
            return map;
        }

        lMap = new Mappings();
        process(cstart, memory);
        putMappings(cstart, lMap);
        return lMap;
    }

    /**
     * Retrieve solution in cache Note: manage two tables for two possible index
     */
    Mappings getMappings(Node cstart) {
        if (isCache() && cstart != null) {
            return store.get(cstart.getIndex());
        }
        return null;
    }

    void putMappings(Node start, Mappings map) {
        if (isCache() && start != null) {
            store.put(start.getIndex(), map);
        }
    }

    /**
     * Enumerate path in a parallel thread, return a synchronised buffer Useful
     * if backjump or have a limit in sparql query
     */
    public Iterable<Mapping> candidate(Node gNode, List<Node> from, Environment env) {
        isStop = false;
        if (mem != null) {
            mem.setGraphNode(gNode);
        }
        if (isList) {
            return candidate2(gNode, from, env);
        }
        this.gNode = gNode;
        this.from = from;
        this.memory = env;
        mstart(env);
        // return path enumeration (read the synchronized buffer)
        return mbuffer;
    }

    int getIndex() {
        return index;
    }

    void mstart(Environment mem) {
        // buffer store path enumeration
        mbuffer = new PathMappingBuffer();
        // path enumeration in a thread
        GraphPath graphPath = new GraphPath(this, mem);
        // launch path computing (one by one) eg launch process() below
        graphPath.start();
    }

    public void stop() {
        isStop = true;
    }

    /**
     * init at creation time, no need to change. pmax comes from pathLength() &lt;=
     * pmax
     */
    public void init(Regex exp, int pmin, int pmax) {
        regexp = exp;

        isReverse = false;
        isShort = false;
        isOne = false;
        other = 1;

        if (exp.isShort()) {
            isShort = true;
            if (exp.isDistinct()) {
                isOne = true;
            }
        }

        // set min and max path length
        // filter pathLength($path) > val
        // the length of the minimal path that matches regex:
        int length = regexp.regLength();
        min = Math.max(min, length);
        max = Math.max(max, length);

        // user default values
        if (pmin != -1) {
            min = Math.max(min, pmin);
        }
        // either IntegerMax or pathLength($p) <= max
        if (pmax != -1) {
            max = pmax;
        }
    }

    public Edge getEdge() {
        return edge;
    }

    /**
     * start at run time depends on index : which arg is bound or where we start
     */
    void start(int n) {
        index = n;
        if (index == 1) {
            other = 0;
            isReverse = true;
        } else {
            other = 1;
            isReverse = false;
        }
    }

    Node get(Environment memory, int i) {
        if (edge == null) {
            return null;
        }
        Node qc = edge.getNode(i);
        return memory.getNode(qc);
    }

    void process(Node cstart, Environment memory) {
        // Is the source of edge bound ?
        // In which case all path relations come from same source
        Node csource = null;
        if (gNode != null) {
            csource = gNode.isConstant() ? gNode : memory.getNode(gNode);
        }

        // the start concept for path
        Path path = new Path(isReverse);
        path.setMax(max);
        path.setIsShort(isShort);

        if (isShort && cstart != null) {
            // if null, will be done later
            producer.initPath(edge, 0);
        }

        Regex regexp1 = regexp.transform();
        eval(regexp1, path, cstart, csource);

        // in order to stop enumeration, return null
        if (!isList) {
            mbuffer.put(null, false);
        }
    }

    /**
     * Path result as a Mapping start and last nodes the path variable whose
     * index is used (e.g. pathLength) to retrieve the list of edges in the
     * memory the list of edges
     */
    private Mapping result(Path path, Node gNode, Node src, Node start, boolean isReverse) {
        Edge ee = edge;
        int length = 3;
        int ip = 2;
        int is = 3;

        if (!isStorePath) {
            length = 2;
            is = 2;
        }
        if (src != null) {
            length += 1;
        }

        Node n1;
        Node n2;
        if (path.size() == 0) {
            n1 = start;
            n2 = start;
        } else {
            n1 = path.firstNode();
            n2 = path.lastNode();
        }

        if (!check(n1, n2)) {
            return null;
        }

        if (qNodes == null) {
            // computed once and then shared by all Mapping
            qNodes = new Node[length];
            qNodes[0] = ee.getNode(0);
            qNodes[1] = ee.getNode(1);
            if (src != null) {
                qNodes[is] = gNode;
            }
            if (isStorePath) {
                qNodes[ip] = ee.getEdgeVariable();
            }
        }

        Node[] tNodes = new Node[length];
        tNodes[0] = n1;
        tNodes[1] = n2;

        if (src != null) {
            tNodes[is] = src;
        }

        Mapping map = Mapping.create(qNodes, tNodes);
        if (isStorePath) {
            Path edges = path.copy(producer);
            if (isReverse) {
                edges.reverse();
            }
            tNodes[ip] = getPathNode(edges);
        }

        return map;
    }

    /**
     * Generate a unique Blank Node wrt query that represents the path Use a
     * predefined filter pathNode()
     */
    Node getPathNode(Path p) {
        Filter f = query.getGlobalFilter(Query.PATHNODE);
        Node node = producer.getNode(
                f.getExp().evalWE(evaluator, memory.getBind(), memory, producer));
        if (node == null) {
            throw new IllegalStateException("The path-node expression returned no RDF value");
        }
        node.setObject(p);
        return node;
    }

    /**
     * Check if target node match its query node and its binding
     */
    private boolean check(Node n0, Node n1) {
        if (index == 0) {
            if (!matcher.match(edge.getNode(1), n1, memory)) {
                return false;
            }
            return targetNode == null || targetNode.match(n1);
        } else {
            if (!matcher.match(edge.getNode(0), n0, memory)) {
                return false;
            }
            return targetNode == null || targetNode.match(n0);
        }
    }

    /**
     * cstart is bound when edge node is a bound variable
     * cstart is not bound when edge node is a constant, this case
     * is processed by Producer
     */
    private Iterable<Node> getNodeIterator(Node cstart) {
        if (cstart == null) {
            return getNodeIterator(edge, from, null);
        } else {
            // start is bound
            ArrayList<Node> list = new ArrayList<>();
            list.add(cstart);
            return list;
        }
    }

    public Iterable<Node> getNodeIterator(final Edge edge, List<Node> from, List<Regex> regex) {
        Iterable<Node> iter = producer.getNodes(gNode, from, edge, memory, regex, index);

        if (filter == null) {
            return iter;
        }

        final Iterator<Node> it = iter.iterator();

        return () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Node next() {
                while (hasNext()) {
                    Node entity = it.next();
                    if (entity == null) {
                        return null;
                    }
                    Node node = entity.getNode();
                    if (test(node)) {
                        return entity;
                    }
                }
                return null;
            }
        };
    }

    /**
     * exp
     * @ {?this rdf:type c:Person}
     */
    boolean test(Filter filter, Path path, Node qNode, Node node) {
        mem.push(qNode, node);
        if (varNode != null) {
            Node pathNode = getPathNode(path);
            mem.push(varNode, pathNode);
        }
        boolean test;
        try {
            test = filter.getExp().test(evaluator, mem.getBind(), mem, producer);
        } catch (QueryException ex) {
            test = false;
        }
        mem.pop(qNode);
        if (varNode != null) {
            mem.pop(varNode);
        }
        return test;
    }

    boolean test(Node node) {
        Node qNode = edge.getNode(index);
        mem.push(qNode, node);
        boolean test;
        try {
            test = filter.getExp().test(evaluator, mem.getBind(), mem, producer);
        } catch (QueryException ex) {
            test = false;
        }
        mem.pop(qNode);
        return test;
    }

    /*
     * *******************************************************************************
     *
     * New version interprets regex directly with a stack
     *
     *******************************************************************************
     */
    /**
     * rewrite ! (^ p) as ^ (! p) rewrite ^(p/q) as ^q/^p
     */
    void eval(Regex exp, Path path, Node start, Node src) {
        Record stack = new Record(Visit.create(isReverse, isCountPath));
        stack.push(exp);
        try {
            eval(stack, path, start, src);
        } catch (StackOverflowError e) {
            logger.error("** Property Path Size: {}", path.size());
            logger.error("** Property Path Error: \n{}", String.valueOf(e));
        }
    }

    /**
     * top of stack is current exp to eval rest of stack is in sequence path may
     * be walked left to right if start is bound or right to left if end is
     * bound in the later case, index = 1
     */
    void eval(Record stack, Path path, Node start, Node src) {
        if (isStop) {
            return;
        }

        if (stack.isEmpty()) {
            if (stack.getTarget() != null) {
                // this is a parallel path check, path is finished: stop it
                if (start.match(stack.getTarget())) {
                    // it is successful
                    stack.setSuccess();
                }
                return;
            }

            result(stack, path, start, src);
            return;
        }

        Regex exp = stack.pop();

        switch (exp.retype()) {
            case Regex.TEST:
                evalTest(exp, stack, path, start, src);
                break;

            case Regex.LABEL, Regex.NOT:
                evalLabel(exp, stack, path, start, src);
                break;

            case Regex.SEQ:
                evalSeq(exp, stack, path, start, src);
                break;

            case Regex.PARA:
                evalPara(exp, stack, path, start, src);
                break;

            case Regex.CHECK:
                evalCheck(exp, stack, path, start, src);
                break;

            case Regex.PLUS:
                // exp+
                if (start == null && stack.getVisit().knows(exp)) {
                    stack.push(exp);
                    return;
                }
                plus(exp, stack, path, start, src);
                break;

            case Regex.COUNT:
                // regex count: exp[1, n]
                count(exp, stack, path, start, src);
                break;

            case Regex.STAR:
                // exp*
                if (start == null && stack.getVisit().knows(exp)) {
                    stack.push(exp);
                    return;
                }
                star(exp, stack, path, start, src);
                break;

            case Regex.ALT:
                evalAlt(exp, stack, path, start, src);
                break;

            case Regex.OPTION:
                option(exp, stack, path, start, src);
                break;

            default:
                break;
        }
    }

    private void evalTest(Regex exp, Record stack, Path path, Node start, Node src) {
        boolean b = true;
        if (start != null) {
            b = test(exp.getExpr().getFilter(), path, regexNode, start);
        }
        if (b) {
            eval(stack, path, start, src);
        }
        stack.push(exp);
    }

    private void evalSeq(Regex exp, Record stack, Path path, Node start, Node src) {
        int fst = 0;
        int rst = 1;
        if (isReverse) {
            fst = 1;
            rst = 0;
        }

        stack.push(exp.getArg(rst));
        stack.push(exp.getArg(fst));

        eval(stack, path, start, src);

        stack.pop();
        stack.pop();
        stack.push(exp);
    }

    private void evalPara(Regex exp, Record stack, Path path, Node start, Node src) {
        if (start != null) {
            stack.pushStart(start);
        }
        stack.push(exp.getArg(2));
        stack.push(exp.getArg(0));
        eval(stack, path, start, src);
        stack.pop();
        stack.pop();

        if (start != null) {
            stack.popStart();
        }
        stack.push(exp);
    }

    private void evalCheck(Regex exp, Record stack, Path path, Node start, Node src) {
        Regex test = exp.getArg(0);

        if (test.retype() == Regex.PARA) {
            Record st = new Record(Visit.create(isReverse, isCountPath));
            st.push(test.getArg(1));
            st.setTarget(start);
            Node prev = stack.getStart();

            eval(st, path, prev, src);

            if (st.isSuccess()) {
                eval(stack, path, start, src);
            }

            stack.push(exp);
        } else if (test.retype() == Regex.OPTION) {
            if (!stack.getVisit().nloop(test, start)) {
                eval(stack, path, start, src);
            }

            stack.push(exp);
        }
    }

    private void evalAlt(Regex exp, Record stack, Path path, Node start, Node src) {
        stack.push(exp.getArg(0));
        eval(stack, path, start, src);
        stack.pop();

        stack.push(exp.getArg(1));
        eval(stack, path, start, src);
        stack.pop();

        stack.push(exp);
    }

    private void evalLabel(Regex exp, Record stack, Path path, Node start, Node src) {
        if (path.size() >= path.getMax()) {
            stack.push(exp);
            return;
        }

        Node previous = null;
        for (Edge ent : producer.getEdges(gNode, from, edge, memory, exp, src, start, index)) {
            if (isStop || stack.isSuccess()) {
                stack.push(exp);
                return;
            }
            previous = processEdge(ent, exp, stack, path, start, src, previous);
        }

        stack.push(exp);
    }

    private Node processEdge(Edge ent, Regex exp, Record stack, Path path, Node start, Node src, Node previous) {
        if (ent == null) {
            return previous;
        }

        Edge rel = ent;
        Node node = rel.getNode(index);
        if (exp.isInverse() || exp.isReverse()) {
            EdgeInv ei = new EdgeInv(ent);
            rel = ei;
            ent = ei;
            node = rel.getNode(index);
        }

        if (filter != null && start == null && !test(rel.getNode(index))) {
            return previous;
        }

        Node currentSrc = src;
        if (path.size() == 0 && src == null && gNode != null) {
            currentSrc = ent.getGraph();
        } else if (currentSrc != null && !ent.getGraph().match(currentSrc)) {
            return previous;
        }

        Visit visit = stack.getVisit();
        boolean isStart = start == null;
        if (isStart) {
            previous = recordStartNode(node, previous, visit, stack);
        }

        if (isShort && !checkShortestPath(visit, rel.getNode(other), exp, path.weight() + exp.getWeight())) {
            return previous;
        }

        stepPath(ent, rel, exp, stack, path, currentSrc);

        if (isStart) {
            visit.nleave(node);
            stack.popStart();
        }

        return previous;
    }

    private Node recordStartNode(Node node, Node previous, Visit visit, Record stack) {
        boolean isNew = previous == null || !previous.match(node);
        if (isNew) {
            visit.start();
        }
        visit.nstart(node);
        stack.pushStart(node);
        if (isShort && isNew) {
            producer.initPath(edge, 0);
            visit.initPath();
        }
        return node;
    }

    private boolean checkShortestPath(Visit visit, Node otherNode, Regex exp, int length) {
        Integer l = visit.getLength(otherNode, exp);
        if (l == null) {
            visit.setLength(otherNode, exp, length);
            return true;
        }
        if (length > l || (isOne && length == l)) {
            return false;
        }
        visit.setLength(otherNode, exp, length);
        return true;
    }

    private void stepPath(Edge ent, Edge rel, Regex exp, Record stack, Path path, Node currentSrc) {
        if (hasListener) {
            listener.enter(ent, exp, path.size());
        }
        path.add(ent, exp.getWeight());
        boolean suc = kgram.getVisitor().step(kgram, currentSrc, edge, path, path.firstNode(), path.lastNode());
        if (suc) {
            eval(stack, path, rel.getNode(other), currentSrc);
        }
        path.remove(exp.getWeight());
        if (hasListener) {
            listener.leave(ent, exp, path.size());
        }
    }

    boolean isDistinct(Record stack, Node start, Node target) {
        boolean b = stack.getVisit().isDistinct(start, target);
        if (b) {
            stack.getVisit().addDistinct(start, target);
        }
        return b;
    }

    void result(Record stack, Path path, Node start, Node src) {
        if (path.size() > 0) {
            if (isDistinct && !isDistinct(stack, path.firstNode(), path.lastNode())) {
                // distinct (start,target)
                return;
            }

            boolean shouldStore = true;
            if (hasListener) {
                shouldStore = listener.process(path);
            }

            if (shouldStore) {
                Mapping map = result(path, gNode, src, start, isReverse);
                if (map != null) {
                    result(src, map);
                }
            }
        } else {
            resultNode(path, start, src);
        }
    }

    void resultNode(Path path, Node start, Node src) {
        for (Node node : getNodeIterator(start)) {
            if (isStop) {
                return;
            }

            if (node != null) {
                if (gNode != null) {
                    src = node.getGraph();
                }
                Mapping m = result(path, gNode, src, node, isReverse);

                if (m != null) {
                    result(src, m);
                }
            }
        }
    }

    void result(Node src, Mapping map) {
        kgram.getVisitor().path(kgram, src, edge, map.getPath(2), map.getNode(0), map.getNode(1));
        if (isList) {
            lMap.add(map);
        } else {
            mbuffer.put(map, true);
        }
    }

    void option(Regex exp, Record stack, Path path, Node start, Node src) {
        if (stack.getVisit().nloop(exp, start)) {
            stack.push(exp);
            return;
        }

        // skip option
        eval(stack, path, start, src);

        // with option
        // push check:
        stack.push(exp.getArg(1));
        stack.push(exp.getArg(0));
        eval(stack, path, start, src);

        // pop exp and check
        stack.pop();
        stack.pop();
        stack.push(exp);

        stack.getVisit().nunset(exp);
    }

    /**
     * exp*
     */
    void star(Regex exp, Record stack, Path path, Node start, Node src) {
        // start is the first node of exp*
        boolean isFirst = stack.getVisit().nfirst(exp);

        if (stack.getVisit().nloop(exp, start)) {
            // start already met in exp path: stop
            stack.push(exp);
            return;
        }

        // use case: (p*/q)*
        // we must save each visited of p*
        // because it expands to p*/q . p*/q ...
        // and each occurrence of p* must have its own visited
        Visit.VisitedNode save = stack.getVisit().nunset(exp);
        eval(stack, path, start, src);
        stack.getVisit().nset(exp, save);

        // restore exp*
        stack.push(exp);
        // push exp
        stack.push(exp.getArg(0));
        // second step: eval exp once more
        eval(stack, path, start, src);
        // restore stack (exp* on top)
        stack.pop();

        stack.getVisit().nremove(exp, start);
        if (isFirst) {
            stack.getVisit().nunset(exp);
        }
    }

    /**
     * exp = exp+ ; stack = rest
     * Distinguish 1. first execution where index(exp+) = 0  and 2. next executions where index(exp+) = 1
     * 1.    stack := (exp, exp+, rest) ; eval(stack)
     * 2. a) stack = (rest) ; eval(stack) b) stack := (exp, exp+, rest) ; eval(stack)
     * In addition there are two cases whether start node is bound or not
     * If start is bound, OK
     * If start is not bound, it will be bound by case LABEL: above
     * ?x p+ ?y
     * When index(p+) = 0, start is not bound, execute(p/p+). When we come back to p+ in the stack,
     */
    void plus(Regex exp, Record stack, Path path, Node start, Node src) {
        Visit visit = stack.getVisit();
        // start is the first node of exp+
        visit.nfirst(exp);

        if (visit.count(exp) == 0) {
            // case 1: first execution of exp+

            // declare exp such that when start node changes (in case LABEL:)
            // the visitedNode table of exp be cleared by visit.start()
            visit.declare(exp);

            // push exp+ again in stack to loop later
            stack.push(exp);
            // assign index=1 to exp+, hence exp+ will be executed by case 2 below
            visit.count(exp, +1);
            // push exp to execute it now
            stack.push(exp.getArg(0));
            // execute exp ; stack = (exp, exp+, rest) ; exp+ will be executed by case 2 below
            eval(stack, path, start, src);
            stack.pop();
            visit.count(exp, -1);

            if (!isCountPath) {
                // std sparql
                // leave exp+
                visit.nremove(exp, start);
            }
        } else {
            // case 2:
            // next execution of exp+ after first one
            if (visit.nloop(exp, start)) {
                stack.push(exp);
                return;
            }
            // we have executed exp at least once
            // exp = exp+ ; stack = (rest)
            // (1) evaluate the stack if any and store result
            // use case: eval rest in: exp+ / rest
            // switch off exp+ visitedNode table in case: (exp1+/exp2+)+
            // exp1 would be ready to start "again" with "new" visitedNode table
            Visit.VisitedNode save = visit.nunset(exp);
            visit.set(exp, 0);

            // eval rest
            eval(stack, path, start, src);

            // switch on exp+ index and visitedNode table
            visit.set(exp, 1);
            visit.nset(exp, save);

            // (2) loop again
            // push exp+
            stack.push(exp);
            // push exp
            stack.push(exp.getArg(0));
            // stack = (exp, exp+, rest) ; eval(stack)
            eval(stack, path, start, src);
            stack.pop();

            visit.nremove(exp, start);
        }
    }

    /**
     * exp{n,m} exp{n,}
     */
    void count(Regex exp, Record stack, Path path, Node start, Node src) {
        if (stack.getVisit().count(exp) >= exp.getMin()) {
            countMinReached(exp, stack, path, start, src);
        } else {
            countMinNotReached(exp, stack, path, start, src);
        }
    }

    private void countMinReached(Regex exp, Record stack, Path path, Node start, Node src) {
        if (checkLoop(exp) && stack.getVisit().nloop(exp, start)) {
            stack.push(exp);
            return;
        }

        // min length is reached, can leave
        int save = stack.getVisit().count(exp);
        stack.getVisit().set(exp, 0);
        eval(stack, path, start, src);
        stack.getVisit().set(exp, save);

        stack.push(exp);

        if (stack.getVisit().count(exp) < exp.getMax()) {
            // max length not reached, can continue
            stack.getVisit().count(exp, +1);
            stack.push(exp.getArg(0));
            eval(stack, path, start, src);
            stack.pop();
            stack.getVisit().count(exp, -1);
        }

        if (checkLoop(exp)) {
            stack.getVisit().nremove(exp, start);
        }
    }

    private void countMinNotReached(Regex exp, Record stack, Path path, Node start, Node src) {
        if (isReverse) {
            if (checkLoop(exp)) {
                // use case: ?x exp[2,] <uri>
                // path goes backward
                stack.getVisit().ninsert(exp, start);
            }
        } else if (checkLoop && stack.getVisit().nloop(exp, start)) {
            stack.push(exp);
            return;
        }

        stack.push(exp);

        stack.getVisit().count(exp, +1);
        stack.push(exp.getArg(0));
        eval(stack, path, start, src);
        stack.pop();
        stack.getVisit().count(exp, -1);

        if (isReverse) {
            if (checkLoop(exp)) {
                // use case: ?x exp[2,] <uri>
                // path goes backward
                stack.getVisit().nremove(exp, start);
            }
        } else if (checkLoop) {
            stack.getVisit().nremove(exp, start);
        }
    }

    boolean hasMax(Regex exp) {
        return exp.getMax() != -1 && exp.getMax() != Integer.MAX_VALUE;
    }

    // for count exp [n,m]
    boolean checkLoop(Regex exp) {
        return checkLoop || !hasMax(exp);
    }
}
