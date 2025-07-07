package fr.inria.corese.core.next.impl.common.serialization.config;

import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link TriGConfig} class.
 * These tests verify the default configuration settings and the functionality
 * of the builder pattern for customizing TriG serialization options.
 */
class TriGConfigTest {

    @Test
    @DisplayName("defaultConfig() should return a config with expected TriG defaults")
    void defaultConfig_shouldReturnExpectedDefaults() {
        TriGConfig config = TriGConfig.defaultConfig();

        assertNotNull(config, "Default config should not be null");

        assertTrue(config.includeContext(), "Default includeContext should be true for TriG");
        assertEquals(BlankNodeStyleEnum.NAMED, config.getBlankNodeStyle(), "Default blankNodeStyle should be NAMED for TriG");
        assertFalse(config.useCollections(), "Default useCollections should be false for TriG");

        Map<String, String> expectedPrefixes = new HashMap<>();
        expectedPrefixes.put("rdf", SerializationConstants.RDF_NS);
        expectedPrefixes.put("rdfs", SerializationConstants.RDFS_NS);
        expectedPrefixes.put("xsd", SerializationConstants.XSD_NS);
        expectedPrefixes.put("owl", SerializationConstants.OWL_NS);
        assertEquals(expectedPrefixes.size(), config.getCustomPrefixes().size(), "Default custom prefixes size mismatch");
        assertTrue(config.getCustomPrefixes().entrySet().containsAll(expectedPrefixes.entrySet()), "Default custom prefixes should contain common RDF prefixes");

        assertTrue(config.usePrefixes(), "Default usePrefixes should be true");
        assertTrue(config.autoDeclarePrefixes(), "Default autoDeclarePrefixes should be true");
        assertEquals(PrefixOrderingEnum.ALPHABETICAL, config.getPrefixOrdering(), "Default prefixOrdering should be ALPHABETICAL");
        assertTrue(config.useCompactTriples(), "Default useCompactTriples should be true");
        assertTrue(config.useRdfTypeShortcut(), "Default useRdfTypeShortcut should be true");
        assertTrue(config.useMultilineLiterals(), "Default useMultilineLiterals should be true");
        assertTrue(config.prettyPrint(), "Default prettyPrint should be true");
        assertEquals(SerializationConstants.DEFAULT_INDENTATION, config.getIndent(), "Default indent should be " + SerializationConstants.DEFAULT_INDENTATION);
        assertEquals(80, config.getMaxLineLength(), "Default maxLineLength should be 80");
        assertTrue(config.groupBySubject(), "Default groupBySubject should be true");
        assertFalse(config.sortSubjects(), "Default sortSubjects should be false");
        assertFalse(config.sortPredicates(), "Default sortPredicates should be false");

        assertTrue(config.strictMode, "Default strictMode should be true");
        assertFalse(config.escapeUnicode(), "Default escapeUnicode should be false");
        assertEquals(LiteralDatatypePolicyEnum.MINIMAL, config.getLiteralDatatypePolicy(), "Default literalDatatypePolicy should be MINIMAL");
        assertNull(config.getBaseIRI(), "Default baseIRI should be null");
    }

    @Test
    @DisplayName("Builder should allow overriding includeContext")
    void builder_shouldAllowOverridingIncludeContext() {
        TriGConfig config = TriGConfig.builder()
                .includeContext(false)
                .build();
        assertFalse(config.includeContext(), "includeContext should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding blankNodeStyle")
    void builder_shouldAllowOverridingBlankNodeStyle() {
        TriGConfig config = TriGConfig.builder()
                .blankNodeStyle(BlankNodeStyleEnum.ANONYMOUS)
                .build();
        assertEquals(BlankNodeStyleEnum.ANONYMOUS, config.getBlankNodeStyle(), "blankNodeStyle should be overridden to ANONYMOUS");
    }

    @Test
    @DisplayName("Builder should allow overriding useCollections")
    void builder_shouldAllowOverridingUseCollections() {
        TriGConfig config = TriGConfig.builder()
                .useCollections(true)
                .build();
        assertTrue(config.useCollections(), "useCollections should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow adding custom prefixes")
    void builder_shouldAllowAddingCustomPrefixes() {
        String customPrefix = "my";
        String customNamespace = "http://my.example.org/";
        TriGConfig config = TriGConfig.builder()
                .addCustomPrefix(customPrefix, customNamespace)
                .build();

        assertTrue(config.getCustomPrefixes().containsKey(customPrefix), "Custom prefix should be added");
        assertEquals(customNamespace, config.getCustomPrefixes().get(customPrefix), "Custom prefix namespace should be correct");
        assertTrue(config.getCustomPrefixes().containsKey("rdf"));
        assertTrue(config.getCustomPrefixes().containsKey("xsd"));
    }

    @Test
    @DisplayName("Builder should allow overriding usePrefixes")
    void builder_shouldAllowOverridingUsePrefixes() {
        TriGConfig config = TriGConfig.builder()
                .usePrefixes(false)
                .build();
        assertFalse(config.usePrefixes(), "usePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding autoDeclarePrefixes")
    void builder_shouldAllowOverridingAutoDeclarePrefixes() {
        TriGConfig config = TriGConfig.builder()
                .autoDeclarePrefixes(false)
                .build();
        assertFalse(config.autoDeclarePrefixes(), "autoDeclarePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding prefixOrdering")
    void builder_shouldAllowOverridingPrefixOrdering() {
        TriGConfig config = TriGConfig.builder()
                .prefixOrdering(PrefixOrderingEnum.USAGE_ORDER)
                .build();
        assertEquals(PrefixOrderingEnum.USAGE_ORDER, config.getPrefixOrdering(), "prefixOrdering should be overridden to USAGE_ORDER");
    }

    @Test
    @DisplayName("Builder should allow overriding useCompactTriples")
    void builder_shouldAllowOverridingUseCompactTriples() {
        TriGConfig config = TriGConfig.builder()
                .useCompactTriples(false)
                .build();
        assertFalse(config.useCompactTriples(), "useCompactTriples should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding useRdfTypeShortcut")
    void builder_shouldAllowOverridingUseRdfTypeShortcut() {
        TriGConfig config = TriGConfig.builder()
                .useRdfTypeShortcut(false)
                .build();
        assertFalse(config.useRdfTypeShortcut(), "useRdfTypeShortcut should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding useMultilineLiterals")
    void builder_shouldAllowOverridingUseMultilineLiterals() {
        TriGConfig config = TriGConfig.builder()
                .useMultilineLiterals(false)
                .build();
        assertFalse(config.useMultilineLiterals(), "useMultilineLiterals should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding prettyPrint")
    void builder_shouldAllowOverridingPrettyPrint() {
        TriGConfig config = TriGConfig.builder()
                .prettyPrint(false)
                .build();
        assertFalse(config.prettyPrint(), "prettyPrint should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding indent")
    void builder_shouldAllowOverridingIndent() {
        String customIndent = "\t";
        TriGConfig config = TriGConfig.builder()
                .indent(customIndent)
                .build();
        assertEquals(customIndent, config.getIndent(), "indent should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding maxLineLength")
    void builder_shouldAllowOverridingMaxLineLength() {
        int customLength = 120;
        TriGConfig config = TriGConfig.builder()
                .maxLineLength(customLength)
                .build();
        assertEquals(customLength, config.getMaxLineLength(), "maxLineLength should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding groupBySubject")
    void builder_shouldAllowOverridingGroupBySubject() {
        TriGConfig config = TriGConfig.builder()
                .groupBySubject(false)
                .build();
        assertFalse(config.groupBySubject(), "groupBySubject should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding sortSubjects")
    void builder_shouldAllowOverridingSortSubjects() {
        TriGConfig config = TriGConfig.builder()
                .sortSubjects(true)
                .build();
        assertTrue(config.sortSubjects(), "sortSubjects should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding sortPredicates")
    void builder_shouldAllowOverridingSortPredicates() {
        TriGConfig config = TriGConfig.builder()
                .sortPredicates(true)
                .build();
        assertTrue(config.sortPredicates(), "sortPredicates should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding strictMode")
    void builder_shouldAllowOverridingStrictMode() {
        TriGConfig config = TriGConfig.builder()
                .strictMode(false)
                .build();
        assertFalse(config.strictMode, "strictMode should be overridden to false");
    }


    @Test
    @DisplayName("Builder should allow overriding literalDatatypePolicy")
    void builder_shouldAllowOverridingLiteralDatatypePolicy() {
        TriGConfig config = TriGConfig.builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED)
                .build();
        assertEquals(LiteralDatatypePolicyEnum.ALWAYS_TYPED, config.getLiteralDatatypePolicy(), "literalDatatypePolicy should be overridden to ALWAYS_TYPED");
    }

    @Test
    @DisplayName("Builder should allow setting baseIRI")
    void builder_shouldAllowSettingBaseIRI() {
        String testBaseIRI = "http://example.org/base/";
        TriGConfig config = TriGConfig.builder()
                .baseIRI(testBaseIRI)
                .build();
        assertEquals(testBaseIRI, config.getBaseIRI(), "baseIRI should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow overriding lineEnding")
    void builder_shouldAllowOverridingLineEnding() {
        String customLineEnding = "\r\n";
        TriGConfig config = TriGConfig.builder()
                .lineEnding(customLineEnding)
                .build();
        assertEquals(customLineEnding, config.getLineEnding(), "lineEnding should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding validateURIs")
    void builder_shouldAllowOverridingValidateURIs() {
        TriGConfig config = TriGConfig.builder()
                .validateURIs(true)
                .build();
        assertTrue(config.validateURIs(), "validateURIs should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding stableBlankNodeIds")
    void builder_shouldAllowOverridingStableBlankNodeIds() {
        TriGConfig config = TriGConfig.builder()
                .stableBlankNodeIds(true)
                .build();
        assertTrue(config.stableBlankNodeIds(), "stableBlankNodeIds should be overridden to true");
    }



}
