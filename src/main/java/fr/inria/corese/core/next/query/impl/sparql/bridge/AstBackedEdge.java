package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.tool.KgramNodes;

import java.util.Objects;

/**
 * A query-side {@link Edge} produced by the WHERE compiler from a
 * {@link fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst}.
 */
final class AstBackedEdge implements Edge {

    private final Node subject;
    private final Node edgeNode;
    private final Node edgeVariable;
    private final Node object;
    private final Node graph;

    AstBackedEdge(Node subject, Node predicate, Node object) {
        this(subject, predicate, object, null);
    }

    AstBackedEdge(Node subject, Node predicate, Node object, Node graph) {
        this.subject = Objects.requireNonNull(subject, "subject");
        Node checkedPredicate = Objects.requireNonNull(predicate, "predicate");
        if (checkedPredicate.isVariable()) {
            this.edgeNode = KgramNodes.rootProperty();
            this.edgeVariable = checkedPredicate;
        } else {
            this.edgeNode = checkedPredicate;
            this.edgeVariable = null;
        }
        this.object = Objects.requireNonNull(object, "object");
        this.graph = graph;
    }

    @Override
    public Node getNode(int i) {
        return switch (i) {
            case 0 -> subject;
            case 1 -> object;
            default -> throw new IndexOutOfBoundsException(
                    "Triple pattern edge has nodes 0 (subject) and 1 (object), got: " + i);
        };
    }

    @Override
    public Node getProperty() {
        return edgeVariable == null ? edgeNode : edgeVariable;
    }

    @Override
    public Node getEdgeNode() {
        return edgeNode;
    }

    @Override
    public Node getEdgeVariable() {
        return edgeVariable;
    }

    @Override
    public String getEdgeLabel() {
        return edgeNode.getLabel();
    }

    /**
     * The graph this triple pattern is matched in, or {@code null} for the default graph.
     *
     */
    @Override
    public Node getGraph() {
        return graph;
    }

    @Override
    public boolean contains(Node node) {
        if (node == null) {
            return false;
        }
        for (int i = 0; i < nbNode(); i++) {
            Node n = getNode(i);
            if (n != null && (n == node || n.same(node))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Edge getEdge() {
        return this;
    }

    @Override
    public String toString() {
        return subject + " " + getProperty() + " " + object;
    }
}
