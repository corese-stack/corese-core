package fr.inria.corese.core.next.api.model.impl;

import org.junit.Test;

import static org.junit.Assert.*;

public class CoreseIRITest {

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
        assertThrows(IllegalArgumentException.class, () -> new CoreseIRI("test"));
    }

    @Test
    public void isIRITest() {
        CoreseIRI coreseIRI = new CoreseIRI("http://example.org");
        assertTrue(coreseIRI.isIRI());
    }

    @Test
    public void getNamespaceTest() {
        CoreseIRI coreseIRI = new CoreseIRI("http://example.org/test");
        assertEquals("http://example.org/", coreseIRI.getNamespace());
        CoreseIRI coreseIRI2 = new CoreseIRI("http://example.org/test#fragment");
        assertEquals("http://example.org/test#", coreseIRI2.getNamespace());
    }

    @Test
    public void getLocalNameTest() {
        CoreseIRI coreseIRI = new CoreseIRI("http://example.org/test");
        assertEquals("test", coreseIRI.getLocalName());
        CoreseIRI coreseIRI2 = new CoreseIRI("http://example.org/test#fragment");
        assertEquals("fragment", coreseIRI2.getLocalName());
    }

}
