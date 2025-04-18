package fr.inria.corese.core.next.impl.common;

import fr.inria.corese.core.next.api.base.model.AbstractIRI;
import fr.inria.corese.core.next.impl.exception.IncorrectFormatException;

/**
 * Basic implementation of IRI
 */
public class BasicIRI extends AbstractIRI {

    private static final long serialVersionUID = -2444556019914696994L;
    /**
     * Constructor for BasicIRI.
     *
     * @param fullIRI the full IRI string
     * @throws IncorrectFormatException if fullIRI does not form a correct IRI
     */
    public BasicIRI(String fullIRI) {
        super(fullIRI);
    }

    /**
     * Constructor for BasicIRI with namespace and local name.
     *
     * @param namespace the namespace of the IRI
     * @param localName the local name of the IRI
     * @throws IncorrectFormatException if namespace and local name do not form a correct IRI
     */
    public BasicIRI(String namespace, String localName) {
        super(namespace, localName);
    }

}
