package fr.inria.corese.core.next.data.impl.io.parser.trig;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.impl.StorageModel;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.impl.parser.antlr.TriGLexer;
import fr.inria.corese.core.next.impl.parser.antlr.TriGParser;
import fr.inria.corese.core.next.storagemanager.api.plugin.StoragePluginManager;
import fr.inria.corese.core.next.storagemanager.api.support.config.StorageConfig;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the TriGListenerImpl class.
 * These tests verify that the listener correctly processes ANTLR parse tree contexts
 * to extract and unescape RDF terms (IRIs, Blank Nodes, Literals) and add them to the model.
 */
class TriGListenerImplTest {
    private ValueFactory factory = new CoreseAdaptedValueFactory();

    private Model parseTrig(String trigData) throws Exception {
        CharStream input = CharStreams.fromReader(new StringReader(trigData));
        TriGLexer lexer = new TriGLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TriGParser parser = new TriGParser(tokens);
        ParseTree tree = parser.trigDoc();

        StorageConfig config = StorageConfig.builder()
                .property("type", "memory")
                .build();

        Model coreseModel = StorageModel.builder()
                .storage(StoragePluginManager.create(config))
                .valueFactory(factory)
                .build();

        TriGListerner listener = new TriGListerner(coreseModel, factory, null);
        ParseTreeWalker.DEFAULT.walk(listener, tree);

        return coreseModel;
    }

    @Test
    void testSimpleNamedGraph() throws Exception {
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
    void testBlankNodeWithProperties() throws Exception {
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
    void testMultipleGraphsAndBase() throws Exception {
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

        // Total statements: 1 in default graph + 1 in named graph
        assertEquals(2, model.size(), "Should have 2 statements total");

        // Named graphs only (default graph not counted in contexts())
        Set<Resource> contexts = model.contexts();
        assertEquals(1, contexts.size(),
                "Should have 1 named graph (default graph not included in contexts())");

        // Verify the named graph exists
        IRI otherGraph = factory.createIRI("http://example.org/other");
        assertTrue(contexts.contains(otherGraph),
                "Should contain ex:other as a named graph");
    }

    @Test
    void testTypedLiteralsAndLang() throws Exception {
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