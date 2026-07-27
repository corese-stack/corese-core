package fr.inria.corese.core.next.query.kgram.tool;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.api.core.Regex;
import fr.inria.corese.core.next.query.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Mapping;
import fr.inria.corese.core.next.query.kgram.core.Mappings;
import fr.inria.corese.core.next.query.kgram.core.Query;
import fr.inria.corese.core.next.storagemanager.api.StorageManager;
import fr.inria.corese.core.next.storagemanager.api.support.model.StatementPattern;
import fr.inria.corese.core.sparql.api.IDatatype;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * KGRAM producer backed by a Corese-next {@link StorageManager}.
 *
 * <p>The producer is responsible for translating KGRAM graph-pattern requests
 * into storage-layer {@link StatementPattern} queries, then adapting returned
 * RDF statements back into KGRAM edges. It does not decide the final RDF-term
 * matching policy; that remains the matcher responsibility.</p>
 */
public final class StorageManagerProducer extends ProducerDefault {

    private final StorageManager storage;
    private final ValueFactory valueFactory;

    /**
     * Creates a KGRAM producer backed by the given storage manager.
     *
     * @param storage storage manager queried by this producer
     */
    public StorageManagerProducer(StorageManager storage) {
        this(storage, new CoreseAdaptedValueFactory());
    }

    /**
     * Creates a producer with an explicit value factory.
     *
     * @param storage storage manager queried by this producer
     * @param valueFactory factory used to convert KGRAM values to storage values
     */
    StorageManagerProducer(StorageManager storage, ValueFactory valueFactory) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.valueFactory = Objects.requireNonNull(valueFactory, "valueFactory");
    }

    @Override
    public Iterable<Edge> getEdges(Node graphNode, List<Node> from, Edge queryEdge, Environment environment) {
        Objects.requireNonNull(queryEdge, "queryEdge");

        StorageQueryPattern queryPattern = queryPattern(graphNode, from, queryEdge, environment);
        if (queryPattern.noMatch()) {
            return List.of();
        }

        try (Stream<fr.inria.corese.core.next.data.api.Statement> statements =
                     storage.getQueryOperations().query(queryPattern.statementPattern())) {
            return statements
                    .map(StorageManagerEdge::new)
                    .map(Edge.class::cast)
                    .toList();
        }
    }

    @Override
    public Iterable<Node> getGraphNodes(Node graphNode, List<Node> from, Environment environment) {
        List<Node> nodes = new ArrayList<>();
        for (Resource context : storage.getMetadataOperations().getContexts()) {
            Node node = StorageManagerKgramValues.node(context);
            if (matchesFrom(node, from, environment)) {
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
        if (value instanceof Value rdfValue) {
            return StorageManagerKgramValues.node(rdfValue);
        }
        if (value instanceof IDatatype datatype) {
            return StorageManagerKgramValues.node(datatype);
        }
        return null;
    }

    @Override
    public IDatatype getDatatypeValue(Object value) {
        if (value instanceof Node node) {
            return node.getDatatypeValue();
        }
        if (value instanceof Value rdfValue) {
            return StorageManagerKgramValues.datatypeValue(rdfValue);
        }
        if (value instanceof IDatatype datatype) {
            return datatype;
        }
        return null;
    }

    @Override
    public boolean isBindable(Node node) {
        return node != null && node.isVariable();
    }

    /**
     * Materializes mappings for basic graph patterns made of triple {@code EDGE} expressions.
     *
     * <p>Higher-level algebra remains handled by {@link fr.inria.corese.core.next.query.kgram.core.Eval};
     * paths, services and values are explicit follow-ups.</p>
     */
    @Override
    public Mappings getMappings(Node graphNode, List<Node> from, Exp exp, Environment environment) {
        if (!exp.isBGP()) {
            throw new IllegalArgumentException("StorageManagerProducer can only materialize BGP expressions");
        }

        List<BindingSet> bindings = new ArrayList<>();
        bindings.add(new BindingSet());
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
        for (BindingSet binding : bindings) {
            mappings.add(binding.toMapping());
        }
        return mappings;
    }

    /**
     * Converts a KGRAM query edge into the storage-layer statement pattern.
     *
     * <p>Unbound KGRAM variables become {@code null} components, which the storage API
     * interprets as wildcards. Impossible RDF combinations, such as a literal subject
     * or predicate, are represented as an empty-result pattern.
     *
     * @param graphNode current GRAPH node, or {@code null} for the default graph context
     * @param from active FROM/FROM NAMED restriction computed by KGRAM
     * @param queryEdge KGRAM triple pattern to translate
     * @param environment current bindings used to resolve already-bound variables
     * @return a storage query pattern, or an empty-result marker when no RDF statement can match
     */
    private StorageQueryPattern queryPattern(Node graphNode, List<Node> from, Edge queryEdge, Environment environment) {
        Node subjectNode = resolve(queryEdge.getNode(0), environment);
        Node predicateNode = resolve(predicateQueryNode(queryEdge), environment);
        Node objectNode = resolve(queryEdge.getNode(1), environment);

        Resource subject = null;
        IRI predicate = null;
        Value object = null;

        // Subject and predicate have stricter RDF roles than object: subject must
        // be a resource, predicate must be an IRI, while object accepts any RDF value.
        if (subjectNode != null) {
            Value value = StorageManagerKgramValues.rdfValue(subjectNode, valueFactory);
            if (!(value instanceof Resource resource)) {
                return StorageQueryPattern.emptyResult();
            }
            subject = resource;
        }
        if (predicateNode != null) {
            Value value = StorageManagerKgramValues.rdfValue(predicateNode, valueFactory);
            if (!(value instanceof IRI iri)) {
                return StorageQueryPattern.emptyResult();
            }
            predicate = iri;
        }
        if (objectNode != null) {
            object = StorageManagerKgramValues.rdfValue(objectNode, valueFactory);
        }

        // Graph and dataset clauses become the statement contexts passed to storage.
        ContextSelection contextSelection = contextSelection(graphNode, from, environment);
        if (contextSelection.noMatch()) {
            return StorageQueryPattern.emptyResult();
        }
        return StorageQueryPattern.of(StatementPattern.of(
                subject,
                predicate,
                object,
                contextSelection.contextsArray()));
    }

    /**
     * Returns the effective predicate node carried by a KGRAM edge.
     *
     * <p>KGRAM stores variable predicates in {@link Edge#getEdgeVariable()}; {@link Edge#getEdgeNode()}
     * may only be the technical root-property placeholder for {@code ?s ?p ?o} patterns.
     *
     * @param queryEdge KGRAM edge whose predicate must be read
     * @return the predicate variable when present, otherwise the constant predicate node
     */
    private Node predicateQueryNode(Edge queryEdge) {
        return queryEdge.getEdgeVariable() == null ? queryEdge.getEdgeNode() : queryEdge.getEdgeVariable();
    }

    /**
     * Selects the storage contexts for the active graph pattern.
     *
     * <p>SPARQL evaluates {@code GRAPH <g> { ... }} only when {@code <g>} is a named graph
     * in the active dataset; otherwise the graph pattern has no solution.
     *
     * @see <a href="https://www.w3.org/TR/sparql11-query/#sparqlAlgebra">SPARQL 1.1 Query
     * Language - Evaluation of Graph</a>
     */
    private ContextSelection contextSelection(Node graphNode, List<Node> from, Environment environment) {
        if (graphNode != null) {
            // GRAPH <g>: evaluate in the requested named graph, provided it belongs
            // to the active named dataset restriction.
            Node resolvedGraphNode = resolve(graphNode, environment);
            if (resolvedGraphNode == null) {
                return ContextSelection.allContexts();
            }
            Value value = StorageManagerKgramValues.rdfValue(resolvedGraphNode, valueFactory);
            if (!(value instanceof Resource resource)) {
                return ContextSelection.emptyResult();
            }
            if (!matchesFrom(resolvedGraphNode, from, environment)) {
                return ContextSelection.emptyResult();
            }
            return ContextSelection.of(List.of(resource));
        }

        if (from == null || from.isEmpty()) {
            // No GRAPH and no dataset restriction: leave storage contexts unrestricted.
            return ContextSelection.allContexts();
        }

        // No explicit GRAPH: use the dataset restriction as storage contexts.
        List<Resource> contexts = new ArrayList<>();
        for (Node node : from) {
            Node resolvedNode = resolve(node, environment);
            if (resolvedNode == null) {
                continue;
            }
            Value value = StorageManagerKgramValues.rdfValue(resolvedNode, valueFactory);
            if (!(value instanceof Resource resource)) {
                return ContextSelection.emptyResult();
            }
            contexts.add(resource);
        }
        return ContextSelection.of(contexts);
    }

    /**
     * Checks whether a resolved graph node is allowed by the active dataset restriction.
     *
     * @param graphNode resolved graph node to test
     * @param from active FROM/FROM NAMED restriction computed by KGRAM
     * @param environment current bindings used to resolve graph variables in {@code from}
     * @return {@code true} when no restriction exists or when {@code graphNode} belongs to it
     */
    private boolean matchesFrom(Node graphNode, List<Node> from, Environment environment) {
        if (from == null || from.isEmpty()) {
            return true;
        }
        for (Node fromNode : from) {
            Node resolvedNode = resolve(fromNode, environment);
            if (resolvedNode != null && resolvedNode.match(graphNode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves a query node against the current KGRAM environment.
     *
     * <p>Constants resolve to themselves, bound variables resolve to their current target node,
     * and unbound variables resolve to {@code null}.
     *
     * @param queryNode KGRAM query node to resolve
     * @param environment current bindings, or {@code null}
     * @return resolved node, or {@code null} when the node is unbound
     */
    private Node resolve(Node queryNode, Environment environment) {
        if (queryNode == null) {
            return null;
        }
        if (queryNode.isConstant()) {
            return queryNode;
        }
        return environment == null ? null : environment.getNode(queryNode);
    }

    /**
     * Extends partial bindings with the matches of one triple pattern.
     *
     * <p>Each input binding is exposed through a layered {@link BindingEnvironment}, so already
     * bound variables are pushed into {@link #queryPattern(Node, List, Edge, Environment)} before
     * querying storage.
     *
     * @param graphNode active graph node
     * @param from active dataset restriction
     * @param queryEdge triple pattern to join
     * @param environment base KGRAM environment
     * @param inputBindings bindings produced by previous BGP edges
     * @return bindings extended with the matches of {@code queryEdge}
     */
    private List<BindingSet> join(
            Node graphNode,
            List<Node> from,
            Edge queryEdge,
            Environment environment,
            List<BindingSet> inputBindings) {
        List<BindingSet> results = new ArrayList<>();
        for (BindingSet binding : inputBindings) {
            Environment joinedEnvironment = new BindingEnvironment(environment, binding);
            for (Edge candidate : getEdges(graphNode, from, queryEdge, joinedEnvironment)) {
                BindingSet next = binding.copy();
                if (next.bind(queryEdge.getNode(0), candidate.getNode(0))
                        && next.bind(queryEdge.getNode(1), candidate.getNode(1))
                        && next.bind(queryEdge.getEdgeVariable(), candidate.getEdgeNode())) {
                    results.add(next);
                }
            }
        }
        return results;
    }

    /**
     * Statement pattern plus an explicit empty-result marker.
     *
     * <p>This avoids using {@code null} to represent impossible RDF patterns while still
     * keeping a non-null placeholder pattern for the record state.
     */
    private record StorageQueryPattern(StatementPattern statementPattern, boolean noMatch) {

        private static StorageQueryPattern of(StatementPattern statementPattern) {
            return new StorageQueryPattern(statementPattern, false);
        }

        private static StorageQueryPattern emptyResult() {
            return new StorageQueryPattern(StatementPattern.matchAll(), true);
        }
    }

    /**
     * Storage contexts selected for a query pattern.
     *
     * <p>An empty context list means "all contexts" for the storage API; {@code noMatch}
     * distinguishes this from a graph/dataset restriction that cannot produce results.
     */
    private record ContextSelection(List<Resource> contexts, boolean noMatch) {

        private static ContextSelection of(List<Resource> contexts) {
            return new ContextSelection(List.copyOf(contexts), false);
        }

        private static ContextSelection allContexts() {
            return of(List.of());
        }

        private static ContextSelection emptyResult() {
            return new ContextSelection(List.of(), true);
        }

        private Resource[] contextsArray() {
            return contexts.toArray(Resource[]::new);
        }
    }

    private static final class BindingSet {

        private final List<Node> queryNodes = new ArrayList<>();
        private final List<Node> targetNodes = new ArrayList<>();

        private BindingSet copy() {
            BindingSet copy = new BindingSet();
            copy.queryNodes.addAll(queryNodes);
            copy.targetNodes.addAll(targetNodes);
            return copy;
        }

        /**
         * Adds a query-variable binding, or validates it against an existing binding.
         *
         * @param queryNode query-side node, usually a variable
         * @param targetNode storage match node
         * @return {@code true} when the binding is compatible with previous bindings
         */
        private boolean bind(Node queryNode, Node targetNode) {
            if (queryNode == null || queryNode.isConstant()) {
                return true;
            }
            Node current = get(queryNode);
            if (current == null) {
                queryNodes.add(queryNode);
                targetNodes.add(targetNode);
                return true;
            }
            return current.match(targetNode);
        }

        /**
         * Looks up the target node already bound to a query node.
         *
         * @param queryNode query node to find
         * @return bound target node, or {@code null} when the query node is unbound
         */
        private Node get(Node queryNode) {
            if (queryNode == null) {
                return null;
            }
            for (int i = 0; i < queryNodes.size(); i++) {
                if (queryNodes.get(i) == queryNode || queryNodes.get(i).same(queryNode)) {
                    return targetNodes.get(i);
                }
            }
            return null;
        }

        /**
         * Converts this local binding set into a KGRAM mapping.
         *
         * @return mapping containing all query-to-target bindings
         */
        private Mapping toMapping() {
            return Mapping.create(queryNodes, targetNodes);
        }
    }

    /**
     * Environment overlay used while joining BGP edges.
     *
     * <p>Local bindings produced by earlier triple patterns take precedence over the delegate
     * environment, which lets later patterns see already-bound variables.
     */
    private static final class BindingEnvironment implements Environment {

        private final Environment delegate;
        private final BindingSet bindings;

        private BindingEnvironment(Environment delegate, BindingSet bindings) {
            this.delegate = delegate;
            this.bindings = bindings;
        }

        @Override
        public Node getNode(Node queryNode) {
            Node node = bindings.get(queryNode);
            if (node != null) {
                return node;
            }
            return delegate == null ? null : delegate.getNode(queryNode);
        }

        @Override
        public Query getQuery() {
            return delegate == null ? null : delegate.getQuery();
        }

        @Override
        public fr.inria.corese.core.next.query.kgram.api.core.BindingContext getBind() {
            return delegate == null ? null : delegate.getBind();
        }

        @Override
        public void setBind(fr.inria.corese.core.next.query.kgram.api.core.BindingContext bindingContext) {
            if (delegate != null) {
                delegate.setBind(bindingContext);
            }
        }

        @Override
        public boolean hasBind() {
            return delegate != null && delegate.hasBind();
        }

        @Override
        public Node getGraphNode() {
            return delegate == null ? null : delegate.getGraphNode();
        }

        @Override
        public Node getNode(fr.inria.corese.core.next.query.kgram.api.core.Expr varExpr) {
            return delegate == null ? null : delegate.getNode(varExpr);
        }

        @Override
        public Node getNode(String label) {
            return delegate == null ? null : delegate.getNode(label);
        }

        @Override
        public Node getQueryNode(int n) {
            return delegate == null ? null : delegate.getQueryNode(n);
        }

        @Override
        public Node getQueryNode(String label) {
            return delegate == null ? null : delegate.getQueryNode(label);
        }

        @Override
        public boolean isBound(Node queryNode) {
            return getNode(queryNode) != null;
        }

        @Override
        public int pathLength(Node queryNode) {
            return delegate == null ? 0 : delegate.pathLength(queryNode);
        }

        @Override
        public fr.inria.corese.core.next.query.kgram.path.Path getPath(Node queryNode) {
            return delegate == null ? null : delegate.getPath(queryNode);
        }

        @Override
        public int count() {
            return delegate == null ? 0 : delegate.count();
        }

        @Override
        public fr.inria.corese.core.next.query.kgram.event.EventManager getEventManager() {
            return delegate == null ? null : delegate.getEventManager();
        }

        @Override
        public Object getObject() {
            return delegate == null ? null : delegate.getObject();
        }

        @Override
        public void setObject(Object object) {
            if (delegate != null) {
                delegate.setObject(object);
            }
        }

        @Override
        public Exp getExp() {
            return delegate == null ? null : delegate.getExp();
        }

        @Override
        public void setExp(Exp exp) {
            if (delegate != null) {
                delegate.setExp(exp);
            }
        }

        @Override
        public java.util.Map<String, IDatatype> getMap() {
            return delegate == null ? java.util.Map.of() : delegate.getMap();
        }

        @Override
        public Edge[] getEdges() {
            return delegate == null ? new Edge[0] : delegate.getEdges();
        }

        @Override
        public Node[] getNodes() {
            return delegate == null ? new Node[0] : delegate.getNodes();
        }

        @Override
        public Node[] getQueryNodes() {
            return delegate == null ? new Node[0] : delegate.getQueryNodes();
        }

        @Override
        public Mappings getMappings() {
            return delegate == null ? null : delegate.getMappings();
        }

        @Override
        public Mapping getMapping() {
            return delegate == null ? null : delegate.getMapping();
        }

        @Override
        public Iterable<Mapping> getAggregate() {
            return delegate == null ? List.of() : delegate.getAggregate();
        }

        @Override
        public void aggregate(Mapping mapping, int n) {
            if (delegate != null) {
                delegate.aggregate(mapping, n);
            }
        }

        @Override
        public Node get(fr.inria.corese.core.next.query.kgram.api.core.Expr varExpr) {
            return delegate == null ? null : delegate.get(varExpr);
        }

        @Override
        public fr.inria.corese.core.sparql.triple.parser.ASTExtension getExtension() {
            return delegate == null ? null : delegate.getExtension();
        }

        @Override
        public ApproximateSearchEnv getAppxSearchEnv() {
            return delegate == null ? null : delegate.getAppxSearchEnv();
        }

        @Override
        public fr.inria.corese.core.next.query.kgram.core.Eval getEval() {
            return delegate == null ? null : delegate.getEval();
        }

        @Override
        public void setEval(fr.inria.corese.core.next.query.kgram.core.Eval eval) {
            if (delegate != null) {
                delegate.setEval(eval);
            }
        }

        @Override
        public fr.inria.corese.core.next.query.kgram.api.query.ProcessVisitor getVisitor() {
            return delegate == null ? null : delegate.getVisitor();
        }

        @Override
        public IDatatype getReport() {
            return delegate == null ? null : delegate.getReport();
        }

        @Override
        public void setReport(IDatatype datatype) {
            if (delegate != null) {
                delegate.setReport(datatype);
            }
        }

        @Override
        public int size() {
            return delegate == null ? 0 : delegate.size();
        }
    }
}
