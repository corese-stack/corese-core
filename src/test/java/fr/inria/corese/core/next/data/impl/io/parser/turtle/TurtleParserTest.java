package fr.inria.corese.core.next.data.impl.io.parser.turtle;

import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.io.parser.RDFParser;
import fr.inria.corese.core.next.data.impl.common.vocabulary.RDFS;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseModel;
import org.junit.jupiter.api.Test;



import java.io.StringReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the ANTLRTurtle class.
 * These tests verify the parser's ability to correctly parse Turtle
 * and interact with the Model and ValueFactory, including error handling
 * and unescaping of IRIs and literals, and named graphs.
 */
public class TurtleParserTest {

    private static final Logger logger = LoggerFactory.getLogger(TurtleParserTest.class);

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
        String turtle = """
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                <#0214>\s
                    rdfs:comment ""\""abc""\"" .""";

        Model model = new CoreseModel();
        RDFParser parser = new TurtleParser(model, factory, new TurtleParserOptions.Builder().baseIRI("http://inria.fr/").build());
        parser.parse(new StringReader(turtle));
        assertEquals(1, model.size());
        model.contains(factory.createIRI("http://inria.fr/#0214"), RDFS.comment.getIRI(), factory.createLiteral("\"abc\""));
    }

    @Test
    public void testRdfaManifest() {
        String turtle = """
                @base <http://rdfa.info/test-suite/test-cases/rdfa1.1/xml/manifest> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                
                <#0006> rdfs:label \"""Test 0006: @rel and @rev\""";
                  rdfs:comment \"""Tests @rev and @rel together, with the object being specified by @href, ignoring content\""";
                  .
                  """;

        Model model = new CoreseModel();
        RDFParser parser = new TurtleParser(model, factory, new TurtleParserOptions.Builder().baseIRI("http://inria.fr/").build());
        parser.parse(new StringReader(turtle));

        Model refModel = new CoreseModel();
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
