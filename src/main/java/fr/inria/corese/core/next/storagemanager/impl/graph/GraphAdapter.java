package fr.inria.corese.core.next.storagemanager.impl.graph;

import fr.inria.corese.core.Graph;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.data.api.*;
import fr.inria.corese.core.sparql.api.IDatatype;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Adapter between legacy {@link Graph} and new Storage API.
 */
public record GraphAdapter(Graph graph, ValueFactory valueFactory) {

    /**
     * Compact constructor with validation.
     *
     * @throws IllegalArgumentException if graph or valueFactory is null
     */
    public GraphAdapter {
        if (graph == null) throw new IllegalArgumentException("Graph cannot be null");
        if (valueFactory == null) throw new IllegalArgumentException("ValueFactory cannot be null");
    }

    /**
     * Adds a statement to the underlying Graph.
     *
     * @param stmt the statement to add
     * @return true if the statement was added, false if it already existed
     */
    public boolean add(Statement stmt) {
        Edge edge = statementToEdge(stmt);
        return graph.addEdge(edge) != null;
    }

    /**
     * Removes a statement from the underlying Graph.
     *
     * @param stmt the statement to remove
     * @return true (Graph.delete() does not return a boolean)
     */
    public boolean remove(Statement stmt) {
        Edge edge = statementToEdge(stmt);
        graph.delete(edge);
        return true;
    }

    /**
     * Checks if a statement exists in the underlying Graph.
     *
     * @param stmt the statement to check
     * @return true if the statement exists
     */
    public boolean contains(Statement stmt) {
        Edge edge = statementToEdge(stmt);
        return graph.exist(edge);
    }

    /**
     * Finds all statements matching the given pattern.
     *
     * @param s        subject filter, or null for any
     * @param p        predicate filter, or null for any
     * @param o        object filter, or null for any
     * @param contexts context filters; empty or null means any context
     * @return set of matching statements
     */
    public Set<Statement> find(Resource s, IRI p, Value o, Resource[] contexts) {
        Node subject = s != null ? resourceToNode(s) : null;
        Node predicate = p != null ? iriToNode(p) : null;
        Node object = o != null ? valueToNode(o) : null;

        Iterable<Edge> edges;
        if (contexts == null || contexts.length == 0) {
            edges = graph.getEdgesRDF4J(subject, predicate, object);
        } else {
            Node[] ctxNodes = Arrays.stream(contexts)
                    .map(this::resourceToNode)
                    .toArray(Node[]::new);
            edges = graph.getEdgesRDF4J(subject, predicate, object, ctxNodes);
        }

        Set<Statement> results = new HashSet<>();
        for (Edge edge : edges) {
            results.add(edgeToStatement(edge));
        }
        return results;
    }

    /**
     * Returns the total number of statements in the Graph.
     *
     * @return statement count
     */
    public int size() {
        return graph.size();
    }

    /**
     * Removes all statements from the Graph.
     */
    public void clear() {
        graph.clear();
    }

    /**
     * Removes all statements from a specific named graph.
     *
     * @param context the named graph to clear
     */
    public void clearContext(Resource context) {
        Node ctx = resourceToNode(context);
        graph.clear(ctx.getLabel());
    }

    /**
     * Returns all unique subject resources in the Graph.
     *
     * @return set of subjects
     */
    public Set<Resource> getSubjects() {
        Set<Resource> subjects = new HashSet<>();
        for (Edge edge : graph.getEdges()) {
            subjects.add(nodeToResource(edge.getNode(0)));
        }
        return subjects;
    }

    /**
     * Returns all unique predicate IRIs in the Graph.
     *
     * @return set of predicates
     */
    public Set<IRI> getPredicates() {
        Set<IRI> predicates = new HashSet<>();
        for (Edge edge : graph.getEdges()) {
            predicates.add(nodeToIRI(edge.getEdgeNode()));
        }
        return predicates;
    }

    /**
     * Returns all unique object values in the Graph.
     *
     * @return set of objects
     */
    public Set<Value> getObjects() {
        Set<Value> objects = new HashSet<>();
        for (Edge edge : graph.getEdges()) {
            objects.add(nodeToValue(edge.getNode(1)));
        }
        return objects;
    }

    /**
     * Returns all unique named graph identifiers in the Graph.
     *
     * @return set of context resources
     */
    public Set<Resource> getContexts() {
        Set<Resource> contexts = new HashSet<>();
        for (Node ctx : graph.getGraphNodes()) {
            contexts.add(nodeToResource(ctx));
        }
        return contexts;
    }

    /**
     * Converts a Graph {@link Edge} to a Storage {@link Statement}.
     *
     * <p>Extracts subject, predicate, object and optional context from the Edge
     * and creates the corresponding Statement using the ValueFactory.</p>
     *
     * @param edge the Graph edge to convert
     * @return the corresponding Statement
     */
    private Statement edgeToStatement(Edge edge) {
        Resource subject = nodeToResource(edge.getNode(0));
        IRI predicate = nodeToIRI(edge.getEdgeNode());
        Value object = nodeToValue(edge.getNode(1));

        // Context (named graph)
        Node graphNode = edge.getGraph();
        Resource context = (graphNode != null) ? nodeToResource(graphNode) : null;

        if (context == null) {
            return valueFactory.createStatement(subject, predicate, object);
        } else {
            return valueFactory.createStatement(subject, predicate, object, context);
        }
    }

    /**
     * Converts a Graph {@link Node} to a Storage {@link Resource} (IRI or BNode).
     *
     * @param node the Graph node to convert
     * @return the corresponding Resource (IRI or BNode)
     * @throws IllegalArgumentException if the node is not a URI, blank, or triple reference
     */
    private Resource nodeToResource(Node node) {
        IDatatype dt = node.getDatatypeValue();

        if (dt.isURI()) {
            return valueFactory.createIRI(dt.getLabel());
        } else if (dt.isBlank()) {
            return valueFactory.createBNode(dt.getLabel());
        } else if (dt.isTriple()) {
            // RDF-star: triple reference node
            // For now, treat as blank node
            return valueFactory.createBNode(dt.getLabel());
        } else {
            throw new IllegalArgumentException("Node is not a Resource: " + node);
        }
    }

    /**
     * Converts a Graph {@link Node} (predicate) to a Storage {@link IRI}.
     *
     * @param node the Graph predicate node to convert
     * @return the corresponding IRI
     * @throws IllegalArgumentException if the node is not a URI
     */
    private IRI nodeToIRI(Node node) {
        IDatatype dt = node.getDatatypeValue();

        if (!dt.isURI()) {
            throw new IllegalArgumentException("Node is not an IRI: " + node);
        }

        return valueFactory.createIRI(dt.getLabel());
    }

    /**
     * Converts a Graph {@link Node} to a Storage {@link Value} (Resource or Literal).
     *
     * @param node the Graph node to convert
     * @return the corresponding Value (Resource or Literal)
     * @throws IllegalArgumentException if the node type is unknown
     */
    private Value nodeToValue(Node node) {
        IDatatype dt = node.getDatatypeValue();

        if (dt.isURI() || dt.isBlank() || dt.isTriple()) {
            // It's a Resource
            return nodeToResource(node);
        } else if (dt.isLiteral()) {
            // It's a Literal
            String label = dt.getLabel();
            String lang = dt.getLang();
            String datatypeIRI = dt.getDatatypeURI();

            if (lang != null && !lang.isEmpty()) {
                // Language-tagged string
                return valueFactory.createLiteral(label, lang);
            } else if (datatypeIRI != null && !datatypeIRI.isEmpty()) {
                // Typed literal
                IRI datatype = valueFactory.createIRI(datatypeIRI);
                return valueFactory.createLiteral(label, datatype);
            } else {
                // Plain string (xsd:string)
                return valueFactory.createLiteral(label);
            }
        } else {
            throw new IllegalArgumentException("Unknown node type: " + node);
        }
    }

    /**
     * Converts a Storage {@link Statement} to a Graph {@link Edge}.
     *
     * @param stmt the Statement to convert
     * @return the corresponding Edge
     */
    private Edge statementToEdge(Statement stmt) {
        Node subject = resourceToNode(stmt.getSubject());
        Node predicate = iriToNode(stmt.getPredicate());
        Node object = valueToNode(stmt.getObject());

        Resource ctxResource = stmt.getContext();
        Node context = (ctxResource != null) ? resourceToNode(ctxResource) : null;

        return graph.create(subject, predicate, object, context);
    }

    /**
     * Converts a Storage {@link Resource} to a Graph {@link Node}.
     *
     * @param resource the Resource to convert (IRI or BNode)
     * @return the corresponding Graph Node
     * @throws IllegalArgumentException if the resource type is unknown
     */
    private Node resourceToNode(Resource resource) {
        if (resource.isIRI()) {
            IRI iri = (IRI) resource;
            return graph.addResource(iri.stringValue());
        } else if (resource.isBNode()) {
            BNode bnode = (BNode) resource;
            return graph.addBlank(bnode.getID());
        } else {
            throw new IllegalArgumentException("Unknown Resource type: " + resource);
        }
    }

    /**
     * Converts a Storage {@link IRI} to a Graph {@link Node} (property).
     *
     * @param iri the IRI to convert (predicate)
     * @return the corresponding Graph Node
     */
    private Node iriToNode(IRI iri) {
        return graph.addProperty(iri.stringValue());
    }

    /**
     * Converts a Storage {@link Value} to a Graph {@link Node}.
     *
     * @param value the Value to convert (Resource or Literal)
     * @return the corresponding Graph Node
     * @throws IllegalArgumentException if the value type is unknown
     */
    private Node valueToNode(Value value) {
        if (value.isResource()) {
            // It's a Resource (IRI or BNode)
            return resourceToNode((Resource) value);
        } else if (value.isLiteral()) {
            // It's a Literal
            Literal literal = (Literal) value;
            String label = literal.getLabel();

            if (literal.getLanguage().isPresent()) {
                // Language-tagged string
                String lang = literal.getLanguage().get();
                return graph.addLiteral(label, null, lang);
            } else if (literal.getDatatype() != null) {
                // Typed literal
                String datatypeIRI = literal.getDatatype().stringValue();
                return graph.addLiteral(label, datatypeIRI);
            } else {
                // Plain string
                return graph.addLiteral(label);
            }
        } else {
            throw new IllegalArgumentException("Unknown Value type: " + value);
        }
    }
}