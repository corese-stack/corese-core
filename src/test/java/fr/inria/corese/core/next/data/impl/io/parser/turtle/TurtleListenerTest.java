package fr.inria.corese.core.next.data.impl.io.parser.turtle;

import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserTestBase;
import fr.inria.corese.core.next.generated.antlr.TurtleLexer;
import fr.inria.corese.core.next.generated.antlr.TurtleParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the TurtleListenerImpl parser.
 */
class TurtleListenerTest extends ParserTestBase {

    private static final Logger logger = LoggerFactory.getLogger(TurtleListenerTest.class);

    /**
     * Parses a Turtle string and returns the RDF model.
     *
     * @param turtleData Turtle syntax input as a string
     * @return parsed RDF model
     * @throws Exception if parsing fails
     */
    private Model parseAndPrintModel(String turtleData) throws Exception {
        CharStream input = CharStreams.fromReader(new StringReader(turtleData));
        TurtleLexer lexer = new TurtleLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TurtleParser parser = new TurtleParser(tokens);
        ParseTree tree = parser.turtleDoc();

        Model model = createTestModel();

        TurtleListener listener = new TurtleListener(model, valueFactory, null);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);

        return model;
    }

    @ParameterizedTest
    @MethodSource("provideTurtleTestCases")
    void testTurtleParsing(String turtleData, int expectedTriples, int expectedNamespaces) throws Exception {
        Model model = parseAndPrintModel(turtleData);
        assertEquals(expectedTriples, model.size(), "Triple count mismatch");
        assertEquals(expectedNamespaces, model.getNamespaces().size(), "Namespace count mismatch");
    }

    private static Stream<Arguments> provideTurtleTestCases() {
        return Stream.of(
            Arguments.of(
                "@prefix ex: <http://example.org/> .\nex:subject ex:predicate 1 .",
                1, 1
            ),
            Arguments.of(
                """
                @prefix ex: <http://example.org/> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                ex:subject ex:age "27"^^xsd:integer .
                """,
                1, 2
            ),
            Arguments.of(
                """
                @prefix ex: <http://example.org/> .
                ex:subject ex:knows ex:Alice , ex:Bob ;
                           ex:likes ex:Pizza .
                """,
                3, 1
            ),
            Arguments.of(
                """
                @prefix ex: <http://example.org/> .
                ex:Alice a ex:Person .
                ex:subject ex:knows ex:Alice , ex:Bob ;
                           ex:likes ex:Pizza .
                """,
                4, 1
            )
        );
    }

    @Test
    void testBaseIRI() throws Exception {
        String turtleData = """
            @base <http://example.org/base/> .
            @prefix : <http://example.org/prefix/> .
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

            <http://example.org/prefix/Name> rdf:type rdf:Property .
            :phone rdf:type rdf:Property .
            """;

        Model model = parseAndPrintModel(turtleData);
        assertEquals(2, model.size(), "Triple count mismatch");
        assertEquals(2, model.getNamespaces().size(), "Namespace count mismatch");
    }

    @Test
    void testTypedIntegerLiteral() throws Exception {
        String turtleData = """
            @prefix : <http://example.org/> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            :John :age "42"^^xsd:integer .
            """;

        Model model = parseAndPrintModel(turtleData);
        model.objects().forEach(obj -> {
            assertTrue(obj.isLiteral(), "Expected object to be a literal");
            assertDoesNotThrow(() -> {
                int value = Integer.parseInt(obj.stringValue());
                logger.info("Parsed integer: {}", value);
            }, "Literal should be a valid integer: " + obj.stringValue());
        });

        assertEquals(1, model.size(), "Triple count mismatch");
        assertEquals(2, model.getNamespaces().size(), "Namespace count mismatch");
    }
}