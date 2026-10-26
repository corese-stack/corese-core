package fr.inria.corese.core.next.query.impl.engine.pattern;

import fr.inria.corese.core.next.query.impl.engine.diagnostics.ExecutionDiagnostics;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import java.util.List;
import java.util.Map;

/**
 * Immutable compilation image, enforced by exclusive ownership rather than by
 * reusing an executable Query. Capture detaches every mutable component from
 * the compiler. The private image is never evaluated or exposed; each execution
 * receives another independent graph. Only immutable syntax, RDF values and
 * private read-only prologue lookups are shared. No producer, storage cursor,
 * result mappings, initial bindings or execution context is retained.
 */
public final class CompiledQueryPlan {
    private final QueryPlan image;
    private final List<Estimate> estimates;

    private CompiledQueryPlan(QueryPlan query, Map<Edge, Long> counts) {
        PlanCopy copies = new PlanCopy();
        image = copies.copy(query);
        estimates = counts.entrySet().stream()
                .map(entry -> new Estimate(copies.copy(entry.getKey()), entry.getValue())).toList();
    }

    /**
     * Captures a completed query plan without retaining any compiler-owned mutable object.
     *
     * @param query completed native query plan
     * @param counts estimates obtained while ordering its patterns
     * @return immutable compilation image
     */
    public static CompiledQueryPlan capture(QueryPlan query, Map<Edge, Long> counts) {
        return new CompiledQueryPlan(query, counts);
    }

    /**
     * Creates independent runtime containers without AST translation or planning.
     *
     * @param diagnostics optional request-scoped collector, or null
     * @return completed query plan owned by one execution
     */
    public QueryPlan newExecution(ExecutionDiagnostics diagnostics) {
        PlanCopy copies = new PlanCopy();
        QueryPlan runtime = copies.copy(image);
        if (diagnostics != null) {
            for (Estimate estimate : estimates) {
                diagnostics.estimated(copies.copy(estimate.edge()), estimate.cardinality());
            }
        }
        return runtime;
    }

    private record Estimate(Edge edge, long cardinality) { }
}
