package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class SparqlParserAskQueryTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("should parse a basic ASK WHERE {?s ?p ?o} query")
    public void shouldParseBasicAskQueryTest() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                ASK WHERE {
                  ?s ?p ?o .
                }
                """);

        assertNotNull(ast);
        assertInstanceOf(AskQueryAst.class, ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(1, where.patterns().size(), "WHERE should contain 1 pattern (BGP)");

        PatternAst p0 = where.patterns().getFirst();
        assertInstanceOf(BgpAst.class, p0, "First pattern should be a BGP");

        BgpAst bgp = (BgpAst) p0;
        assertEquals(1, bgp.triples().size(), "BGP should contain 1 triple");

        TriplePatternAst t = bgp.triples().getFirst();

        assertInstanceOf(VarAst.class, t.subject());
        assertInstanceOf(VarAst.class, t.predicate());
        assertInstanceOf(VarAst.class, t.object());

        assertEquals("s", ((VarAst) t.subject()).name());
        assertEquals("p", ((VarAst) t.predicate()).name());
        assertEquals("o", ((VarAst) t.object()).name());
    }

    @Test
    @DisplayName("should parse a short form ASK {uri ?p ?o} query")
    public void shouldParseShortAskQueryTest() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                ASK {
                  <https://ns.inria.fr/corese> ?p ?o .
                }
                """);

        assertNotNull(ast);
        assertInstanceOf(AskQueryAst.class, ast);
        assertNotNull(ast.whereClause());

        GroupGraphPatternAst where = ast.whereClause();
        assertEquals(1, where.patterns().size(), "WHERE clause should contain 1 pattern (BGP)");

        PatternAst p0 = where.patterns().getFirst();
        assertInstanceOf(BgpAst.class, p0, "First pattern should be a BGP");

        BgpAst bgp = (BgpAst) p0;
        assertEquals(1, bgp.triples().size(), "BGP should contain 1 triple");

        TriplePatternAst t = bgp.triples().getFirst();

        assertInstanceOf(IriAst.class, t.subject());
        assertInstanceOf(VarAst.class, t.predicate());
        assertInstanceOf(VarAst.class, t.object());

        assertEquals("<https://ns.inria.fr/corese>", ((IriAst)t.subject()).raw());
        assertEquals("p", ((VarAst) t.predicate()).name());
        assertEquals("o", ((VarAst) t.object()).name());
    }
}
