package fr.inria.corese.core.next.data.api.term;

import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleTripleTest {

    private final CoreseValueFactory values = new CoreseValueFactory();

    @Test
    void factoryCreatesImmutableTripleTerm() {
        Resource subject = values.createIRI("http://example.org/s");
        IRI predicate = values.createIRI("http://example.org/p");
        Value object = values.createLiteral("value");

        Triple triple = values.createTriple(subject, predicate, object);

        assertEquals(subject, triple.subject());
        assertEquals(predicate, triple.predicate());
        assertEquals(object, triple.object());
        assertTrue(triple.isTriple());
        assertFalse(triple.isResource());
    }

    @Test
    void equalityUsesTheThreeRdfTerms() {
        Resource subject = values.createIRI("http://example.org/s");
        IRI predicate = values.createIRI("http://example.org/p");
        Value object = values.createLiteral("value");

        Triple first = new SimpleTriple(subject, predicate, object);
        Triple second = new SimpleTriple(subject, predicate, object);
        Triple different = new SimpleTriple(subject, predicate, values.createLiteral("other"));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, different);
    }

    @Test
    void rejectsNullComponents() {
        Resource subject = values.createIRI("http://example.org/s");
        IRI predicate = values.createIRI("http://example.org/p");
        Value object = values.createLiteral("value");

        assertThrows(NullPointerException.class, () -> new SimpleTriple(null, predicate, object));
        assertThrows(NullPointerException.class, () -> new SimpleTriple(subject, null, object));
        assertThrows(NullPointerException.class, () -> new SimpleTriple(subject, predicate, null));
    }
}
