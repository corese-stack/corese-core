package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.impl.common.serialization.config.BlankNodeStyleEnum;
import fr.inria.corese.core.next.impl.common.serialization.config.FormatConfig;
import fr.inria.corese.core.next.impl.common.serialization.config.LiteralDatatypePolicyEnum;
import fr.inria.corese.core.next.impl.common.serialization.config.PrefixOrderingEnum;
import fr.inria.corese.core.next.impl.common.serialization.util.SerializationConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatConfigTest {
    @Test
    @DisplayName("ntriplesConfig() should return correct default config for N-Triples")
    void ntriplesConfigReturnsCorrectConfig() {
        FormatConfig config = FormatConfig.ntriplesConfig();

        assertFalse(config.usePrefixes());
        assertFalse(config.autoDeclarePrefixes());
        assertFalse(config.useCompactTriples());
        assertFalse(config.useRdfTypeShortcut());
        assertFalse(config.useCollections());
        assertEquals(BlankNodeStyleEnum.NAMED, config.getBlankNodeStyle());
        assertFalse(config.prettyPrint());
        assertEquals("", config.getIndent());
        assertEquals(0, config.getMaxLineLength());
        assertFalse(config.groupBySubject());
        assertFalse(config.sortSubjects());
        assertFalse(config.sortPredicates());
        assertEquals(LiteralDatatypePolicyEnum.ALWAYS_TYPED, config.getLiteralDatatypePolicy());
        assertTrue(config.escapeUnicode());
        assertTrue(config.trailingDot());
        assertNull(config.getBaseIRI());
        assertTrue(config.stableBlankNodeIds());
        assertTrue(config.isStrictMode());
        assertTrue(config.validateURIs());
        assertFalse(config.includeContext());
        assertEquals(SerializationConstants.DEFAULT_LINE_ENDING, config.getLineEnding());
        assertTrue(config.getCustomPrefixes().isEmpty());
    }

    @Test
    @DisplayName("nquadsConfig() should return correct default config for N-Quads")
    void nquadsConfigReturnsCorrectConfig() {
        FormatConfig config = FormatConfig.nquadsConfig();

        assertFalse(config.usePrefixes());
        assertFalse(config.autoDeclarePrefixes());
        assertFalse(config.useCompactTriples());
        assertFalse(config.useRdfTypeShortcut());
        assertFalse(config.useCollections());
        assertEquals(BlankNodeStyleEnum.NAMED, config.getBlankNodeStyle());
        assertFalse(config.prettyPrint());
        assertEquals("", config.getIndent());
        assertEquals(0, config.getMaxLineLength());
        assertFalse(config.groupBySubject());
        assertFalse(config.sortSubjects());
        assertFalse(config.sortPredicates());
        assertEquals(LiteralDatatypePolicyEnum.ALWAYS_TYPED, config.getLiteralDatatypePolicy());
        assertTrue(config.escapeUnicode());
        assertTrue(config.trailingDot());
        assertNull(config.getBaseIRI());
        assertTrue(config.stableBlankNodeIds());
        assertTrue(config.isStrictMode());
        assertTrue(config.validateURIs());
        assertTrue(config.includeContext());
        assertEquals(SerializationConstants.DEFAULT_LINE_ENDING, config.getLineEnding());
        assertTrue(config.getCustomPrefixes().isEmpty());
    }

    @Test
    @DisplayName("rdfXmlConfig() devrait retourner la configuration par défaut correcte pour RDF/XML")
    void rdfXmlConfigReturnsCorrectConfig() {
        FormatConfig config = FormatConfig.rdfXmlConfig();

        assertTrue(config.usePrefixes());
        assertTrue(config.autoDeclarePrefixes());
        assertEquals(PrefixOrderingEnum.ALPHABETICAL, config.getPrefixOrdering());
        assertFalse(config.useCompactTriples());
        assertFalse(config.useRdfTypeShortcut());
        assertFalse(config.useCollections());
        assertEquals(BlankNodeStyleEnum.NAMED, config.getBlankNodeStyle());
        assertTrue(config.prettyPrint());
        assertEquals(SerializationConstants.DEFAULT_INDENTATION, config.getIndent());
        assertEquals(0, config.getMaxLineLength());
        assertFalse(config.groupBySubject());
        assertFalse(config.sortSubjects());
        assertFalse(config.sortPredicates());
        assertEquals(LiteralDatatypePolicyEnum.ALWAYS_TYPED, config.getLiteralDatatypePolicy());
        assertFalse(config.escapeUnicode());
        assertFalse(config.trailingDot());
        assertNull(config.getBaseIRI());
        assertTrue(config.stableBlankNodeIds());
        assertFalse(config.includeContext());
        assertEquals(SerializationConstants.DEFAULT_LINE_ENDING, config.getLineEnding());


        Map<String, String> customPrefixes = config.getCustomPrefixes();
        assertFalse(customPrefixes.isEmpty());
        assertEquals(SerializationConstants.RDF_NS, customPrefixes.get("rdf"));
        assertEquals(SerializationConstants.RDFS_NS, customPrefixes.get("rdfs"));
        assertEquals(SerializationConstants.XSD_NS, customPrefixes.get("xsd"));
        assertEquals(SerializationConstants.OWL_NS, customPrefixes.get("owl"));
    }



    @Test
    @DisplayName("FormatConfig constructor should be private and only accessible via builder")
    void constructorIsPrivateAndAccessibleViaBuilder() {

        FormatConfig config = new FormatConfig.Builder().build();
        assertNotNull(config);
    }

    @Test
    @DisplayName("blankNodeStyle method in Builder should throw NullPointerException for null style")
    void blankNodeStyleShouldThrowForNull() {
        FormatConfig.Builder builder = new FormatConfig.Builder();
        assertThrows(NullPointerException.class, () -> builder.blankNodeStyle(null),
                "Setting a null blankNodeStyle should throw NullPointerException");
    }

    @Test
    @DisplayName("indent method in Builder should throw NullPointerException for null indent")
    void indentShouldThrowForNull() {
        FormatConfig.Builder builder = new FormatConfig.Builder();
        assertThrows(NullPointerException.class, () -> builder.indent(null),
                "Setting a null indent should throw NullPointerException");
    }

    @Test
    @DisplayName("lineEnding method in Builder should throw NullPointerException for null lineEnding")
    void lineEndingShouldThrowForNull() {
        FormatConfig.Builder builder = new FormatConfig.Builder();
        assertThrows(NullPointerException.class, () -> builder.lineEnding(null),
                "Setting a null lineEnding should throw NullPointerException");
    }

    @Test
    @DisplayName("prefixOrdering method in Builder should throw NullPointerException for null prefixOrdering")
    void prefixOrderingShouldThrowForNull() {
        FormatConfig.Builder builder = new FormatConfig.Builder();
        assertThrows(NullPointerException.class, () -> builder.prefixOrdering(null),
                "Setting a null prefixOrdering should throw NullPointerException");
    }

    @Test
    @DisplayName("Builder should create FormatConfig with all default values")
    void builderShouldCreateWithAllDefaults() {
        FormatConfig config = new FormatConfig.Builder().build();

        assertNotNull(config, "FormatConfig should not be null");

        // --- Syntax Sugar Defaults ---
        assertTrue(config.usePrefixes(), "Default usePrefixes should be true");
        assertTrue(config.autoDeclarePrefixes(), "Default autoDeclarePrefixes should be true");
        assertEquals(PrefixOrderingEnum.ALPHABETICAL, config.getPrefixOrdering(), "Default prefixOrdering should be ALPHABETICAL");
        assertTrue(config.getCustomPrefixes().isEmpty(), "Default customPrefixes map should be empty");
        assertTrue(config.useCompactTriples(), "Default useCompactTriples should be true");
        assertTrue(config.useRdfTypeShortcut(), "Default useRdfTypeShortcut should be true");
        assertFalse(config.useCollections(), "Default useCollections should be false");
        assertEquals(BlankNodeStyleEnum.NAMED, config.getBlankNodeStyle(), "Default blankNodeStyle should be NAMED");

        // --- Pretty-Printing Defaults ---
        assertTrue(config.prettyPrint(), "Default prettyPrint should be true");
        assertEquals(SerializationConstants.DEFAULT_INDENTATION, config.getIndent(), "Default indent should be " + SerializationConstants.DEFAULT_INDENTATION);
        assertEquals(80, config.getMaxLineLength(), "Default maxLineLength should be 80");
        assertTrue(config.groupBySubject(), "Default groupBySubject should be true");
        assertFalse(config.sortSubjects(), "Default sortSubjects should be false");
        assertFalse(config.sortPredicates(), "Default sortPredicates should be false");

        // --- Technical Output Defaults ---
        assertEquals(LiteralDatatypePolicyEnum.MINIMAL, config.getLiteralDatatypePolicy(), "Default literalDatatypePolicy should be MINIMAL");
        assertFalse(config.escapeUnicode(), "Default escapeUnicode should be false");
        assertTrue(config.trailingDot(), "Default trailingDot should be true");
        assertNull(config.getBaseIRI(), "Default baseIRI should be null");
        assertFalse(config.stableBlankNodeIds(), "Default stableBlankNodeIds should be false");

        // --- Validation & Context Defaults ---
        assertTrue(config.isStrictMode(), "Default strictMode should be true");
        assertTrue(config.validateURIs(), "Default validateURIs should be true");
        assertFalse(config.includeContext(), "Default includeContext should be false");
        assertEquals(SerializationConstants.DEFAULT_LINE_ENDING, config.getLineEnding(), "Default lineEnding should be " + SerializationConstants.DEFAULT_LINE_ENDING);
    }
}
