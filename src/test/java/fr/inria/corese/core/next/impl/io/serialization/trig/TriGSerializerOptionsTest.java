package fr.inria.corese.core.next.impl.io.serialization.trig;

import fr.inria.corese.core.next.impl.common.prefix.PrefixHandler;
import fr.inria.corese.core.next.impl.common.vocabulary.OWL;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.common.vocabulary.RDFS;
import fr.inria.corese.core.next.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.impl.io.serialization.option.BlankNodeStyleEnum;
import fr.inria.corese.core.next.impl.io.serialization.option.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.io.serialization.option.PrefixOrderingEnum;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link TriGSerializerOptions} class.
 * These tests verify the default configuration settings and the functionality
 * of the builder pattern for customizing TriG serialization options.
 */
class TriGSerializerOptionsTest {

    @Test
    @DisplayName("defaultConfig() should return a config with expected TriG defaults")
    void defaultConfig_shouldReturnExpectedDefaults() {
        TriGSerializerOptions config = TriGSerializerOptions.defaultConfig();

        assertNotNull(config, "Default config should not be null");

        assertTrue(config.includeContext(), "Default includeContext should be true for TriG");
        assertEquals(BlankNodeStyleEnum.NAMED, config.getBlankNodeStyle(), "Default blankNodeStyle should be NAMED for TriG");
        assertFalse(config.useCollections(), "Default useCollections should be false for TriG");

        PrefixHandler prefixHandler = config.getPrefixHandler();
        assertNotNull(prefixHandler, "PrefixHandler should not be null");

        assertTrue(prefixHandler.hasPrefix(RDF.getVocabularyPreferredPrefix()), "Should contain rdf prefix");
        assertTrue(prefixHandler.hasPrefix(RDFS.getVocabularyPreferredPrefix()), "Should contain rdfs prefix");
        assertTrue(prefixHandler.hasPrefix(XSD.getVocabularyPreferredPrefix()), "Should contain xsd prefix");
        assertTrue(prefixHandler.hasPrefix(OWL.getVocabularyPreferredPrefix()), "Should contain owl prefix");

        assertEquals(RDF.getVocabularyNamespace(), prefixHandler.getNamespace(RDF.getVocabularyPreferredPrefix()));
        assertEquals(RDFS.getVocabularyNamespace(), prefixHandler.getNamespace(RDFS.getVocabularyPreferredPrefix()));
        assertEquals(XSD.getVocabularyNamespace(), prefixHandler.getNamespace(XSD.getVocabularyPreferredPrefix()));
        assertEquals(OWL.getVocabularyNamespace(), prefixHandler.getNamespace(OWL.getVocabularyPreferredPrefix()));

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

        assertTrue(config.isStrictMode(), "Default strictMode should be true");
        assertFalse(config.escapeUnicode(), "Default escapeUnicode should be false");
        assertEquals(LiteralDatatypePolicyEnum.MINIMAL, config.getLiteralDatatypePolicy(), "Default literalDatatypePolicy should be MINIMAL");
        assertNull(config.getBaseIRI(), "Default baseIRI should be null");
    }

    @Test
    @DisplayName("Builder should allow overriding includeContext")
    void builder_shouldAllowOverridingIncludeContext() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .includeContext(false)
                .build();
        assertFalse(config.includeContext(), "includeContext should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding blankNodeStyle")
    void builder_shouldAllowOverridingBlankNodeStyle() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .blankNodeStyle(BlankNodeStyleEnum.ANONYMOUS)
                .build();
        assertEquals(BlankNodeStyleEnum.ANONYMOUS, config.getBlankNodeStyle(), "blankNodeStyle should be overridden to ANONYMOUS");
    }

    @Test
    @DisplayName("Builder should allow overriding useCollections")
    void builder_shouldAllowOverridingUseCollections() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .useCollections(true)
                .build();
        assertTrue(config.useCollections(), "useCollections should be overridden to true");
    }


    @Test
    @DisplayName("Builder should allow overriding usePrefixes")
    void builder_shouldAllowOverridingUsePrefixes() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .usePrefixes(false)
                .build();
        assertFalse(config.usePrefixes(), "usePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding autoDeclarePrefixes")
    void builder_shouldAllowOverridingAutoDeclarePrefixes() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .autoDeclarePrefixes(false)
                .build();
        assertFalse(config.autoDeclarePrefixes(), "autoDeclarePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding prefixOrdering")
    void builder_shouldAllowOverridingPrefixOrdering() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .prefixOrdering(PrefixOrderingEnum.USAGE_ORDER)
                .build();
        assertEquals(PrefixOrderingEnum.USAGE_ORDER, config.getPrefixOrdering(), "prefixOrdering should be overridden to USAGE_ORDER");
    }

    @Test
    @DisplayName("Builder should allow overriding useCompactTriples")
    void builder_shouldAllowOverridingUseCompactTriples() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .useCompactTriples(false)
                .build();
        assertFalse(config.useCompactTriples(), "useCompactTriples should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding useRdfTypeShortcut")
    void builder_shouldAllowOverridingUseRdfTypeShortcut() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .useRdfTypeShortcut(false)
                .build();
        assertFalse(config.useRdfTypeShortcut(), "useRdfTypeShortcut should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding useMultilineLiterals")
    void builder_shouldAllowOverridingUseMultilineLiterals() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .useMultilineLiterals(false)
                .build();
        assertFalse(config.useMultilineLiterals(), "useMultilineLiterals should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding prettyPrint")
    void builder_shouldAllowOverridingPrettyPrint() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .prettyPrint(false)
                .build();
        assertFalse(config.prettyPrint(), "prettyPrint should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding indent")
    void builder_shouldAllowOverridingIndent() {
        String customIndent = "\t";
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .indent(customIndent)
                .build();
        assertEquals(customIndent, config.getIndent(), "indent should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding maxLineLength")
    void builder_shouldAllowOverridingMaxLineLength() {
        int customLength = 120;
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .maxLineLength(customLength)
                .build();
        assertEquals(customLength, config.getMaxLineLength(), "maxLineLength should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding groupBySubject")
    void builder_shouldAllowOverridingGroupBySubject() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .groupBySubject(false)
                .build();
        assertFalse(config.groupBySubject(), "groupBySubject should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding sortSubjects")
    void builder_shouldAllowOverridingSortSubjects() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .sortSubjects(true)
                .build();
        assertTrue(config.sortSubjects(), "sortSubjects should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding sortPredicates")
    void builder_shouldAllowOverridingSortPredicates() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .sortPredicates(true)
                .build();
        assertTrue(config.sortPredicates(), "sortPredicates should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding strictMode")
    void builder_shouldAllowOverridingStrictMode() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .strictMode(false)
                .build();
        assertFalse(config.isStrictMode(), "strictMode should be overridden to false");
    }


    @Test
    @DisplayName("Builder should allow overriding literalDatatypePolicy")
    void builder_shouldAllowOverridingLiteralDatatypePolicy() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED)
                .build();
        assertEquals(LiteralDatatypePolicyEnum.ALWAYS_TYPED, config.getLiteralDatatypePolicy(), "literalDatatypePolicy should be overridden to ALWAYS_TYPED");
    }

    @Test
    @DisplayName("Builder should allow setting baseIRI")
    void builder_shouldAllowSettingBaseIRI() {
        String testBaseIRI = "http://example.org/base/";
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .baseIRI(testBaseIRI)
                .build();
        assertEquals(testBaseIRI, config.getBaseIRI(), "baseIRI should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow overriding lineEnding")
    void builder_shouldAllowOverridingLineEnding() {
        String customLineEnding = "\r\n";
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .lineEnding(customLineEnding)
                .build();
        assertEquals(customLineEnding, config.getLineEnding(), "lineEnding should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding validateURIs")
    void builder_shouldAllowOverridingValidateURIs() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .validateURIs(true)
                .build();
        assertTrue(config.validateURIs(), "validateURIs should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding stableBlankNodeIds")
    void builder_shouldAllowOverridingStableBlankNodeIds() {
        TriGSerializerOptions config = TriGSerializerOptions.builder()
                .stableBlankNodeIds(true)
                .build();
        assertTrue(config.stableBlankNodeIds(), "stableBlankNodeIds should be overridden to true");
    }



}
