package fr.inria.corese.core.next.impl.common.serialization.config;

import fr.inria.corese.core.next.impl.io.serialization.option.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.io.serialization.option.PrefixOrderingEnum;
import fr.inria.corese.core.next.impl.io.serialization.rdfxml.XmlOption;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link XmlOption} class.
 * These tests verify the default configuration settings and the functionality
 * of the builder pattern for customizing RDF/XML serialization options.
 */
class XmlConfigTest {

    @Test
    @DisplayName("defaultConfig() should return a config with expected RDF/XML defaults")
    void defaultConfig_shouldReturnExpectedDefaults() {
        XmlOption config = XmlOption.defaultConfig();

        assertNotNull(config, "Default config should not be null");

        assertTrue(config.usePrefixes(), "Default usePrefixes should be true for XML");
        assertTrue(config.autoDeclarePrefixes(), "Default autoDeclarePrefixes should be true for XML");
        assertEquals(PrefixOrderingEnum.ALPHABETICAL, config.getPrefixOrdering(), "Default prefixOrdering should be ALPHABETICAL for XML");

        Map<String, String> expectedPrefixes = new HashMap<>();
        expectedPrefixes.put("rdf", SerializationConstants.RDF_NS);
        expectedPrefixes.put("rdfs", SerializationConstants.RDFS_NS);
        expectedPrefixes.put("xsd", SerializationConstants.XSD_NS);
        expectedPrefixes.put("owl", SerializationConstants.OWL_NS);
        assertEquals(expectedPrefixes.size(), config.getCustomPrefixes().size(), "Default custom prefixes size mismatch");
        assertTrue(config.getCustomPrefixes().entrySet().containsAll(expectedPrefixes.entrySet()), "Default custom prefixes should contain common RDF prefixes");

        assertTrue(config.prettyPrint(), "Default prettyPrint should be true for XML");
        assertEquals(SerializationConstants.DEFAULT_INDENTATION, config.getIndent(), "Default indent should be " + SerializationConstants.DEFAULT_INDENTATION);
        assertEquals(0, config.getMaxLineLength(), "Default maxLineLength should be 0 (no line length constraint) for XML");
        assertFalse(config.sortSubjects(), "Default sortSubjects should be false for XML");
        assertFalse(config.sortPredicates(), "Default sortPredicates should be false for XML");
        assertTrue(config.useMultilineLiterals(), "Default useMultilineLiterals should be true for XML");

        assertTrue(config.isStrictMode(), "Default strictMode should be true");
        assertFalse(config.escapeUnicode(), "Default escapeUnicode should be false for XML");
        assertEquals(LiteralDatatypePolicyEnum.ALWAYS_TYPED, config.getLiteralDatatypePolicy(), "Default literalDatatypePolicy should be ALWAYS_TYPED for XML");
        assertNull(config.getBaseIRI(), "Default baseIRI should be null");
        assertFalse(config.includeContext(), "Default includeContext should be false for XML (RDF/XML doesn't support named graphs)");
    }

    @Test
    @DisplayName("Builder should allow overriding usePrefixes")
    void builder_shouldAllowOverridingUsePrefixes() {
        XmlOption config = new XmlOption.Builder()
                .usePrefixes(false)
                .build();
        assertFalse(config.usePrefixes(), "usePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding autoDeclarePrefixes")
    void builder_shouldAllowOverridingAutoDeclarePrefixes() {
        XmlOption config = new XmlOption.Builder()
                .autoDeclarePrefixes(false)
                .build();
        assertFalse(config.autoDeclarePrefixes(), "autoDeclarePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding prefixOrdering")
    void builder_shouldAllowOverridingPrefixOrdering() {
        XmlOption config = new XmlOption.Builder()
                .prefixOrdering(PrefixOrderingEnum.USAGE_ORDER)
                .build();
        assertEquals(PrefixOrderingEnum.USAGE_ORDER, config.getPrefixOrdering(), "prefixOrdering should be overridden to USAGE_ORDER");
    }

    @Test
    @DisplayName("Builder should allow adding custom prefixes")
    void builder_shouldAllowAddingCustomPrefixes() {
        String customPrefix = "my";
        String customNamespace = "http://my.example.org/";
        XmlOption config = new XmlOption.Builder()
                .addCustomPrefix(customPrefix, customNamespace)
                .build();

        assertTrue(config.getCustomPrefixes().containsKey(customPrefix), "Custom prefix should be added");
        assertEquals(customNamespace, config.getCustomPrefixes().get(customPrefix), "Custom prefix namespace should be correct");
        assertTrue(config.getCustomPrefixes().containsKey("rdf"));
        assertTrue(config.getCustomPrefixes().containsKey("xsd"));
    }

    @Test
    @DisplayName("Builder should allow overriding prettyPrint")
    void builder_shouldAllowOverridingPrettyPrint() {
        XmlOption config = new XmlOption.Builder()
                .prettyPrint(false)
                .build();
        assertFalse(config.prettyPrint(), "prettyPrint should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding indent")
    void builder_shouldAllowOverridingIndent() {
        String customIndent = "\t";
        XmlOption config = new XmlOption.Builder()
                .indent(customIndent)
                .build();
        assertEquals(customIndent, config.getIndent(), "indent should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding maxLineLength")
    void builder_shouldAllowOverridingMaxLineLength() {
        int customLength = 120;
        XmlOption config = new XmlOption.Builder()
                .maxLineLength(customLength)
                .build();
        assertEquals(customLength, config.getMaxLineLength(), "maxLineLength should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding sortSubjects")
    void builder_shouldAllowOverridingSortSubjects() {
        XmlOption config = new XmlOption.Builder()
                .sortSubjects(true)
                .build();
        assertTrue(config.sortSubjects(), "sortSubjects should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding sortPredicates")
    void builder_shouldAllowOverridingSortPredicates() {
        XmlOption config = new XmlOption.Builder()
                .sortPredicates(true)
                .build();
        assertTrue(config.sortPredicates(), "sortPredicates should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding useMultilineLiterals")
    void builder_shouldAllowOverridingUseMultilineLiterals() {
        XmlOption config = new XmlOption.Builder()
                .useMultilineLiterals(false)
                .build();
        assertFalse(config.useMultilineLiterals(), "useMultilineLiterals should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding strictMode")
    void builder_shouldAllowOverridingStrictMode() {
        XmlOption config = new XmlOption.Builder()
                .strictMode(false)
                .build();
        assertFalse(config.isStrictMode(), "strictMode should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding escapeUnicode")
    void builder_shouldAllowOverridingEscapeUnicode() {
        XmlOption config = new XmlOption.Builder()
                .escapeUnicode(true)
                .build();
        assertTrue(config.escapeUnicode(), "escapeUnicode should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding literalDatatypePolicy")
    void builder_shouldAllowOverridingLiteralDatatypePolicy() {
        XmlOption config = new XmlOption.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.MINIMAL)
                .build();
        assertEquals(LiteralDatatypePolicyEnum.MINIMAL, config.getLiteralDatatypePolicy(), "literalDatatypePolicy should be overridden to MINIMAL");
    }

    @Test
    @DisplayName("Builder should allow setting baseIRI")
    void builder_shouldAllowSettingBaseIRI() {
        String testBaseIRI = "http://example.org/base/";
        XmlOption config = new XmlOption.Builder()
                .baseIRI(testBaseIRI)
                .build();
        assertEquals(testBaseIRI, config.getBaseIRI(), "baseIRI should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow overriding lineEnding")
    void builder_shouldAllowOverridingLineEnding() {
        String customLineEnding = "\r\n";
        XmlOption config = new XmlOption.Builder()
                .lineEnding(customLineEnding)
                .build();
        assertEquals(customLineEnding, config.getLineEnding(), "lineEnding should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding validateURIs")
    void builder_shouldAllowOverridingValidateURIs() {
        XmlOption config = new XmlOption.Builder()
                .validateURIs(true)
                .build();
        assertTrue(config.validateURIs(), "validateURIs should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding stableBlankNodeIds")
    void builder_shouldAllowOverridingStableBlankNodeIds() {
        XmlOption config = new XmlOption.Builder()
                .stableBlankNodeIds(false)
                .build();
        assertFalse(config.stableBlankNodeIds(), "stableBlankNodeIds should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding includeContext")
    void builder_shouldAllowOverridingIncludeContext() {
        XmlOption config = new XmlOption.Builder()
                .includeContext(true)
                .build();
        assertTrue(config.includeContext(), "includeContext should be overridden to true");
    }


}
