package fr.inria.corese.core.next.data.api.term;

import fr.inria.corese.core.next.data.api.support.term.AbstractIRI;
import fr.inria.corese.core.next.data.api.exception.IncorrectFormatException;

/**
 * Immutable default implementation of an RDF IRI.
 */
public final class SimpleIRI extends AbstractIRI {

    private static final long serialVersionUID = -2444556019914696994L;
    /**
     * Creates an IRI from its complete string representation.
     *
     * @param fullIRI the full IRI string
     * @throws IncorrectFormatException if fullIRI does not form a correct IRI
     */
    public SimpleIRI(String fullIRI) {
        super(fullIRI);
    }

    /**
     * Creates an IRI from a namespace and a local name.
     *
     * @param namespace the namespace of the IRI
     * @param localName the local name of the IRI
     * @throws IncorrectFormatException if namespace and local name do not form a correct IRI
     */
    public SimpleIRI(String namespace, String localName) {
        super(namespace, localName);
    }

}
