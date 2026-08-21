package fr.inria.corese.core.next.data.api.namespace;

import java.io.Serializable;

/**
 * A binding between a prefix and a namespace IRI.
 *
 * <p>For example, {@code ex -> https://example.org/} lets RDF syntaxes write
 * {@code ex:resource}.</p>
 */
public interface Namespace extends Serializable {

    /**
     * @return The prefix of the namespace.
     */
    String getPrefix();

    /**
     * @return The name of the namespace, which is the start of an IRI.
     */
    String getNamespace();

    /**
     * @param o the object to compare for equality
     * @return true if o is a Namespace and has the same prefix and name as this Namespace.
     */
    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

}
