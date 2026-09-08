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
     * @throws IllegalArgumentException if the identifier is null or blank
     */
    public SimpleBNode(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Blank node identifier must not be null or blank");
        }
        this.id = id;
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
