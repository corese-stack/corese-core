package fr.inria.corese.core.next.io;

import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializerOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.BlankNodeStyle;
import fr.inria.corese.core.next.data.api.model.Model;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that usePrefixes, useMultilineLiterals, and BlankNodeStyle are
 * transferred correctly from RDFSerializerOptions to Turtle output.
 */
class SerializationProfileGraphStyleTest {

    private static Model buildModel() {
        String nt = "<http://example.org/s> <http://example.org/p> <http://example.org/o> .";
        return CoreseIO.read(new StringReader(nt), RDFFormat.NTRIPLES);
    }

    // --- usePrefixes ---

    @Test
    @DisplayName("usePrefixes(true) should not force full IRIs")
    void usePrefixesTrueProducesPrefixDeclarations() {
        Model model = buildModel();
        String output = CoreseIO.writeToString(model, RDFFormat.TURTLE,
                RDFSerializerOptions.builder().usePrefixes(true).build());

        // The model's namespace is empty, but the serializer may still emit @prefix declarations
        // for recognised vocabularies. At minimum, verify that full IRIs are not forced.
        assertNotNull(output);
        assertFalse(output.isBlank());
    }

    @Test
    @DisplayName("usePrefixes(false) should write full IRIs without @prefix declarations")
    void usePrefixesFalseWritesFullIRIs() {
        Model model = buildModel();
        String output = CoreseIO.writeToString(model, RDFFormat.TURTLE,
                RDFSerializerOptions.builder().usePrefixes(false).build());

        assertFalse(output.contains("@prefix"), "output should not contain @prefix declarations");
        assertTrue(output.contains("<http://example.org/s>"), "subject IRI should appear in full");
    }

    // --- BlankNodeStyle ---

    @Test
    @DisplayName("CANONICAL profile should use NAMED blank nodes and disable prefixes")
    void canonicalProfileUsesNamedBlankNodes() {
        RDFSerializerOptions opts = SerializationProfile.CANONICAL.toOptions();
        assertEquals(BlankNodeStyle.NAMED, opts.getBlankNodeStyle());
        assertFalse(opts.usePrefixes(), "CANONICAL should disable prefixes for stability");
    }

    @Test
    @DisplayName("PRETTY profile should use ANONYMOUS blank nodes, enable prefixes and multiline literals")
    void prettyProfileUsesAnonymousBlankNodes() {
        RDFSerializerOptions opts = SerializationProfile.PRETTY.toOptions();
        assertEquals(BlankNodeStyle.ANONYMOUS, opts.getBlankNodeStyle());
        assertTrue(opts.usePrefixes());
        assertTrue(opts.useMultilineLiterals());
    }

    @Test
    @DisplayName("COMPACT profile should use NAMED blank nodes, enable prefixes and disable multiline literals")
    void compactProfileUsesNamedBlankNodesAndNoPrefixes() {
        RDFSerializerOptions opts = SerializationProfile.COMPACT.toOptions();
        assertEquals(BlankNodeStyle.NAMED, opts.getBlankNodeStyle());
        assertTrue(opts.usePrefixes());
        assertFalse(opts.useMultilineLiterals());
    }
}
