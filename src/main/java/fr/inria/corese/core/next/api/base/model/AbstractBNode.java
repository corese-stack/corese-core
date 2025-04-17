package fr.inria.corese.core.next.api.base.model;

import fr.inria.corese.core.next.api.BNode;

/**
 * Abstract implementation of the {@link BNode} interface, providing common functionality for blank node representations.
 * A blank node (BNode) https://www.w3.org/TR/rdf12-concepts/#section-blank-nodes
 */
public abstract class AbstractBNode implements BNode {

    /**
     * Returns the string value of this blank node, which is its unique identifier.
     * This method is an implementation of {@link BNode#stringValue()} and simply returns the result of {@link #getID()}.
     *
     * @return The string value of the blank node (its unique identifier).
     */
    @Override
    public String stringValue() {
        return getID();
    }

    /**
     * Checks whether this blank node is equal to another object.
     * Two blank nodes are considered equal if they are the same object in memory or if they have the same unique identifier (ID).
     * This method is an implementation of {@link BNode#equals(Object)}.
     *
     * @param o The object to compare this blank node to.
     * @return {@code true} if the two blank nodes are the same object or have the same unique identifier; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof BNode
                && getID().equals(((BNode) o).getID());
    }

    /**
     * Returns the hash code for this blank node. The hash code is based on the unique identifier of the blank node,
     * using the hash code of the ID returned by {@link #getID()}.
     * This method is an implementation of {@link BNode#hashCode()}.
     *
     * @return The hash code for this blank node.
     */
    @Override
    public int hashCode() {
        return getID().hashCode();
    }

    /**
     * Returns a string representation of this blank node in the form "_:{ID}" where {ID} is the unique identifier of the blank node.
     *
     * @return A string representing this blank node, prefixed with "_:" and followed by its unique identifier.
     */
    @Override
    public String toString() {
        return "_:" + getID();
    }
}