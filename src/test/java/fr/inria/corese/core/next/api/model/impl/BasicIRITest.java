package fr.inria.corese.core.next.api.model.impl;

import fr.inria.corese.core.next.api.model.impl.basic.BasicIRI;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BasicIRITest {

    @Test
    public void testIsIRI() {
        BasicIRI basicIRI = new BasicIRI("http://example.org");
        assertTrue(basicIRI.isIRI());
    }

    @Test
    public void testGetNamespace() {
        BasicIRI basicIRI = new BasicIRI("http://example.org/test");
        assertEquals("http://example.org/", basicIRI.getNamespace());
        BasicIRI basicIRI2 = new BasicIRI("http://example.org/test#fragment");
        assertEquals("http://example.org/test#", basicIRI2.getNamespace());
    }

    @Test
    public void testGetLocalName() {
        BasicIRI basicIRI = new BasicIRI("http://example.org/test");
        assertEquals("test", basicIRI.getLocalName());
        BasicIRI basicIRI2 = new BasicIRI("http://example.org/test#fragment");
        assertEquals("fragment", basicIRI2.getLocalName());
    }
}
