package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;

import java.util.Objects;

/**
 * A query-side {@link Edge} produced by the WHERE compiler from a
 * {@link fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst}.
 */
final class AstBackedEdge implements Edge {

    private final Node subject;
    private final Node predicate;
    private final Node object;

    AstBackedEdge(Node subject, Node predicate, Node object) {
        this.subject = Objects.requireNonNull(subject, "subject");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.object = Objects.requireNonNull(object, "object");
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
        return predicate;
    }

    @Override
    public String getEdgeLabel() {
        return predicate.getLabel();
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
    public String toString() {
        return subject + " " + predicate + " " + object;
    }
}