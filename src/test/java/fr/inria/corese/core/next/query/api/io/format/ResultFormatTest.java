package fr.inria.corese.core.next.query.api.io.format;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResultFormat Tests")
class ResultFormatTest {

    @Test
    void findsFormatsByNameExtensionAndMimeType() {
        assertEquals(ResultFormat.JSON, ResultFormat.byName("json").orElseThrow());
        assertEquals(ResultFormat.JSON, ResultFormat.byExtension(".SRJ").orElseThrow());
        assertEquals(ResultFormat.JSON,
                ResultFormat.byMimeType("application/sparql-results+json; charset=UTF-8").orElseThrow());
        assertTrue(ResultFormat.byName(null).isEmpty());
    }

    @Test
    @DisplayName("Should contain standard result formats")
    void testStandardFormats() {
        assertNotNull(ResultFormat.CSV);
        assertEquals("CSV", ResultFormat.CSV.getName());
        assertTrue(ResultFormat.CSV.getExtensions().contains("csv"));
        assertTrue(ResultFormat.CSV.getMimeTypes().contains("text/csv"));

        assertNotNull(ResultFormat.TSV);
        assertEquals("TSV", ResultFormat.TSV.getName());
        assertTrue(ResultFormat.TSV.getExtensions().contains("tsv"));
        assertTrue(ResultFormat.TSV.getMimeTypes().contains("text/tab-separated-values"));

        assertNotNull(ResultFormat.JSON);
        assertEquals("JSON", ResultFormat.JSON.getName());
        assertTrue(ResultFormat.JSON.getExtensions().contains("json"));
        assertTrue(ResultFormat.JSON.getMimeTypes().contains("application/sparql-results+json"));

        assertNotNull(ResultFormat.XML);
        assertEquals("XML", ResultFormat.XML.getName());
        assertTrue(ResultFormat.XML.getExtensions().contains("xml"));
        assertTrue(ResultFormat.XML.getMimeTypes().contains("application/sparql-results+xml"));
    }

    @Test
    @DisplayName("all() should return unmodifiable list of all formats")
    void testAll() {
        List<ResultFormat> formats = ResultFormat.all();

        assertEquals(4, formats.size());
        assertTrue(formats.contains(ResultFormat.CSV));
        assertTrue(formats.contains(ResultFormat.TSV));
        assertTrue(formats.contains(ResultFormat.JSON));
        assertTrue(formats.contains(ResultFormat.XML));
        assertThrows(UnsupportedOperationException.class, () -> formats.add(ResultFormat.CSV));
    }
}
