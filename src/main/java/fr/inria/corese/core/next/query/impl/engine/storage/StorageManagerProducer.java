package fr.inria.corese.core.next.query.impl.engine.storage;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.model.Regex;
import fr.inria.corese.core.next.query.impl.engine.path.NativePropertyPathEvaluator;
import fr.inria.corese.core.next.query.impl.engine.path.PropertyPathEdge;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.storage.api.StorageManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * KGRAM producer backed by a Corese-next {@link StorageManager}.
 *
 * <p>The producer is responsible for translating KGRAM graph-pattern requests
 * into storage-layer {@link fr.inria.corese.core.next.storage.api.model.StatementPattern} queries,
 * then adapting returned RDF statements back into KGRAM edges. It does not decide the final RDF-term
 * matching policy; that remains the matcher responsibility.</p>
 */
public final class StorageManagerProducer extends ProducerDefault {

    private final StorageManager storage;

    /**
     * Creates a KGRAM producer backed by the given storage manager.
     *
     * @param storage storage manager queried by this producer
     */
    public StorageManagerProducer(StorageManager storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
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

        try (Stream<Statement> statements =
                     storage.queries().find(queryPattern.statementPattern())) {
            // A default graph formed from several contexts is an RDF graph: the
            // same triple must match once, even when stored in multiple contexts.
            // Deduplicate triples here, before projection, to preserve solution bags.
            Set<TripleKey> seen = new HashSet<>();
            return statements
                    .filter(statement -> graphNode != null || seen.add(new TripleKey(
                            statement.getSubject(), statement.getPredicate(), statement.getObject())))
                    .map(StorageManagerEdge::new)
                    .map(Edge.class::cast)
                    .toList();
        }
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
    public Iterable<Edge> getEdges(
            Node graphNode,
            List<Node> from,
            Edge queryEdge,
            Environment environment,
            Regex regex,
            Node source,
            Node start,
            int index) {
        throw new UnsupportedQueryFeatureException(
                "Property path edge enumeration is not supported yet by StorageManagerProducer");
    }

    @Override
    public void start(Query query) {
        // No per-query storage state is required.
    }

    @Override
    public void finish(Query query) {
        // No per-query storage state is required.
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
            for (Edge candidate : getEdges(graphNode, from, queryEdge, joinedEnvironment)) {
                StorageBindingSet next = binding.copy();
                if (next.bind(queryEdge.getNode(0), candidate.getNode(0))
                        && next.bind(queryEdge.getNode(1), candidate.getNode(1))
                        && next.bind(queryEdge.getEdgeVariable(), candidate.getEdgeNode())) {
                    results.add(next);
                }
            }
        }
        return results;
    }
}
