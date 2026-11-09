package fr.inria.corese.core.next.data.impl.io.serializer;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.data.api.io.serializer.RDFSerializer;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.impl.io.parser.DefaultRDFParserFactory;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserTestBase;
import fr.inria.corese.core.next.data.impl.io.serializer.rdfxml.RDFXMLSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.trig.TriGSerializerOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.turtle.TurtleSerializerOptions;
import fr.inria.corese.core.next.data.Values;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying that model-declared namespace prefixes are preserved
 * through serialization round-trips (issue #631).
 */
@DisplayName("Prefix round-trip tests")
class PrefixRoundTripTest extends ParserTestBase {

    private static final String CUSTOM_NS = "http://example.org/custom/ns#";
    private static final String CUSTOM_PREFIX = "myprefix";

    private ValueFactory valueFactory;
    private DefaultRDFParserFactory parserFactory;
    private DefaultRDFSerializerFactory serializerFactory;

    @BeforeEach
    void setUp() {
        valueFactory = Values.factory();
        parserFactory = new DefaultRDFParserFactory();
        serializerFactory = new DefaultRDFSerializerFactory();
    }

    private Model parseInput(String turtle, RDFFormat format) {
        Model model = createTestModel();
        RDFParser parser = parserFactory.createRDFParser(format, model, valueFactory);
        parser.parse(new ByteArrayInputStream(turtle.getBytes(StandardCharsets.UTF_8)));
        return model;
    }

    private String serialize(Model model, RDFFormat format, TurtleSerializerOptions config) {
        RDFSerializer serializer = serializerFactory.createSerializer(format, model, config);
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        return writer.toString();
    }

    private String serialize(Model model, RDFFormat format, TriGSerializerOptions config) {
        RDFSerializer serializer = serializerFactory.createSerializer(format, model, config);
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        return writer.toString();
    }

    private String serialize(Model model, RDFFormat format, RDFXMLSerializerOptions config) {
        RDFSerializer serializer = serializerFactory.createSerializer(format, model, config);
        StringWriter writer = new StringWriter();
        serializer.write(writer);
        return writer.toString();
    }

    @Test
    @DisplayName("Turtle round-trip preserves custom prefix")
    void testTurtleRoundTripPreservesPrefix() {
        String input = "@prefix " + CUSTOM_PREFIX + ": <" + CUSTOM_NS + "> .\n"
                + CUSTOM_PREFIX + ":item1 " + CUSTOM_PREFIX + ":prop \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);
        String output = serialize(model, RDFFormat.TURTLE, TurtleSerializerOptions.defaultConfig());

        assertTrue(output.contains(CUSTOM_PREFIX + ":"),
                "Serialized Turtle should contain declared prefix '" + CUSTOM_PREFIX + ":'\n" + output);
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
    }

    @Test
    @DisplayName("TriG round-trip preserves custom prefix")
    void testTriGRoundTripPreservesPrefix() {
        String input = "@prefix " + CUSTOM_PREFIX + ": <" + CUSTOM_NS + "> .\n"
                + CUSTOM_PREFIX + ":item1 " + CUSTOM_PREFIX + ":prop \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TRIG);
        String output = serialize(model, RDFFormat.TRIG, TriGSerializerOptions.defaultConfig());

        assertTrue(output.contains(CUSTOM_PREFIX + ":"),
                "Serialized TriG should contain declared prefix '" + CUSTOM_PREFIX + ":'\n" + output);
    }

    @Test
    @DisplayName("RDF/XML round-trip preserves custom prefix")
    void testRDFXMLRoundTripPreservesPrefix() {
        String input = "@prefix " + CUSTOM_PREFIX + ": <" + CUSTOM_NS + "> .\n"
                + CUSTOM_PREFIX + ":item1 " + CUSTOM_PREFIX + ":prop \"value\" .\n";

        Model model = parseInput(input, RDFFormat.TURTLE);
        String output = serialize(model, RDFFormat.RDFXML, RDFXMLSerializerOptions.defaultConfig());

        assertTrue(output.contains(CUSTOM_PREFIX),
                "Serialized RDF/XML should contain declared prefix '" + CUSTOM_PREFIX + "'\n" + output);
    }
}
