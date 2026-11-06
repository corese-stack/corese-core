package fr.inria.corese.core.next.data.impl.io.parser.support;

import fr.inria.corese.core.next.data.api.exception.ParsingException;
import fr.inria.corese.core.next.data.api.io.format.RDFFormat;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.impl.io.parser.DefaultRDFParserFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.StringReader;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integration tests for document scope and incremental Turtle/TriG input. */
class ScannerDocumentTest extends ParserTestBase {

    static Stream<RDFFormat> formats() {
        return Stream.of(RDFFormat.TURTLE, RDFFormat.TRIG);
    }

    private RDFParser parser(RDFFormat format, Model model) {
        return new DefaultRDFParserFactory().createRDFParser(format, model, valueFactory);
    }

    @ParameterizedTest
    @MethodSource("formats")
    void repeatedBlankNodeLabelIdentifiesOneNodeWithinDocument(RDFFormat format) {
        Model model = createTestModel();
        parser(format, model).parse(new StringReader("""
                _:x <http://example.org/p> _:x .
                _:x <http://example.org/q> "value" .
                """));

        assertEquals(2, model.size());
        assertEquals(1, model.subjects().size());
        assertTrue(model.subjects().iterator().next().isBNode());
        assertTrue(model.contains(model.subjects().iterator().next(),
                valueFactory.createIRI("http://example.org/p"),
                model.subjects().iterator().next()));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void differentBlankNodeLabelsIdentifyDifferentNodes(RDFFormat format) {
        Model model = createTestModel();
        parser(format, model).parse(new StringReader("""
                _:x <http://example.org/p> "first" .
                _:y <http://example.org/p> "second" .
                """));

        assertEquals(2, model.subjects().size());
        assertTrue(model.subjects().stream().allMatch(Resource::isBNode));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void reusedParserScopesBlankNodeLabelsToEachDocument(RDFFormat format) {
        Model model = createTestModel();
        RDFParser parser = parser(format, model);
        String document = "_:x <http://example.org/p> _:x .";
        parser.parse(new StringReader(document));
        parser.parse(new StringReader(document));

        assertEquals(2, model.size());
        assertEquals(2, model.subjects().size());
        model.forEach(statement -> assertEquals(statement.getSubject(), statement.getObject()));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void separateParsersScopeBlankNodeLabelsToEachDocument(RDFFormat format) {
        Model model = createTestModel();
        String document = "_:x <http://example.org/p> _:x .";
        parser(format, model).parse(new StringReader(document));
        parser(format, model).parse(new StringReader(document));

        assertEquals(2, model.size());
        assertEquals(2, model.subjects().size());
    }

    @Test
    void trigSharesBlankNodeLabelsAcrossGraphLabelsAndTriples() {
        Model model = createTestModel();
        parser(RDFFormat.TRIG, model).parse(new StringReader("""
                GRAPH _:x { _:x <http://example.org/p> _:y . }
                GRAPH <http://example.org/g> { _:y <http://example.org/p> _:x . }
                """));

        assertEquals(2, model.size());
        assertEquals(2, model.subjects().size());
        var first = model.stream()
                .filter(statement -> statement.getContext().isBNode())
                .findFirst().orElseThrow();
        assertEquals(first.getSubject(), first.getContext());
        assertTrue(model.contains((Resource) first.getObject(), first.getPredicate(),
                first.getSubject(), valueFactory.createIRI("http://example.org/g")));
    }

    @ParameterizedTest
    @MethodSource("formats")
    void reusedParserDoesNotKeepDocumentPrefixes(RDFFormat format) {
        Model model = createTestModel();
        RDFParser parser = parser(format, model);
        parser.parse(new StringReader("@prefix ex: <http://example.org/> . ex:s ex:p ex:o ."));

        StringReader reader = new StringReader("ex:s ex:p ex:o .");
        assertThrows(ParsingException.class, () -> parser.parse(reader));
        assertEquals(1, model.size());
    }

    @ParameterizedTest
    @MethodSource("formats")
    void dottedPrefixAndBooleanWorkAcrossReaderChunks(RDFFormat format) {
        Model model = createTestModel();
        String document = "@prefix ex..ample: <http://example.org/> . ex..ample:s ex..ample:p true.";
        try (var reader = new StringReader(document) {
            @Override
            public int read(char[] buffer, int offset, int length) throws java.io.IOException {
                return super.read(buffer, offset, Math.min(length, 1));
            }
        }) {
            parser(format, model).parse(reader);
        }

        assertEquals(1, model.size());
        assertTrue(model.contains(valueFactory.createIRI("http://example.org/s"),
                valueFactory.createIRI("http://example.org/p"), valueFactory.createLiteral(true)));
    }
}
