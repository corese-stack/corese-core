package fr.inria.corese.core.next.data.impl.io.parser.turtle;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.io.parser.RDFParser;
import fr.inria.corese.core.next.data.api.vocabulary.RDFS;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserTestBase;
import fr.inria.corese.core.next.data.impl.adapter.CoreseValueFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the ANTLRTurtle class.
 * These tests verify the parser's ability to correctly parse Turtle
 * and interact with the Model and ValueFactory, including error handling
 * and unescaping of IRIs and literals, and named graphs.
 */
class TurtleParserTest extends ParserTestBase {

    private final ValueFactory factory = new CoreseValueFactory();

    @Test
    void testParseWithPrefixAndTriple() {
        String turtle = """
                @prefix ex: <http://example.org/> .
                ex:Alice ex:knows ex:Bob .""";

        Model model = createTestModel();
        RDFParser parser = new TurtleParser(model, factory, new TurtleParserOptions.Builder().baseIRI("http://inria.fr/").build());
        parser.parse(new StringReader(turtle));
        assertEquals(1, model.size());
        assertEquals(1, model.getNamespaces().size()); // Should contains ex: and the relative base uri :
    }

    @ParameterizedTest
    @MethodSource("provideTripleQuoteTestCases")
    void testTripleQuoteLiteralWithDoubleQuotes(String turtleInput, String expectedLiteralValue) {
        Model model = createTestModel();
        RDFParser parser = new TurtleParser(model, factory, new TurtleParserOptions.Builder().baseIRI("http://inria.fr/").build());
        parser.parse(new StringReader(turtleInput));
        assertEquals(1, model.size());
        assertTrue(model.contains(factory.createIRI("http://inria.fr/#0214"), RDFS.comment.getIRI(), factory.createLiteral(expectedLiteralValue)));
    }

    private static Stream<Arguments> provideTripleQuoteTestCases() {
        return Stream.of(
            Arguments.of(
                """
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                <#0214>\s
                    rdfs:comment \"""blabla"abc""\"" .""",
                "blabla\"abc\""
            ),
            Arguments.of(
                """
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                <#0214>\s
                    rdfs:comment ""\""abc"blabla\""" .""",
                "\"abc\"blabla"
            ),
            Arguments.of(
                """
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                <#0214>\s
                    rdfs:comment \"""blabla"abc"blabla\""" .""",
                "blabla\"abc\"blabla"
            ),
            Arguments.of(
                """
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                <#0214>\s
                    rdfs:comment ""\""abc""\"" .""",
                "\"abc\""
            )
        );
    }

    @Test
    void testRdfaManifest() {
        String turtle = """
                @base <http://rdfa.info/test-suite/test-cases/rdfa1.1/xml/manifest> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

                <#0006> rdfs:label \"""Test 0006: @rel and @rev\""";
                  rdfs:comment \"""Tests @rev and @rel together, with the object being specified by @href, ignoring content\""";
                  .
                 \s""";

        Model model = createTestModel();
        RDFParser parser = new TurtleParser(model, factory, new TurtleParserOptions.Builder().baseIRI("http://inria.fr/").build());
        parser.parse(new StringReader(turtle));

        Model refModel = createTestModel();
        refModel.add(factory.createIRI("http://rdfa.info/test-suite/test-cases/rdfa1.1/xml/manifest#0006"), RDFS.label.getIRI(), factory.createLiteral("Test 0006: @rel and @rev"));
        refModel.add(factory.createIRI("http://rdfa.info/test-suite/test-cases/rdfa1.1/xml/manifest#0006"), RDFS.comment.getIRI(), factory.createLiteral("Tests @rev and @rel together, with the object being specified by @href, ignoring content"));

        for(Statement stat : model) {
            assertTrue(refModel.contains(stat));
        }
        for(Statement stat : refModel) {
            assertTrue(model.contains(stat));
        }

        assertEquals(refModel.size(), model.size());
    }

}
