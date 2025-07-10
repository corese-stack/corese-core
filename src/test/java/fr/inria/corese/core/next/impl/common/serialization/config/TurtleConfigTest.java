package fr.inria.corese.core.next.impl.common.serialization.config;

import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link TurtleConfig} class.
 * These tests verify the default configuration settings and the functionality
 * of the builder pattern for customizing Turtle serialization options.
 */
class TurtleConfigTest {

    @Test
    @DisplayName("defaultConfig() should return a config with expected Turtle defaults")
    void defaultConfig_shouldReturnExpectedDefaults() {
        TurtleConfig config = TurtleConfig.defaultConfig();

        assertNotNull(config, "Default config should not be null");

        assertTrue(config.useCollections(), "Default useCollections should be true for Turtle");
        assertEquals(BlankNodeStyleEnum.ANONYMOUS, config.getBlankNodeStyle(), "Default blankNodeStyle should be ANONYMOUS for Turtle");

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

        assertTrue(config.strictMode, "Default strictMode should be true");
        assertFalse(config.escapeUnicode(), "Default escapeUnicode should be false");
        assertEquals(LiteralDatatypePolicyEnum.MINIMAL, config.getLiteralDatatypePolicy(), "Default literalDatatypePolicy should be MINIMAL");
        assertNull(config.getBaseIRI(), "Default baseIRI should be null");
        assertEquals(System.lineSeparator(), config.getLineEnding(), "Default lineEnding should be system's line separator");
        assertFalse(config.validateURIs(), "Default validateURIs should be false");
        assertFalse(config.stableBlankNodeIds(), "Default stableBlankNodeIds should be false");
    }

    @Test
    @DisplayName("Builder should allow overriding useCollections")
    void builder_shouldAllowOverridingUseCollections() {
        TurtleConfig config = new TurtleConfig.Builder()
                .useCollections(false)
                .build();
        assertFalse(config.useCollections(), "useCollections should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding blankNodeStyle")
    void builder_shouldAllowOverridingBlankNodeStyle() {
        TurtleConfig config = new TurtleConfig.Builder()
                .blankNodeStyle(BlankNodeStyleEnum.NAMED)
                .build();
        assertEquals(BlankNodeStyleEnum.NAMED, config.getBlankNodeStyle(), "blankNodeStyle should be overridden to NAMED");
    }

    @Test
    @DisplayName("Builder should allow adding custom prefixes")
    void builder_shouldAllowAddingCustomPrefixes() {
        String customPrefix = "my";
        String customNamespace = "http://my.example.org/";
        TurtleConfig config = new TurtleConfig.Builder()
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
        TurtleConfig config = new TurtleConfig.Builder()
                .usePrefixes(false)
                .build();
        assertFalse(config.usePrefixes(), "usePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding autoDeclarePrefixes")
    void builder_shouldAllowOverridingAutoDeclarePrefixes() {
        TurtleConfig config = new TurtleConfig.Builder()
                .autoDeclarePrefixes(false)
                .build();
        assertFalse(config.autoDeclarePrefixes(), "autoDeclarePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding prefixOrdering")
    void builder_shouldAllowOverridingPrefixOrdering() {
        TurtleConfig config = new TurtleConfig.Builder()
                .prefixOrdering(PrefixOrderingEnum.USAGE_ORDER)
                .build();
        assertEquals(PrefixOrderingEnum.USAGE_ORDER, config.getPrefixOrdering(), "prefixOrdering should be overridden to USAGE_ORDER");
    }

    @Test
    @DisplayName("Builder should allow overriding useCompactTriples")
    void builder_shouldAllowOverridingUseCompactTriples() {
        TurtleConfig config = new TurtleConfig.Builder()
                .useCompactTriples(false)
                .build();
        assertFalse(config.useCompactTriples(), "useCompactTriples should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding useRdfTypeShortcut")
    void builder_shouldAllowOverridingUseRdfTypeShortcut() {
        TurtleConfig config = new TurtleConfig.Builder()
                .useRdfTypeShortcut(false)
                .build();
        assertFalse(config.useRdfTypeShortcut(), "useRdfTypeShortcut should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding useMultilineLiterals")
    void builder_shouldAllowOverridingUseMultilineLiterals() {
        TurtleConfig config = new TurtleConfig.Builder()
                .useMultilineLiterals(false)
                .build();
        assertFalse(config.useMultilineLiterals(), "useMultilineLiterals should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding prettyPrint")
    void builder_shouldAllowOverridingPrettyPrint() {
        TurtleConfig config = new TurtleConfig.Builder()
                .prettyPrint(false)
                .build();
        assertFalse(config.prettyPrint(), "prettyPrint should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding indent")
    void builder_shouldAllowOverridingIndent() {
        String customIndent = "\t";
        TurtleConfig config = new TurtleConfig.Builder()
                .indent(customIndent)
                .build();
        assertEquals(customIndent, config.getIndent(), "indent should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding maxLineLength")
    void builder_shouldAllowOverridingMaxLineLength() {
        int customLength = 120;
        TurtleConfig config = new TurtleConfig.Builder()
                .maxLineLength(customLength)
                .build();
        assertEquals(customLength, config.getMaxLineLength(), "maxLineLength should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding groupBySubject")
    void builder_shouldAllowOverridingGroupBySubject() {
        TurtleConfig config = new TurtleConfig.Builder()
                .groupBySubject(false)
                .build();
        assertFalse(config.groupBySubject(), "groupBySubject should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding sortSubjects")
    void builder_shouldAllowOverridingSortSubjects() {
        TurtleConfig config = new TurtleConfig.Builder()
                .sortSubjects(true)
                .build();
        assertTrue(config.sortSubjects(), "sortSubjects should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding sortPredicates")
    void builder_shouldAllowOverridingSortPredicates() {
        TurtleConfig config = new TurtleConfig.Builder()
                .sortPredicates(true)
                .build();
        assertTrue(config.sortPredicates(), "sortPredicates should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding strictMode")
    void builder_shouldAllowOverridingStrictMode() {
        TurtleConfig config = new TurtleConfig.Builder()
                .strictMode(false)
                .build();
        assertFalse(config.strictMode, "strictMode should be overridden to false");
    }


    @Test
    @DisplayName("Builder should allow overriding literalDatatypePolicy")
    void builder_shouldAllowOverridingLiteralDatatypePolicy() {
        TurtleConfig config = new TurtleConfig.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED)
                .build();
        assertEquals(LiteralDatatypePolicyEnum.ALWAYS_TYPED, config.getLiteralDatatypePolicy(), "literalDatatypePolicy should be overridden to ALWAYS_TYPED");
    }

    @Test
    @DisplayName("Builder should allow setting baseIRI")
    void builder_shouldAllowSettingBaseIRI() {
        String testBaseIRI = "http://example.org/base/";
        TurtleConfig config = new TurtleConfig.Builder()
                .baseIRI(testBaseIRI)
                .build();
        assertEquals(testBaseIRI, config.getBaseIRI(), "baseIRI should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow overriding lineEnding")
    void builder_shouldAllowOverridingLineEnding() {
        String customLineEnding = "\r\n";
        TurtleConfig config = new TurtleConfig.Builder()
                .lineEnding(customLineEnding)
                .build();
        assertEquals(customLineEnding, config.getLineEnding(), "lineEnding should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding validateURIs")
    void builder_shouldAllowOverridingValidateURIs() {
        TurtleConfig config = new TurtleConfig.Builder()
                .validateURIs(true)
                .build();
        assertTrue(config.validateURIs(), "validateURIs should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding stableBlankNodeIds")
    void builder_shouldAllowOverridingStableBlankNodeIds() {
        TurtleConfig config = new TurtleConfig.Builder()
                .stableBlankNodeIds(true)
                .build();
        assertTrue(config.stableBlankNodeIds(), "stableBlankNodeIds should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding includeContext")
    void builder_shouldAllowOverridingIncludeContext() {
        TurtleConfig config = new TurtleConfig.Builder()
                .includeContext(true)
                .build();
        assertTrue(config.includeContext(), "includeContext should be overridden to true");
    }


}
