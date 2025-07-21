package fr.inria.corese.core.next.impl.common;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.IRITest;

public class BasicIRITest extends IRITest {
    @Override
    public IRI createIRI(String iri) {
        return new BasicIRI(iri);
    }

    @Override
    public IRI createIRI(String namespace, String localName) {
        return new BasicIRI(namespace, localName);
    }
}
