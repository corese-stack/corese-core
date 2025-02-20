package fr.inria.corese.core.next.api.model.impl;

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
