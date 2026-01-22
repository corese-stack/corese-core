package fr.inria.corese.core.next.impl.io.parser.turtle;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.api.io.parser.RDFParser;
import fr.inria.corese.core.next.impl.common.vocabulary.RDFS;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the ANTLRTurtle class.
 * These tests verify the parser's ability to correctly parse Turtle
 * and interact with the Model and ValueFactory, including error handling
 * and unescaping of IRIs and literals, and named graphs.
 */
public class TurtleParserTest {

    private ValueFactory factory = new CoreseAdaptedValueFactory();

    @Test
    public void testParseWithPrefixAndTriple() {
        String turtle = " @prefix ex: <http://example.org/> . " +
            "ex:Alice ex:knows ex:Bob .";

        Model model = new CoreseModel();
        RDFParser parser = new TurtleParser(model, factory, new TurtleParserOptions.Builder().baseIRI("http://inria.fr/").build());
        parser.parse(new StringReader(turtle));
        assertEquals(1, model.size());
        assertEquals(1, model.getNamespaces().size()); // Should contains ex: and the relative base uri :
    }

    @Test
    public void testTripleQuoteLiteralWithDoubleQuoteIncludedTextBefore() {
        String turtle = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "<#0214> \n" +
                "    rdfs:comment \"\"\"blabla\"abc\"\"\"\" .";

        Model model = new CoreseModel();
        RDFParser parser = new TurtleParser(model, factory, new TurtleParserOptions.Builder().baseIRI("http://inria.fr/").build());
        parser.parse(new StringReader(turtle));
        assertEquals(1, model.size());
        model.contains(factory.createIRI("http://inria.fr/#0214"), RDFS.comment.getIRI(), factory.createLiteral("blabla\"abc\""));
    }

    @Test
    public void testTripleQuoteLiteralWithDoubleQuoteIncludedTextAfter() {
        String turtle = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "<#0214> \n" +
                "    rdfs:comment \"\"\"\"abc\"blabla\"\"\" .";

        Model model = new CoreseModel();
        RDFParser parser = new TurtleParser(model, factory, new TurtleParserOptions.Builder().baseIRI("http://inria.fr/").build());
        parser.parse(new StringReader(turtle));
        assertEquals(1, model.size());
        model.contains(factory.createIRI("http://inria.fr/#0214"), RDFS.comment.getIRI(), factory.createLiteral("\"abc\"blabla"));
    }

    @Test
    public void testTripleQuoteLiteralWithDoubleQuoteIncludedTextBeforeAndAfter() {
        String turtle = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "<#0214> \n" +
                "    rdfs:comment \"\"\"blabla\"abc\"blabla\"\"\" .";

        Model model = new CoreseModel();
        RDFParser parser = new TurtleParser(model, factory, new TurtleParserOptions.Builder().baseIRI("http://inria.fr/").build());
        parser.parse(new StringReader(turtle));
        assertEquals(1, model.size());
        model.contains(factory.createIRI("http://inria.fr/#0214"), RDFS.comment.getIRI(), factory.createLiteral("blabla\"abc\"blabla"));
    }

    @Test
    public void testTripleQuoteLiteralWithDoubleQuoteIncludedNoText() {
        String turtle = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "<#0214> \n" +
                "    rdfs:comment \"\"\"\"abc\"\"\"\" .";

        Model model = new CoreseModel();
        RDFParser parser = new TurtleParser(model, factory, new TurtleParserOptions.Builder().baseIRI("http://inria.fr/").build());
        parser.parse(new StringReader(turtle));
        assertEquals(1, model.size());
        model.contains(factory.createIRI("http://inria.fr/#0214"), RDFS.comment.getIRI(), factory.createLiteral("\"abc\""));
    }

}
