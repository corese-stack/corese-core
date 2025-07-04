package fr.inria.corese.core.next.impl.io;

import com.apicatalog.jsonld.JsonLdVersion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class TitaniumJSONLDProcessorOptionsTest {

    private TitaniumJSONLDProcessorOptions optionAllTrue = new TitaniumJSONLDProcessorOptions(new TitaniumJSONLDProcessorOptions.Builder().base("http://example.org/AllTrue")
            .extractAllScripts(true)
            .compactToRelative(true)
            .compactArrays(true)
            .omitDefault(true)
            .omitGraph(true)
            .ordered(true)
            .useRdfType(true)
            .useNativeTypes(true)
    );

    private TitaniumJSONLDProcessorOptions optionAllFalse = new TitaniumJSONLDProcessorOptions(new TitaniumJSONLDProcessorOptions.Builder().base("http://example.org/AllFalse")
            .extractAllScripts(false)
            .compactArrays(false)
            .compactToRelative(false)
            .omitDefault(false)
            .omitGraph(false)
            .ordered(false)
            .useRdfType(false)
            .useNativeTypes(false)
    );

    @Test
    void isCompactArrays() {
        assertTrue(optionAllTrue.isCompactArrays());
        assertFalse(optionAllFalse.isCompactArrays());
    }

    @Test
    void isCompactToRelative() {
        assertTrue(optionAllTrue.isCompactToRelative());
        assertFalse(optionAllFalse.isCompactToRelative());
    }

    @Test
    void isExtractAllScripts() {
        assertTrue(optionAllTrue.isExtractAllScripts());
        assertFalse(optionAllFalse.isExtractAllScripts());
    }

    @Test
    void isOmitDefault() {
        assertTrue(optionAllTrue.isOmitDefault());
        assertFalse(optionAllFalse.isOmitDefault());
    }

    @Test
    void isOmitGraph() {
        assertTrue(optionAllTrue.isOmitGraph());
        assertFalse(optionAllFalse.isOmitGraph());
    }

    @Test
    void isOrdered() {
        assertTrue(optionAllTrue.isOrdered());
        assertFalse(optionAllFalse.isOrdered());
    }

    @Test
    void getProcessingMode() {
        TitaniumJSONLDProcessorOptions option10 = new TitaniumJSONLDProcessorOptions(new TitaniumJSONLDProcessorOptions.Builder().processingMode(JsonLdVersion.V1_0));
        TitaniumJSONLDProcessorOptions option11 = new TitaniumJSONLDProcessorOptions(new TitaniumJSONLDProcessorOptions.Builder().processingMode(JsonLdVersion.V1_1));
        assertEquals(JsonLdVersion.V1_0, option10.getProcessingMode());
        assertEquals(JsonLdVersion.V1_1, option11.getProcessingMode());
    }

    @Test
    void getTimeout() {
        TitaniumJSONLDProcessorOptions option10seconds = new TitaniumJSONLDProcessorOptions(new TitaniumJSONLDProcessorOptions.Builder().timeout(Duration.of(10, ChronoUnit.SECONDS)));
        assertNull(optionAllTrue.getTimeout());
        assertEquals(Duration.of(10, ChronoUnit.SECONDS), option10seconds.getTimeout());
    }

    @Test
    void isUseNativeTypes() {
        assertTrue(optionAllTrue.isUseNativeTypes());
        assertFalse(optionAllFalse.isUseNativeTypes());
    }

    @Test
    void isUseRdfType() {
        assertTrue(optionAllTrue.isUseRdfType());
        assertFalse(optionAllFalse.isUseRdfType());
    }

    @Test
    void getJsonLdOptions() {
        assertNotNull(optionAllTrue.getJsonLdOptions());
        assertNotNull(optionAllFalse.getJsonLdOptions());
    }

    @Test
    void getBase() {
        assertEquals("http://example.org/AllTrue", optionAllTrue.getBase());
        assertEquals("http://example.org/AllFalse", optionAllFalse.getBase());
    }
}