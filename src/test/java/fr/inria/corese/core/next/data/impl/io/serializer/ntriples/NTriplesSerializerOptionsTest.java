package fr.inria.corese.core.next.data.impl.io.serializer.ntriples;

import fr.inria.corese.core.next.data.api.io.serializer.option.LiteralDatatypePolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link NTriplesSerializerOptions} class.
 * These tests verify the default configuration settings and the functionality
 * of the builder pattern for customizing N-Triples serialization options.
 */
class NTriplesSerializerOptionsTest {

    @Test
    @DisplayName("defaultConfig() should return a config with expected N-Triples defaults")
    void defaultConfig_shouldReturnExpectedDefaults() {
        NTriplesSerializerOptions config = NTriplesSerializerOptions.defaultConfig();

        assertNotNull(config, "Default config should not be null");

        assertTrue(config.isStrictMode(), "Default strictMode should be true for N-Triples");
        assertTrue(config.escapeUnicode(), "Default escapeUnicode should be true for N-Triples");
        assertEquals(LiteralDatatypePolicy.ALWAYS_TYPED, config.getLiteralDatatypePolicy(), "Default literalDatatypePolicy should be ALWAYS_TYPED");
        assertNull(config.getBaseIRI(), "Default baseIRI should be null");
        assertFalse(config.stableBlankNodeIds(), "Default stableBlankNodeIds should be false");

        assertFalse(config.includeContext(), "Default includeContext should be false for N-Triples");
    }

    @Test
    @DisplayName("Builder should allow overriding includeContext")
    void builder_shouldAllowOverridingIncludeContext() {
        NTriplesSerializerOptions config = NTriplesSerializerOptions.builder()
                .includeContext(true)
                .build();

        assertTrue(config.includeContext(), "includeContext should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding literalDatatypePolicy")
    void builder_shouldAllowOverridingLiteralDatatypePolicy() {
        NTriplesSerializerOptions config = NTriplesSerializerOptions.builder()
                .literalDatatypePolicy(LiteralDatatypePolicy.MINIMAL)
                .build();

        assertEquals(LiteralDatatypePolicy.MINIMAL, config.getLiteralDatatypePolicy(), "literalDatatypePolicy should be overridden to MINIMAL");
    }

    @Test
    @DisplayName("Builder should allow overriding escapeUnicode")
    void builder_shouldAllowOverridingEscapeUnicode() {
        NTriplesSerializerOptions config = NTriplesSerializerOptions.builder()
                .escapeUnicode(false)
                .build();

        assertFalse(config.escapeUnicode(), "escapeUnicode should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding strictMode")
    void builder_shouldAllowOverridingStrictMode() {
        NTriplesSerializerOptions config = NTriplesSerializerOptions.builder()
                .strictMode(false)
                .build();

        assertFalse(config.isStrictMode(), "strictMode should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow setting baseIRI")
    void builder_shouldAllowSettingBaseIRI() {
        String testBaseIRI = "http://example.org/base/";
        NTriplesSerializerOptions config = NTriplesSerializerOptions.builder()
                .baseIRI(testBaseIRI)
                .build();

        assertEquals(testBaseIRI, config.getBaseIRI(), "baseIRI should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow setting lineEnding")
    void builder_shouldAllowSettingLineEnding() {
        String customLineEnding = "\r\n";
        NTriplesSerializerOptions config = NTriplesSerializerOptions.builder()
                .lineEnding(customLineEnding)
                .build();

        assertEquals(customLineEnding, config.getLineEnding(), "lineEnding should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow overriding validateURIs")
    void builder_shouldAllowOverridingValidateURIs() {
        NTriplesSerializerOptions config = NTriplesSerializerOptions.builder()
                .validateURIs(true)
                .build();

        assertTrue(config.validateURIs(), "validateURIs should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding stableBlankNodeIds")
    void builder_shouldAllowOverridingStableBlankNodeIds() {
        NTriplesSerializerOptions config = NTriplesSerializerOptions.builder()
                .stableBlankNodeIds(true)
                .build();

        assertTrue(config.stableBlankNodeIds(), "stableBlankNodeIds should be overridden to true");
    }

    @Test
    @DisplayName("Builder should handle null values for optional fields gracefully (e.g., baseIRI)")
    void builder_shouldHandleNullForOptionalFields() {
        NTriplesSerializerOptions config = NTriplesSerializerOptions.builder()
                .baseIRI(null)
                .build();

        assertNull(config.getBaseIRI(), "baseIRI should be null when explicitly set to null");
    }


}
