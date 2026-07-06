package fr.inria.corese.core.next.query.kgram.tool;

import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.impl.temp.CoreseValueConverter;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.sparql.triple.parser.Constant;

import java.util.Objects;

/**
 * KGRAM edge view over a statement returned by the Corese-next storage layer.
 *
 * <p>KGRAM models a triple edge with the subject at {@code getNode(0)}, the object at
 * {@code getNode(1)}, and the predicate as the edge node. Keeping that contract here
 * lets the storage-backed producer feed regular SPO patterns into the next evaluator.</p>
 */
public final class StorageManagerEdge implements Edge {

    private static final CoreseValueConverter VALUE_CONVERTER = new CoreseValueConverter();

    private final Statement statement;
    private final Node subject;
    private final Node predicate;
    private final Node object;
    private final Node graph;

    public StorageManagerEdge(Statement statement) {
        this.statement = Objects.requireNonNull(statement, "statement");
        this.subject = node(statement.getSubject());
        this.predicate = node(statement.getPredicate());
        this.object = node(statement.getObject());
        this.graph = statement.getContext() == null ? null : node(statement.getContext());
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

    private static Node node(Value value) {
        fr.inria.corese.core.kgram.api.core.Node node = VALUE_CONVERTER.toCoreseNode(value);
        return new NodeImpl(Constant.create(node.getDatatypeValue()));
    }
}
