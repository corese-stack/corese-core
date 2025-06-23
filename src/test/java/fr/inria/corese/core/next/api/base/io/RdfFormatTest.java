package fr.inria.corese.core.next.api.base.io;

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

        fr.inria.corese.core.next.api.base.io.RdfFormat format = new fr.inria.corese.core.next.api.base.io.RdfFormat(name, extensions, mimeTypes, supportsNamespaces, supportsNamedGraphs);

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
                () -> new fr.inria.corese.core.next.api.base.io.RdfFormat(null, extensions, mimeTypes, true, false),
                "Constructor should throw NPE for null name");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null extensions list")
    void constructorThrowsForNullExtensions() {
        List<String> mimeTypes = List.of("text/plain");
        assertThrows(NullPointerException.class,
                () -> new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", null, mimeTypes, true, false),
                "Constructor should throw NPE for null extensions list");
    }


    @Test
    @DisplayName("Constructor should throw NullPointerException for null mimeTypes list")
    void constructorThrowsForNullMimeTypes() {
        List<String> extensions = List.of("tf");
        assertThrows(NullPointerException.class,
                () -> new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", extensions, null, true, false),
                "Constructor should throw NPE for null mimeTypes list");
    }


    @Test
    @DisplayName("toString() should return a meaningful string representation")
    void testToString() {
        fr.inria.corese.core.next.api.base.io.RdfFormat format = fr.inria.corese.core.next.api.base.io.RdfFormat.TURTLE;
        String expected = "Turtle [extensions: ttl, mimeTypes: text/turtle, prefixes: true, namedGraphs: false]";

        assertEquals(expected, format.toString(), "toString() should match expected format");
    }

    @Test
    @DisplayName("equals() should return true for identical objects")
    void equalsIdenticalObjects() {
        fr.inria.corese.core.next.api.base.io.RdfFormat format1 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", List.of("t"), List.of("text/t"), true, false);

        assertEquals(format1, format1, "Object should be equal to itself");
    }

    @Test
    @DisplayName("equals() should return true for logically equal objects")
    void equalsLogicallyEqualObjects() {
        fr.inria.corese.core.next.api.base.io.RdfFormat format1 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", List.of("t"), List.of("text/t"), true, false);
        fr.inria.corese.core.next.api.base.io.RdfFormat format2 = new fr.inria.corese.core.next.api.base.io.RdfFormat("test", List.of("t"), List.of("text/t"), true, false); // Name case-insensitive

        assertEquals(format1, format2, "Objects with same properties (case-insensitive name) should be equal");
    }

    @Test
    @DisplayName("equals() should return false for objects with different names")
    void equalsDifferentNames() {
        fr.inria.corese.core.next.api.base.io.RdfFormat format1 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test1", List.of("t"), List.of("text/t"), true, false);
        fr.inria.corese.core.next.api.base.io.RdfFormat format2 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test2", List.of("t"), List.of("text/t"), true, false);

        assertNotEquals(format1, format2, "Objects with different names should not be equal");
    }

    @Test
    @DisplayName("equals() should return false for objects with different extensions")
    void equalsDifferentExtensions() {
        fr.inria.corese.core.next.api.base.io.RdfFormat format1 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", List.of("t1"), List.of("text/t"), true, false);
        fr.inria.corese.core.next.api.base.io.RdfFormat format2 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", List.of("t2"), List.of("text/t"), true, false);

        assertNotEquals(format1, format2, "Objects with different extensions should not be equal");
    }

    @Test
    @DisplayName("equals() should return false for objects with different mimeTypes")
    void equalsDifferentMimeTypes() {
        fr.inria.corese.core.next.api.base.io.RdfFormat format1 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", List.of("t"), List.of("text/t1"), true, false);
        fr.inria.corese.core.next.api.base.io.RdfFormat format2 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", List.of("t"), List.of("text/t2"), true, false);

        assertNotEquals(format1, format2, "Objects with different mimeTypes should not be equal");
    }

    @Test
    @DisplayName("equals() should return false for objects with different supportsNamespaces")
    void equalsDifferentSupportsNamespaces() {
        fr.inria.corese.core.next.api.base.io.RdfFormat format1 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", List.of("t"), List.of("text/t"), true, false);
        fr.inria.corese.core.next.api.base.io.RdfFormat format2 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", List.of("t"), List.of("text/t"), false, false);

        assertNotEquals(format1, format2, "Objects with different supportsNamespaces should not be equal");
    }

    @Test
    @DisplayName("equals() should return false for objects with different supportsNamedGraphs")
    void equalsDifferentSupportsNamedGraphs() {
        fr.inria.corese.core.next.api.base.io.RdfFormat format1 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", List.of("t"), List.of("text/t"), true, true);
        fr.inria.corese.core.next.api.base.io.RdfFormat format2 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", List.of("t"), List.of("text/t"), true, false);

        assertNotEquals(format1, format2, "Objects with different supportsNamedGraphs should not be equal");
    }

    @Test
    @DisplayName("hashCode() should be consistent with equals()")
    void hashCodeConsistency() {
        fr.inria.corese.core.next.api.base.io.RdfFormat format1 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test", List.of("t"), List.of("text/t"), true, false);
        fr.inria.corese.core.next.api.base.io.RdfFormat format2 = new fr.inria.corese.core.next.api.base.io.RdfFormat("test", List.of("t"), List.of("text/t"), true, false);

        assertEquals(format1, format2, "Objects should be equal");
        assertEquals(format1.hashCode(), format2.hashCode(), "Hash codes must be equal for equal objects");
    }

    @Test
    @DisplayName("hashCode() should return different values for unequal objects (high probability)")
    void hashCodeDifference() {
        fr.inria.corese.core.next.api.base.io.RdfFormat format1 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test1", List.of("t"), List.of("text/t"), true, false);
        fr.inria.corese.core.next.api.base.io.RdfFormat format2 = new fr.inria.corese.core.next.api.base.io.RdfFormat("Test2", List.of("t"), List.of("text/t"), true, false);

        assertNotEquals(format1.hashCode(), format2.hashCode(), "Hash codes should ideally be different for unequal objects");
    }


    @Test
    @DisplayName("TURTLE constant should be correctly defined")
    void turtleConstant() {
        fr.inria.corese.core.next.api.base.io.RdfFormat turtle = fr.inria.corese.core.next.api.base.io.RdfFormat.TURTLE;

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
        fr.inria.corese.core.next.api.base.io.RdfFormat ntriples = fr.inria.corese.core.next.api.base.io.RdfFormat.NTRIPLES;

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
        fr.inria.corese.core.next.api.base.io.RdfFormat nquads = fr.inria.corese.core.next.api.base.io.RdfFormat.NQUADS;

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
        fr.inria.corese.core.next.api.base.io.RdfFormat jsonld = fr.inria.corese.core.next.api.base.io.RdfFormat.JSONLD;

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
        fr.inria.corese.core.next.api.base.io.RdfFormat rdfxml = fr.inria.corese.core.next.api.base.io.RdfFormat.RDFXML;

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
        Optional<fr.inria.corese.core.next.api.base.io.RdfFormat> format = fr.inria.corese.core.next.api.base.io.RdfFormat.byName("TuRtLe");

        assertTrue(format.isPresent(), "Turtle format should be found by name");
        assertEquals(fr.inria.corese.core.next.api.base.io.RdfFormat.TURTLE, format.get(), "Found format should be the TURTLE constant");
    }

    @Test
    @DisplayName("byName() should return empty Optional for non-existent name")
    void byNameNotFound() {
        Optional<fr.inria.corese.core.next.api.base.io.RdfFormat> format = fr.inria.corese.core.next.api.base.io.RdfFormat.byName("NonExistentFormat");

        assertFalse(format.isPresent(), "Non-existent format should not be found");
    }

    @Test
    @DisplayName("byExtension() should find existing format by extension (case-insensitive)")
    void byExtensionFound() {
        Optional<fr.inria.corese.core.next.api.base.io.RdfFormat> format = fr.inria.corese.core.next.api.base.io.RdfFormat.byExtension("TTL");

        assertTrue(format.isPresent(), "Turtle format should be found by extension");
        assertEquals(fr.inria.corese.core.next.api.base.io.RdfFormat.TURTLE, format.get(), "Found format should be the TURTLE constant");

        Optional<fr.inria.corese.core.next.api.base.io.RdfFormat> rdfXmlFormat = fr.inria.corese.core.next.api.base.io.RdfFormat.byExtension("XML");
        assertTrue(rdfXmlFormat.isPresent());
        assertEquals(fr.inria.corese.core.next.api.base.io.RdfFormat.RDFXML, rdfXmlFormat.get());
    }

    @Test
    @DisplayName("byExtension() should return empty Optional for non-existent extension")
    void byExtensionNotFound() {
        Optional<fr.inria.corese.core.next.api.base.io.RdfFormat> format = fr.inria.corese.core.next.api.base.io.RdfFormat.byExtension("xyz");

        assertFalse(format.isPresent(), "Non-existent extension should not find a format");
    }

    @Test
    @DisplayName("byMimeType() should find existing format by MIME type (case-insensitive)")
    void byMimeTypeFound() {
        Optional<fr.inria.corese.core.next.api.base.io.RdfFormat> format = fr.inria.corese.core.next.api.base.io.RdfFormat.byMimeType("text/TuRtLe");

        assertTrue(format.isPresent(), "Turtle format should be found by MIME type");
        assertEquals(fr.inria.corese.core.next.api.base.io.RdfFormat.TURTLE, format.get(), "Found format should be the TURTLE constant");

        Optional<fr.inria.corese.core.next.api.base.io.RdfFormat> nTriplesFormat = fr.inria.corese.core.next.api.base.io.RdfFormat.byMimeType("text/plain");

        assertTrue(nTriplesFormat.isPresent());
        assertEquals(fr.inria.corese.core.next.api.base.io.RdfFormat.NTRIPLES, nTriplesFormat.get());
    }

    @Test
    @DisplayName("byMimeType() should return empty Optional for non-existent MIME type")
    void byMimeTypeNotFound() {
        Optional<fr.inria.corese.core.next.api.base.io.RdfFormat> format = fr.inria.corese.core.next.api.base.io.RdfFormat.byMimeType("application/x-unknown");

        assertFalse(format.isPresent(), "Non-existent MIME type should not find a format");
    }

    @Test
    @DisplayName("all() should return a list containing all predefined formats")
    void allFormats() {
        List<fr.inria.corese.core.next.api.base.io.RdfFormat> allFormats = fr.inria.corese.core.next.api.base.io.RdfFormat.all();

        assertNotNull(allFormats, "List of all formats should not be null");
        assertEquals(5, allFormats.size(), "List should contain 5 predefined formats"); // TURTLE, NTRIPLES, NQUADS, JSONLD, RDFXML

        assertTrue(allFormats.contains(fr.inria.corese.core.next.api.base.io.RdfFormat.TURTLE));
        assertTrue(allFormats.contains(fr.inria.corese.core.next.api.base.io.RdfFormat.NTRIPLES));
        assertTrue(allFormats.contains(fr.inria.corese.core.next.api.base.io.RdfFormat.NQUADS));
        assertTrue(allFormats.contains(fr.inria.corese.core.next.api.base.io.RdfFormat.JSONLD));
        assertTrue(allFormats.contains(fr.inria.corese.core.next.api.base.io.RdfFormat.RDFXML));

        assertThrows(UnsupportedOperationException.class, () -> allFormats.add(RdfFormat.TURTLE),
                "The list returned by all() should be unmodifiable");
    }
}