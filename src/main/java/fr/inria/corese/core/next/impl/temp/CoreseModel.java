package fr.inria.corese.core.next.impl.temp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import fr.inria.corese.core.EdgeFactory;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Namespace;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.base.model.AbstractModel;
import fr.inria.corese.core.next.api.base.model.serialization.Rdf4jSerializationUtil;

import org.eclipse.rdf4j.model.impl.TreeModel;


/**
 * CoreseModel provides an implementation of the RDF Model interface
 * backed by a Corese Graph instance. It supports basic RDF operations
 * such as add, remove, contains, filtering, and namespace management.
 * This class has been extended to include a method for serializing
 * its content into various RDF4J formats using Rdf4jSerializationUtil.
 */
public class CoreseModel extends AbstractModel {

    // --- Fields ---

    /** The underlying Corese graph. */
    private final Graph coreseGraph;

    /** Utility for converting RDF4J-like Values into Corese Nodes. */
    private final CoreseValueConverter converter;

    /** A set of RDF namespaces associated with this model. */
    private final Set<Namespace> namespaces;

    // --- Constructors ---

    /**
     * Constructs a new CoreseModel with an empty Corese Graph
     * and an empty set of namespaces.
     */
    public CoreseModel() {
        this(Graph.create(), new HashSet<>());
    }

    /**
     * Constructs a CoreseModel from an existing RDF4J-style model.
     * Statements are copied into a new Corese graph.
     *
     * @param model the source model to import statements from
     */
    public CoreseModel(Model model) {
        this();
        addAll(model);
    }

    /**
     * Constructs a CoreseModel by copying the given collection of statements
     * into a new Corese graph.
     *
     * @param statements the collection of statements to import
     */
    public CoreseModel(Collection<? extends Statement> statements) {
        this();
        addAll(statements);
    }

    /**
     * Constructs a CoreseModel with a predefined set of namespaces.
     *
     * @param namespaces the set of namespaces to associate with this model
     */
    public CoreseModel(Set<Namespace> namespaces) {
        this(Graph.create(), new HashSet<>(Objects.requireNonNull(namespaces)));
    }

    /**
     * Constructs a CoreseModel with a predefined set of namespaces
     * and initial statements to add to the underlying graph.
     *
     * @param namespaces the set of namespaces to associate
     * @param statements the collection of statements to import
     */
    public CoreseModel(Set<Namespace> namespaces, Collection<? extends Statement> statements) {
        this(namespaces);
        addAll(statements);
    }

    /**
     * Constructs a CoreseModel using an existing Corese graph
     * and an empty set of namespaces.
     *
     * @param graph the Corese graph to wrap (must not be null)
     */
    public CoreseModel(Graph graph) {
        this(graph, new HashSet<>());
    }

    /**
     * Constructs a CoreseModel using the given Corese graph and set of namespaces.
     *
     * @param graph      the Corese graph to wrap (must not be null)
     * @param namespaces the set of namespaces to associate (null means empty)
     */
    public CoreseModel(Graph graph, Set<Namespace> namespaces) {
        this.coreseGraph = Objects.requireNonNull(graph, "Graph must not be null");
        this.converter = new CoreseValueConverter();
        this.namespaces = namespaces != null ? namespaces : new HashSet<>();
    }

    // --- Public Methods ---

    // --- Add functions ---

    @Override
    public boolean add(Resource subject, IRI predicate, Value object, Resource... contexts) {
        Objects.requireNonNull(subject, "Subject must not be null");
        Objects.requireNonNull(predicate, "Predicate must not be null");
        Objects.requireNonNull(object, "Object must not be null");

        Node coreseSubj = converter.toCoreseNode(subject);
        Node coresePred = converter.toCoreseNode(predicate);
        Node coreseObj = converter.toCoreseNode(object);
        Node[] coreseContexts = converter.toCoreseContextArray(contexts);

        // Handle the case where no context is provided
        if (contexts.length == 0) {
            return this.addEdgeToGraph(coreseSubj, coresePred, coreseObj, null);
        }

        // Handle one or more contexts is provided
        boolean changed = false;
        for (Node coreseContext : coreseContexts) {
            if (coreseContext != null) {
                changed |= this.addEdgeToGraph(coreseSubj, coresePred, coreseObj, coreseContext);
            } else {
                changed |= this.addEdgeToGraph(coreseSubj, coresePred, coreseObj, null);
            }
        }
        return changed;
    }

    // --- Contains functions ---

    @Override
    public boolean contains(Resource subject, IRI predicate, Value object, Resource... contexts) {

        Node subjectNode = converter.toCoreseNode(subject);
        Node predicateNode = converter.toCoreseNode(predicate);
        Node objectNode = converter.toCoreseNode(object);
        Node[] contextNodes = converter.toCoreseContextArray(contexts);

        Iterator<Edge> it = selectEdgesFromCorese(subjectNode, predicateNode, objectNode, contextNodes).iterator();
        return it.hasNext() && it.next() != null;
    }

    // --- Remove functions ---

    @Override
    public boolean remove(Resource subject, IRI predicate, Value object, Resource... contexts) {

        Node subjectNode = converter.toCoreseNode(subject);
        Node predicateNode = converter.toCoreseNode(predicate);
        Node objectNode = converter.toCoreseNode(object);
        Node[] contextNodes = converter.toCoreseContextArray(contexts);

        Iterable<Edge> edges = selectEdgesFromCorese(subjectNode, predicateNode, objectNode, contextNodes);
        boolean removed = false;

        for (Edge edge : edges) {
            if (edge != null) {
                // delete() returns a list of removed edges (possibly empty)
                boolean deleted = !coreseGraph.delete(edge).isEmpty();
                removed |= deleted;
            }
        }

        return removed;
    }

    // --- Namespace functions ---

    @Override
    public Set<Namespace> getNamespaces() {
        return namespaces;
    }

    @Override
    public void setNamespace(Namespace namespace) {
        Objects.requireNonNull(namespace, "Namespace cannot be null");
        removeNamespace(namespace.getPrefix());
        namespaces.add(namespace);
    }

    @Override
    public Optional<Namespace> removeNamespace(String prefix) {
        return getNamespace(prefix).filter(namespaces::remove);
    }

    // --- Filter functions ---

    @Override
    public Model filter(Resource subject, IRI predicate, Value object, Resource... contexts) {
        return new FilteredModel(this, subject, predicate, object, contexts) {

            @Override
            public Iterator<Statement> iterator() {
                return CoreseModel.this.getFilterIterator(subject, predicate, object, contexts);
            }

            @Override
            protected void removeFilteredTermIteration(
                    Iterator<Statement> iterator,
                    Resource subject,
                    IRI predicate,
                    Value object,
                    Resource... contexts) {
                CoreseModel.this.removeTermIteration(iterator, subject, predicate, object, contexts);
            }
        };
    }

    // --- Iterator functions ---

    @Override
    public Iterator<Statement> iterator() {

        return getFilterIterator(null, null, null, (Resource[]) null);
    }

    @Override
    public void removeTermIteration(
            Iterator<Statement> iterator,
            Resource subject,
            IRI predicate,
            Value object,
            Resource... contexts) {
        remove(subject, predicate, object, contexts);
    }

    // --- Size functions ---

    @Override
    public int size() {
        return coreseGraph.size();
    }

    // --- Corese graph accessors ---

    /**
     * Returns the underlying Corese graph instance.
     *
     * @return the underlying Corese {@link Graph}
     */
    public Graph getCoreseGraph() {
        return coreseGraph;
    }

    // --- Utility functions ---

    /**
     * Returns Corese edges matching the given subject, predicate, object, and
     * contexts.
     *
     * All parameters are Corese nodes (not RDF4J resources).
     * Null values are interpreted as wildcards.
     *
     * @param subject   Corese subject node (nullable)
     * @param predicate Corese predicate node (nullable)
     * @param object    Corese object node (nullable)
     * @param contexts  Corese context nodes (nullable or empty for wildcard)
     * @return Iterable of copied Corese edges (never null, possibly empty)
     */
    private Iterable<Edge> selectEdgesFromCorese(Node subject, Node predicate, Node object, Node... contexts) {
        coreseGraph.init();

        Iterable<Edge> rawEdges = coreseGraph.getEdgesRDF4J(subject, predicate, object, contexts);

        List<Edge> result = new ArrayList<>();
        EdgeFactory edgeFactory = coreseGraph.getEdgeFactory();

        for (Edge edge : rawEdges) {
            if (edge != null) {
                result.add(edgeFactory.copy(edge));
            }
        }

        return result;
    }

    /**
     * Add one statement ({@code subj}, {@code pred}, {@code obj}, {@code context})
     * in the Corese graph.
     *
     * @param subject   Subject of the statement.
     * @param predicate Predicate of the statement.
     * @param object    Object of the statement.
     * @param context   Context of the statement.
     * @return true if the Corese graph was modified, false otherwise.
     */
    private boolean addEdgeToGraph(Node subject, Node predicate, Node object, Node context) {

        Node subj = this.coreseGraph.addNode(subject);
        Node pred = this.coreseGraph.addProperty(predicate.getLabel());
        Node obj = this.coreseGraph.addNode(object);

        Edge edge;
        if (context == null) {
            edge = this.coreseGraph.addEdge(subj, pred, obj);
        } else {
            Node context_node = this.coreseGraph.addGraph(context.getLabel());
            edge = this.coreseGraph.addEdge(context_node, subj, pred, obj);
        }

        return edge != null;
    }

    /**
     * Get a Corese model iterator with Statements that match the specified subject,
     * predicate, object and (optionally) context. The subject, predicate and object
     * parameters can be null to indicate wildcards. The contexts parameter is a
     * wildcard and accepts zero or more values. If no contexts are specified,
     * statements will match disregarding their context. If one or more contexts are
     * specified, statements with a context matching one of these will match. Note:
     * to match statements without an associated context, specify the value null and
     * explicitly cast it to type Resource.
     *
     * @param subject   The subject of the statements to match, null to match
     *                  statements with any subject.
     * @param predicate The Predicate of the statements to match, null to match
     *                  statements with any predicate.
     * @param object    The Object of the statements to match, null to match
     *                  statements with any object.
     * @param contexts  The Contexts of the statements to match. If no contexts are
     *                  specified, statements will match disregarding their context.
     *                  If one or more contexts are specified, statements with a
     *                  context matching one of these will match.
     * @return Corese model iterator on Statements that match the specified subject,
     *         predicate, object and (optionally) context.
     */
    private Iterator<Statement> getFilterIterator(Resource subject, IRI predicate, Value object, Resource... contexts) {
        this.coreseGraph.init();

        /**
         * Iterator for the Corese model
         */
        class CoreseModelIterator implements Iterator<Statement> {

            private Iterator<Statement> iter;

            private Statement last;

            public CoreseModelIterator(Iterator<Statement> iter) {
                this.iter = iter;
            }

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Statement next() {
                return last = iter.next();
            }

            @Override
            public void remove() {
                if (last == null) {
                    throw new IllegalStateException();
                }
                CoreseModel.this.remove(last);
            }
        }

        // get edges
        Node subjectNode = converter.toCoreseNode(subject);
        Node predicateNode = converter.toCoreseNode(predicate);
        Node objectNode = converter.toCoreseNode(object);
        Node[] contextNodes = converter.toCoreseContextArray(contexts);

        Iterable<Edge> edges = selectEdgesFromCorese(subjectNode, predicateNode, objectNode, contextNodes);
        List<Statement> statements = new ArrayList<>();

        for (Edge edge : edges) {
            if (edge != null) {
                statements.add(new CoreseStatement(edge));
            }
        }

        return new CoreseModelIterator(statements.iterator());
    }

    /**
     * Serializes this CoreseModel to the specified OutputStream in a given RDF4J format.
     * This method first converts the CoreseModel's statements into an
     * org.eclipse.rdf4j.model.Model, then uses Rdf4jSerializationUtil for serialization.
     *
     * @param outputStream The OutputStream to write the serialized data to. Must not be null.
     * @param formatString The string identifier of the desired RDF format (e.g., "turtle", "jsonld", "rdfxml"). Must not be null.
     * @throws IOException              If an I/O error occurs during serialization or if an unsupported Corese value type is encountered.
     * @throws IllegalArgumentException If the provided formatString is not recognized by Rdf4jSerializationUtil.
     * @throws NullPointerException     If outputStream or formatString is null.
     */
    public void serializeToRdf4jFormat(OutputStream outputStream, String formatString) throws IOException {
        Objects.requireNonNull(outputStream, "OutputStream cannot be null");
        Objects.requireNonNull(formatString, "Format string cannot be null");


        org.eclipse.rdf4j.model.Model rdf4jModel = new TreeModel();

        for (fr.inria.corese.core.next.api.Namespace ns : this.getNamespaces()) {
            rdf4jModel.setNamespace(ns.getPrefix(), ns.getName());
        }


        for (Statement coreseStatement : this) {


            org.eclipse.rdf4j.model.Resource rdf4jSubject = (org.eclipse.rdf4j.model.Resource) converter.valuetoRdf4jValue(converter.toCoreseNode(coreseStatement.getSubject()).getDatatypeValue());
            org.eclipse.rdf4j.model.IRI rdf4jPredicate = (org.eclipse.rdf4j.model.IRI) converter.valuetoRdf4jValue(converter.toCoreseNode(coreseStatement.getPredicate()).getDatatypeValue());
            org.eclipse.rdf4j.model.Value rdf4jObject = converter.valuetoRdf4jValue(converter.toCoreseNode(coreseStatement.getObject()).getDatatypeValue());

            org.eclipse.rdf4j.model.Resource rdf4jContext = null;
            if (coreseStatement.getContext() != null) {
                rdf4jContext = converter.resourcetoRdf4jValueContext(converter.toCoreseNode(coreseStatement.getContext()).getDatatypeValue());
            }


            if (rdf4jContext != null) {
                rdf4jModel.add(rdf4jSubject, rdf4jPredicate, rdf4jObject, rdf4jContext);
            } else {
                rdf4jModel.add(rdf4jSubject, rdf4jPredicate, rdf4jObject);
            }
        }


        Rdf4jSerializationUtil.serialize(rdf4jModel, outputStream, formatString);
    }
}
