package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.api.core.PointerType;
import fr.inria.corese.core.next.query.kgram.core.PointerObject;

/**
 * Implementation of KGRAM {@link Edge} for triple patterns produced from the SPARQL AST.
 *
 * <p>
 * This class represents a single triple pattern (subject–predicate–object)
 * in the KGRAM query model. It is typically created by the SPARQL → KGRAM
 * bridge layer (e.g., {@code CoresePatternBuilder}) when converting
 * {@code TriplePatternAst} nodes into executable KGRAM expressions.
 * </p>
 *
 * <p>
 * The edge holds three {@link Node} instances:
 * <ul>
 *     <li>index 0 → subject</li>
 *     <li>property → predicate</li>
 *     <li>index 1 → object</li>
 * </ul>
 * </p>
 *
 * <p>
 * Graph information is not handled yet. The {@link #getGraph()}
 * method therefore returns {@code null}.
 * </p>
 *
 * <p>
 * The {@code edgeLabel} is derived from the predicate if it is a constant,
 * and is used by KGRAM internal mechanisms for indexing and debugging.
 * </p>
 */
public final class AstEdge extends PointerObject implements Edge {
    /** Subject node of the triple pattern (position 0). */
    private final Node subject;

    /** Predicate node of the triple pattern (property). */
    private final Node predicate;

    /** Object node of the triple pattern (position 1). */
    private final Node object;

    /**
     * Cached edge label derived from the predicate if constant,
     * otherwise empty string.
     */
    private final String edgeLabel;

    /**
     * Internal edge index used by KGRAM during query planning/execution.
     * Default is -1 (unset).
     */
    private int edgeIndex = -1;

    /**
     * Constructs a new triple-pattern edge.
     *
     * @param subject   subject node (must not be null)
     * @param predicate predicate node (must not be null)
     * @param object    object node (must not be null)
     */
    public AstEdge(Node subject, Node predicate, Node object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.edgeLabel = predicate != null && predicate.isConstant()
                ? predicate.getLabel()
                : "";
    }

    /**
     * Factory method for creating a new {@link Edge} instance.
     *
     * @param subject   subject node
     * @param predicate predicate node
     * @param object    object node
     * @return a new {@link AstEdge}
     */
    public static Edge create(Node subject, Node predicate, Node object) {
        return new AstEdge(subject, predicate, object);
    }

    /**
     * Returns the node at the given index:
     * <ul>
     *     <li>0 → subject</li>
     *     <li>1 → object</li>
     * </ul>
     *
     * @param i index (0 or 1)
     * @return corresponding node
     * @throws IndexOutOfBoundsException if index is not 0 or 1
     */
    @Override
    public Node getNode(int i) {
        return switch (i) {
            case 0 -> subject;
            case 1 -> object;
            default -> throw new IndexOutOfBoundsException("node index: " + i);
        };
    }

    /**
     * Returns the predicate node of the triple pattern.
     *
     * @return predicate node
     */
    @Override
    public Node getProperty() {
        return predicate;
    }

    /**
     * Returns the predicate if it is a variable, otherwise {@code null}.
     *
     * <p>This is used by KGRAM when handling triple patterns
     * with variable predicates.</p>
     *
     * @return variable predicate node or null
     */
    @Override
    public Node getEdgeVariable() {
        return (predicate != null && predicate.isVariable()) ? predicate : null;
    }

    /**
     * Returns the cached edge label (derived from predicate if constant).
     *
     * @return edge label (empty string if predicate is not constant)
     */
    @Override
    public String getEdgeLabel() {
        return edgeLabel;
    }

    /**
     * Returns the internal edge index assigned during query planning.
     *
     * @return edge index, or -1 if unset
     */
    @Override
    public int getEdgeIndex() {
        return edgeIndex;
    }

    /**
     * Sets the internal edge index.
     *
     * @param n index value
     */
    @Override
    public void setEdgeIndex(int n) {
        this.edgeIndex = n;
    }

    /**
     * Returns true if the given node matches subject, predicate or object.
     *
     * @param node node to test
     * @return true if contained in this triple pattern
     */
    @Override
    public boolean contains(Node node) {
        return node != null && (node.equals(subject) || node.equals(predicate) || node.equals(object));
    }

    /**
     * Returns the primary node of this pointer (subject).
     *
     * <p>This satisfies the {@link PointerObject} contract.</p>
     *
     * @return subject node
     */
    @Override
    public Node getNode() {
        return subject;
    }

    /**
     * Returns the graph node for this edge.
     *
     * Not implemented yet
     *
     * @return
     */
    @Override
    public Node getGraph() {
        return null;
    }

    /**
     * Returns this edge instance.
     *
     * @return this edge
     */
    @Override
    public Edge getEdge() {
        return this;
    }

    /**
     * Returns the pointer type for this object.
     *
     * @return {@link PointerType#STATEMENT}
     */
    @Override
    public PointerType pointerType() {
        return PointerType.STATEMENT;
    }

    /**
     * Returns a human-readable representation of the triple pattern.
     *
     * @return string representation "subject predicate object"
     */
    @Override
    public String toString() {
        return String.format("%s %s %s", subject, predicate, object);
    }
}