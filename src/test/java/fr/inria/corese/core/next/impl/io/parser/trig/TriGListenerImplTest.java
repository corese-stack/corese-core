package fr.inria.corese.core.next.impl.io.parser.trig;

import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.impl.parser.antlr.TriGLexer;
import fr.inria.corese.core.next.impl.parser.antlr.TriGParser;
import fr.inria.corese.core.next.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.temp.CoreseModel;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

public class TriGListenerImplTest {
    private Model parseTrig(String trigData) throws Exception {
        ValueFactory factory = new CoreseAdaptedValueFactory();

        CharStream input = CharStreams.fromReader(new StringReader(trigData));
        TriGLexer lexer = new TriGLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TriGParser parser = new TriGParser(tokens);
        ParseTree tree = parser.trigDoc();

        Model model = new CoreseModel();
        TriGListerner listener = new TriGListerner(model, factory, null);
        ParseTreeWalker.DEFAULT.walk(listener, tree);

        return model;
    }

    @Test
    public void testSimpleNamedGraph() throws Exception {
        String trig = """
                @prefix ex: <http://example.org/> .

                GRAPH ex:graph {
                    ex:subject ex:predicate "Hello" .
                }
                """;

        Model model = parseTrig(trig);
        assertEquals(1, model.size());
        assertEquals(1, model.contexts().size());
    }

    @Test
    public void testBlankNodeWithProperties() throws Exception {
        String trig = """
                @prefix ex: <http://example.org/> .
                GRAPH ex:graph {
                ex:Bob ex:knows [ ex:name "Charlie" ] .
                }
                """;

        Model model = parseTrig(trig);
        assertEquals(2, model.size());
    }

    @Test
    public void testMultipleGraphsAndBase() throws Exception {
        String trig = """
                @base <http://example.org/> .
                @prefix dc: <http://purl.org/dc/elements/1.1/> .
                @prefix ex: <http://example.org/> .

                <http://example.org/bob> dc:creator "Bob" .

                GRAPH ex:other {
                    <http://example.org/alice> dc:creator "Alice" .
                }
                """;

        Model model = parseTrig(trig);
        assertEquals(2, model.contexts().size());
        assertEquals(2, model.size());
    }

    @Test
    public void testTypedLiteralsAndLang() throws Exception {
        String trig = """
                @prefix ex: <http://example.org/> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

                ex:subject ex:age "30"^^xsd:integer ;
                           ex:name "Jean"@fr .
                """;

        Model model = parseTrig(trig);
        assertEquals(2, model.size());
    }
}
