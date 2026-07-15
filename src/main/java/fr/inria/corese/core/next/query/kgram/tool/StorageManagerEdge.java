package fr.inria.corese.core.next.query.kgram.tool;

import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;

import java.util.Objects;

/**
 * KGRAM edge view over a statement returned by the Corese-next storage layer.
 *
 * <p>KGRAM models a triple edge with the subject at {@code getNode(0)}, the object at
 * {@code getNode(1)}, and the predicate as the edge node. Keeping that contract here
 * lets the storage-backed producer feed regular SPO patterns into the next evaluator.</p>
 */
public final class StorageManagerEdge implements Edge {

    private final Statement statement;
    private final Node subject;
    private final Node predicate;
    private final Node object;
    private final Node graph;

    public StorageManagerEdge(Statement statement) {
        this.statement = Objects.requireNonNull(statement, "statement");
        this.subject = StorageManagerKgramValues.node(statement.getSubject());
        this.predicate = StorageManagerKgramValues.node(statement.getPredicate());
        this.object = StorageManagerKgramValues.node(statement.getObject());
        this.graph = statement.getContext() == null ? null : StorageManagerKgramValues.node(statement.getContext());
    }

    public Statement getSourceStatement() {
        return statement;
    }

    @Override
    public Node getNode(int i) {
        return switch (i) {
            case 0 -> subject;
            case 1 -> object;
            default -> throw new IndexOutOfBoundsException(
                    "Statement edge has nodes 0 (subject) and 1 (object), got: " + i);
        };
    }

    @Override
    public Node getProperty() {
        return predicate;
    }

    @Override
    public String getEdgeLabel() {
        return predicate.getLabel();
    }

    @Override
    public Node getGraph() {
        return graph;
    }

    @Override
    public boolean contains(Node node) {
        if (node == null) {
            return false;
        }
        return sameNode(subject, node) || sameNode(predicate, node) || sameNode(object, node);
    }

    @Override
    public Edge getEdge() {
        return this;
    }

    private static boolean sameNode(Node left, Node right) {
        return left == right || left.same(right);
    }
}
