package fr.inria.corese.core.next.query.impl.engine.storage;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.path.NativePropertyPathEvaluator;
import fr.inria.corese.core.next.query.impl.engine.path.PropertyPathEdge;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.planning.QueryStatistics;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.storage.api.StorageManager;
import fr.inria.corese.core.next.storage.api.operations.StatementSpliterator;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Stream;

/**
 * KGRAM producer backed by a Corese-next {@link StorageManager}.
 *
 * <p>The producer is responsible for translating KGRAM graph-pattern requests
 * into storage-layer {@link fr.inria.corese.core.next.storage.api.model.StatementPattern} queries,
 * then adapting returned RDF statements back into KGRAM edges. It does not decide the final RDF-term
 * matching policy; that remains the matcher responsibility.</p>
 */
public class StorageManagerProducer extends ProducerDefault implements QueryStatistics {

    private final StorageManager storage;
    private final Set<EdgeCursor> cursors = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean closed;

    /**
     * Creates a KGRAM producer backed by the given storage manager.
     *
     * @param storage storage manager queried by this producer
     */
    public StorageManagerProducer(StorageManager storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    /**
     * Counts a pattern directly in the session-visible indexes. No cursor or
     * transaction is created. Complexity depends on the selected index prefix and
     * number of contexts; a merged default graph count is an upper bound because
     * identical triples in different graphs are counted separately.
     *
     * @param query query supplying dataset restrictions
     * @param graphNode enclosing graph, or {@code null} for the default graph
     * @param edge triple pattern to estimate
     * @return matching statement count, or unknown for paths and variable graphs
     */
    @Override
    public long estimateCardinality(Query query, Node graphNode, Edge edge) {
        if (edge instanceof PropertyPathEdge || graphNode != null && graphNode.isVariable()) {
            return UNKNOWN_CARDINALITY;
        }
        checkInterrupted();
        List<Node> contexts = query.getFrom(graphNode);
        if (query.isDatasetSpecified() && contexts.isEmpty()) {
            return 0;
        }
        var pattern = StoragePatternTranslator.translate(graphNode, contexts, edge, null);
        if (pattern.noMatch()) {
            return 0;
        }
        try {
            return storage.queries().count(pattern.statementPattern());
        } catch (UnsupportedOperationException unavailable) {
            return UNKNOWN_CARDINALITY;
        }
    }

    @Override
    public Iterable<Edge> getEdges(Node graphNode, List<Node> from, Edge queryEdge, Environment environment) {
        Objects.requireNonNull(queryEdge, "queryEdge");
        if (queryEdge instanceof PropertyPathEdge path) {
            return NativePropertyPathEvaluator.evaluate(this, graphNode, from, path, environment);
        }

        StoragePatternTranslator.StorageQueryPattern queryPattern =
                StoragePatternTranslator.translate(graphNode, from, queryEdge, environment);
        if (queryPattern.noMatch()) {
            return List.of();
        }

        StatementPattern pattern = queryPattern.statementPattern();
        // Statements within one explicitly selected RDF graph already form a set.
        boolean deduplicate = graphNode == null && pattern.getContexts().length != 1;
        return new EdgeIterable(pattern, deduplicate);
    }

    /** Repeatable sequential iterable; each iteration owns one storage snapshot. */
    private final class EdgeIterable implements Iterable<Edge>, AutoCloseable {
        private final StatementPattern pattern;
        private final boolean deduplicate;
        private EdgeCursor cursor;

        private EdgeIterable(StatementPattern pattern, boolean deduplicate) {
            this.pattern = pattern;
            this.deduplicate = deduplicate;
        }

        @Override
        public Iterator<Edge> iterator() {
            close();
            synchronized (cursors) {
                checkInterrupted();
                if (closed) {
                    throw new QueryEvaluationException("Query producer is closed");
                }
                cursor = new EdgeCursor(storage.queries().find(pattern), deduplicate);
                cursors.add(cursor);
                return cursor;
            }
        }

        @Override
        public void close() {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }
    }

    private final class EdgeCursor implements Iterator<Edge>, AutoCloseable {
        private Stream<Statement> stream;
        private Iterator<Statement> statements;
        private final Set<TripleKey> seen;
        private Statement next;

        // Release the owned snapshot before rethrowing a traversal setup failure unchanged.
        @SuppressWarnings("java:S1181")
        private EdgeCursor(Stream<Statement> stream, boolean deduplicate) {
            this.stream = stream;
            try {
                var source = stream.spliterator();
                // Read-version metadata, never a separate live metadata lookup:
                // a writer may change the graph registry while this cursor is open.
                boolean distinct = !deduplicate || source instanceof StatementSpliterator traversal
                        && traversal.hasDistinctTriples();
                this.seen = deduplicate && !distinct ? new HashSet<>() : null;
                statements = Spliterators.iterator(source);
            } catch (RuntimeException | Error failure) {
                closeAfterFailure(stream::close, failure);
                throw failure;
            }
        }

        // Error is rethrown unchanged after releasing the snapshot, as for RuntimeException.
        @Override
        @SuppressWarnings("java:S1181")
        public synchronized boolean hasNext() {
            if (stream == null) {
                return false;
            }
            try {
                checkInterrupted();
                if (next != null) {
                    return true;
                }
                while (statements.hasNext()) {
                    checkInterrupted();
                    Statement candidate = statements.next();
                    // The merged default graph must match each RDF triple once.
                    if (seen == null || seen.add(new TripleKey(candidate.getSubject(),
                            candidate.getPredicate(), candidate.getObject()))) {
                        next = candidate;
                        return true;
                    }
                }
                close();
                return false;
            } catch (RuntimeException | Error failure) {
                closeAfterFailure(this::close, failure);
                throw failure;
            }
        }

        // Edge adaptation failure must also release the snapshot before propagation.
        @Override
        @SuppressWarnings("java:S1181")
        public synchronized Edge next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Statement statement = next;
            next = null;
            try {
                return new StorageManagerEdge(statement);
            } catch (RuntimeException | Error failure) {
                closeAfterFailure(this::close, failure);
                throw failure;
            }
        }

        @Override
        public synchronized void close() {
            Stream<Statement> current = stream;
            stream = null;
            statements = null;
            next = null;
            if (seen != null) {
                seen.clear();
            }
            if (current != null) {
                try {
                    current.close();
                } finally {
                    synchronized (cursors) {
                        cursors.remove(this);
                    }
                }
            }
        }
    }

    /** Preserves the original traversal failure when a close callback also fails. */
    @SuppressWarnings("java:S1181") // Fatal failures retain their identity after resource cleanup.
    private static void closeAfterFailure(Runnable release, Throwable failure) {
        try {
            release.run();
        } catch (RuntimeException | Error cleanup) {
            if (cleanup != failure) failure.addSuppressed(cleanup);
        }
    }

    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new QueryEvaluationException("Query evaluation was interrupted");
        }
    }

    @Override
    public boolean evaluatesBGPWithEdges() {
        return true;
    }

    @Override
    public void closeEdges(Iterable<Edge> edges) {
        if (edges instanceof StorageManagerProducer.EdgeIterable iterable) {
            iterable.close();
        }
    }

    /** Closes remaining cursors on completion, failure or cancellation. */
    private void closeCursors(boolean terminal) {
        List<EdgeCursor> pending;
        synchronized (cursors) {
            closed |= terminal;
            pending = new ArrayList<>(cursors);
            cursors.clear();
        }
        // Never acquire a cursor monitor while holding the registry monitor:
        // timeout cancellation may run concurrently with cursor traversal.
        RuntimeException failure = null;
        for (EdgeCursor cursor : pending) {
            try {
                cursor.close();
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }

    @Override
    public void close() {
        closeCursors(true);
    }

    private record TripleKey(Resource subject, IRI predicate, Value object) { }

    @Override
    public Iterable<Node> getGraphNodes(Node graphNode, List<Node> from, Environment environment) {
        List<Node> namedGraphs = StoragePatternTranslator.isExplicitDataset(environment)
                ? environment.getQuery().getNamed()
                : from;
        if (StoragePatternTranslator.isExplicitDataset(environment) && (namedGraphs == null || namedGraphs.isEmpty())) {
            return List.of();
        }
        List<Node> nodes = new ArrayList<>();
        for (Resource context : storage.metadata().getContexts()) {
            Node node = NodeImpl.forValue(context);
            if (StoragePatternTranslator.matchesFrom(node, namedGraphs, environment)) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    @Override
    public void start(Query query) {
        // Cursors are registered when their iterators are opened.
    }

    @Override
    public void finish(Query query) {
        closeCursors(false);
    }

    @Override
    public Node getNode(Object value) {
        if (value instanceof Node node) {
            return node;
        }
        if (value instanceof DatatypeValue datatypeValue) {
            return NodeImpl.forValue(datatypeValue);
        }
        return null;
    }

    @Override
    public DatatypeValue getDatatypeValue(Object value) {
        if (value instanceof Node node) {
            return node.getDatatypeValue();
        }
        if (value instanceof DatatypeValue datatypeValue) {
            return datatypeValue;
        }
        return null;
    }

    @Override
    public DatatypeValue getValue(Object value) {
        return getDatatypeValue(value);
    }

    @Override
    public boolean isBindable(Node node) {
        return node != null && node.isVariable();
    }

    /**
     * Materializes mappings for basic graph patterns made of triple {@code EDGE} expressions.
     *
     * <p>Higher-level algebra remains handled by {@link fr.inria.corese.core.next.query.impl.engine.eval.Eval};
     * paths, services and values are explicit follow-ups.</p>
     */
    @Override
    public Mappings getMappings(Node graphNode, List<Node> from, Exp exp, Environment environment) {
        if (!exp.isBGP()) {
            throw new IllegalArgumentException("StorageManagerProducer can only materialize BGP expressions");
        }

        List<StorageBindingSet> bindings = new ArrayList<>();
        bindings.add(new StorageBindingSet());
        for (Exp element : exp) {
            if (!element.isEdge()) {
                throw new IllegalArgumentException(
                        "StorageManagerProducer can only materialize EDGE expressions inside BGP mappings");
            }
            bindings = join(graphNode, from, element.getEdge(), environment, bindings);
            if (bindings.isEmpty()) {
                break;
            }
        }

        Mappings mappings = Mappings.create(environment.getQuery());
        for (StorageBindingSet binding : bindings) {
            mappings.add(binding.toMapping());
        }
        return mappings;
    }

    /**
     * Extends partial bindings with the matches of one triple pattern.
     *
     * <p>Each input binding is exposed through a layered {@link StorageBindingEnvironment}, so already
     * bound variables are pushed into {@link StoragePatternTranslator#translate} before
     * querying storage.</p>
     *
     * @param graphNode active graph node
     * @param from active dataset restriction
     * @param queryEdge triple pattern to join
     * @param environment base KGRAM environment
     * @param inputBindings bindings produced by previous BGP edges
     * @return bindings extended with the matches of {@code queryEdge}
     */
    private List<StorageBindingSet> join(
            Node graphNode,
            List<Node> from,
            Edge queryEdge,
            Environment environment,
            List<StorageBindingSet> inputBindings) {
        List<StorageBindingSet> results = new ArrayList<>();
        for (StorageBindingSet binding : inputBindings) {
            Environment joinedEnvironment = new StorageBindingEnvironment(environment, binding);
            Iterable<Edge> edges = getEdges(graphNode, from, queryEdge, joinedEnvironment);
            try {
                for (Edge candidate : edges) {
                    StorageBindingSet next = binding.copy();
                    if (next.bind(queryEdge.getNode(0), candidate.getNode(0))
                            && next.bind(queryEdge.getNode(1), candidate.getNode(1))
                            && next.bind(queryEdge.getEdgeVariable(), candidate.getEdgeNode())) {
                        results.add(next);
                    }
                }
            } finally {
                closeEdges(edges);
            }
        }
        return results;
    }
}
