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
        // Convert statement components to nodes
        Node subject = resourceToNode(stmt.getSubject());
        Node predicate = iriToNode(stmt.getPredicate());
        Node object = valueToNode(stmt.getObject());

        Resource ctxResource = stmt.getContext();
        Node context = (ctxResource != null)
                ? resourceToNode(ctxResource)
                : graph.getDefaultGraphNode();

        // Always use 4-argument form with explicit context
        Edge edge = graph.addEdge(context, subject, predicate, object);

        return edge != null;
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
     * Finds all statements matching the given pattern.
     *
     * @param s        subject filter, or null for any
     * @param p        predicate filter, or null for any
     * @param o        object filter, or null for any
     * @param contexts context filters; null/empty = wildcard, {null} = default graph only
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
            // Normalize contexts: convert null to default graph node
            Node[] ctxNodes = normalizeContexts(contexts);
            edges = graph.getEdgesRDF4J(subject, predicate, object, ctxNodes);
        }

        Set<Statement> results = new HashSet<>();
        for (Edge edge : edges) {
            if (edge != null) {
                try {
                    results.add(edgeToStatement(edge));
                } catch (IllegalArgumentException e) {
                    // Skip edges that cannot be converted
                }
            }
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
            if (edge != null && edge.getSubjectNode() != null) {
                try {
                    subjects.add(nodeToResource(edge.getSubjectNode()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid nodes
                }
            }
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
            if (edge != null && edge.getEdgeNode() != null) {
                try {
                    predicates.add(nodeToIRI(edge.getEdgeNode()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid nodes
                }
            }
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
            if (edge != null && edge.getObjectNode() != null) {
                try {
                    objects.add(nodeToValue(edge.getObjectNode()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid nodes
                }
            }
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
            if (ctx != null) {
                try {
                    contexts.add(nodeToResource(ctx));
                } catch (IllegalArgumentException e) {
                    // Skip invalid nodes
                }
            }
        }
        return contexts;
    }

    /**
     * Normalizes context array for Graph backend.
     *
     * @param contexts the context resources (may contain null)
     * @return normalized array of Graph nodes (never contains null)
     */
    private Node[] normalizeContexts(Resource[] contexts) {
        return Arrays.stream(contexts)
                .map(ctx -> ctx != null ? resourceToNode(ctx) : graph.getDefaultGraphNode())
                .toArray(Node[]::new);
    }

    /**
     * Converts a Graph {@link Edge} to a Storage {@link Statement}.
     *
     * @param edge the Graph edge to convert
     * @return the corresponding Statement
     * @throws IllegalArgumentException if edge or any of its required nodes is null
     */
    private Statement edgeToStatement(Edge edge) {
        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }

        Node subjectNode = edge.getSubjectNode();
        Node predicateNode = edge.getEdgeNode();
        Node objectNode = edge.getObjectNode();

        if (subjectNode == null) {
            throw new IllegalArgumentException("Edge subject node is null");
        }
        if (predicateNode == null) {
            throw new IllegalArgumentException("Edge predicate node is null");
        }
        if (objectNode == null) {
            throw new IllegalArgumentException("Edge object node is null");
        }

        Resource subject = nodeToResource(subjectNode);
        IRI predicate = nodeToIRI(predicateNode);
        Value object = nodeToValue(objectNode);

        Node graphNode = edge.getGraph();
        Resource context = (graphNode == null || graph.isDefaultGraphNode(graphNode))
                ? null
                : nodeToResource(graphNode);

        if (context == null) {
            return valueFactory.createStatement(subject, predicate, object);
        } else {
            return valueFactory.createStatement(subject, predicate, object, context);
        }
    }

    /**
     * Converts a Graph {@link Node} to a Storage {@link Resource} (IRI or BNode).
     */
    private Resource nodeToResource(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        IDatatype dt = node.getDatatypeValue();
        if (dt == null) {
            throw new IllegalArgumentException("Node datatype is null: " + node);
        }

        if (dt.isURI()) {
            return valueFactory.createIRI(dt.getLabel());
        } else if (dt.isBlank()) {
            return valueFactory.createBNode(dt.getLabel());
        } else if (dt.isTriple()) {
            return valueFactory.createBNode(dt.getLabel());
        } else {
            throw new IllegalArgumentException("Node is not a Resource: " + node);
        }
    }

    /**
     * Converts a Graph {@link Node} (predicate) to a Storage {@link IRI}.
     */
    private IRI nodeToIRI(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        IDatatype dt = node.getDatatypeValue();
        if (dt == null) {
            throw new IllegalArgumentException("Node datatype is null: " + node);
        }

        if (!dt.isURI()) {
            throw new IllegalArgumentException("Node is not an IRI: " + node);
        }

        return valueFactory.createIRI(dt.getLabel());
    }

    /**
     * Converts a Graph {@link Node} to a Storage {@link Value} (Resource or Literal).
     */
    private Value nodeToValue(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        IDatatype dt = node.getDatatypeValue();
        if (dt == null) {
            throw new IllegalArgumentException("Node datatype is null: " + node);
        }

        if (dt.isURI() || dt.isBlank() || dt.isTriple()) {
            return nodeToResource(node);
        } else if (dt.isLiteral()) {
            String label = dt.getLabel();
            String lang = dt.getLang();
            String datatypeIRI = dt.getDatatypeURI();

            if (lang != null && !lang.isEmpty()) {
                return valueFactory.createLiteral(label, lang);
            } else if (datatypeIRI != null && !datatypeIRI.isEmpty()) {
                IRI datatype = valueFactory.createIRI(datatypeIRI);
                return valueFactory.createLiteral(label, datatype);
            } else {
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
        Node context = (ctxResource != null)
                ? resourceToNode(ctxResource)
                : graph.getDefaultGraphNode();

        return graph.create(context, subject, predicate, object);
    }

    /**
     * Converts a Storage {@link Resource} to a Graph {@link Node}.
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
     */
    private Node iriToNode(IRI iri) {
        return graph.addProperty(iri.stringValue());
    }

    /**
     * Converts a Storage {@link Value} to a Graph {@link Node}.
     */
    private Node valueToNode(Value value) {
        if (value.isResource()) {
            return resourceToNode((Resource) value);
        } else if (value.isLiteral()) {
            Literal literal = (Literal) value;
            String label = literal.getLabel();

            if (literal.getLanguage().isPresent()) {
                String lang = literal.getLanguage().get();
                return graph.addLiteral(label, null, lang);
            } else if (literal.getDatatype() != null) {
                String datatypeIRI = literal.getDatatype().stringValue();
                return graph.addLiteral(label, datatypeIRI);
            } else {
                return graph.addLiteral(label);
            }
        } else {
            throw new IllegalArgumentException("Unknown Value type: " + value);
        }
    }
}