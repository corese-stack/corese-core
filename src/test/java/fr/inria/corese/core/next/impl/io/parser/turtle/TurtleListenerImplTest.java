package fr.inria.corese.core.next.impl.io.parser.turtle;

import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.impl.parser.antlr.TurtleLexer;
import fr.inria.corese.core.next.impl.parser.antlr.TurtleParser;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the TurtleListenerImpl parser.
 */
class TurtleListenerImplTest {

    private static final Logger logger = LoggerFactory.getLogger(TurtleListenerImplTest.class);

    /**
     * Parses a Turtle string and returns the RDF model.
     *
     * @param turtleData Turtle syntax input as a string
     * @return parsed RDF model
     * @throws Exception if parsing fails
     */
    private Model parseAndPrintModel(String turtleData) throws Exception {
        ValueFactory factory = new CoreseAdaptedValueFactory();

        CharStream input = CharStreams.fromReader(new StringReader(turtleData));
        TurtleLexer lexer = new TurtleLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TurtleParser parser = new TurtleParser(tokens);
        ParseTree tree = parser.turtleDoc();

        Model model = new CoreseModel();
        TurtleListenerImpl listener = new TurtleListenerImpl(model, factory, null);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk((ParseTreeListener) listener, tree);

        return model;
    }

    @Test
    void testNamespace() throws Exception {
        String turtleData = "@prefix ex: <http://example.org/> .\n" +
                "ex:subject ex:predicate 1 .";

        Model model = parseAndPrintModel(turtleData);
        assertEquals(1, model.getNamespaces().size(), "Namespace count mismatch");
    }

    @Test
    void testTypedLiteral() throws Exception {
        String turtleData = """
            @prefix ex: <http://example.org/> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            ex:subject ex:age "27"^^xsd:integer .
            """;

        Model model = parseAndPrintModel(turtleData);
        assertEquals(1, model.size(), "Triple count mismatch");
        assertEquals(2, model.getNamespaces().size(), "Namespace count mismatch");
    }

    @Test
    void testMultipleObjects() throws Exception {
        String turtleData = """
            @prefix ex: <http://example.org/> .
            ex:subject ex:knows ex:Alice , ex:Bob ;
                       ex:likes ex:Pizza .
            """;

        Model model = parseAndPrintModel(turtleData);
        assertEquals(3, model.size(), "Triple count mismatch");
        assertEquals(1, model.getNamespaces().size(), "Namespace count mismatch");
    }

    @Test
    void testRDFtype() throws Exception {
        String turtleData = """
            @prefix ex: <http://example.org/> .
            ex:Alice a ex:Person .
            ex:subject ex:knows ex:Alice , ex:Bob ;
                       ex:likes ex:Pizza .
            """;

        Model model = parseAndPrintModel(turtleData);
        assertEquals(4, model.size(), "Triple count mismatch");
        assertEquals(1, model.getNamespaces().size(), "Namespace count mismatch");
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
            try {
                int value = Integer.parseInt(obj.stringValue());
                logger.info("Parsed integer: {}", value);
            } catch (NumberFormatException e) {
                fail("Literal is not a valid integer: " + obj.stringValue());
            }
        });

        assertEquals(1, model.size(), "Triple count mismatch");
        assertEquals(2, model.getNamespaces().size(), "Namespace count mismatch");
    }
}
