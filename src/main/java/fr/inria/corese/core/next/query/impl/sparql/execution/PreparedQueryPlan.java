package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.query.api.dataset.Dataset;
import fr.inria.corese.core.next.query.impl.engine.diagnostics.ExecutionDiagnostics;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.CompiledQueryPlan;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.storage.StorageManagerProducer;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.storage.api.StorageManager;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Prepared-query-owned compilation cache, confined to its connection's calling
 * thread. The AST and cached compilation image carry no execution state. Bindings
 * and timeouts are deliberately absent from the cache key: cardinalities describe
 * the unbound pattern, and runtime bindings are supplied to a fresh Eval/Memory.
 */
public final class PreparedQueryPlan {
    private final QueryAst ast;
    private CachedPlan cached;

    PreparedQueryPlan(QueryAst ast) {
        this.ast = Objects.requireNonNull(ast, "ast");
    }

    Query resolve(SparqlQueryExecutor executor, StorageManager storage, Dataset dataset,
            ExecutionDiagnostics diagnostics) {
        long version = storage.queries().getDataVersion();
        CachedPlan previous = cached;
        if (version >= 0 && previous != null && previous.version() == version
                && sameDataset(previous.dataset(), dataset)) {
            return instantiate(previous.plan(), diagnostics);
        }
        Query compiled = executor.compile(ast);
        if (dataset != null) executor.applyDataset(compiled, dataset);
        RecordingProducer statistics = new RecordingProducer(storage);
        long started = diagnostics == null ? 0 : System.nanoTime();
        compiled.complete(statistics);
        long planning = diagnostics == null ? 0 : System.nanoTime() - started;
        if (version < 0) {
            if (diagnostics != null) {
                statistics.counts.forEach(diagnostics::estimated);
                diagnostics.planned(compiled, planning);
            }
            return compiled;
        }
        started = diagnostics == null ? 0 : System.nanoTime();
        CompiledQueryPlan image = CompiledQueryPlan.capture(compiled, statistics.counts);
        if (diagnostics != null) diagnostics.compilationTime(System.nanoTime() - started);
        // Do not label a plan with a version published by a racing writer. Estimates
        // can be stale during concurrent writes, but are never used to prune results.
        if (version >= 0 && storage.queries().getDataVersion() == version) {
            cached = new CachedPlan(version, dataset, image);
        }
        // Capture has already detached the image. The freshly compiled graph can
        // serve this first execution directly; no second structural copy is needed.
        if (diagnostics != null) {
            statistics.counts.forEach(diagnostics::estimated);
            diagnostics.planned(compiled, planning);
        }
        return compiled;
    }

    private static Query instantiate(CompiledQueryPlan image, ExecutionDiagnostics diagnostics) {
        long started = diagnostics == null ? 0 : System.nanoTime();
        Query query = image.newExecution(diagnostics);
        if (diagnostics != null) {
            diagnostics.executionSetupTime(System.nanoTime() - started);
            diagnostics.reusedPlan(true);
            diagnostics.planned(query, 0);
        }
        return query;
    }

    private static boolean sameDataset(Dataset previous, Dataset current) {
        return previous == current || previous != null && current != null
                && sameOrder(previous.getDefaultGraphs(), current.getDefaultGraphs())
                && sameOrder(previous.getNamedGraphs(), current.getNamedGraphs());
    }

    private static boolean sameOrder(Set<IRI> previous, Set<IRI> current) {
        if (previous.size() != current.size()) return false;
        Iterator<IRI> left = previous.iterator();
        Iterator<IRI> right = current.iterator();
        while (left.hasNext()) {
            if (!left.next().equals(right.next())) return false;
        }
        return true;
    }

    /** Counts only during a cache miss; execution uses its separate ordinary/diagnostic producer. */
    private static final class RecordingProducer extends StorageManagerProducer {
        private final Map<Edge, Long> counts = new IdentityHashMap<>();

        private RecordingProducer(StorageManager storage) {
            super(storage);
        }

        @Override
        public long estimateCardinality(Query query, Node graph, Edge edge) {
            long count = super.estimateCardinality(query, graph, edge);
            counts.put(edge, count);
            return count;
        }
    }

    private record CachedPlan(long version, Dataset dataset, CompiledQueryPlan plan) { }
}
