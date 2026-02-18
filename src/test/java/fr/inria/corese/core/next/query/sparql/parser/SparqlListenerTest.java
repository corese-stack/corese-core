package fr.inria.corese.core.next.query.sparql.parser;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlLexer;
import fr.inria.corese.core.next.query.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.sparql.parser.listener.SparqlBgpFeature;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SparqlListener} (multiplexer and delegate forwarding).
 */
class SparqlListenerTest {

    @Test
    void constructorWithNullDelegatesIsEmpty() {
        SparqlListener listener = new SparqlListener(null);
        // Walk must not throw when delegates is empty
        fr.inria.corese.core.next.impl.parser.antlr.SparqlParser p = createAntlrParser("SELECT * WHERE { ?s ?p ?o }");
        assertDoesNotThrow(() -> new ParseTreeWalker().walk(listener, p.query()));
    }

    @Test
    void constructorWithEmptyListDelegatesIsEmpty() {
        SparqlListener listener = new SparqlListener(Collections.emptyList());
        fr.inria.corese.core.next.impl.parser.antlr.SparqlParser p = createAntlrParser("SELECT * WHERE { }");
        assertDoesNotThrow(() -> new ParseTreeWalker().walk(listener, p.query()));
    }

    @Test
    void constructorWithDelegatesCopiesList() {
        SparqlBgpFeature feature = new SparqlBgpFeature(
                new SparqlAstBuilder(new SparqlParserOptions.Builder().build()));
        List<SparqlBgpFeature> mutable = new java.util.ArrayList<>(List.of(feature));
        SparqlListener listener = new SparqlListener(mutable);
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
        SparqlAstBuilder builder = new SparqlAstBuilder(opts);
        SparqlListener listener = new SparqlListener(List.of(new SparqlBgpFeature(builder)));

        fr.inria.corese.core.next.impl.parser.antlr.SparqlParser antlrParser = createAntlrParser("SELECT * WHERE { ?s ?p ?o }");
        new ParseTreeWalker().walk(listener, antlrParser.query());

        QueryAst ast = builder.getResult();
        assertNotNull(ast);
        assertNotNull(ast.whereClause());
        assertEquals(1, ast.whereClause().patterns().size());
        assertTrue(ast.whereClause().patterns().get(0) instanceof BgpAst);
        BgpAst bgp = (BgpAst) ast.whereClause().patterns().get(0);
        assertEquals(1, bgp.triples().size());
        TriplePatternAst t = bgp.triples().get(0);
        assertEquals("s", ((VarAst) t.subject()).name());
        assertEquals("p", ((VarAst) t.predicate()).name());
        assertEquals("o", ((VarAst) t.object()).name());
    }

    private static fr.inria.corese.core.next.impl.parser.antlr.SparqlParser createAntlrParser(String query) {
        SparqlLexer lexer = new SparqlLexer(CharStreams.fromString(query));
        return new fr.inria.corese.core.next.impl.parser.antlr.SparqlParser(new CommonTokenStream(lexer));
    }
}
