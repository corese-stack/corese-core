package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class SparqlParserSelectQueryTest extends AbstractSparqlParserFeatureTest {
    @Test
    @DisplayName("Should parse a basic SELECT ?s ?o ?p WHERE {?s ?p ?o} query")
    public void shouldParseBasicSelectQueryTest() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?s ?o ?p WHERE {
                    ?s ?p ?o
                }
                """);

        assertNotNull(ast);
        assertInstanceOf(QueryAst.class, ast);

        SelectQueryAst selectQueryAst = (SelectQueryAst) ast;
        assertNotNull(selectQueryAst);
        assertInstanceOf(SelectQueryAst.class, selectQueryAst);
        assertNotNull(selectQueryAst.whereClause());
        assertNotNull(selectQueryAst.projection());

        // --- WHERE ---
        GroupGraphPatternAst where = selectQueryAst.whereClause();
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

        // --- PROJECTION ---

        ProjectionAst projection = selectQueryAst.projection();

        assertFalse(projection.selectAll(), "Projection should not be SELECT *");

        List<VarAst> vars = projection.variables();

        assertEquals(3, vars.size());

        assertEquals("s", vars.get(0).name());
        assertEquals("o", vars.get(1).name());
        assertEquals("p", vars.get(2).name());
    }

    @Test
    @DisplayName("Should parse a basic SELECT * WHERE { ?s ?p ?o } query")
    public void shouldParseBasicSelectAllQueryTest() {

        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
            SELECT * WHERE {
                ?s ?p ?o
            }
            """);

        assertNotNull(ast);
        assertInstanceOf(QueryAst.class, ast);

        SelectQueryAst selectQueryAst = (SelectQueryAst) ast;

        assertNotNull(selectQueryAst.whereClause());
        assertNotNull(selectQueryAst.projection());

        // --- WHERE ---
        GroupGraphPatternAst where = selectQueryAst.whereClause();

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

        // --- PROJECTION ---
        ProjectionAst projection = selectQueryAst.projection();

        assertTrue(projection.selectAll(), "Projection should be SELECT *");
        assertTrue(projection.variables().isEmpty(), "SELECT * projection should not contain explicit variables");
    }

    @Test
    @DisplayName("Should parse a short form SELECT ?s ?o { ?s ?p ?o } query")
    public void shouldParseBasicSelectShortQueryTest() {

        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
            SELECT ?s ?o {
                ?s ?p ?o
            }
            """);

        assertNotNull(ast);
        assertInstanceOf(QueryAst.class, ast);

        SelectQueryAst selectQueryAst = (SelectQueryAst) ast;

        assertNotNull(selectQueryAst.whereClause());
        assertNotNull(selectQueryAst.projection());

        // --- WHERE ---
        GroupGraphPatternAst where = selectQueryAst.whereClause();

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
        assertEquals("o", ((VarAst) t.object()).name());

        // --- PROJECTION ---
        ProjectionAst projection = selectQueryAst.projection();

        assertEquals(2, projection.variables().size(), "Projection should contains 2 variables");
        assertEquals("s", projection.variables().get(0).name());
        assertEquals("o", projection.variables().get(1).name());
    }

    @Test
    @DisplayName("Should parse SELECT DISTINCT without ORDER BY, LIMIT or OFFSET")
    void shouldParseDistinctOnly() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
            SELECT DISTINCT ?s WHERE {
                ?s ?p ?o
            }
            """);

        assertInstanceOf(SelectQueryAst.class, ast);
        SelectQueryAst select = (SelectQueryAst) ast;

        SolutionModifierAst solutionModifier = select.solutionModifier();

        assertTrue(solutionModifier.distinct());
        assertFalse(solutionModifier.reduced());
        assertTrue(solutionModifier.orderBy().isEmpty());
        assertNull(solutionModifier.limit());
        assertNull(solutionModifier.offset());
    }

    @Test
    @DisplayName("Should parse SELECT REDUCED without ORDER BY, LIMIT or OFFSET")
    void shouldParseReducedOnly() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
            SELECT REDUCED ?s WHERE {
                ?s ?p ?o
            }
            """);

        assertInstanceOf(SelectQueryAst.class, ast);
        SelectQueryAst select = (SelectQueryAst) ast;

        SolutionModifierAst solutionModifier = select.solutionModifier();

        assertFalse(solutionModifier.distinct());
        assertTrue(solutionModifier.reduced());
        assertTrue(solutionModifier.orderBy().isEmpty());
        assertNull(solutionModifier.limit());
        assertNull(solutionModifier.offset());
    }
}