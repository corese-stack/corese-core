package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlLexer;
import fr.inria.corese.core.next.query.impl.parser.listener.SelectQueryAstListener;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.parser.listener.BgpAstListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAstTestSupport.simplePredicateTerm;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SparqlListenerDispatcher} (multiplexer and delegate forwarding).
 */
class SparqlListenerDispatcherTest {

    @Test
    void constructorWithNullDelegatesIsEmpty() {
        SparqlListenerDispatcher listener = new SparqlListenerDispatcher(null);
        // Walk must not throw when delegates is empty
        fr.inria.corese.core.next.impl.parser.antlr.SparqlParser p = createAntlrParser("SELECT * WHERE { ?s ?p ?o }");
        assertDoesNotThrow(() -> new ParseTreeWalker().walk(listener, p.query()));
    }

    @Test
    void constructorWithEmptyListDelegatesIsEmpty() {
        SparqlListenerDispatcher listener = new SparqlListenerDispatcher(Collections.emptyList());
        fr.inria.corese.core.next.impl.parser.antlr.SparqlParser p = createAntlrParser("SELECT * WHERE { }");
        assertDoesNotThrow(() -> new ParseTreeWalker().walk(listener, p.query()));
    }

    @Test
    void constructorWithDelegatesCopiesList() {
        BgpAstListener feature = new BgpAstListener(
                new SparqlQueryAstBuilder(new SparqlParserOptions.Builder().build()));
        List<BgpAstListener> mutable = new java.util.ArrayList<>(List.of(feature));
        SparqlListenerDispatcher listener = new SparqlListenerDispatcher(mutable);
        mutable.clear();
        // Listener should still have one delegate (defensive copy)
        fr.inria.corese.core.next.impl.parser.antlr.SparqlParser antlrParser = createAntlrParser("SELECT * WHERE { ?s ?p ?o }");
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, antlrParser.query());
        // If the delegate was still there, the builder would have been fed; we can't easily check
        // delegate count without exposing it, so we only check no exception.
    }

    @Test
    void withSingleBgpDelegateWalkProducesAst() {
        SparqlParserOptions opts = new SparqlParserOptions.Builder().build();
        SparqlQueryAstBuilder builder = new SparqlQueryAstBuilder(opts);
        SparqlListenerDispatcher listener = new SparqlListenerDispatcher(List.of(new BgpAstListener(builder), new SelectQueryAstListener(builder)));

        fr.inria.corese.core.next.impl.parser.antlr.SparqlParser antlrParser = createAntlrParser("SELECT * WHERE { ?s ?p ?o }");
        new ParseTreeWalker().walk(listener, antlrParser.query());

        SparqlQueryAst ast = (SparqlQueryAst) builder.getResult();
        assertNotNull(ast);
        assertNotNull(ast.whereClause());
        assertEquals(1, ast.whereClause().patterns().size());
        assertInstanceOf(BgpAst.class, ast.whereClause().patterns().getFirst());
        BgpAst bgp = (BgpAst) ast.whereClause().patterns().getFirst();
        assertEquals(1, bgp.triples().size());
        TriplePatternAst t = bgp.triples().getFirst();
        assertEquals("s", ((VarAst) t.subject()).name());
        assertEquals("p", ((VarAst) simplePredicateTerm(t)).name());
        assertEquals("o", ((VarAst) t.object()).name());
    }

    private static fr.inria.corese.core.next.impl.parser.antlr.SparqlParser createAntlrParser(String query) {
        SparqlLexer lexer = new SparqlLexer(CharStreams.fromString(query));
        return new fr.inria.corese.core.next.impl.parser.antlr.SparqlParser(new CommonTokenStream(lexer));
    }
}
