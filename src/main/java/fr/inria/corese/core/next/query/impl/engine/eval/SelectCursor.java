package fr.inria.corese.core.next.query.impl.engine.eval;

import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.FallbackReason;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.ExpType.Type;
import fr.inria.corese.core.next.query.impl.engine.path.PropertyPathEdge;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.QueryPlan;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Memory;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Resumable, depth-first execution of a non-blocking SELECT pipeline on its calling
 * thread. Frames retain only the current bindings and candidate iterator at each
 * depth. Projection, expression evaluation, RDF matching and DISTINCT use the same
 * engine services as full evaluation; accepted mappings are never added to results.
 * DISTINCT still needs its semantic seen-set. Unsupported algebra uses full evaluation.
 *
 * <p>The owner must close this cursor, including after partial consumption. The
 * surrounding execution owns cancellation and the producer's stable read snapshot.</p>
 */
public final class SelectCursor implements Iterator<Mapping>, AutoCloseable {
    private final Eval eval;
    private final Eval cancellation;
    private final Mapping parameters;
    private final Node scopeGraph;
    private final boolean nested;
    private final boolean applySlice;
    private final QueryPlan query;
    private final Memory memory;
    private final Producer producer;
    private final List<Exp> instructions = new ArrayList<>();
    private final Frame[] frames;
    private int depth;
    private long accepted;
    private long emitted;
    private Mapping next;
    private boolean closed;

    /**
     * Returns the first guard selecting full evaluation, without reading any data.
     * NONE means that the algebra is eligible; the executor must still capture an
     * independent read snapshot. The decision also drives diagnostics so reporting
     * cannot drift from the strategy actually selected.
     *
     * @param query completed execution query
     * @return first materialization guard, or NONE for eligible algebra
     */
    public static FallbackReason fallbackReason(QueryPlan query) {
        if (!query.isSelect()) return FallbackReason.QUERY_FORM;
        if (query.isOrderBy()) return FallbackReason.ORDER_BY;
        return operatorFallbackReason(query);
    }

    /**
     * Whether this ordered SELECT can supply unsliced solutions to a bounded selector.
     * Aggregate and scope guards remain identical to those of the progressive cursor.
     *
     * @param query completed execution query
     * @return whether ORDER BY has a finite, representable LIMIT plus OFFSET and eligible input
     */
    public static boolean supportsOrderedInput(QueryPlan query) {
        return query.isSelect() && query.isOrderBy() && query.getLimit() < Integer.MAX_VALUE
                && (long) query.getLimit() + query.getOffset() <= Integer.MAX_VALUE
                && operatorFallbackReason(query) == FallbackReason.NONE;
    }

    private static FallbackReason operatorFallbackReason(QueryPlan query) {
        if (query.hasGroupBy() || query.isAggregate() || query.getHaving() != null) return FallbackReason.AGGREGATION;
        return fallbackReason(query.getBody(), query.getValues() != null && !singleton(query.getValues()));
    }

    private static FallbackReason fallbackReason(Exp exp, boolean repeated) {
        return switch (exp.type()) {
            case AND, BGP -> sequenceReason(exp, repeated);
            case UNION -> unionReason(exp, repeated);
            case JOIN -> joinReason(exp, repeated);
            case GRAPH -> graphReason(exp, repeated);
            case OPTIONAL -> optionalReason(exp, repeated);
            case EDGE -> exp.getEdge() instanceof PropertyPathEdge ? FallbackReason.PROPERTY_PATH : FallbackReason.NONE;
            case QUERY -> FallbackReason.SUBQUERY;
            case SERVICE -> FallbackReason.SERVICE;
            case MINUS -> FallbackReason.MINUS;
            case FILTER -> exp.isPostpone() ? FallbackReason.COMPLEX_OPTIONAL : FallbackReason.NONE;
            case BIND -> exp.isFunctional() ? FallbackReason.UNSUPPORTED_OPERATOR : FallbackReason.NONE;
            case EMPTY, VALUES -> FallbackReason.NONE;
            default -> FallbackReason.UNSUPPORTED_OPERATOR;
        };
    }

    private static FallbackReason sequenceReason(Exp exp, boolean repeated) {
        boolean inputRepeats = repeated;
        for (Exp child : exp) {
            FallbackReason reason = fallbackReason(child, inputRepeats);
            if (reason != FallbackReason.NONE) return reason;
            inputRepeats |= !singleton(child);
        }
        return FallbackReason.NONE;
    }

    private static FallbackReason unionReason(Exp exp, boolean repeated) {
        for (Exp branch : exp) {
            if (repeated && !tripleJoin(branch) && !singleton(branch)) return FallbackReason.REPEATED_SCOPE;
            FallbackReason reason = fallbackReason(branch, false);
            if (reason != FallbackReason.NONE) return reason;
        }
        return FallbackReason.NONE;
    }

    private static FallbackReason joinReason(Exp exp, boolean repeated) {
        if (tripleJoin(exp)) return FallbackReason.NONE;
        if (!singleton(exp.first())) return FallbackReason.SCOPED_JOIN;
        FallbackReason left = fallbackReason(exp.first(), repeated);
        return left == FallbackReason.NONE ? fallbackReason(exp.rest(), repeated) : left;
    }

    private static FallbackReason graphReason(Exp exp, boolean repeated) {
        if (repeated && !tripleJoin(exp.get(1)) && !singleton(exp.get(1))) return FallbackReason.REPEATED_SCOPE;
        return fallbackReason(exp.get(1), false);
    }

    private static FallbackReason optionalReason(Exp exp, boolean repeated) {
        if (exp.isPostpone()) return FallbackReason.COMPLEX_OPTIONAL;
        FallbackReason left = fallbackReason(exp.first(), repeated);
        if (left != FallbackReason.NONE) return left;
        return tripleJoin(exp.rest()) ? FallbackReason.NONE : FallbackReason.COMPLEX_OPTIONAL;
    }

    /** Avoid repeated independent scans where the existing evaluator uses a buffered join. */
    private static boolean singleton(Exp exp) {
        return switch (exp.type()) {
            case AND, BGP, JOIN -> exp.getExpList().stream().allMatch(SelectCursor::singleton);
            case EMPTY, FILTER, BIND -> true;
            case VALUES -> exp.getMappings().size() <= 1;
            default -> false;
        };
    }

    private static boolean tripleJoin(Exp exp) {
        return switch (exp.type()) {
            case AND, BGP, JOIN -> exp.getExpList().stream().allMatch(SelectCursor::tripleJoin);
            case EDGE -> !(exp.getEdge() instanceof PropertyPathEdge);
            case EMPTY -> true;
            default -> false;
        };
    }

    /**
     * Initializes one independent execution without requesting any candidate edge.
     *
     * @param eval fresh evaluator owned by this execution
     * @param query completed eligible SELECT
     * @param bindings initial parameter bindings, or null
     */
    public SelectCursor(Eval eval, QueryPlan query, Mapping bindings) {
        this(null, eval, query, bindings, query.getBody(), null, true);
    }

    /**
     * Opens eligible ORDER BY input without applying OFFSET or LIMIT before ranking.
     * Projection and DISTINCT still run for every candidate, using the ordinary evaluator.
     * The caller owns both this cursor and its producer's stable read view.
     *
     * @param eval fresh evaluator owned by this execution
     * @param query completed query accepted by {@link #supportsOrderedInput(QueryPlan)}
     * @param bindings initial parameter bindings, or null
     * @return unsliced input cursor
     */
    public static SelectCursor orderedInput(Eval eval, QueryPlan query, Mapping bindings) {
        return new SelectCursor(null, eval, query, bindings, query.getBody(), null, false);
    }

    private SelectCursor(SelectCursor parent, Eval eval, QueryPlan query, Mapping bindings, Exp body, Node graph,
            boolean applySlice) {
        this.eval = eval;
        this.query = query;
        this.scopeGraph = graph;
        this.applySlice = applySlice;
        parameters = bindings;
        nested = parent != null;
        cancellation = nested ? parent.cancellation : eval;
        producer = eval.getProducer();
        eval.setSubEval(nested);
        eval.init(query);
        memory = eval.getMemory();
        eval.share(bindings);
        if (!nested) {
            producer.start(query);
            eval.getVisitor().init(query);
            eval.getVisitor().before(query);
        }
        if (bindings != null) eval.bind(bindings);
        if (!nested && query.getValues() != null) append(query.getValues());
        append(body);
        frames = new Frame[instructions.size()];
    }

    private void append(Exp exp) {
        append(instructions, exp);
    }

    private static void append(List<Exp> target, Exp exp) {
        if (exp.isFail()) {
            target.add(exp);
        } else if (exp.isAnd() || exp.isBGP() || exp.type() == Type.JOIN && tripleJoin(exp)) {
            for (Exp child : exp) append(target, child);
        } else {
            target.add(exp);
        }
    }

    @Override
    public boolean hasNext() {
        if (next != null) return true;
        if (closed) return false;
        if (query.isFail() || emitted >= limit()) {
            close();
            return false;
        }
        while (depth >= 0 && !cancellation.isStop()) {
            checkInterrupted();
            if (depth == frames.length) {
                depth--;
                if (selectRow()) return true;
            } else if (advance()) {
                depth++;
            } else {
                frames[depth].close();
                frames[depth--] = null;
            }
        }
        close();
        return false;
    }

    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) throw new QueryEvaluationException("QueryPlan interrupted");
    }

    private boolean selectRow() {
        Mapping row = memory.store(query, producer, nested);
        if (row == null || !nested && !eval.acceptable(row)
                || accepted++ < (nested || !applySlice ? 0 : query.getOffset())) return false;
        if (++emitted >= limit()) close();
        next = row;
        return true;
    }

    /** Requests cooperative cancellation without accessing the owner's binding frames. */
    public void cancel() {
        cancellation.finish();
    }

    private long limit() {
        return nested || !applySlice ? Long.MAX_VALUE : query.getLimit();
    }

    private boolean advance() {
        if (frames[depth] == null) frames[depth] = frame(instructions.get(depth), scopeGraph, depth);
        return frames[depth].advance();
    }

    private Frame frame(Exp exp, Node graph, int index) {
        if (exp.isFail()) return new OnceFrame(exp, graph, index, false);
        return switch (exp.type()) {
            case EDGE -> new EdgeFrame(exp, graph, index);
            case VALUES -> new ValuesFrame(exp, index);
            case JOIN -> new JoinFrame(exp, graph, index);
            case UNION -> new UnionFrame(exp, graph, index);
            case GRAPH -> new GraphFrame(exp, index);
            case OPTIONAL -> new OptionalFrame(exp, graph, index);
            default -> new OnceFrame(exp, graph, index, true);
        };
    }

    @Override
    public Mapping next() {
        if (!hasNext()) throw new NoSuchElementException();
        Mapping row = next;
        next = null;
        return row;
    }

    /** @return whether the limit was reached and the final buffered row consumed */
    public boolean isFinished() {
        return closed && next == null;
    }

    /** Releases active frames from the innermost binding scope outward. Idempotent. */
    @Override
    public void close() {
        next = null;
        if (closed) return;
        closed = true;
        try {
            for (int i = frames.length - 1; i >= 0; i--) {
                if (frames[i] != null) {
                    frames[i].close();
                    frames[i] = null;
                }
            }
        } finally {
            try {
                eval.getEvaluator().finish(memory);
            } finally {
                if (!nested) producer.finish(query);
            }
        }
    }

    private interface Frame extends AutoCloseable {
        boolean advance();
        @Override void close();
    }

    /** Pure triples can reuse bindings; lexical expression scopes need fresh memory. */
    private Frame scope(Exp body, Node graph, int index) {
        return tripleJoin(body) ? new Pipeline(body, graph, index) : new ScopedFrame(body, graph, index);
    }

    /** Evaluates a lexical group independently, then joins each local row into its caller. */
    private final class ScopedFrame implements Frame {
        private final SelectCursor child;
        private final int index;
        private Mapping bound;

        private ScopedFrame(Exp body, Node graph, int index) {
            this.index = index;
            Eval evaluator = Eval.create(producer, eval.getEvaluator(), eval.getMatcher());
            child = new SelectCursor(SelectCursor.this, evaluator, query, parameters, body, graph, true);
        }

        @Override
        public boolean advance() {
            release();
            while (child.hasNext()) {
                Mapping row = child.next();
                if (memory.push(row, index)) {
                    bound = row;
                    return true;
                }
            }
            return false;
        }

        private void release() {
            if (bound != null) {
                memory.pop(bound);
                bound = null;
            }
        }

        @Override
        public void close() {
            release();
            child.close();
        }
    }

    private final class JoinFrame implements Frame {
        private final Exp exp;
        private final Node graph;
        private final int index;
        private final Frame left;
        private Frame right;

        private JoinFrame(Exp exp, Node graph, int index) {
            this.exp = exp;
            this.graph = graph;
            this.index = index;
            left = scope(exp.first(), graph, index);
        }

        @Override
        public boolean advance() {
            if (right != null && right.advance()) return true;
            releaseRight();
            while (left.advance() && !cancellation.isStop()) {
                right = scope(exp.rest(), graph, index + 1);
                if (right.advance()) return true;
                releaseRight();
            }
            return false;
        }

        private void releaseRight() {
            if (right != null) {
                right.close();
                right = null;
            }
        }

        @Override
        public void close() {
            releaseRight();
            left.close();
        }
    }

    /** A resumable binding scope; closing it restores its input environment. */
    private final class Pipeline implements Frame {
        private final List<Exp> steps = new ArrayList<>();
        private final Frame[] active;
        private final Node graph;
        private final int base;
        private int position;
        private boolean yielded;

        private Pipeline(Exp exp, Node graph, int base) {
            this.graph = graph;
            this.base = base;
            append(steps, exp);
            active = new Frame[steps.size()];
        }

        @Override
        public boolean advance() {
            if (yielded) {
                position--;
                yielded = false;
            }
            while (position >= 0 && !cancellation.isStop()) {
                checkInterrupted();
                if (position == active.length) {
                    yielded = true;
                    return true;
                }
                if (active[position] == null) active[position] = frame(steps.get(position), graph, base + position);
                if (active[position].advance()) {
                    position++;
                } else {
                    active[position].close();
                    active[position--] = null;
                }
            }
            return false;
        }

        @Override
        public void close() {
            for (int i = active.length - 1; i >= 0; i--) {
                if (active[i] != null) {
                    active[i].close();
                    active[i] = null;
                }
            }
            position = -1;
        }
    }

    private final class UnionFrame implements Frame {
        private final Exp exp;
        private final Node graph;
        private final int index;
        private int branch;
        private Frame current;

        private UnionFrame(Exp exp, Node graph, int index) {
            this.exp = exp;
            this.graph = graph;
            this.index = index;
        }

        @Override
        public boolean advance() {
            while (!cancellation.isStop()) {
                if (current != null && current.advance()) return true;
                close();
                if (branch == exp.size()) return false;
                current = scope(exp.get(branch++), graph, index);
            }
            return false;
        }

        @Override
        public void close() {
            if (current != null) {
                current.close();
                current = null;
            }
        }
    }

    private final class OptionalFrame implements Frame {
        private final Exp exp;
        private final Node graph;
        private final int index;
        private final Frame left;
        private Frame right;
        private boolean matched;

        private OptionalFrame(Exp exp, Node graph, int index) {
            this.exp = exp;
            this.graph = graph;
            this.index = index;
            left = scope(exp.first(), graph, index);
        }

        @Override
        public boolean advance() {
            while (!cancellation.isStop()) {
                if (right != null) {
                    if (right.advance()) {
                        matched = true;
                        return true;
                    }
                    right.close();
                    right = null;
                    if (!matched) return unmatched();
                }
                if (!left.advance()) return false;
                matched = false;
                right = scope(exp.rest(), graph, index);
            }
            return false;
        }

        private boolean unmatched() {
            matched = true;
            return true;
        }

        @Override
        public void close() {
            if (right != null) right.close();
            right = null;
            left.close();
        }
    }

    private final class GraphFrame implements Frame {
        private final Exp body;
        private final Node name;
        private final int index;
        private final Iterator<Node> graphs;
        private Frame current;
        private Node selected;
        private boolean bound;

        private GraphFrame(Exp exp, int index) {
            body = exp.get(1);
            name = exp.getGraphName();
            this.index = index;
            graphs = producer.getGraphNodes(name, query.getFrom(name), memory).iterator();
        }

        @Override
        public boolean advance() {
            releaseName();
            while (!cancellation.isStop()) {
                if (advanceBody()) return true;
                close();
                if (!graphs.hasNext()) return false;
                selected = graphs.next();
                if (eval.getMatcher().match(name, selected, memory)) {
                    current = scope(body, selected, index + 1);
                }
            }
            return false;
        }

        private boolean advanceBody() {
            while (current != null && current.advance()) {
                if (memory.push(name, selected, index)) {
                    bound = true;
                    return true;
                }
            }
            return false;
        }

        private void releaseName() {
            if (bound) {
                memory.pop(name);
                bound = false;
            }
        }

        @Override
        public void close() {
            releaseName();
            if (current != null) {
                current.close();
                current = null;
            }
        }
    }

    private final class EdgeFrame implements Frame {
        private final Edge pattern;
        private final int index;
        private final Iterable<Edge> source;
        private final Iterator<Edge> candidates;
        private boolean bound;

        private EdgeFrame(Exp exp, Node graph, int index) {
            this.index = index;
            pattern = exp.getEdge();
            memory.setExp(exp);
            source = producer.getEdges(graph, query.getFrom(graph), pattern, memory);
            candidates = source.iterator();
        }

        @Override
        public boolean advance() {
            release();
            while (candidates.hasNext()) {
                Edge candidate = candidates.next();
                if (candidate != null && eval.getMatcher().match(pattern, candidate, memory)
                        && (!pattern.isMatchArity() || pattern.nbNode() == candidate.nbNode())
                        && memory.push(producer, pattern, candidate, index)) {
                    bound = true;
                    return true;
                }
            }
            return false;
        }

        private void release() {
            if (bound) {
                memory.pop(pattern);
                bound = false;
            }
        }

        @Override
        public void close() {
            release();
            producer.closeEdges(source);
        }
    }

    private final class ValuesFrame implements Frame {
        private final Exp exp;
        private final Iterator<Mapping> rows;
        private final int index;
        private Mapping bound;

        private ValuesFrame(Exp exp, int index) {
            this.exp = exp;
            this.index = index;
            rows = exp.getMappings().iterator();
        }

        @Override
        public boolean advance() {
            close();
            while (rows.hasNext()) {
                checkInterrupted();
                Mapping row = rows.next();
                if (eval.valuesBinding(exp.getNodeList(), row, index)) {
                    bound = row;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void close() {
            if (bound != null) {
                eval.free(exp.getNodeList(), bound);
                bound = null;
            }
        }
    }

    private final class OnceFrame implements Frame {
        private final Exp exp;
        private final boolean enabled;
        private final Node graph;
        private final int index;
        private boolean used;
        private boolean bound;

        private OnceFrame(Exp exp, Node graph, int index, boolean enabled) {
            this.exp = exp;
            this.graph = graph;
            this.index = index;
            this.enabled = enabled;
        }

        @Override
        public boolean advance() {
            close();
            if (used || !enabled) return false;
            used = true;
            return switch (exp.type()) {
                case FILTER -> eval.test(graph, exp.getFilter(), memory, producer);
                case BIND -> bind();
                default -> true;
            };
        }

        private boolean bind() {
            Node value;
            try {
                value = eval.eval(graph, exp.getFilter(), memory, producer);
            } catch (QueryTypeErrorException error) {
                return true;
            }
            if (value == null) return true;
            bound = memory.push(exp.getNode(), value, index);
            return bound;
        }

        @Override
        public void close() {
            if (bound) {
                memory.pop(exp.getNode());
                bound = false;
            }
        }
    }
}
