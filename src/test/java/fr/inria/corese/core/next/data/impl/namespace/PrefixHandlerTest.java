package fr.inria.corese.core.next.data.impl.namespace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PrefixHandler.
 */
@DisplayName("Unit Tests for PrefixHandler")
class PrefixHandlerTest {

    private PrefixHandler handler;

    /**
     * Initializes a new {@link PrefixHandler} instance before each test.
     * The handler is set to start without loading standard vocabularies.
     */
    @BeforeEach
    void setUp() {
        handler = new PrefixHandler(false);
    }

    /**
     * Tests basic prefix setting and namespace retrieval.
     */
    @Test
    @DisplayName("Should set and retrieve a prefix")
    void testSetAndGetPrefix() {
        handler.setPrefix("ex", "http://example.org/");
        assertEquals("http://example.org/", handler.getNamespace("ex"));
    }

    /**
     * Tests setting and retrieving multiple distinct prefixes.
     */
    @Test
    @DisplayName("Should handle multiple prefixes")
    void testSetAndGetMultiplePrefixes() {
        handler.setPrefix("ex", "http://example.org/");
        handler.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");

        assertEquals("http://example.org/", handler.getNamespace("ex"));
        assertEquals("http://xmlns.com/foaf/0.1/", handler.getNamespace("foaf"));
    }

    /**
     * Tests that requesting a non-existent prefix returns null.
     */
    @Test
    @DisplayName("Should return null for a non-existent prefix")
    void testGetNonExistentPrefix() {
        assertNull(handler.getNamespace("unknown"));
    }


    /**
     * Tests for {@link IllegalArgumentException} when setting a null prefix.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException if prefix is null")
    void testSetPrefixNullPrefix() {
        assertThrows(IllegalArgumentException.class, () -> handler.setPrefix(null, "http://example.org/"));
    }

    /**
     * Tests for {@link IllegalArgumentException} when setting a null namespace.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException if namespace is null")
    void testSetPrefixNullNamespace() {
        assertThrows(IllegalArgumentException.class, () -> handler.setPrefix("ex", null));
    }


    /**
     * Tests the reverse lookup functionality, getting the prefix from a namespace URI.
     */
    @Test
    @DisplayName("Should get prefix for a namespace (reverse lookup)")
    void testGetPrefixForNamespace() {
        handler.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");
        assertEquals("foaf", handler.getPrefix("http://xmlns.com/foaf/0.1/"));
    }


    /**
     * Tests the removal of a prefix and ensures both forward and reverse mappings are cleared.
     */
    @Test
    @DisplayName("Should remove a prefix and its reverse mapping")
    void testRemovePrefix() {
        handler.setPrefix("ex", "http://example.org/");
        assertTrue(handler.removePrefix("ex"));

        assertNull(handler.getNamespace("ex"));
        assertNull(handler.getPrefix("http://example.org/"));
    }


    /**
     * Tests the {@code clear} method to ensure all mappings are removed.
     */
    @Test
    @DisplayName("Should completely clear the handler")
    void testClear() {
        handler.setPrefix("ex", "http://example.org/");
        handler.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");

        handler.clear();

        assertTrue(handler.isEmpty());
        assertEquals(0, handler.size());
    }

    /**
     * Tests the retrieval of the set of all registered prefixes.
     */
    @Test
    @DisplayName("Should return the set of all prefixes")
    void testGetPrefixes() {
        handler.setPrefix("ex", "http://example.org/");
        handler.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");

        var prefixes = handler.getPrefixes();
        assertEquals(2, prefixes.size());
        assertTrue(prefixes.contains("ex"));
        assertTrue(prefixes.contains("foaf"));
    }

    /**
     * Tests the retrieval of the set of all registered namespaces.
     */
    @Test
    @DisplayName("Should return the set of all namespaces")
    void testGetNamespaces() {
        handler.setPrefix("ex", "http://example.org/");
        handler.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");

        var namespaces = handler.getNamespaces();
        assertEquals(2, namespaces.size());
        assertTrue(namespaces.contains("http://example.org/"));
        assertTrue(namespaces.contains("http://xmlns.com/foaf/0.1/"));
    }

    /**
     * Tests the correct count of registered prefix mappings.
     */
    @Test
    @DisplayName("Should return the correct size")
    void testSize() {
        assertEquals(0, handler.size());

        handler.setPrefix("ex", "http://example.org/");
        assertEquals(1, handler.size());

        handler.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");
        assertEquals(2, handler.size());
    }

    /**
     * Tests the {@code isEmpty} method in various states.
     */
    @Test
    @DisplayName("Should check if the handler is empty")
    void testIsEmpty() {
        assertTrue(handler.isEmpty());

        handler.setPrefix("ex", "http://example.org/");
        assertFalse(handler.isEmpty());

        handler.clear();
        assertTrue(handler.isEmpty());
    }


    /**
     * Tests the successful expansion of a prefixed name to a full IRI.
     */
    @Test
    @DisplayName("Should expand a prefixed name to a full IRI")
    void testExpandPrefix() {
        handler.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");

        String expanded = handler.expandPrefix("foaf:Person");
        assertEquals("http://xmlns.com/foaf/0.1/Person", expanded);
    }

    /**
     * Tests expansion using the empty string as a prefix (default namespace).
     */
    @Test
    @DisplayName("Should handle expansion with an empty prefix")
    void testExpandPrefixWithEmptyPrefix() {
        handler.setPrefix("", "http://example.org/");

        String expanded = handler.expandPrefix(":localName");
        assertEquals("http://example.org/localName", expanded);
    }

    /**
     * Tests that expansion of a name with an unknown prefix returns null.
     */
    @Test
    @DisplayName("Should return null for unknown prefix expansion")
    void testExpandPrefixUnknownPrefix() {
        String expanded = handler.expandPrefix("unknown:term");
        assertNull(expanded);
    }

    /**
     * Tests for {@link IllegalArgumentException} when expanding a null input string.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException if expansion input is null")
    void testExpandPrefixNullInput() {
        assertThrows(IllegalArgumentException.class, () -> handler.expandPrefix(null));
    }

    /**
     * Tests the successful compression of a full IRI to a prefixed name.
     */
    @Test
    @DisplayName("Should compress an IRI to a prefixed name")
    void testCompressIRI() {
        handler.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");

        String compressed = handler.compressIRI("http://xmlns.com/foaf/0.1/Person");
        assertEquals("foaf:Person", compressed);
    }

    /**
     * Tests for {@link IllegalArgumentException} when compressing a null input string.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException if compression input is null")
    void testCompressIRINullInput() {
        assertThrows(IllegalArgumentException.class, () -> handler.compressIRI(null));
    }

    /**
     * Tests that standard W3C vocabularies (rdf, rdfs, xsd, owl) are included
     * when the handler is constructed with {@code true}.
     */
    @Test
    @DisplayName("Should include all standard vocabularies (RDF, RDFS, XSD, OWL, FOAF)")
    void testStandardVocabulariesIncluded() {
        PrefixHandler handlerWithStd = new PrefixHandler(true);

        assertNotNull(handlerWithStd.getNamespace("rdf"), "RDF should be included.");
        assertNotNull(handlerWithStd.getNamespace("rdfs"), "RDFS should be included.");
        assertNotNull(handlerWithStd.getNamespace("xsd"), "XSD should be included.");
        assertNotNull(handlerWithStd.getNamespace("owl"), "OWL should be included.");
    }

    /**
     * Tests that standard vocabularies are *not* included when the handler is constructed with {@code false}.
     */
    @Test
    @DisplayName("Should not include standard vocabularies if disabled")
    void testStandardVocabulariesNotIncluded() {
        PrefixHandler handlerNoStd = new PrefixHandler(false);

        assertNull(handlerNoStd.getNamespace("rdf"), "RDF should not be included.");
        assertNull(handlerNoStd.getNamespace("rdfs"), "RDFS should not be included.");
        assertTrue(handlerNoStd.isEmpty(), "Handler should be empty.");
    }

    /**
     * Tests that a standard vocabulary prefix (like rdf) expands correctly after inclusion.
     */
    @Test
    @DisplayName("Should allow expansion of included standard vocabularies")
    void testStandardVocabularyExpansion() {
        PrefixHandler handlerWithStd = new PrefixHandler(true);

        String expanded = handlerWithStd.expandPrefix("rdf:type");
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", expanded);
    }


    /**
     * Tests that all prefix mappings from a source handler are correctly copied to the current handler.
     */
    @Test
    @DisplayName("Should copy all mappings from another handler")
    void testCopyFrom() {
        PrefixHandler source = new PrefixHandler(false);
        source.setPrefix("ex", "http://example.org/");
        source.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");

        handler.copyFrom(source);

        assertEquals("http://example.org/", handler.getNamespace("ex"));
        assertEquals("http://xmlns.com/foaf/0.1/", handler.getNamespace("foaf"));
        assertEquals(2, handler.size());
    }

    /**
     * Tests for {@link IllegalArgumentException} when copying from a null handler.
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException if copy source is null")
    void testCopyFromNull() {
        assertThrows(IllegalArgumentException.class, () -> handler.copyFrom(null));
    }

    /**
     * Tests that existing mappings in the destination handler are preserved when copying new mappings.
     */
    @Test
    @DisplayName("Should preserve existing mappings during copy")
    void testCopyFromPreservesExisting() {
        handler.setPrefix("existing", "http://existing.org/");

        PrefixHandler source = new PrefixHandler(false);
        source.setPrefix("ex", "http://example.org/");

        handler.copyFrom(source);

        assertEquals("http://existing.org/", handler.getNamespace("existing"));
        assertEquals("http://example.org/", handler.getNamespace("ex"));
        assertEquals(2, handler.size());
    }


    /**
     * Tests the {@code toString} representation when the handler is empty.
     */
    @Test
    @DisplayName("Should display correct toString() output for an empty handler")
    void testToStringEmpty() {
        String str = handler.toString();
        assertEquals("PrefixHandler{}", str);
    }
}
