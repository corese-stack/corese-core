package fr.inria.corese.core.next.query.impl.engine.diagnostics;

import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.storage.StorageManagerProducer;
import fr.inria.corese.core.next.storage.api.StorageManager;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Opt-in producer decorator. The ordinary producer and its cursors have no
 * diagnostic fields, counter updates or per-statement branches. The underlying
 * producer retains cursor ownership, cancellation and snapshot release.
 */
public final class DiagnosticProducer extends StorageManagerProducer {
    private final ExecutionDiagnostics diagnostics;

    /**
     * Creates a request-scoped instrumented producer.
     *
     * @param storage session-visible storage
     * @param diagnostics collector confined to the evaluation thread
     * @throws NullPointerException if either argument is null
     */
    public DiagnosticProducer(StorageManager storage, ExecutionDiagnostics diagnostics) {
        super(storage);
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    @Override
    public long estimateCardinality(Query query, Node graphNode, Edge edge) {
        long count = super.estimateCardinality(query, graphNode, edge);
        diagnostics.estimated(edge, count);
        return count;
    }

    @Override
    public Iterable<Edge> getEdges(Node graphNode, List<Node> from, Edge queryEdge, Environment environment) {
        Iterable<Edge> delegate = super.getEdges(graphNode, from, queryEdge, environment);
        return new DiagnosticEdges(delegate, diagnostics.executing(queryEdge, graphNode));
    }

    @Override
    public void closeEdges(Iterable<Edge> edges) {
        super.closeEdges(edges instanceof DiagnosticEdges measured ? measured.delegate : edges);
    }

    /** Wraps only explicitly profiled traversals and preserves repeatable iteration. */
    private record DiagnosticEdges(Iterable<Edge> delegate,
            ExecutionDiagnostics.PatternCounters counters) implements Iterable<Edge> {
        @Override
        public Iterator<Edge> iterator() {
            Iterator<Edge> source = delegate.iterator();
            counters.opened();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return source.hasNext();
                }

                @Override
                public Edge next() {
                    Edge edge = source.next();
                    counters.returned();
                    return edge;
                }
            };
        }
    }
}
