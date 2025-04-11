package fr.inria.corese.core.next.api;

import java.io.Serializable;

/**
 * Represents a namespace with a prefix and the start of an IRI as its name.
 */
public interface Namespace extends Serializable, Comparable<Namespace> {

    String getPrefix();

    String getName();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

}
