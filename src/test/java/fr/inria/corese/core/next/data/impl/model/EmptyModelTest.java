package fr.inria.corese.core.next.data.impl.model;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.namespace.Namespace;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EmptyModelTest extends ParserTestBase {

    private final ValueFactory vf = new CoreseValueFactory();
    private Model baseModel;
    private EmptyModel emptyModel;

    @BeforeEach
    void setUp() {
        baseModel = createTestModel();
        emptyModel = new EmptyModel(baseModel);
    }

    @Test
    void testSizeAndEmptiness() {
        assertEquals(0, emptyModel.size());
        assertFalse(emptyModel.iterator().hasNext());
    }

    @Test
    void testContainsReturnsFalse() {
        Resource s = vf.createIRI("http://example.org/s");
        IRI p = vf.createIRI("http://example.org/p");
        Value o = vf.createLiteral("test");

        assertFalse(emptyModel.contains(s, p, o));
    }

    @Test
    void testAddThrowsException() {
        Resource s = vf.createIRI("http://example.org/s");
        IRI p = vf.createIRI("http://example.org/p");
        Value o = vf.createLiteral("test");

        assertThrows(UnsupportedOperationException.class, () -> emptyModel.add(s, p, o));
    }

    @Test
    void testRemoveReturnsFalse() {
        Resource s = vf.createIRI("http://example.org/s");
        IRI p = vf.createIRI("http://example.org/p");
        Value o = vf.createLiteral("test");

        assertFalse(emptyModel.remove(s, p, o));
    }

    @Test
    void testFilterReturnsSelf() {
        Resource s = vf.createIRI("http://example.org/s");
        IRI p = vf.createIRI("http://example.org/p");
        Value o = vf.createLiteral("test");

        assertSame(emptyModel, emptyModel.filter(s, p, o));
    }

    @Test
    void testRemoveTermIterationNoOp() {
        assertDoesNotThrow(() -> emptyModel.removeTermIteration(Collections.emptyIterator(), null, null, null));
    }

    @Test
    void testNamespaceOperationsDelegate() {
        emptyModel.setNamespace("ex", "http://example.org/");
        Optional<Namespace> ns = emptyModel.getNamespace("ex");

        assertTrue(ns.isPresent());
        assertEquals("ex", ns.get().getPrefix());
        assertEquals("http://example.org/", ns.get().getNamespace());

        assertEquals(baseModel.getNamespaces(), emptyModel.getNamespaces());

        Optional<Namespace> removed = emptyModel.removeNamespace("ex");
        assertTrue(removed.isPresent());
        assertFalse(emptyModel.getNamespace("ex").isPresent());
    }
}
