package fr.inria.corese.core.next.data.impl.adapter.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.IRITest;
import fr.inria.corese.core.next.data.api.exception.IncorrectFormatException;

class CoreseIRITest extends IRITest {

    @Override
    public IRI createIRI(String iri) {
        return new CoreseIRI(iri);
    }

    @Override
    public IRI createIRI(String namespace, String localName) {
        return new CoreseIRI(namespace, localName);
    }

    @Test
    void constructorStringTest() {
        CoreseIRI coreseIRI = new CoreseIRI("http://example.org/test");
        assertEquals("http://example.org/test", coreseIRI.stringValue());
        assertEquals("http://example.org/test", coreseIRI.getCoreseNode().getLabel());
        assertEquals("http://example.org/", coreseIRI.getNamespace());
        assertEquals("test", coreseIRI.getLocalName());
    }

    @Test
    void constructorStringTest_otherURIS() {
        CoreseIRI coreseIriNoSlash = new CoreseIRI("http://www.monicamurphy.org");
        assertEquals("http://www.monicamurphy.org", coreseIriNoSlash.stringValue());
        assertEquals("http://www.monicamurphy.org", coreseIriNoSlash.getCoreseNode().getLabel());
        assertEquals("http://www.monicamurphy.org", coreseIriNoSlash.getNamespace());
        assertEquals("", coreseIriNoSlash.getLocalName());

        CoreseIRI coreseIriEmail = new CoreseIRI("mailto:monica@monicamurphy.org");
        assertEquals("mailto:monica@monicamurphy.org", coreseIriEmail.stringValue());
        assertEquals("mailto:monica@monicamurphy.org", coreseIriEmail.getCoreseNode().getLabel());
        assertEquals("mailto:monica@monicamurphy.org", coreseIriEmail.getNamespace());
        assertEquals("", coreseIriEmail.getLocalName());
    }

    @Test
    void constructorIriTest() {
        CoreseIRI coreseIRI = new CoreseIRI("http://example.org/test");
        CoreseIRI coreseIRI2 = new CoreseIRI(coreseIRI.getCoreseNode());
        assertEquals("http://example.org/test", coreseIRI2.stringValue());
        assertEquals("http://example.org/test", coreseIRI2.getCoreseNode().getLabel());
        assertEquals("http://example.org/", coreseIRI2.getNamespace());
        assertEquals("test", coreseIRI2.getLocalName());
    }

    @Test
    void constructorCoreseNodeTest() {
        fr.inria.corese.core.sparql.api.IDatatype node = fr.inria.corese.core.sparql.datatype.DatatypeMap.createResource("http://example.org/testNode");
        CoreseIRI coreseIRI = new CoreseIRI(node);
        assertEquals("http://example.org/testNode", coreseIRI.stringValue());
        assertEquals("http://example.org/testNode", coreseIRI.getCoreseNode().getLabel());
        assertEquals("http://example.org/", coreseIRI.getNamespace());
        assertEquals("testNode", coreseIRI.getLocalName());
    }

    @Test
    void constructorStringException() {

        assertThrows(IncorrectFormatException.class, () -> new CoreseIRI("   "));

        assertThrows(IncorrectFormatException.class, () -> new CoreseIRI("\u00A0"));

        assertThrows(IncorrectFormatException.class, () -> new CoreseIRI(""));

        assertThrows(IncorrectFormatException.class, () -> new CoreseIRI("test string"));

    }

}
