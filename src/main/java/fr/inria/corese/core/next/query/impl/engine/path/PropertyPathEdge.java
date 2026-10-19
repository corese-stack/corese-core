package fr.inria.corese.core.next.query.impl.engine.path;

import fr.inria.corese.core.next.query.impl.engine.pattern.PlanCopy;
import fr.inria.corese.core.next.query.impl.engine.pattern.PlanCopyable;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.KgramNodes;
import fr.inria.corese.core.next.query.impl.engine.model.Node;

/** Logical binary relation carried through the engine's ordinary edge joins. */
public record PropertyPathEdge(Node subject, PropertyPath path, Node object, Node graph) implements Edge, PlanCopyable<PropertyPathEdge> {
    /** Copies the path relation and all of its indexed nodes. */
    @Override
    public PropertyPathEdge copyPlan(PlanCopy copies) {
        PropertyPathEdge result = new PropertyPathEdge(copies.copy(subject), copies.copy(path),
                copies.copy(object), copies.copy(graph));
        copies.remember(this, result);
        return result;
    }

    @Override
    public Node getNode(int index) {
        return switch (index) {
            case 0 -> subject;
            case 1 -> object;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public Node getProperty() {
        return KgramNodes.rootProperty();
    }

    @Override
    public String getEdgeLabel() {
        return KgramNodes.ROOT_PROPERTY_URI;
    }

    @Override
    public Node getGraph() {
        return graph;
    }

    @Override
    public boolean contains(Node node) {
        return subject.same(node) || object.same(node);
    }

    @Override
    public Edge getEdge() {
        return this;
    }
}
