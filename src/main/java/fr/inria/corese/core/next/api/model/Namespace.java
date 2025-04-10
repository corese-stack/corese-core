package fr.inria.corese.core.next.api.model;

import java.io.Serializable;

/**
 * Represents a namespace with a prefix and the start of an IRI as its name.
 */
public interface Namespace extends Serializable, Comparable<Namespace> {

    /**
     * @return The prefix of the namespace.
     */
    String getPrefix();

    /**
     * @return The name of the namespace, which is the start of an IRI.
     */
    String getName();

    /**
     * @param o
     * @return true if o is a Namespace and has the same prefix and name as this Namespace.
     */
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

}
