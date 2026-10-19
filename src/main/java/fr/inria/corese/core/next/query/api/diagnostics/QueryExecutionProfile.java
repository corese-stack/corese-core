package fr.inria.corese.core.next.query.api.diagnostics;

import java.util.List;
import java.util.Objects;

/**
 * Immutable measurements from an explicitly instrumented evaluation. Timings
 * include instrumentation overhead and are wall-clock durations, not CPU times.
 * Patterns follow the compiled traversal order within each operator scope; this
 * is not a chronological trace of joins, branches or repeated invocations.
 *
 * @param compilationNanos time translating the parsed AST and capturing its compilation image;
 *                         excludes parsing and initial query preparation
 * @param planningNanos time completing the runtime query, including ordering,
 *                      statistics and node indexing
 * @param executionSetupNanos time creating independent execution containers from a
 *                            compilation image; zero for direct non-prepared calls
 * @param planReused whether a cached compilation image was reused without translation or planning
 * @param evaluationNanos time executing the completed runtime query, including
 *                        result mappings and cursor cleanup; excludes
 *                        client idle time and graph template materialization
 * @param patterns ordered logical patterns, followed by auxiliary runtime lookups
 *                  such as path steps and dynamically compiled EXISTS patterns
 * @param delivery actual result-delivery mode, fallback reason and first-row observation
 */
public record QueryExecutionProfile(long compilationNanos, long planningNanos, long executionSetupNanos,
        long evaluationNanos, boolean planReused, List<Pattern> patterns, ResultDelivery delivery) {

    /**
     * Copies the pattern list so the profile retains no mutable engine state.
     *
     * @throws IllegalArgumentException if a duration is negative
     * @throws NullPointerException if the list, an element or the delivery observation is null
     */
    public QueryExecutionProfile {
        if (compilationNanos < 0 || planningNanos < 0 || executionSetupNanos < 0 || evaluationNanos < 0) {
            throw new IllegalArgumentException("Durations must be non-negative");
        }
        patterns = List.copyOf(patterns);
        Objects.requireNonNull(delivery, "delivery");
    }

    /** How this invocation makes its result available to the caller. */
    public enum ExecutionMode {
        /** Eligible SELECT rows are produced as the caller advances the cursor. */
        STREAMING,
        /** Evaluation completes before the result is returned. */
        MATERIALIZED
    }

    /** First guard that selected full evaluation; this is not an exhaustive query analysis. */
    public enum FallbackReason {
        /** No fallback: a progressive cursor was actually opened. */
        NONE,
        /** The invocation is ASK, CONSTRUCT or DESCRIBE rather than SELECT. */
        QUERY_FORM,
        /** ORDER BY requires completing the input before delivering the selected rows. */
        ORDER_BY,
        /** GROUP BY, aggregate expressions or HAVING require full evaluation. */
        AGGREGATION,
        /** A property path uses the established complete evaluator. */
        PROPERTY_PATH,
        /** A nested SELECT uses the established subquery evaluator. */
        SUBQUERY,
        /** MINUS uses the established buffered evaluator. */
        MINUS,
        /** SERVICE uses the established service evaluator. */
        SERVICE,
        /** An OPTIONAL condition or right-hand scope requires full evaluation. */
        COMPLEX_OPTIONAL,
        /** A scoped join retains its buffered strategy instead of repeated independent scans. */
        SCOPED_JOIN,
        /** Repeated evaluation of an independent scope would risk quadratic work. */
        REPEATED_SCOPE,
        /** Another operator or extension is not supported by the progressive cursor. */
        UNSUPPORTED_OPERATOR,
        /** The backend cannot capture an independent stable read snapshot. */
        SNAPSHOT_UNAVAILABLE
    }

    /** Meaning of the first-row timing for this invocation. */
    public enum FirstRowStatus {
        /** First-row timing applies only to SELECT results. */
        NOT_APPLICABLE,
        /** Neither availability nor successful exhaustion has been observed, including early close or failure. */
        UNOBSERVED,
        /** At least one projected solution is ready, whether or not the caller consumed it. */
        AVAILABLE,
        /** Evaluation completed successfully with no projected solution after OFFSET and LIMIT. */
        EMPTY
    }

    /**
     * Result strategy and first-row observation, independent of the query's row count.
     * The timing accumulates work from entry into the profiled evaluation through
     * the first successful availability observation. It includes compilation and
     * planning, excludes initial preparation and client idle time, and is frozen
     * once a row is ready. Materialized SELECT rows are ready before evaluate returns;
     * streaming rows become ready in the first successful hasNext or next call.
     * Closing without a successful observation does not establish that a query is empty.
     *
     * @param mode actual result-delivery mode for this invocation
     * @param fallbackReason first materialization guard, or NONE for a progressive cursor
     * @param firstRowStatus whether a SELECT row is ready, absent or not observed; not applicable to other query forms
     * @param firstRowNanos accumulated active wall-clock nanoseconds to availability, or -1 unless AVAILABLE
     */
    public record ResultDelivery(ExecutionMode mode, FallbackReason fallbackReason,
            FirstRowStatus firstRowStatus, long firstRowNanos) {
        /**
         * Validates the strategy and observation without inventing a timing for an absent row.
         *
         * @throws NullPointerException if a classification is null
         * @throws IllegalArgumentException if the strategy or first-row timing is inconsistent
         */
        public ResultDelivery {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(fallbackReason, "fallbackReason");
            Objects.requireNonNull(firstRowStatus, "firstRowStatus");
            if ((mode == ExecutionMode.STREAMING) != (fallbackReason == FallbackReason.NONE)) {
                throw new IllegalArgumentException("Only progressive delivery has no fallback reason");
            }
            if ((firstRowStatus == FirstRowStatus.NOT_APPLICABLE) != (fallbackReason == FallbackReason.QUERY_FORM)) {
                throw new IllegalArgumentException("First-row observations apply only to SELECT");
            }
            if (firstRowNanos < -1 || (firstRowStatus == FirstRowStatus.AVAILABLE) != (firstRowNanos >= 0)) {
                throw new IllegalArgumentException("Only an available first row has a non-negative timing");
            }
        }
    }

    /**
     * A logical triple/path pattern or an auxiliary lookup compiled during execution (paths or EXISTS).
     * Estimates describe unbound patterns at planning time, before runtime bindings
     * and inter-graph deduplication. They are neither join-output predictions nor
     * guarantees about the state later read by execution. No extra counts are
     * requested for diagnostics: {@code -1} means the planner did not obtain one.
     *
     * @param location operator scope and child positions in the completed plan
     * @param pattern textual pattern, including its GRAPH scope when applicable
     * @param auxiliary whether this is an unplanned runtime lookup
     * @param estimatedCardinality planner's count, or {@code -1} if unavailable
     * @param iteratorOpenings successful openings of this producer iterable;
     *                         these are not necessarily storage cursors
     * @param candidateEdges edges returned by {@code next()} to the engine,
     *                       after producer deduplication and before RDF matching;
     *                       summed across invocations, not distinct solutions or
     *                       internal index visits
     */
    public record Pattern(String location, String pattern, boolean auxiliary,
            long estimatedCardinality, long iteratorOpenings, long candidateEdges) {
        /**
         * Checks the pattern description and non-negative counters.
         *
         * @throws NullPointerException if a description is null
         * @throws IllegalArgumentException if a count is outside its documented range
         */
        public Pattern {
            Objects.requireNonNull(location, "location");
            Objects.requireNonNull(pattern, "pattern");
            if (estimatedCardinality < -1 || iteratorOpenings < 0 || candidateEdges < 0) {
                throw new IllegalArgumentException("Invalid pattern counters");
            }
        }
    }

    /**
     * Formats the measurements without inspecting the repository or executing a query.
     *
     * @return a human-readable plan and its producer counters
     */
    public String format() {
        StringBuilder text = new StringBuilder("Compilation: ").append(compilationNanos)
                .append(" ns; planning: ").append(planningNanos)
                .append(" ns; execution setup: ").append(executionSetupNanos)
                .append(" ns; reused plan: ").append(planReused).append("; evaluation: ").append(evaluationNanos).append(" ns\n");
        text.append("Delivery: ").append(delivery.mode()).append("; fallback: ").append(delivery.fallbackReason())
                .append("; first row: ").append(delivery.firstRowStatus());
        if (delivery.firstRowStatus() == FirstRowStatus.AVAILABLE) {
            text.append(" (").append(delivery.firstRowNanos()).append(" ns)");
        }
        text.append('\n');
        for (Pattern entry : patterns) {
            text.append(entry.location()).append(" ").append(entry.pattern())
                    .append(" | estimate=").append(entry.estimatedCardinality() < 0
                            ? "unknown" : Long.toString(entry.estimatedCardinality()))
                    .append("; iterations=").append(entry.iteratorOpenings())
                    .append("; candidates=").append(entry.candidateEdges()).append('\n');
        }
        return text.toString();
    }
}
