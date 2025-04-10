package fr.inria.corese.core.next.impl.basic;

import fr.inria.corese.core.next.api.base.model.AbstractIRI;

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
