package fr.inria.corese.core.next.data.api.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import fr.inria.corese.core.next.data.api.exception.IncorrectFormatException;

class SimpleIRITest extends IRITest {
    private static final String NAMESPACE = "http://example.org/";
    private static final String FULL_IRI = NAMESPACE + "term";

    @Override
    public IRI createIRI(String iri) {
        return new SimpleIRI(iri);
    }

    @Override
    public IRI createIRI(String namespace, String localName) {
        return new SimpleIRI(namespace, localName);
    }

    @Test
    void createsIRIFromFullString() {
        SimpleIRI iri = new SimpleIRI(FULL_IRI);

        assertEquals(NAMESPACE, iri.getNamespace());
        assertEquals("term", iri.getLocalName());
        assertEquals(FULL_IRI, iri.stringValue());
    }

    @Test
    void createsIRIFromNamespaceAndLocalName() {
        SimpleIRI iri = new SimpleIRI(NAMESPACE, "term");

        assertEquals(NAMESPACE, iri.getNamespace());
        assertEquals("term", iri.getLocalName());
        assertEquals(FULL_IRI, iri.stringValue());
    }

    @Test
    void equalityAndHashCodeDependOnFullIRI() {
        SimpleIRI full = new SimpleIRI(FULL_IRI);
        SimpleIRI split = new SimpleIRI(NAMESPACE, "term");

        assertEquals(full, split);
        assertEquals(split, full);
        assertEquals(full.hashCode(), split.hashCode());
        assertNotEquals(full, new SimpleIRI(NAMESPACE, "other"));
        assertNotEquals(null, full);
        assertNotEquals(FULL_IRI, full);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "relative/path", "http://example.org/invalid path"})
    void rejectsInvalidFullIRI(String iri) {
        assertThrows(IncorrectFormatException.class, () -> new SimpleIRI(iri));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid name", "\t", "\n"})
    void rejectsInvalidNamespaceAndLocalNameCombination(String localName) {
        assertThrows(IncorrectFormatException.class, () -> new SimpleIRI(NAMESPACE, localName));
    }
}
