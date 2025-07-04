package fr.inria.corese.core.next.impl.common.serialization.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link NQuadsConfig} class.
 * These tests verify the default configuration settings and the functionality
 * of the builder pattern for customizing N-Quads serialization options.
 */
class NQuadsConfigTest {

    @Test
    @DisplayName("defaultConfig() should return a config with expected N-Quads defaults")
    void defaultConfig_shouldReturnExpectedDefaults() {
        NQuadsConfig config = NQuadsConfig.defaultConfig();

        assertNotNull(config, "Default config should not be null");

        assertTrue(config.strictMode, "Default strictMode should be true for N-Quads");
        assertTrue(config.escapeUnicode(), "Default escapeUnicode should be true for N-Quads");
        assertEquals(LiteralDatatypePolicyEnum.ALWAYS_TYPED, config.getLiteralDatatypePolicy(), "Default literalDatatypePolicy should be ALWAYS_TYPED");
        assertNull(config.getBaseIRI(), "Default baseIRI should be null");
        assertFalse(config.stableBlankNodeIds(), "Default stableBlankNodeIds should be false");

        assertTrue(config.includeContext(), "Default includeContext should be true for N-Quads");
    }

    @Test
    @DisplayName("Builder should allow overriding includeContext")
    void builder_shouldAllowOverridingIncludeContext() {
        NQuadsConfig config = NQuadsConfig.builder()
                .includeContext(false)
                .build();

        assertFalse(config.includeContext(), "includeContext should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding literalDatatypePolicy")
    void builder_shouldAllowOverridingLiteralDatatypePolicy() {
        NQuadsConfig config = NQuadsConfig.builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.MINIMAL)
                .build();

        assertEquals(LiteralDatatypePolicyEnum.MINIMAL, config.getLiteralDatatypePolicy(), "literalDatatypePolicy should be overridden to MINIMAL");
    }

    @Test
    @DisplayName("Builder should allow overriding escapeUnicode")
    void builder_shouldAllowOverridingEscapeUnicode() {
        NQuadsConfig config = NQuadsConfig.builder()
                .escapeUnicode(false)
                .build();

        assertFalse(config.escapeUnicode(), "escapeUnicode should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding strictMode")
    void builder_shouldAllowOverridingStrictMode() {
        NQuadsConfig config = NQuadsConfig.builder()
                .strictMode(false)
                .build();

        assertFalse(config.strictMode, "strictMode should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow setting baseIRI")
    void builder_shouldAllowSettingBaseIRI() {
        String testBaseIRI = "http://example.org/base/";
        NQuadsConfig config = NQuadsConfig.builder()
                .baseIRI(testBaseIRI)
                .build();

        assertEquals(testBaseIRI, config.getBaseIRI(), "baseIRI should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow setting lineEnding")
    void builder_shouldAllowSettingLineEnding() {
        String customLineEnding = "\r\n";
        NQuadsConfig config = NQuadsConfig.builder()
                .lineEnding(customLineEnding)
                .build();

        assertEquals(customLineEnding, config.getLineEnding(), "lineEnding should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow overriding validateURIs")
    void builder_shouldAllowOverridingValidateURIs() {
        NQuadsConfig config = NQuadsConfig.builder()
                .validateURIs(true)
                .build();

        assertTrue(config.validateURIs(), "validateURIs should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding stableBlankNodeIds")
    void builder_shouldAllowOverridingStableBlankNodeIds() {
        NQuadsConfig config = NQuadsConfig.builder()
                .stableBlankNodeIds(true)
                .build();

        assertTrue(config.stableBlankNodeIds(), "stableBlankNodeIds should be overridden to true");
    }

    @Test
    @DisplayName("Builder should handle null values for optional fields gracefully (e.g., baseIRI)")
    void builder_shouldHandleNullForOptionalFields() {
        NQuadsConfig config = NQuadsConfig.builder()
                .baseIRI(null)
                .build();

        assertNull(config.getBaseIRI(), "baseIRI should be null when explicitly set to null");
    }

}
