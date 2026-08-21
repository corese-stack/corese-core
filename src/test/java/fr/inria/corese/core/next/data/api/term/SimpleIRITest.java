package fr.inria.corese.core.next.data.api.term;

public class SimpleIRITest extends IRITest {
    @Override
    public IRI createIRI(String iri) {
        return new SimpleIRI(iri);
    }

    @Override
    public IRI createIRI(String namespace, String localName) {
        return new SimpleIRI(namespace, localName);
    }
}
