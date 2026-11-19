package fr.inria.corese.core.next.io;

import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializerOptions;
import fr.inria.corese.core.next.data.api.io.serializer.option.LiteralDatatypePolicy;
import fr.inria.corese.core.next.data.api.model.Model;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SerializationProfileTest {

    // --- Profile option values ---

    @Test
    void compactProfileHasPrettyPrintDisabled() {
        RDFSerializerOptions opts = SerializationProfile.COMPACT.toOptions();
        assertFalse(opts.prettyPrint());
    }

    @Test
    void compactProfileHasSortingDisabled() {
        RDFSerializerOptions opts = SerializationProfile.COMPACT.toOptions();
        assertFalse(opts.sortSubjects());
        assertFalse(opts.sortPredicates());
        assertFalse(opts.sortObjects());
    }

    @Test
    void prettyProfileHasPrettyPrintEnabled() {
        RDFSerializerOptions opts = SerializationProfile.PRETTY.toOptions();
        assertTrue(opts.prettyPrint());
        assertFalse(opts.sortSubjects());
    }

    @Test
    void canonicalProfileHasAllSortFlagsEnabled() {
        RDFSerializerOptions opts = SerializationProfile.CANONICAL.toOptions();
        assertTrue(opts.sortSubjects());
        assertTrue(opts.sortPredicates());
        assertTrue(opts.sortObjects());
    }

    @Test
    void canonicalProfileHasStableBlankNodeIds() {
        RDFSerializerOptions opts = SerializationProfile.CANONICAL.toOptions();
        assertTrue(opts.stableBlankNodeIds());
    }

    @Test
    void canonicalProfileHasAlwaysTypedDatatypePolicy() {
        RDFSerializerOptions opts = SerializationProfile.CANONICAL.toOptions();
        assertEquals(LiteralDatatypePolicy.ALWAYS_TYPED, opts.getLiteralDatatypePolicy());
    }

    // --- CoreseIO integration ---

    @Test
    void canonicalOutputIsIdempotent() {
        Model model = buildSimpleModel();

        String first = CoreseIO.writeToString(model, RDFFormat.TURTLE, SerializationProfile.CANONICAL);
        String second = CoreseIO.writeToString(model, RDFFormat.TURTLE, SerializationProfile.CANONICAL);

        assertEquals(first, second, "CANONICAL output must be byte-for-byte identical across calls");
    }

    @Test
    void canonicalSubjectsAreSortedInTurtle() {
        Model model = buildSimpleModel();

        String output = CoreseIO.writeToString(model, RDFFormat.TURTLE, SerializationProfile.CANONICAL);

        int posA = output.indexOf("aaa");
        int posB = output.indexOf("bbb");
        assertTrue(posA >= 0 && posB >= 0, "output should contain both subjects");
        assertTrue(posA < posB, "aaa should appear before bbb in canonical Turtle");
    }

    @Test
    void canonicalNTriplesOutputIsIdempotent() {
        Model model = buildSimpleModel();

        String first = CoreseIO.writeToString(model, RDFFormat.NTRIPLES, SerializationProfile.CANONICAL);
        String second = CoreseIO.writeToString(model, RDFFormat.NTRIPLES, SerializationProfile.CANONICAL);

        assertEquals(first, second, "CANONICAL N-Triples output must be byte-for-byte identical");
    }

    @Test
    void canonicalNTriplesLinesAreSorted() {
        Model model = buildSimpleModel();

        String output = CoreseIO.writeToString(model, RDFFormat.NTRIPLES, SerializationProfile.CANONICAL);
        String[] lines = output.strip().split("\n");

        assertTrue(lines.length >= 2, "should have at least 2 lines");
        assertTrue(lines[0].compareTo(lines[1]) <= 0, "lines should be in lexicographic order");
    }

    @Test
    void writeWithProfileToWriterProducesOutput() {
        Model model = buildSimpleModel();
        java.io.StringWriter writer = new java.io.StringWriter();

        CoreseIO.write(model, RDFFormat.TURTLE, SerializationProfile.PRETTY, writer);

        assertFalse(writer.toString().isEmpty(), "output should not be empty");
    }

    // --- Helpers ---

    private static Model buildSimpleModel() {
        // Insert bbb before aaa to verify sort actually reorders them
        String turtle = """
                <http://example.org/bbb> <http://example.org/p> <http://example.org/o> .
                <http://example.org/aaa> <http://example.org/p> <http://example.org/o> .
                """;
        return CoreseIO.read(new StringReader(turtle), RDFFormat.NTRIPLES);
    }
}
