package fr.inria.corese.core.next.data.api.term;

import java.io.Serial;

import fr.inria.corese.core.next.data.spi.term.AbstractBNode;

/**
 * Immutable default implementation of an RDF blank node.
 */
public final class SimpleBNode extends AbstractBNode {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;

    /**
     * Creates a blank node with the supplied identifier.
     *
     * @param id the blank node identifier
     * @throws NullPointerException if the identifier is null
     */
    public SimpleBNode(String id) {
        this.id = java.util.Objects.requireNonNull(id, "Blank node identifier must not be null");
    }

    /**
     * Returns this blank node's identifier.
     *
     * @return the identifier supplied at construction
     */
    @Override
    public String getID() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof BNode other && id.equals(other.getID()));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
