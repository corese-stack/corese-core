package fr.inria.corese.core.next.impl.common.serialization.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link NTriplesConfig} class.
 * These tests verify the default configuration settings and the functionality
 * of the builder pattern for customizing N-Triples serialization options.
 */
class NTriplesConfigTest {

    @Test
    @DisplayName("defaultConfig() should return a config with expected N-Triples defaults")
    void defaultConfig_shouldReturnExpectedDefaults() {
        NTriplesConfig config = NTriplesConfig.defaultConfig();

        assertNotNull(config, "Default config should not be null");

        assertTrue(config.strictMode, "Default strictMode should be true for N-Triples");
        assertTrue(config.escapeUnicode(), "Default escapeUnicode should be true for N-Triples");
        assertEquals(LiteralDatatypePolicyEnum.ALWAYS_TYPED, config.getLiteralDatatypePolicy(), "Default literalDatatypePolicy should be ALWAYS_TYPED");
        assertNull(config.getBaseIRI(), "Default baseIRI should be null");
        assertFalse(config.stableBlankNodeIds(), "Default stableBlankNodeIds should be false");

        assertFalse(config.includeContext(), "Default includeContext should be false for N-Triples");
    }

    @Test
    @DisplayName("Builder should allow overriding includeContext")
    void builder_shouldAllowOverridingIncludeContext() {
        NTriplesConfig config = NTriplesConfig.builder()
                .includeContext(true)
                .build();

        assertTrue(config.includeContext(), "includeContext should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding literalDatatypePolicy")
    void builder_shouldAllowOverridingLiteralDatatypePolicy() {
        NTriplesConfig config = NTriplesConfig.builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.MINIMAL)
                .build();

        assertEquals(LiteralDatatypePolicyEnum.MINIMAL, config.getLiteralDatatypePolicy(), "literalDatatypePolicy should be overridden to MINIMAL");
    }

    @Test
    @DisplayName("Builder should allow overriding escapeUnicode")
    void builder_shouldAllowOverridingEscapeUnicode() {
        NTriplesConfig config = NTriplesConfig.builder()
                .escapeUnicode(false)
                .build();

        assertFalse(config.escapeUnicode(), "escapeUnicode should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding strictMode")
    void builder_shouldAllowOverridingStrictMode() {
        NTriplesConfig config = NTriplesConfig.builder()
                .strictMode(false)
                .build();

        assertFalse(config.strictMode, "strictMode should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow setting baseIRI")
    void builder_shouldAllowSettingBaseIRI() {
        String testBaseIRI = "http://example.org/base/";
        NTriplesConfig config = NTriplesConfig.builder()
                .baseIRI(testBaseIRI)
                .build();

        assertEquals(testBaseIRI, config.getBaseIRI(), "baseIRI should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow setting lineEnding")
    void builder_shouldAllowSettingLineEnding() {
        String customLineEnding = "\r\n";
        NTriplesConfig config = NTriplesConfig.builder()
                .lineEnding(customLineEnding)
                .build();

        assertEquals(customLineEnding, config.getLineEnding(), "lineEnding should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow overriding validateURIs")
    void builder_shouldAllowOverridingValidateURIs() {
        NTriplesConfig config = NTriplesConfig.builder()
                .validateURIs(true)
                .build();

        assertTrue(config.validateURIs(), "validateURIs should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding stableBlankNodeIds")
    void builder_shouldAllowOverridingStableBlankNodeIds() {
        NTriplesConfig config = NTriplesConfig.builder()
                .stableBlankNodeIds(true)
                .build();

        assertTrue(config.stableBlankNodeIds(), "stableBlankNodeIds should be overridden to true");
    }

    @Test
    @DisplayName("Builder should handle null values for optional fields gracefully (e.g., baseIRI)")
    void builder_shouldHandleNullForOptionalFields() {
        NTriplesConfig config = NTriplesConfig.builder()
                .baseIRI(null)
                .build();

        assertNull(config.getBaseIRI(), "baseIRI should be null when explicitly set to null");
    }


}
