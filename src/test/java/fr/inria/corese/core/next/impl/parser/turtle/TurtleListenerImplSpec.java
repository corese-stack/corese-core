package fr.inria.corese.core.next.impl.parser.turtle;

import fr.inria.corese.core.next.impl.parser.antlr.TurtleLexer;
import fr.inria.corese.core.next.impl.parser.antlr.TurtleParser;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TurtleListenerImplSpec {
    private Model parseAndPrintModel(String turtleData) throws Exception {
        CharStream input = CharStreams.fromReader(new StringReader(turtleData));
        TurtleLexer lexer = new TurtleLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TurtleParser parser = new TurtleParser(tokens);
        ParseTreeWalker walker = new ParseTreeWalker();
        ParseTree tree = parser.turtleDoc();

        Model model = new CoreseModel();
        TurtleListenerImpl listener = new TurtleListenerImpl(model, null);
        walker.walk((ParseTreeListener) listener, tree);


        model.forEach(stmt -> {
            System.out.println(stmt.getSubject().stringValue() + " " +
                    stmt.getPredicate().stringValue() + " " +
                    stmt.getObject().stringValue());
        });


        return model;
    }

    @Test
    public void testNamespace() throws Exception {
        String turtleData = " @prefix ex: <http://example.org/> .  " +
                "ex:subject ex:predicate 1 . ";

        Model model = parseAndPrintModel(turtleData);
        assertEquals(model.getNamespaces().size(), 1);
    }

    @Test
    public void testTypedLiteral() throws Exception {
        String turtleData = "@prefix ex: <http://example.org/> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "ex:subject ex:age \"27\"^^xsd:integer .";

        Model model = parseAndPrintModel(turtleData);
        assertEquals(model.size(), 1);
        assertEquals(model.getNamespaces().size(), 2);

    }

    @Test
    public void testMultipleObjects() throws Exception {
        String turtleData = "@prefix ex: <http://example.org/> .\n" +
                "ex:subject ex:knows ex:Alice , ex:Bob ; ex:likes ex:Pizza .";

        Model model = parseAndPrintModel(turtleData);
        assertEquals(model.size(), 3);
        assertEquals(model.getNamespaces().size(), 1);

    }

    @Test
    public void testRDFtype() throws Exception {
        String turtleData = "@prefix ex: <http://example.org/> .\n" +
                "ex:Alice a ex:Person .\n" +
                "ex:subject ex:knows ex:Alice , ex:Bob ; ex:likes ex:Pizza .";

        Model model = parseAndPrintModel(turtleData);
        assertEquals(model.size(), 4);
        assertEquals(model.getNamespaces().size(), 1);
    }

    @Test
    public void testBaseIRI() throws Exception {
        String turtleData = "@base <http://example.org/base/> .\n" +
                "@prefix : <http://example.org/prefix/> .\n" +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . \n" +
                "\n" +
                "<http://example.org/prefix/Name> rdf:type rdf:Property .\n" +
                ":phone rdf:type rdf:Property .";

        Model model = parseAndPrintModel(turtleData);
        assertEquals(model.size(), 2);
        assertEquals(model.getNamespaces().size(), 2);
    }
}