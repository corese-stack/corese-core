package fr.inria.corese.core.next.data.impl.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.inria.corese.core.next.data.impl.adapter.CoreseIRI;
import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.IRITest;
import fr.inria.corese.core.next.data.impl.exception.IncorrectFormatException;

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
    public void constructorStringTest_otherURIS() {
        CoreseIRI coreseIRI_noSlash = new CoreseIRI("http://www.monicamurphy.org");
        assertEquals("http://www.monicamurphy.org", coreseIRI_noSlash.stringValue());
        assertEquals("http://www.monicamurphy.org", coreseIRI_noSlash.getCoreseNode().getLabel());
        assertEquals("http://www.monicamurphy.org", coreseIRI_noSlash.getNamespace());
        assertEquals("", coreseIRI_noSlash.getLocalName());

        CoreseIRI coreseIRI_email = new CoreseIRI("mailto:monica@monicamurphy.org");
        assertEquals("mailto:monica@monicamurphy.org", coreseIRI_email.stringValue());
        assertEquals("mailto:monica@monicamurphy.org", coreseIRI_email.getCoreseNode().getLabel());
        assertEquals("mailto:monica@monicamurphy.org", coreseIRI_email.getNamespace());
        assertEquals("", coreseIRI_email.getLocalName());
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

        assertThrows(IncorrectFormatException.class, () -> new CoreseIRI("   "));

        assertThrows(IncorrectFormatException.class, () -> new CoreseIRI("\u00A0"));

        assertThrows(IncorrectFormatException.class, () -> new CoreseIRI(""));

        assertThrows(IncorrectFormatException.class, () -> new CoreseIRI("test string"));

    }

}
