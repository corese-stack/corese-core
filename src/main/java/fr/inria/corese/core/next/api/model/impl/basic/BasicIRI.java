package fr.inria.corese.core.next.api.model.impl.basic;

import fr.inria.corese.core.next.api.model.impl.AbstractIRI;

/**
 * Basic implementation of IRI
 */
public class BasicIRI extends AbstractIRI {

    public BasicIRI(String fullIRI) {
        super(fullIRI);
    }

    public BasicIRI(String namespace, String localName) {
        super(namespace, localName);
    }

}
