package fr.inria.corese.core.next.query.kgram.tool;

import fr.inria.corese.core.next.data.api.BNode;
import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Literal;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Graph;
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
import fr.inria.corese.core.sparql.triple.parser.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * KGRAM producer backed by the Corese-next {@link StorageManager}.
 */
public final class StorageManagerProducer extends ProducerDefault {

    private final StorageManager storage;
    private final ValueFactory valueFactory;

    public StorageManagerProducer(StorageManager storage) {
        this(storage, new CoreseAdaptedValueFactory());
    }

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
            Node node = node(context);
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
        throw new UnsupportedOperationException("StorageManagerProducer does not support path/regex edge enumeration yet");
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
            return node(rdfValue);
        }
        if (value instanceof IDatatype datatype) {
            return node(datatype);
        }
        return null;
    }

    @Override
    public IDatatype getValue(Object value) {
        return getDatatypeValue(value);
    }

    @Override
    public IDatatype getDatatypeValue(Object value) {
        if (value instanceof Node node) {
            return node.getDatatypeValue();
        }
        if (value instanceof Value rdfValue) {
            return node(rdfValue).getDatatypeValue();
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
            throw new UnsupportedOperationException("StorageManagerProducer only supports BGP mappings");
        }

        List<BindingSet> bindings = new ArrayList<>();
        bindings.add(new BindingSet());
        for (Exp element : exp) {
            if (!element.isEdge()) {
                throw new UnsupportedOperationException(
                        "StorageManagerProducer only supports EDGE expressions inside BGP mappings");
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

    @Override
    public Graph getGraph() {
        return null;
    }

    private StorageQueryPattern queryPattern(Node graphNode, List<Node> from, Edge queryEdge, Environment environment) {
        Node subjectNode = resolve(queryEdge.getNode(0), environment);
        Node predicateNode = resolve(predicateQueryNode(queryEdge), environment);
        Node objectNode = resolve(queryEdge.getNode(1), environment);

        Resource subject = null;
        IRI predicate = null;
        Value object = null;

        if (subjectNode != null) {
            Value value = rdfValue(subjectNode);
            if (!(value instanceof Resource resource)) {
                return StorageQueryPattern.emptyResult();
            }
            subject = resource;
        }
        if (predicateNode != null) {
            Value value = rdfValue(predicateNode);
            if (!(value instanceof IRI iri)) {
                return StorageQueryPattern.emptyResult();
            }
            predicate = iri;
        }
        if (objectNode != null) {
            object = rdfValue(objectNode);
        }

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

    private Node predicateQueryNode(Edge queryEdge) {
        return queryEdge.getEdgeVariable() == null ? queryEdge.getEdgeNode() : queryEdge.getEdgeVariable();
    }

    private ContextSelection contextSelection(Node graphNode, List<Node> from, Environment environment) {
        if (graphNode != null) {
            Node resolvedGraphNode = resolve(graphNode, environment);
            if (resolvedGraphNode == null) {
                return ContextSelection.allContexts();
            }
            Value value = rdfValue(resolvedGraphNode);
            if (!(value instanceof Resource resource)) {
                return ContextSelection.emptyResult();
            }
            return ContextSelection.of(List.of(resource));
        }

        if (from == null || from.isEmpty()) {
            return ContextSelection.allContexts();
        }

        List<Resource> contexts = new ArrayList<>();
        for (Node node : from) {
            Node resolvedNode = resolve(node, environment);
            if (resolvedNode == null) {
                continue;
            }
            Value value = rdfValue(resolvedNode);
            if (!(value instanceof Resource resource)) {
                return ContextSelection.emptyResult();
            }
            contexts.add(resource);
        }
        return ContextSelection.of(contexts);
    }

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

    private Node resolve(Node queryNode, Environment environment) {
        if (queryNode == null) {
            return null;
        }
        if (queryNode.isConstant()) {
            return queryNode;
        }
        return environment == null ? null : environment.getNode(queryNode);
    }

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

    private Node node(Value value) {
        return node(datatypeValue(value));
    }

    private static Node node(IDatatype datatype) {
        return new NodeImpl(Constant.create(datatype));
    }

    private Value rdfValue(Node node) {
        return rdfValue(node.getDatatypeValue());
    }

    private Value rdfValue(IDatatype datatype) {
        if (datatype.isURI()) {
            return valueFactory.createIRI(datatype.getLabel());
        }
        if (datatype.isBlank()) {
            return valueFactory.createBNode(datatype.getLabel());
        }
        if (datatype.isLiteral()) {
            String language = datatype.getLang();
            if (language != null && !language.isEmpty()) {
                return valueFactory.createLiteral(datatype.getLabel(), language);
            }
            String datatypeUri = datatype.getDatatypeURI();
            if (datatypeUri != null && !datatypeUri.isEmpty()) {
                return valueFactory.createLiteral(datatype.getLabel(), valueFactory.createIRI(datatypeUri));
            }
            return valueFactory.createLiteral(datatype.getLabel());
        }
        throw new IllegalArgumentException("Unsupported KGRAM datatype value: " + datatype);
    }

    private IDatatype datatypeValue(Value value) {
        if (value instanceof IRI iri) {
            return fr.inria.corese.core.sparql.datatype.DatatypeMap.newResource(iri.stringValue());
        }
        if (value instanceof BNode bNode) {
            return fr.inria.corese.core.sparql.datatype.DatatypeMap.createBlank(bNode.getID());
        }
        if (value instanceof Literal literal) {
            return literal.getLanguage()
                    .<IDatatype>map(language -> fr.inria.corese.core.sparql.datatype.DatatypeMap
                            .createLiteral(literal.getLabel(), null, language))
                    .orElseGet(() -> fr.inria.corese.core.sparql.datatype.DatatypeMap
                            .createLiteral(literal.getLabel(), literal.getDatatype().stringValue(), null));
        }
        throw new IllegalArgumentException("Unsupported RDF value: " + value);
    }

    private record StorageQueryPattern(StatementPattern statementPattern, boolean noMatch) {

        private static StorageQueryPattern of(StatementPattern statementPattern) {
            return new StorageQueryPattern(statementPattern, false);
        }

        private static StorageQueryPattern emptyResult() {
            return new StorageQueryPattern(StatementPattern.matchAll(), true);
        }
    }

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

        private Mapping toMapping() {
            return Mapping.create(queryNodes, targetNodes);
        }
    }

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
