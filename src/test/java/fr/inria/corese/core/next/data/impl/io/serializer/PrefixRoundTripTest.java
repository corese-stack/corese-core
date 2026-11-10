package fr.inria.corese.core.next.data.impl.io.serializer;

import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.option.RDFSerializationOptions;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.impl.io.parser.DefaultRDFParserFactory;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserTestBase;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfxml.RDFXMLSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.trig.TriGSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.turtle.TurtleSerializerOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying that model-declared namespace prefixes are preserved
 * through serialization round-trips (issue #631).
 */
@DisplayName("Prefix round-trip tests")
class PrefixRoundTripTest extends ParserTestBase {

    private static final String CUSTOM_NS = "http://example.org/custom/ns#";
    private static final String CUSTOM_PREFIX = "myprefix";

    private DefaultRDFParserFactory parserFactory;
    private DefaultRDFSerializerFactory serializerFactory;

    @BeforeEach
    void setUp() {
        parserFactory = new DefaultRDFParserFactory();
        serializerFactory = new DefaultRDFSerializerFactory();
    }

    private Model parseInput(String turtle, RDFFormat format) {
        Model model = createTestModel();
        RDFParser parser = parserFactory.createRDFParser(format, model, valueFactory);
        parser.parse(new ByteArrayInputStream(turtle.getBytes(StandardCharsets.UTF_8)));
        return model;
    }

    private String serialize(Model model, RDFFormat format, RDFSerializationOptions config) {
        RDFSerializer serializer = serializerFactory.createSerializer(format, model, config);
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        return writer.toString();
    }

    private void assertPrefixRetained(String output) {
        String expectedPrefixLine = "@prefix " + PrefixRoundTripTest.CUSTOM_PREFIX + ": <" + PrefixRoundTripTest.CUSTOM_NS + "> .";
        assertTrue(output.contains(expectedPrefixLine),
                "Serialized output should keep the model-declared prefix line: \n" + expectedPrefixLine + "\nActual output:\n" + output);
        assertFalse(output.contains("@prefix ns: <" + PrefixRoundTripTest.CUSTOM_NS + "> ."), "Serializer should not replace the original prefix with generated 'ns:'\n" + output);
    }

    // ── Core round-trip tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Turtle round-trip preserves custom prefix")
    void testTurtleRoundTripPreservesPrefix() {
        String input = "@prefix " + CUSTOM_PREFIX + ": <" + CUSTOM_NS + "> .\n"
                + CUSTOM_PREFIX + ":item1 " + CUSTOM_PREFIX + ":prop \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);
        String output = serialize(model, RDFFormat.TURTLE, TurtleSerializerOptions.defaultConfig());

        assertPrefixRetained(output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("Turtle serializer config prefix overrides model prefix on conflict")
    void testTurtleConfigPrefixOverridesModelPrefix() {
        String input = "@prefix " + CUSTOM_PREFIX + ": <" + CUSTOM_NS + "> .\n"
                + CUSTOM_PREFIX + ":item1 " + CUSTOM_PREFIX + ":prop \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);

        TurtleSerializerOptions config = TurtleSerializerOptions.builder()
                .addPrefix("override", CUSTOM_NS)
                .build();
        String output = serialize(model, RDFFormat.TURTLE, config);

        assertTrue(output.contains("override:"),
                "Config-specified prefix 'override' should override model prefix\n" + output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("TriG round-trip preserves custom prefix")
    void testTriGRoundTripPreservesPrefix() {
        String input = "@prefix " + CUSTOM_PREFIX + ": <" + CUSTOM_NS + "> .\n"
                + CUSTOM_PREFIX + ":item1 " + CUSTOM_PREFIX + ":prop \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TRIG);
        String output = serialize(model, RDFFormat.TRIG, TriGSerializerOptions.defaultConfig());

        assertPrefixRetained(output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TRIG));
    }

    @Test
    @DisplayName("RDF/XML round-trip preserves custom prefix")
    void testRDFXMLRoundTripPreservesPrefix() {
        String input = "@prefix " + CUSTOM_PREFIX + ": <" + CUSTOM_NS + "> .\n"
                + CUSTOM_PREFIX + ":item1 " + CUSTOM_PREFIX + ":prop \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);
        String output = serialize(model, RDFFormat.RDFXML, RDFXMLSerializerOptions.defaultConfig());

        assertTrue(output.contains("xmlns:" + CUSTOM_PREFIX + "=\"" + CUSTOM_NS + "\""),
                "Serialized RDF/XML should retain the model prefix '" + CUSTOM_PREFIX + "'\n" + output);
        assertFalse(output.contains("xmlns:ns=\"" + CUSTOM_NS + "\""), "Serialized RDF/XML should not overwrite the model prefix with a generated 'ns:' prefix\n" + output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.RDFXML));
    }

    @Test
    @DisplayName("IRI whose local name ends with '.' is emitted as full IRI — no dangling QName")
    void testLocalNameEndingWithDotEmitsFullIRI() {
        // <http://example.org/ns#foo.> has local name "foo." — invalid PN_LOCAL last char
        String nsWithHash = "http://example.org/ns#";
        String input = "<" + nsWithHash + "foo.> <" + nsWithHash + "prop> \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);
        TurtleSerializerOptions config = TurtleSerializerOptions.builder()
                .addPrefix("ex", nsWithHash)
                .build();
        String output = serialize(model, RDFFormat.TURTLE, config);

        // "ex:foo." is an invalid Turtle QName — must fall back to full IRI
        assertFalse(output.contains("ex:foo."),
                "Local name ending with '.' must not be shortened to a QName\n" + output);
        assertTrue(output.contains("<" + nsWithHash + "foo.>"),
                "IRI with trailing-dot local name must be written in full\n" + output);
        // Round-trip must still recover the original IRI
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("IRI whose local name starts with '-' is emitted as full IRI")
    void testLocalNameStartingWithDashEmitsFullIRI() {
        String nsWithHash = "http://example.org/ns#";
        String input = "<" + nsWithHash + "-item> <" + nsWithHash + "prop> \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);
        TurtleSerializerOptions config = TurtleSerializerOptions.builder()
                .addPrefix("ex", nsWithHash)
                .build();
        String output = serialize(model, RDFFormat.TURTLE, config);

        assertFalse(output.contains("ex:-item"),
                "Local name starting with '-' must not be shortened to a QName\n" + output);
        assertTrue(output.contains("<" + nsWithHash + "-item>"),
                "IRI with dash-leading local name must be written in full\n" + output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("IRI with non-ASCII local name is emitted as a full IRI")
    void testNonAsciiLocalNameEmitsFullIRI() {
        String nsWithHash = "http://example.org/ns#";
        String iri = nsWithHash + "foo\u00A0bar";
        String input = "<" + iri + "> <" + nsWithHash + "prop> \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);
        TurtleSerializerOptions config = TurtleSerializerOptions.builder()
                .addPrefix("ex", nsWithHash)
                .build();
        String output = serialize(model, RDFFormat.TURTLE, config);

        assertTrue(output.contains("<" + iri + ">"),
                "Non-ASCII local name must be written as a full IRI\n" + output);
        assertFalse(output.contains("ex:foo"),
                "Non-ASCII local name must not be shortened to a QName\n" + output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("Declared namespace without trailing slash/hash remains valid and declared")
    void testDeclaredNamespaceWithoutTrailingDelimiter() {
        String input = """
                @prefix item: <http://example.org/item> .
                item:123 item:prop "value" .
                """;

        Model model = parseInput(input, RDFFormat.TURTLE);
        String output = serialize(model, RDFFormat.TURTLE, TurtleSerializerOptions.defaultConfig());

        assertTrue(output.contains("@prefix item: <http://example.org/item> ."),
                "Header must declare the exact mapped namespace even when it has no trailing '/' or '#'.\n" + output);
        assertTrue(output.contains("item:123"), "Body should keep the declared prefix shortening.\n" + output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("Config prefix override keeps model namespace data intact")
    void testConfigPrefixOverrideDoesNotCorruptModelData() {
        String input = """
                <http://example.org/nsA#item1> <http://example.org/nsA#prop> "a" .
                <http://example.org/nsB#item2> <http://example.org/nsB#prop> "b" .
                """;

        Model model = parseInput(input, RDFFormat.TURTLE);
        TurtleSerializerOptions config = TurtleSerializerOptions.builder()
                .addPrefix("ex", "http://example.org/nsB#")
                .build();
        String output = serialize(model, RDFFormat.TURTLE, config);

        assertTrue(output.contains("@prefix ex: <http://example.org/nsB#> ."),
                "Explicit config prefix should be used for the configured namespace\n" + output);
        assertTrue(output.contains("@prefix nsa: <http://example.org/nsA#> ."),
                "The other model namespace should still be emitted under a valid alternate prefix\n" + output);
        assertTrue(output.contains("nsa:item1") || output.contains("<http://example.org/nsA#item1>"),
                "Model data in the other namespace must remain valid and readable\n" + output);
        assertTrue(output.contains("ex:item2") || output.contains("<http://example.org/nsB#item2>"),
                "Configured namespace data must remain intact\n" + output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("usePrefixes(false) emits full IRIs without any @prefix declarations")
    void testUsePrefixesFalseEmitsFullIRIs() {
        String input = "@prefix " + CUSTOM_PREFIX + ": <" + CUSTOM_NS + "> .\n"
                + CUSTOM_PREFIX + ":item1 " + CUSTOM_PREFIX + ":prop \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);
        TurtleSerializerOptions config = TurtleSerializerOptions.builder().usePrefixes(false).build();
        String output = serialize(model, RDFFormat.TURTLE, config);

        assertFalse(output.contains("@prefix"), "No @prefix declarations expected when usePrefixes=false\n" + output);
        assertTrue(output.contains("<" + CUSTOM_NS), "Full IRI should appear in output\n" + output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("autoDeclarePrefixes(false) without config prefix emits full IRIs — no dangling QName")
    void testAutoDeclarePrefixesFalseNoConfigPrefix() {
        String input = "@prefix " + CUSTOM_PREFIX + ": <" + CUSTOM_NS + "> .\n"
                + CUSTOM_PREFIX + ":item1 " + CUSTOM_PREFIX + ":prop \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);
        TurtleSerializerOptions config = TurtleSerializerOptions.builder()
                .autoDeclarePrefixes(false)
                .build();
        String output = serialize(model, RDFFormat.TURTLE, config);

        // Model namespace is not loaded when autoDeclarePrefixes=false → no @prefix, no QName
        assertFalse(output.contains("@prefix " + CUSTOM_PREFIX + ":"),
                "Model prefix must not be auto-declared when autoDeclarePrefixes=false\n" + output);
        assertFalse(output.contains(CUSTOM_PREFIX + ":item1"),
                "IRI must not be shortened to a dangling QName\n" + output);
        // Full IRIs are used → output remains re-parseable
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("autoDeclarePrefixes(false) with explicit config prefix uses and declares only that prefix")
    void testAutoDeclarePrefixesFalseWithConfigPrefix() {
        String input = "@prefix " + CUSTOM_PREFIX + ": <" + CUSTOM_NS + "> .\n"
                + CUSTOM_PREFIX + ":item1 " + CUSTOM_PREFIX + ":prop \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);
        TurtleSerializerOptions config = TurtleSerializerOptions.builder()
                .autoDeclarePrefixes(false)
                .addPrefix("explicit", CUSTOM_NS)
                .build();
        String output = serialize(model, RDFFormat.TURTLE, config);

        assertTrue(output.contains("@prefix explicit: <" + CUSTOM_NS + "> ."),
                "Explicitly configured prefix must be declared\n" + output);
        assertTrue(output.contains("explicit:"),
                "Config prefix must be used for shortening\n" + output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("Empty default prefix (':') is preserved in Turtle round-trip")
    void testEmptyDefaultPrefixRoundTrip() {
        String input = """
                @prefix : <http://example.org/> .
                :item1 :prop "value" .
                """;

        Model model = parseInput(input, RDFFormat.TURTLE);
        String output = serialize(model, RDFFormat.TURTLE, TurtleSerializerOptions.defaultConfig());

        assertTrue(output.contains("@prefix : <http://example.org/> ."),
                "Empty default prefix declaration should be preserved\n" + output);
        assertTrue(output.contains(":item1") || output.contains(":prop"),
                "IRIs should use the empty prefix shorthand\n" + output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("Config prefix wins over model prefix when both map the same namespace")
    void testConfigPrefixWinsOnConflict() {
        // Model declares "myprefix: <CUSTOM_NS>"
        String input = "@prefix " + CUSTOM_PREFIX + ": <" + CUSTOM_NS + "> .\n"
                + CUSTOM_PREFIX + ":item1 " + CUSTOM_PREFIX + ":prop \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);

        // Config declares a different prefix for the same namespace
        TurtleSerializerOptions config = TurtleSerializerOptions.builder()
                .addPrefix("configprefix", CUSTOM_NS)
                .build();
        String output = serialize(model, RDFFormat.TURTLE, config);

        assertTrue(output.contains("configprefix:"),
                "Config prefix 'configprefix' should win over model prefix '" + CUSTOM_PREFIX + "'\n" + output);
        assertFalse(output.contains(CUSTOM_PREFIX + ":"),
                "Model prefix '" + CUSTOM_PREFIX + "' should be overridden\n" + output);
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }

    @Test
    @DisplayName("Same prefix 'ex' bound to namespace A in model and namespace B in config — config wins, both namespaces remain intact")
    void testSamePrefixDistinctNamespacesConfigWins() {
        // Model: ex: → NS_A, with IRIs from both NS_A and NS_B
        String nsA = "http://example.org/nsA#";
        String nsB = "http://example.org/nsB#";
        String input = "@prefix ex: <" + nsA + "> .\n"
                + "ex:subjectA ex:predA <" + nsB + "objectB> .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);

        // Config also declares ex: but pointing to NS_B (conflict)
        TurtleSerializerOptions config = TurtleSerializerOptions.builder()
                .addPrefix("ex", nsB)
                .build();
        String output = serialize(model, RDFFormat.TURTLE, config);

        // Config prefix wins: ex: must resolve to NS_B in the header
        assertTrue(output.contains("@prefix ex: <" + nsB + "> ."),
                "Config binding of 'ex' to NS_B should win\n" + output);
        // NS_A IRIs must not be corrupted — they appear as full IRIs or under a generated prefix
        assertFalse(output.contains("ex:subjectA") && !output.contains("@prefix ex: <" + nsA),
                "NS_A IRI must not be shortened with 'ex:' when ex: now resolves to NS_B\n" + output);
        // Round-trip: model data must survive intact
        assertModelsIsomorphic(model, parseInput(output, RDFFormat.TURTLE));
    }
}
