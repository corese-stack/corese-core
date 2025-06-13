package fr.inria.corese.core.next.impl.common.serialization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RdfFormatTest {


    @Test
    @DisplayName("Constructor should correctly initialize fields and getters should return correct values")
    void constructorAndGetters() {
        String name = "TestFormat";
        List<String> extensions = List.of("tf", "testf");
        List<String> mimeTypes = List.of("application/x-test", "text/test");
        boolean supportsNamespaces = true;
        boolean supportsNamedGraphs = false;

        RdfFormat format = new RdfFormat(name, extensions, mimeTypes, supportsNamespaces, supportsNamedGraphs);

        assertEquals(name, format.getName(), "Name should match constructor argument");
        assertEquals(extensions, format.getExtensions(), "Extensions list should match constructor argument");
        assertEquals(mimeTypes, format.getMimeTypes(), "MIME types list should match constructor argument");
        assertEquals(supportsNamespaces, format.supportsNamespaces(), "supportsNamespaces should match constructor argument");
        assertEquals(supportsNamedGraphs, format.supportsNamedGraphs(), "supportsNamedGraphs should match constructor argument");

        assertEquals("tf", format.getDefaultExtension(), "Default extension should be the first in list");
        assertEquals("application/x-test", format.getDefaultMimeType(), "Default MIME type should be the first in list");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null name")
    void constructorThrowsForNullName() {
        List<String> extensions = List.of("tf");
        List<String> mimeTypes = List.of("text/plain");
        assertThrows(NullPointerException.class,
                () -> new RdfFormat(null, extensions, mimeTypes, true, false),
                "Constructor should throw NPE for null name");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null extensions list")
    void constructorThrowsForNullExtensions() {
        List<String> mimeTypes = List.of("text/plain");
        assertThrows(NullPointerException.class,
                () -> new RdfFormat("Test", null, mimeTypes, true, false),
                "Constructor should throw NPE for null extensions list");
    }


    @Test
    @DisplayName("Constructor should throw NullPointerException for null mimeTypes list")
    void constructorThrowsForNullMimeTypes() {
        List<String> extensions = List.of("tf");
        assertThrows(NullPointerException.class,
                () -> new RdfFormat("Test", extensions, null, true, false),
                "Constructor should throw NPE for null mimeTypes list");
    }


    @Test
    @DisplayName("toString() should return a meaningful string representation")
    void testToString() {
        RdfFormat format = RdfFormat.TURTLE;
        String expected = "Turtle [extensions: ttl, mimeTypes: text/turtle, prefixes: true, namedGraphs: false]";

        assertEquals(expected, format.toString(), "toString() should match expected format");
    }

    @Test
    @DisplayName("equals() should return true for identical objects")
    void equalsIdenticalObjects() {
        RdfFormat format1 = new RdfFormat("Test", List.of("t"), List.of("text/t"), true, false);

        assertEquals(format1, format1, "Object should be equal to itself");
    }

    @Test
    @DisplayName("equals() should return true for logically equal objects")
    void equalsLogicallyEqualObjects() {
        RdfFormat format1 = new RdfFormat("Test", List.of("t"), List.of("text/t"), true, false);
        RdfFormat format2 = new RdfFormat("test", List.of("t"), List.of("text/t"), true, false); // Name case-insensitive

        assertEquals(format1, format2, "Objects with same properties (case-insensitive name) should be equal");
    }

    @Test
    @DisplayName("equals() should return false for objects with different names")
    void equalsDifferentNames() {
        RdfFormat format1 = new RdfFormat("Test1", List.of("t"), List.of("text/t"), true, false);
        RdfFormat format2 = new RdfFormat("Test2", List.of("t"), List.of("text/t"), true, false);

        assertNotEquals(format1, format2, "Objects with different names should not be equal");
    }

    @Test
    @DisplayName("equals() should return false for objects with different extensions")
    void equalsDifferentExtensions() {
        RdfFormat format1 = new RdfFormat("Test", List.of("t1"), List.of("text/t"), true, false);
        RdfFormat format2 = new RdfFormat("Test", List.of("t2"), List.of("text/t"), true, false);

        assertNotEquals(format1, format2, "Objects with different extensions should not be equal");
    }

    @Test
    @DisplayName("equals() should return false for objects with different mimeTypes")
    void equalsDifferentMimeTypes() {
        RdfFormat format1 = new RdfFormat("Test", List.of("t"), List.of("text/t1"), true, false);
        RdfFormat format2 = new RdfFormat("Test", List.of("t"), List.of("text/t2"), true, false);

        assertNotEquals(format1, format2, "Objects with different mimeTypes should not be equal");
    }

    @Test
    @DisplayName("equals() should return false for objects with different supportsNamespaces")
    void equalsDifferentSupportsNamespaces() {
        RdfFormat format1 = new RdfFormat("Test", List.of("t"), List.of("text/t"), true, false);
        RdfFormat format2 = new RdfFormat("Test", List.of("t"), List.of("text/t"), false, false);

        assertNotEquals(format1, format2, "Objects with different supportsNamespaces should not be equal");
    }

    @Test
    @DisplayName("equals() should return false for objects with different supportsNamedGraphs")
    void equalsDifferentSupportsNamedGraphs() {
        RdfFormat format1 = new RdfFormat("Test", List.of("t"), List.of("text/t"), true, true);
        RdfFormat format2 = new RdfFormat("Test", List.of("t"), List.of("text/t"), true, false);

        assertNotEquals(format1, format2, "Objects with different supportsNamedGraphs should not be equal");
    }

    @Test
    @DisplayName("hashCode() should be consistent with equals()")
    void hashCodeConsistency() {
        RdfFormat format1 = new RdfFormat("Test", List.of("t"), List.of("text/t"), true, false);
        RdfFormat format2 = new RdfFormat("test", List.of("t"), List.of("text/t"), true, false);

        assertEquals(format1, format2, "Objects should be equal");
        assertEquals(format1.hashCode(), format2.hashCode(), "Hash codes must be equal for equal objects");
    }

    @Test
    @DisplayName("hashCode() should return different values for unequal objects (high probability)")
    void hashCodeDifference() {
        RdfFormat format1 = new RdfFormat("Test1", List.of("t"), List.of("text/t"), true, false);
        RdfFormat format2 = new RdfFormat("Test2", List.of("t"), List.of("text/t"), true, false);

        assertNotEquals(format1.hashCode(), format2.hashCode(), "Hash codes should ideally be different for unequal objects");
    }


    @Test
    @DisplayName("TURTLE constant should be correctly defined")
    void turtleConstant() {
        RdfFormat turtle = RdfFormat.TURTLE;

        assertNotNull(turtle, "TURTLE constant should not be null");
        assertEquals("Turtle", turtle.getName());
        assertTrue(turtle.getExtensions().contains("ttl"));
        assertTrue(turtle.getMimeTypes().contains("text/turtle"));
        assertTrue(turtle.supportsNamespaces());
        assertFalse(turtle.supportsNamedGraphs());
    }

    @Test
    @DisplayName("NTRIPLES constant should be correctly defined")
    void nTriplesConstant() {
        RdfFormat ntriples = RdfFormat.NTRIPLES;

        assertNotNull(ntriples, "NTRIPLES constant should not be null");
        assertEquals("N-Triples", ntriples.getName());
        assertTrue(ntriples.getExtensions().contains("nt"));
        assertTrue(ntriples.getMimeTypes().contains("application/n-triples"));
        assertFalse(ntriples.supportsNamespaces());
        assertFalse(ntriples.supportsNamedGraphs());
    }

    @Test
    @DisplayName("NQUADS constant should be correctly defined")
    void nQuadsConstant() {
        RdfFormat nquads = RdfFormat.NQUADS;

        assertNotNull(nquads, "NQUADS constant should not be null");
        assertEquals("N-Quads", nquads.getName());
        assertTrue(nquads.getExtensions().contains("nq"));
        assertTrue(nquads.getMimeTypes().contains("application/n-quads"));
        assertFalse(nquads.supportsNamespaces());
        assertTrue(nquads.supportsNamedGraphs());
    }

    @Test
    @DisplayName("JSONLD constant should be correctly defined")
    void jsonLdConstant() {
        RdfFormat jsonld = RdfFormat.JSONLD;

        assertNotNull(jsonld, "JSONLD constant should not be null");
        assertEquals("JSON-LD", jsonld.getName());
        assertTrue(jsonld.getExtensions().contains("jsonld"));
        assertTrue(jsonld.getMimeTypes().contains("application/ld+json"));
        assertTrue(jsonld.supportsNamespaces());
        assertTrue(jsonld.supportsNamedGraphs());
    }

    @Test
    @DisplayName("RDFXML constant should be correctly defined")
    void rdfXmlConstant() {
        RdfFormat rdfxml = RdfFormat.RDFXML;

        assertNotNull(rdfxml, "RDFXML constant should not be null");
        assertEquals("RDF/XML", rdfxml.getName());
        assertTrue(rdfxml.getExtensions().contains("rdf"));
        assertTrue(rdfxml.getExtensions().contains("xml"));
        assertTrue(rdfxml.getMimeTypes().contains("application/rdf+xml"));
        assertTrue(rdfxml.supportsNamespaces());
        assertFalse(rdfxml.supportsNamedGraphs());
    }


    @Test
    @DisplayName("byName() should find existing format by name (case-insensitive)")
    void byNameFound() {
        Optional<RdfFormat> format = RdfFormat.byName("TuRtLe");

        assertTrue(format.isPresent(), "Turtle format should be found by name");
        assertEquals(RdfFormat.TURTLE, format.get(), "Found format should be the TURTLE constant");
    }

    @Test
    @DisplayName("byName() should return empty Optional for non-existent name")
    void byNameNotFound() {
        Optional<RdfFormat> format = RdfFormat.byName("NonExistentFormat");

        assertFalse(format.isPresent(), "Non-existent format should not be found");
    }

    @Test
    @DisplayName("byExtension() should find existing format by extension (case-insensitive)")
    void byExtensionFound() {
        Optional<RdfFormat> format = RdfFormat.byExtension("TTL");

        assertTrue(format.isPresent(), "Turtle format should be found by extension");
        assertEquals(RdfFormat.TURTLE, format.get(), "Found format should be the TURTLE constant");

        Optional<RdfFormat> rdfXmlFormat = RdfFormat.byExtension("XML");
        assertTrue(rdfXmlFormat.isPresent());
        assertEquals(RdfFormat.RDFXML, rdfXmlFormat.get());
    }

    @Test
    @DisplayName("byExtension() should return empty Optional for non-existent extension")
    void byExtensionNotFound() {
        Optional<RdfFormat> format = RdfFormat.byExtension("xyz");

        assertFalse(format.isPresent(), "Non-existent extension should not find a format");
    }

    @Test
    @DisplayName("byMimeType() should find existing format by MIME type (case-insensitive)")
    void byMimeTypeFound() {
        Optional<RdfFormat> format = RdfFormat.byMimeType("text/TuRtLe");

        assertTrue(format.isPresent(), "Turtle format should be found by MIME type");
        assertEquals(RdfFormat.TURTLE, format.get(), "Found format should be the TURTLE constant");

        Optional<RdfFormat> nTriplesFormat = RdfFormat.byMimeType("text/plain");

        assertTrue(nTriplesFormat.isPresent());
        assertEquals(RdfFormat.NTRIPLES, nTriplesFormat.get());
    }

    @Test
    @DisplayName("byMimeType() should return empty Optional for non-existent MIME type")
    void byMimeTypeNotFound() {
        Optional<RdfFormat> format = RdfFormat.byMimeType("application/x-unknown");

        assertFalse(format.isPresent(), "Non-existent MIME type should not find a format");
    }

    @Test
    @DisplayName("all() should return a list containing all predefined formats")
    void allFormats() {
        List<RdfFormat> allFormats = RdfFormat.all();

        assertNotNull(allFormats, "List of all formats should not be null");
        assertEquals(5, allFormats.size(), "List should contain 5 predefined formats"); // TURTLE, NTRIPLES, NQUADS, JSONLD, RDFXML

        assertTrue(allFormats.contains(RdfFormat.TURTLE));
        assertTrue(allFormats.contains(RdfFormat.NTRIPLES));
        assertTrue(allFormats.contains(RdfFormat.NQUADS));
        assertTrue(allFormats.contains(RdfFormat.JSONLD));
        assertTrue(allFormats.contains(RdfFormat.RDFXML));

        assertThrows(UnsupportedOperationException.class, () -> allFormats.add(RdfFormat.TURTLE),
                "The list returned by all() should be unmodifiable");
    }
}