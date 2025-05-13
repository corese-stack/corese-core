package fr.inria.corese.core.next.impl.temp;

import fr.inria.corese.core.next.api.IRITest;
import fr.inria.corese.core.next.impl.exception.IncorrectFormatException;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.impl.temp.CoreseIRI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CoreseIRITest extends IRITest {

    @Override
    public IRI createIRI(String iri) {
        return new CoreseIRI(iri);
    }

    @Override
    public IRI createIRI(String namespace, String localName) {
        return new CoreseIRI(namespace, localName);
    }

    @Test
    public void constructorStringTest() {
        CoreseIRI coreseIRI = new CoreseIRI("http://example.org/test");
        assertEquals("http://example.org/test", coreseIRI.stringValue());
        assertEquals("http://example.org/test", coreseIRI.getCoreseNode().getLabel());
        assertEquals("http://example.org/", coreseIRI.getNamespace());
        assertEquals("test", coreseIRI.getLocalName());
    }

    @Test
    public void constructorIriTest() {
        CoreseIRI coreseIRI = new CoreseIRI("http://example.org/test");
        CoreseIRI coreseIRI2 = new CoreseIRI(coreseIRI.getCoreseNode());
        assertEquals("http://example.org/test", coreseIRI2.stringValue());
        assertEquals("http://example.org/test", coreseIRI2.getCoreseNode().getLabel());
        assertEquals("http://example.org/", coreseIRI2.getNamespace());
        assertEquals("test", coreseIRI2.getLocalName());
    }

    @Test
    public void constructorCoreseNodeTest() {
        CoreseIRI coreseIRI = new CoreseIRI("http://example.org/test");
        CoreseIRI coreseIRI2 = new CoreseIRI(coreseIRI.getCoreseNode());
        assertEquals("http://example.org/test", coreseIRI2.stringValue());
        assertEquals("http://example.org/test", coreseIRI2.getCoreseNode().getLabel());
        assertEquals("http://example.org/", coreseIRI2.getNamespace());
        assertEquals("test", coreseIRI2.getLocalName());
    }

    @Test
    public void constructorStringException() {
        assertThrows(IncorrectFormatException.class, () -> new CoreseIRI("test"));
    }

}
