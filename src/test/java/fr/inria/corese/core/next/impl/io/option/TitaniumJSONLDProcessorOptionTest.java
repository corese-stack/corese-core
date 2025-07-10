package fr.inria.corese.core.next.impl.io.option;

import com.apicatalog.jsonld.JsonLdVersion;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to check that values are properly set
 */
class TitaniumJSONLDProcessorOptionTest {

    private TitaniumJSONLDProcessorOption optionAllTrue = new TitaniumJSONLDProcessorOption.Builder().base("http://example.org/AllTrue")
            .extractAllScripts(true)
            .compactToRelative(true)
            .compactArrays(true)
            .ordered(true)
            .useRdfType(true)
            .useNativeTypes(true)
            .build();

    private TitaniumJSONLDProcessorOption optionAllFalse = new TitaniumJSONLDProcessorOption.Builder().base("http://example.org/AllFalse")
            .extractAllScripts(false)
            .compactArrays(false)
            .compactToRelative(false)
            .ordered(false)
            .useRdfType(false)
            .useNativeTypes(false)
            .build();

    @Test
    void isCompactArrays() {
        assertTrue(optionAllTrue.compactsArrays());
        assertFalse(optionAllFalse.compactsArrays());
    }

    @Test
    void isCompactToRelative() {
        assertTrue(optionAllTrue.compactsToRelative());
        assertFalse(optionAllFalse.compactsToRelative());
    }

    @Test
    void isExtractAllScripts() {
        assertTrue(optionAllTrue.isExtractAllScripts());
        assertFalse(optionAllFalse.isExtractAllScripts());
    }

    @Test
    void isOrdered() {
        assertTrue(optionAllTrue.isOrdered());
        assertFalse(optionAllFalse.isOrdered());
    }

    @Test
    void getProcessingMode() {
        TitaniumJSONLDProcessorOption option10 = new TitaniumJSONLDProcessorOption.Builder().processingMode(JsonLdVersion.V1_0).build();
        TitaniumJSONLDProcessorOption option11 = new TitaniumJSONLDProcessorOption.Builder().processingMode(JsonLdVersion.V1_1).build();
        assertEquals(JsonLdVersion.V1_0, option10.getProcessingMode());
        assertEquals(JsonLdVersion.V1_1, option11.getProcessingMode());
    }

    @Test
    void getTimeout() {
        TitaniumJSONLDProcessorOption option10seconds = new TitaniumJSONLDProcessorOption.Builder().timeout(Duration.of(10, ChronoUnit.SECONDS)).build();
        assertNull(optionAllTrue.getTimeout());
        assertEquals(Duration.of(10, ChronoUnit.SECONDS), option10seconds.getTimeout());
    }

    @Test
    void isUseNativeTypes() {
        assertTrue(optionAllTrue.usesNativeTypes());
        assertFalse(optionAllFalse.usesNativeTypes());
    }

    @Test
    void isUseRdfType() {
        assertTrue(optionAllTrue.usesRdfType());
        assertFalse(optionAllFalse.usesRdfType());
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