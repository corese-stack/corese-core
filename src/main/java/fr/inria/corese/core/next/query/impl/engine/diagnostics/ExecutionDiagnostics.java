package fr.inria.corese.core.next.query.impl.engine.diagnostics;

import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.ExecutionMode;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.FallbackReason;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.FirstRowStatus;
import fr.inria.corese.core.next.query.api.diagnostics.QueryExecutionProfile.ResultDelivery;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfc10.StatementUtils;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.path.PropertyPathEdge;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Collector confined to the evaluation thread. Timeout cancellation closes the
 * producer but never reads or updates these counters. Only immutable strings,
 * counts and copied lists escape through {@link #snapshot()}.
 */
public final class ExecutionDiagnostics {
    private final LongSupplier nanoTime;
    private final IdentityHashMap<Edge, PatternCounters> counters = new IdentityHashMap<>();
    private final Map<String, PatternCounters> auxiliary = new HashMap<>();
    private final List<PatternCounters> ordered = new ArrayList<>();
    private final Set<Exp> expressions = Collections.newSetFromMap(new IdentityHashMap<>());
    private long compilationNanos;
    private long planningNanos;
    private long evaluationNanos;
    private long executionSetupNanos;
    private boolean planReused;
    private ExecutionMode mode = ExecutionMode.MATERIALIZED;
    private FallbackReason fallbackReason = FallbackReason.QUERY_FORM;
    private FirstRowStatus firstRowStatus = FirstRowStatus.NOT_APPLICABLE;
    private long firstRowNanos = -1;
    private long activeNanos;
    private long workStarted;
    private int workDepth;

    /** Creates an evaluation-thread collector using the monotonic system clock. */
    public ExecutionDiagnostics() {
        this(System::nanoTime);
    }

    /** Creates a collector with a monotonic clock, allowing deterministic timing verification. */
    ExecutionDiagnostics(LongSupplier nanoTime) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    }

    /** Starts a segment of query work; nested segments do not double-count time. */
    public void beginWork() {
        if (workDepth++ == 0) workStarted = nanoTime.getAsLong();
    }

    /**
     * Ends a query-work segment, excluding subsequent client idle time.
     *
     * @return elapsed nanoseconds for an outer segment, or zero for a nested segment
     * @throws IllegalStateException if no query-work segment is active
     */
    public long endWork() {
        if (workDepth == 0) throw new IllegalStateException("No diagnostic work segment is active");
        if (--workDepth != 0) return 0;
        long elapsed = nanoTime.getAsLong() - workStarted;
        activeNanos += elapsed;
        return elapsed;
    }

    /**
     * Records the actual SELECT strategy after checking algebra and snapshot availability.
     *
     * @param reason NONE for streaming, otherwise the guard selecting full evaluation
     * @throws IllegalArgumentException if the reason describes a non-SELECT query form
     */
    public void selectDelivery(FallbackReason reason) {
        Objects.requireNonNull(reason, "reason");
        if (reason == FallbackReason.QUERY_FORM) throw new IllegalArgumentException("Expected a SELECT strategy");
        fallbackReason = reason;
        mode = reason == FallbackReason.NONE ? ExecutionMode.STREAMING : ExecutionMode.MATERIALIZED;
        firstRowStatus = FirstRowStatus.UNOBSERVED;
        firstRowNanos = -1;
    }

    /**
     * Records the first successful SELECT availability or exhaustion observation.
     * Later observations cannot overwrite the first timing or turn an early close into emptiness.
     *
     * @param available whether a projected solution is ready after OFFSET and LIMIT
     */
    public void firstRow(boolean available) {
        if (firstRowStatus != FirstRowStatus.UNOBSERVED) return;
        firstRowStatus = available ? FirstRowStatus.AVAILABLE : FirstRowStatus.EMPTY;
        if (available) {
            firstRowNanos = activeNanos + (workDepth == 0 ? 0 : nanoTime.getAsLong() - workStarted);
        }
    }

    /**
     * Records runtime-query translation duration.
     *
     * @param nanos elapsed wall-clock nanoseconds
     */
    public void compilationTime(long nanos) {
        compilationNanos += nanos;
    }

    /**
     * Records completion duration and captures the resulting operator order.
     *
     * @param query completed runtime query
     * @param nanos elapsed completion nanoseconds
     */
    public void planned(Query query, long nanos) {
        planningNanos += nanos;
        visit(query, "query", null);
    }

    /**
     * Records evaluation duration, excluding completion and plan capture.
     *
     * @param nanos elapsed evaluation nanoseconds
     */
    public void evaluationTime(long nanos) {
        evaluationNanos += nanos;
    }

    /** Records structural execution-frame creation, independently of compilation and planning.
     * @param nanos elapsed frame creation nanoseconds
     */
    public void executionSetupTime(long nanos) {
        executionSetupNanos += nanos;
    }

    /** Records whether this evaluation used an already cached compilation image.
     * @param reused whether compilation and planning were reused
     */
    public void reusedPlan(boolean reused) {
        planReused = reused;
    }

    /**
     * Remembers an estimate already requested by the planner.
     *
     * @param edge identity of the compiled pattern
     * @param count estimate or {@code -1}
     */
    public void estimated(Edge edge, long count) {
        counters.computeIfAbsent(edge, ignored -> new PatternCounters()).estimate = count;
    }

    /**
     * Finds the counters for an executed edge, adding auxiliary runtime lookups when needed.
     *
     * @param edge compiled edge or dynamically compiled pattern
     * @param graph effective graph supplied to the producer
     * @return mutable counters confined to the evaluation thread
     */
    PatternCounters executing(Edge edge, Node graph) {
        PatternCounters entry = counters.get(edge);
        if (entry != null && entry.location != null) {
            return entry;
        }
        // Runtime EXISTS/path operators may be recreated for every binding.
        // Aggregate their identical descriptions without retaining each transient edge.
        String text = pattern(edge, graph);
        PatternCounters runtime = auxiliary.get(text);
        long estimate = entry == null ? -1 : entry.estimate;
        if (runtime == null) {
            runtime = new PatternCounters();
            runtime.estimate = estimate;
            describe(runtime, text, "runtime/auxiliary", true);
            auxiliary.put(text, runtime);
        } else if (runtime.estimate != estimate) {
            // Identical auxiliary descriptions may originate in different scopes.
            runtime.estimate = -1;
        }
        return runtime;
    }

    private void visit(Exp expression, String location, Node graph) {
        if (!expressions.add(expression)) {
            return;
        }
        Node scope = expression.isGraph() ? expression.getGraphName() : graph;
        String here = location + "/" + expression.type();
        if (expression.getEdge() != null) {
            Edge edge = expression.getEdge();
            PatternCounters entry = counters.computeIfAbsent(edge, ignored -> new PatternCounters());
            if (entry.location == null) {
                describe(entry, pattern(edge, scope), here, false);
            }
        }
        for (int i = 0; i < expression.size(); i++) {
            visit(expression.get(i), here + "[" + i + "]", scope);
        }
        // EXISTS expressions compile their actual operators during evaluation.
        // Do not call Expr.getPattern(): it would create a different, unexecuted tree.
    }

    private static String pattern(Edge edge, Node graph) {
        String predicate = edge instanceof PropertyPathEdge path
                ? path.path().toString() : node(edge.getProperty());
        String triple = node(edge.getNode(0)) + " " + predicate + " " + node(edge.getNode(1));
        return graph == null ? triple : "GRAPH " + node(graph) + " { " + triple + " }";
    }

    private static String node(Node node) {
        if (node == null) {
            return "*";
        }
        if (!node.isVariable() && node.getDatatypeValue() instanceof Value value) {
            return StatementUtils.serializeForComparison(value);
        }
        return node.toString();
    }

    private void describe(PatternCounters entry, String pattern, String location, boolean auxiliaryLookup) {
        entry.pattern = pattern;
        entry.location = location;
        entry.auxiliary = auxiliaryLookup;
        ordered.add(entry);
    }

    /**
     * Copies measurements without retaining compiled nodes, bindings or storage snapshots.
     *
     * @return immutable execution profile
     */
    public QueryExecutionProfile snapshot() {
        return new QueryExecutionProfile(compilationNanos, planningNanos, executionSetupNanos, evaluationNanos, planReused,
                ordered.stream().map(PatternCounters::snapshot).toList(),
                new ResultDelivery(mode, fallbackReason, firstRowStatus, firstRowNanos));
    }

    /** Mutable producer counters, never shared with the caller or watchdog thread. */
    static final class PatternCounters {
        private String location;
        private String pattern;
        private boolean auxiliary;
        private long estimate = -1;
        private long openings;
        private long candidates;

        /** Records a successful iterable opening. */
        void opened() {
            openings++;
        }

        /** Records one candidate returned by the producer iterator. */
        void returned() {
            candidates++;
        }

        private QueryExecutionProfile.Pattern snapshot() {
            return new QueryExecutionProfile.Pattern(location, pattern, auxiliary, estimate, openings, candidates);
        }
    }
}
