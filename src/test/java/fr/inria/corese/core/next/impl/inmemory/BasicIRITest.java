package fr.inria.corese.core.next.impl.inmemory;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.IRITest;
import fr.inria.corese.core.next.impl.common.BasicIRI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
