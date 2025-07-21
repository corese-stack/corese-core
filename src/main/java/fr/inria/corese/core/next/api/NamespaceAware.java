package fr.inria.corese.core.next.api;

import java.util.Optional;
import java.util.Set;

/**
 * An interface for models or stores that support RDF namespaces.
 */
public interface NamespaceAware {

    /**
     * Returns the set of namespaces defined in the model.
     *
     * @return a set of Namespace objects
     */
    Set<Namespace> getNamespaces();

    /**
     * Returns the namespace associated with the given prefix, if any.
     *
     * @param prefix the namespace prefix
     * @return an Optional containing the Namespace, or empty if none found
     */
    default Optional<Namespace> getNamespace(String prefix) {
        return getNamespaces().stream()
                .filter(ns -> ns.getPrefix().equals(prefix))
                .findFirst();
    }
}
