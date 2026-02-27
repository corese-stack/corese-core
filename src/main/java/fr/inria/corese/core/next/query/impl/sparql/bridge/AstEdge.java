package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.api.core.PointerType;
import fr.inria.corese.core.next.query.kgram.core.PointerObject;

/**
 * Implementation of KGRAM {@link Edge} for triple patterns built from the SPARQL AST.
 */
public final class AstEdge extends PointerObject implements Edge {

    private final Node subject;
    private final Node predicate;
    private final Node object;
    private final String edgeLabel;
    private int edgeIndex = -1;

    public AstEdge(Node subject, Node predicate, Node object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.edgeLabel = predicate != null && predicate.isConstant()
                ? predicate.getLabel()
                : "";
    }

    public static Edge create(Node subject, Node predicate, Node object) {
        return new AstEdge(subject, predicate, object);
    }

    @Override
    public Node getNode(int i) {
        return switch (i) {
            case 0 -> subject;
            case 1 -> object;
            default -> throw new IndexOutOfBoundsException("node index: " + i);
        };
    }

    @Override
    public Node getProperty() {
        return predicate;
    }

    @Override
    public Node getEdgeVariable() {
        return (predicate != null && predicate.isVariable()) ? predicate : null;
    }

    @Override
    public String getEdgeLabel() {
        return edgeLabel;
    }

    @Override
    public int getEdgeIndex() {
        return edgeIndex;
    }

    @Override
    public void setEdgeIndex(int n) {
        this.edgeIndex = n;
    }

    @Override
    public boolean contains(Node node) {
        return node != null && (node.equals(subject) || node.equals(predicate) || node.equals(object));
    }

    @Override
    public Node getNode() {
        return subject;
    }

    @Override
    public Node getGraph() {
        return null;
    }

    @Override
    public Edge getEdge() {
        return this;
    }

    @Override
    public PointerType pointerType() {
        return PointerType.STATEMENT;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", subject, predicate, object);
    }
}
