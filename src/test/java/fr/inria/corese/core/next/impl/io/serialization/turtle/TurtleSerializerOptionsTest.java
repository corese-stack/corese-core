package fr.inria.corese.core.next.impl.io.serialization.turtle;

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
 * Unit tests for the {@link TurtleSerializerOptions} class.
 * These tests verify the default configuration settings and the functionality
 * of the builder pattern for customizing Turtle serialization options.
 */
class TurtleSerializerOptionsTest {

    @Test
    @DisplayName("defaultConfig() should return a config with expected Turtle defaults")
    void defaultConfig_shouldReturnExpectedDefaults() {
        TurtleSerializerOptions config = TurtleSerializerOptions.defaultConfig();

        assertNotNull(config, "Default config should not be null");

        assertTrue(config.useCollections(), "Default useCollections should be true for Turtle");
        assertEquals(BlankNodeStyleEnum.ANONYMOUS, config.getBlankNodeStyle(), "Default blankNodeStyle should be ANONYMOUS for Turtle");

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

        assertTrue(config.isStrictMode(), "Default strictMode should be true");
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
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .useCollections(false)
                .build();
        assertFalse(config.useCollections(), "useCollections should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding blankNodeStyle")
    void builder_shouldAllowOverridingBlankNodeStyle() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .blankNodeStyle(BlankNodeStyleEnum.NAMED)
                .build();
        assertEquals(BlankNodeStyleEnum.NAMED, config.getBlankNodeStyle(), "blankNodeStyle should be overridden to NAMED");
    }



    @Test
    @DisplayName("Builder should allow overriding usePrefixes")
    void builder_shouldAllowOverridingUsePrefixes() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .usePrefixes(false)
                .build();
        assertFalse(config.usePrefixes(), "usePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding autoDeclarePrefixes")
    void builder_shouldAllowOverridingAutoDeclarePrefixes() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .autoDeclarePrefixes(false)
                .build();
        assertFalse(config.autoDeclarePrefixes(), "autoDeclarePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding prefixOrdering")
    void builder_shouldAllowOverridingPrefixOrdering() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .prefixOrdering(PrefixOrderingEnum.USAGE_ORDER)
                .build();
        assertEquals(PrefixOrderingEnum.USAGE_ORDER, config.getPrefixOrdering(), "prefixOrdering should be overridden to USAGE_ORDER");
    }

    @Test
    @DisplayName("Builder should allow overriding useCompactTriples")
    void builder_shouldAllowOverridingUseCompactTriples() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .useCompactTriples(false)
                .build();
        assertFalse(config.useCompactTriples(), "useCompactTriples should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding useRdfTypeShortcut")
    void builder_shouldAllowOverridingUseRdfTypeShortcut() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .useRdfTypeShortcut(false)
                .build();
        assertFalse(config.useRdfTypeShortcut(), "useRdfTypeShortcut should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding useMultilineLiterals")
    void builder_shouldAllowOverridingUseMultilineLiterals() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .useMultilineLiterals(false)
                .build();
        assertFalse(config.useMultilineLiterals(), "useMultilineLiterals should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding prettyPrint")
    void builder_shouldAllowOverridingPrettyPrint() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .prettyPrint(false)
                .build();
        assertFalse(config.prettyPrint(), "prettyPrint should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding indent")
    void builder_shouldAllowOverridingIndent() {
        String customIndent = "\t";
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .indent(customIndent)
                .build();
        assertEquals(customIndent, config.getIndent(), "indent should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding maxLineLength")
    void builder_shouldAllowOverridingMaxLineLength() {
        int customLength = 120;
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .maxLineLength(customLength)
                .build();
        assertEquals(customLength, config.getMaxLineLength(), "maxLineLength should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding groupBySubject")
    void builder_shouldAllowOverridingGroupBySubject() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .groupBySubject(false)
                .build();
        assertFalse(config.groupBySubject(), "groupBySubject should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding sortSubjects")
    void builder_shouldAllowOverridingSortSubjects() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .sortSubjects(true)
                .build();
        assertTrue(config.sortSubjects(), "sortSubjects should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding sortPredicates")
    void builder_shouldAllowOverridingSortPredicates() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .sortPredicates(true)
                .build();
        assertTrue(config.sortPredicates(), "sortPredicates should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding strictMode")
    void builder_shouldAllowOverridingStrictMode() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .strictMode(false)
                .build();
        assertFalse(config.isStrictMode(), "strictMode should be overridden to false");
    }


    @Test
    @DisplayName("Builder should allow overriding literalDatatypePolicy")
    void builder_shouldAllowOverridingLiteralDatatypePolicy() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.ALWAYS_TYPED)
                .build();
        assertEquals(LiteralDatatypePolicyEnum.ALWAYS_TYPED, config.getLiteralDatatypePolicy(), "literalDatatypePolicy should be overridden to ALWAYS_TYPED");
    }

    @Test
    @DisplayName("Builder should allow setting baseIRI")
    void builder_shouldAllowSettingBaseIRI() {
        String testBaseIRI = "http://example.org/base/";
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .baseIRI(testBaseIRI)
                .build();
        assertEquals(testBaseIRI, config.getBaseIRI(), "baseIRI should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow overriding lineEnding")
    void builder_shouldAllowOverridingLineEnding() {
        String customLineEnding = "\r\n";
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .lineEnding(customLineEnding)
                .build();
        assertEquals(customLineEnding, config.getLineEnding(), "lineEnding should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding validateURIs")
    void builder_shouldAllowOverridingValidateURIs() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .validateURIs(true)
                .build();
        assertTrue(config.validateURIs(), "validateURIs should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding stableBlankNodeIds")
    void builder_shouldAllowOverridingStableBlankNodeIds() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .stableBlankNodeIds(true)
                .build();
        assertTrue(config.stableBlankNodeIds(), "stableBlankNodeIds should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding includeContext")
    void builder_shouldAllowOverridingIncludeContext() {
        TurtleSerializerOptions config = new TurtleSerializerOptions.Builder()
                .includeContext(true)
                .build();
        assertTrue(config.includeContext(), "includeContext should be overridden to true");
    }


}
