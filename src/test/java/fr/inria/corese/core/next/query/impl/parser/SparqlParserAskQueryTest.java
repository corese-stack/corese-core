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

    @Test
    @DisplayName("Insertion of a comment before the query should not change the treatment of the query")
    public void shouldIgnoreCommentBeforeQuery() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                # This is a test comment
                ASK {
                    ?s a ?c ;
                        ?p ?o .
                }
                """;
        assertDoesNotThrow(() -> {
            parser.parse(commentedQuery);
        });
        // Test if the query is still exactly as expected
        QueryAst ast = parser.parse(commentedQuery);
        assertNotNull(ast);
        assertInstanceOf(AskQueryAst.class, ast);
        AskQueryAst askQueryAst = (AskQueryAst) ast;
        assertNotNull(askQueryAst.whereClause());
        assertEquals(1, askQueryAst.whereClause().patterns().size());
        assertNotNull(askQueryAst.whereClause().patterns().getFirst());
        assertInstanceOf(BgpAst.class, askQueryAst.whereClause().patterns().getFirst());
        BgpAst bgpAst = (BgpAst) askQueryAst.whereClause().patterns().getFirst();
        assertEquals(2, bgpAst.triples().size());
        assertNotNull(bgpAst.triples().getFirst());
        assertNotNull(bgpAst.triples().getFirst().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getFirst().subject()).name());
        assertInstanceOf(IriAst.class, bgpAst.triples().getFirst().predicate());
        assertEquals("a", ((IriAst)bgpAst.triples().getFirst().predicate()).raw());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().object());
        assertEquals("c", ((VarAst)bgpAst.triples().getFirst().object()).name());
        assertNotNull(bgpAst.triples().getLast());
        assertNotNull(bgpAst.triples().getLast().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getLast().subject()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().predicate());
        assertEquals("p", ((VarAst)bgpAst.triples().getLast().predicate()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().object());
        assertEquals("o", ((VarAst)bgpAst.triples().getLast().object()).name());
    }

    @Test
    @DisplayName("Insertion of a comment after the query should not change the treatment of the query")
    public void shouldIgnoreCommentAfterQuery() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                ASK {
                    ?s a ?c ;
                        ?p ?o .
                }
                # This is a test comment
                """;
        assertDoesNotThrow(() -> {
            parser.parse(commentedQuery);
        });
        // Test if the query is still exactly as expected
        QueryAst ast = parser.parse(commentedQuery);
        assertNotNull(ast);
        assertInstanceOf(AskQueryAst.class, ast);
        AskQueryAst askQueryAst = (AskQueryAst) ast;
        assertNotNull(askQueryAst.whereClause());
        assertEquals(1, askQueryAst.whereClause().patterns().size());
        assertNotNull(askQueryAst.whereClause().patterns().getFirst());
        assertInstanceOf(BgpAst.class, askQueryAst.whereClause().patterns().getFirst());
        BgpAst bgpAst = (BgpAst) askQueryAst.whereClause().patterns().getFirst();
        assertEquals(2, bgpAst.triples().size());
        assertNotNull(bgpAst.triples().getFirst());
        assertNotNull(bgpAst.triples().getFirst().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getFirst().subject()).name());
        assertInstanceOf(IriAst.class, bgpAst.triples().getFirst().predicate());
        assertEquals("a", ((IriAst)bgpAst.triples().getFirst().predicate()).raw());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().object());
        assertEquals("c", ((VarAst)bgpAst.triples().getFirst().object()).name());
        assertNotNull(bgpAst.triples().getLast());
        assertNotNull(bgpAst.triples().getLast().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getLast().subject()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().predicate());
        assertEquals("p", ((VarAst)bgpAst.triples().getLast().predicate()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().object());
        assertEquals("o", ((VarAst)bgpAst.triples().getLast().object()).name());
    }

    @Test
    @DisplayName("Insertion of a comment in the middle of the query should not change the treatment of the query")
    public void shouldIgnoreCommentInTheMiddleOfQuery() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                ASK {
                    ?s a ?c ;
                # This is a test comment
                        ?p ?o .
                }
                """;
        assertDoesNotThrow(() -> {
            parser.parse(commentedQuery);
        });
        // Test if the query is still exactly as expected
        QueryAst ast = parser.parse(commentedQuery);
        assertNotNull(ast);
        assertInstanceOf(AskQueryAst.class, ast);
        AskQueryAst askQueryAst = (AskQueryAst) ast;
        assertNotNull(askQueryAst.whereClause());
        assertEquals(1, askQueryAst.whereClause().patterns().size());
        assertNotNull(askQueryAst.whereClause().patterns().getFirst());
        assertInstanceOf(BgpAst.class, askQueryAst.whereClause().patterns().getFirst());
        BgpAst bgpAst = (BgpAst) askQueryAst.whereClause().patterns().getFirst();
        assertEquals(2, bgpAst.triples().size());
        assertNotNull(bgpAst.triples().getFirst());
        assertNotNull(bgpAst.triples().getFirst().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getFirst().subject()).name());
        assertInstanceOf(IriAst.class, bgpAst.triples().getFirst().predicate());
        assertEquals("a", ((IriAst)bgpAst.triples().getFirst().predicate()).raw());
        assertInstanceOf(VarAst.class, bgpAst.triples().getFirst().object());
        assertEquals("c", ((VarAst)bgpAst.triples().getFirst().object()).name());
        assertNotNull(bgpAst.triples().getLast());
        assertNotNull(bgpAst.triples().getLast().subject());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().subject());
        assertEquals("s", ((VarAst)bgpAst.triples().getLast().subject()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().predicate());
        assertEquals("p", ((VarAst)bgpAst.triples().getLast().predicate()).name());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().object());
        assertEquals("o", ((VarAst)bgpAst.triples().getLast().object()).name());
    }
}
