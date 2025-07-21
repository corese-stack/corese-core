package fr.inria.corese.core.next.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public abstract class IRITest {

    public abstract IRI createIRI(String iri);

    public abstract IRI createIRI(String namespace, String localName);

    @Test
    public void testIsIRI() {
        IRI iri = createIRI("http://example.org");
        assertTrue(iri.isIRI());
    }

    @Test
    public void testGetNamespace() {
        IRI basicIRI = createIRI("http://example.org/test");
        assertEquals("http://example.org/", basicIRI.getNamespace());
        IRI basicIRI2 = createIRI("http://example.org/test#fragment");
        assertEquals("http://example.org/test#", basicIRI2.getNamespace());
    }

    @Test
    public void testGetLocalName() {
        IRI basicIRI = createIRI("http://example.org/test");
        assertEquals("test", basicIRI.getLocalName());
        IRI basicIRI2 = createIRI("http://example.org/test#fragment");
        assertEquals("fragment", basicIRI2.getLocalName());
    }

    @Test
    public void equalsTest() {
        IRI basicIRI = createIRI("http://example.org/test");
        IRI basicIRI2 = createIRI("http://example.org/test");
        assertEquals(basicIRI, basicIRI2);
    }
}
