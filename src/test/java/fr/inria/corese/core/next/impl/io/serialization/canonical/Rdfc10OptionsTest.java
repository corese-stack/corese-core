package fr.inria.corese.core.next.impl.io.serialization.canonical;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Rdfc10Options} class.
 * This class verifies the default configuration and the builder functionality
 * for the Canonical RDF serialization options.
 */
class Rdfc10OptionsTest {

    @Test
    @DisplayName("defaultConfig should return an instance with expected default values")
    void defaultConfig_shouldReturnExpectedValues() {
        Rdfc10Options config = Rdfc10Options.defaultConfig();

        assertNotNull(config, "Default config should not be null");
        assertTrue(config.isStrictMode(), "Default strictMode should be true for canonicalization");
        assertTrue(config.validateURIs(), "Default validateURIs should be true for canonicalization");
        assertFalse(config.escapeUnicode(), "Default escapeUnicode should be false for canonicalization");
        assertTrue(config.trailingDot(), "Default trailingDot should be true for canonicalization");
        assertFalse(config.includeContext(), "Default includeContext should be false for canonicalization (N-Triples like)");
    }

    @Test
    @DisplayName("builder should allow setting custom options")
    void builder_shouldAllowCustomOptions() {
        Rdfc10Options customConfig = Rdfc10Options.builder()
                .strictMode(false)
                .validateURIs(false)
                .escapeUnicode(false)
                .trailingDot(false)
                .includeContext(true)
                .build();

        assertNotNull(customConfig, "Custom config should not be null");
        assertFalse(customConfig.isStrictMode(), "Custom strictMode should be false");
        assertFalse(customConfig.validateURIs(), "Custom validateURIs should be false");
        assertFalse(customConfig.escapeUnicode(), "Custom escapeUnicode should be false");
        assertFalse(customConfig.trailingDot(), "Custom trailingDot should be false");
        assertTrue(customConfig.includeContext(), "Custom includeContext should be true");
    }

    @Test
    @DisplayName("builder should use default values for un-set options")
    void builder_shouldUseDefaultValues_forUnsetOptions() {
        Rdfc10Options config = Rdfc10Options.builder()
                .strictMode(false)
                .build();

        assertFalse(config.isStrictMode(), "strictMode should be overridden to false");
        assertTrue(config.validateURIs(), "validateURIs should remain default (true)");
        assertFalse(config.escapeUnicode(), "escapeUnicode should remain default (false)");
        assertTrue(config.trailingDot(), "trailingDot should remain default (true)");
        assertFalse(config.includeContext(), "includeContext should remain default (false)");
    }
}
