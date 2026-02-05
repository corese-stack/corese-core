package fr.inria.corese.core.next.data.impl.common;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.IRITest;
import fr.inria.corese.core.next.data.impl.common.BasicIRI;

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
