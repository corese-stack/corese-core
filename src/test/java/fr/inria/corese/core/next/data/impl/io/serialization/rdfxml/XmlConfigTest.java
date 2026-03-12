package fr.inria.corese.core.next.data.impl.io.serialization.rdfxml;

import fr.inria.corese.core.next.data.impl.common.prefix.PrefixHandler;
import fr.inria.corese.core.next.data.impl.common.vocabulary.OWL;
import fr.inria.corese.core.next.data.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.data.impl.common.vocabulary.RDFS;
import fr.inria.corese.core.next.data.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.data.impl.io.serialization.option.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.data.impl.io.serialization.option.PrefixOrderingEnum;
import fr.inria.corese.core.next.data.impl.io.serialization.rdfxml.RDFXMLSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serialization.util.SerializationConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link RDFXMLSerializerOptions} class.
 * These tests verify the default configuration settings and the functionality
 * of the builder pattern for customizing RDF/XML serialization options.
 */
class XmlConfigTest {

    @Test
    @DisplayName("defaultConfig() should return a config with expected RDF/XML defaults")
    void defaultConfig_shouldReturnExpectedDefaults() {
        RDFXMLSerializerOptions config = RDFXMLSerializerOptions.defaultConfig();

        assertNotNull(config, "Default config should not be null");

        assertTrue(config.usePrefixes(), "Default usePrefixes should be true for XML");
        assertTrue(config.autoDeclarePrefixes(), "Default autoDeclarePrefixes should be true for XML");
        assertEquals(PrefixOrderingEnum.ALPHABETICAL, config.getPrefixOrdering(), "Default prefixOrdering should be ALPHABETICAL for XML");

        // Vérifier les préfixes standards via PrefixHandler
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

        assertTrue(config.prettyPrint(), "Default prettyPrint should be true for XML");
        assertEquals(SerializationConstants.DEFAULT_INDENTATION, config.getIndent(), "Default indent should be " + SerializationConstants.DEFAULT_INDENTATION);
        assertEquals(0, config.getMaxLineLength(), "Default maxLineLength should be 0 (no line length createFunCall) for XML");
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
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .usePrefixes(false)
                .build();
        assertFalse(config.usePrefixes(), "usePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding autoDeclarePrefixes")
    void builder_shouldAllowOverridingAutoDeclarePrefixes() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .autoDeclarePrefixes(false)
                .build();
        assertFalse(config.autoDeclarePrefixes(), "autoDeclarePrefixes should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding prefixOrdering")
    void builder_shouldAllowOverridingPrefixOrdering() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .prefixOrdering(PrefixOrderingEnum.USAGE_ORDER)
                .build();
        assertEquals(PrefixOrderingEnum.USAGE_ORDER, config.getPrefixOrdering(), "prefixOrdering should be overridden to USAGE_ORDER");
    }

    @Test
    @DisplayName("Builder should allow adding custom prefixes via PrefixHandler")
    void builder_shouldAllowAddingCustomPrefixes() {
        String customPrefix = "my";
        String customNamespace = "http://my.example.org/";

        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .addPrefix(customPrefix, customNamespace)
                .build();

        PrefixHandler prefixHandler = config.getPrefixHandler();
        assertTrue(prefixHandler.hasPrefix(customPrefix), "Custom prefix should be added");
        assertEquals(customNamespace, prefixHandler.getNamespace(customPrefix), "Custom prefix namespace should be correct");

        // Vérifier que les préfixes standards sont toujours présents
        assertTrue(prefixHandler.hasPrefix("rdf"));
        assertTrue(prefixHandler.hasPrefix("xsd"));
    }

    @Test
    @DisplayName("Builder should allow setting custom PrefixHandler")
    void builder_shouldAllowSettingCustomPrefixHandler() {
        PrefixHandler customHandler = new PrefixHandler(false); // sans vocabulaires standards
        customHandler.setPrefix("ex", "http://example.org/");
        customHandler.setPrefix("custom", "http://custom.org/");

        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .prefixHandler(customHandler)
                .build();

        PrefixHandler resultHandler = config.getPrefixHandler();
        assertEquals(customHandler, resultHandler, "PrefixHandler should be the custom one");
        assertTrue(resultHandler.hasPrefix("ex"));
        assertTrue(resultHandler.hasPrefix("custom"));
        assertFalse(resultHandler.hasPrefix("rdf")); // Pas de vocabulaires standards
    }

    @Test
    @DisplayName("Builder should allow adding multiple prefixes at once")
    void builder_shouldAllowAddingMultiplePrefixes() {
        Map<String, String> customPrefixes = new HashMap<>();
        customPrefixes.put("ex", "http://example.org/");
        customPrefixes.put("custom", "http://custom.org/");

        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .addPrefixes(customPrefixes)
                .build();

        PrefixHandler prefixHandler = config.getPrefixHandler();
        assertTrue(prefixHandler.hasPrefix("ex"));
        assertTrue(prefixHandler.hasPrefix("custom"));
        assertEquals("http://example.org/", prefixHandler.getNamespace("ex"));
        assertEquals("http://custom.org/", prefixHandler.getNamespace("custom"));
    }

    @Test
    @DisplayName("Builder should allow overriding prettyPrint")
    void builder_shouldAllowOverridingPrettyPrint() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .prettyPrint(false)
                .build();
        assertFalse(config.prettyPrint(), "prettyPrint should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding indent")
    void builder_shouldAllowOverridingIndent() {
        String customIndent = "\t";
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .indent(customIndent)
                .build();
        assertEquals(customIndent, config.getIndent(), "indent should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding maxLineLength")
    void builder_shouldAllowOverridingMaxLineLength() {
        int customLength = 120;
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .maxLineLength(customLength)
                .build();
        assertEquals(customLength, config.getMaxLineLength(), "maxLineLength should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding sortSubjects")
    void builder_shouldAllowOverridingSortSubjects() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .sortSubjects(true)
                .build();
        assertTrue(config.sortSubjects(), "sortSubjects should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding sortPredicates")
    void builder_shouldAllowOverridingSortPredicates() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .sortPredicates(true)
                .build();
        assertTrue(config.sortPredicates(), "sortPredicates should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding useMultilineLiterals")
    void builder_shouldAllowOverridingUseMultilineLiterals() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .useMultilineLiterals(false)
                .build();
        assertFalse(config.useMultilineLiterals(), "useMultilineLiterals should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding strictMode")
    void builder_shouldAllowOverridingStrictMode() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .strictMode(false)
                .build();
        assertFalse(config.isStrictMode(), "strictMode should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding escapeUnicode")
    void builder_shouldAllowOverridingEscapeUnicode() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .escapeUnicode(true)
                .build();
        assertTrue(config.escapeUnicode(), "escapeUnicode should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding literalDatatypePolicy")
    void builder_shouldAllowOverridingLiteralDatatypePolicy() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .literalDatatypePolicy(LiteralDatatypePolicyEnum.MINIMAL)
                .build();
        assertEquals(LiteralDatatypePolicyEnum.MINIMAL, config.getLiteralDatatypePolicy(), "literalDatatypePolicy should be overridden to MINIMAL");
    }

    @Test
    @DisplayName("Builder should allow setting baseIRI")
    void builder_shouldAllowSettingBaseIRI() {
        String testBaseIRI = "http://example.org/base/";
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .baseIRI(testBaseIRI)
                .build();
        assertEquals(testBaseIRI, config.getBaseIRI(), "baseIRI should be set correctly");
    }

    @Test
    @DisplayName("Builder should allow overriding lineEnding")
    void builder_shouldAllowOverridingLineEnding() {
        String customLineEnding = "\r\n";
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .lineEnding(customLineEnding)
                .build();
        assertEquals(customLineEnding, config.getLineEnding(), "lineEnding should be overridden to custom value");
    }

    @Test
    @DisplayName("Builder should allow overriding validateURIs")
    void builder_shouldAllowOverridingValidateURIs() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .validateURIs(true)
                .build();
        assertTrue(config.validateURIs(), "validateURIs should be overridden to true");
    }

    @Test
    @DisplayName("Builder should allow overriding stableBlankNodeIds")
    void builder_shouldAllowOverridingStableBlankNodeIds() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .stableBlankNodeIds(false)
                .build();
        assertFalse(config.stableBlankNodeIds(), "stableBlankNodeIds should be overridden to false");
    }

    @Test
    @DisplayName("Builder should allow overriding includeContext")
    void builder_shouldAllowOverridingIncludeContext() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .includeContext(true)
                .build();
        assertTrue(config.includeContext(), "includeContext should be overridden to true");
    }

    @Test
    @DisplayName("Builder should maintain default prefixes when adding custom ones")
    void builder_shouldMaintainDefaultPrefixesWhenAddingCustomOnes() {
        RDFXMLSerializerOptions config = new RDFXMLSerializerOptions.Builder()
                .addPrefix("ex", "http://example.org/")
                .build();

        PrefixHandler prefixHandler = config.getPrefixHandler();
        // Vérifier que les préfixes standards sont toujours là
        assertTrue(prefixHandler.hasPrefix("rdf"));
        assertTrue(prefixHandler.hasPrefix("xsd"));
        assertTrue(prefixHandler.hasPrefix("rdfs"));
        assertTrue(prefixHandler.hasPrefix("owl"));
        // Et le nouveau préfixe custom
        assertTrue(prefixHandler.hasPrefix("ex"));
    }
}