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

    @Test
    @DisplayName("Should parse SELECT DISTINCT ?city ?cityLabel WHERE with BGP and UNION")
    public void shouldParseSelectDistinctWithUnionQueryTest() {
        SparqlParser parser = newParserDefault();

        //https://en.wikibooks.org/wiki/SPARQL/UNION
        QueryAst ast = parser.parse("""
                SELECT DISTINCT ?city ?cityLabel
                WHERE {
                  wd:Q458 wdt:P150 ?country.
                  { ?country wdt:P36 ?city. }
                  UNION
                  { ?city wdt:P17 ?country.
                    ?city wdt:P31 wd:Q1637706.   }
                }
                """);

        assertNotNull(ast);
        assertInstanceOf(QueryAst.class, ast);

        SelectQueryAst selectQueryAst = (SelectQueryAst) ast;
        assertNotNull(selectQueryAst);
        assertNotNull(selectQueryAst.whereClause());
        assertNotNull(selectQueryAst.projection());

        // --- WHERE: BGP + UnionAst (two branches) ---
        GroupGraphPatternAst where = selectQueryAst.whereClause();
        assertEquals(2, where.patterns().size(), "WHERE should contain 2 patterns (BGP + UNION)");

        // First pattern: BGP with wd:Q458 wdt:P150 ?country
        PatternAst p0 = where.patterns().get(0);
        assertInstanceOf(BgpAst.class, p0, "First pattern should be a BGP");
        BgpAst bgp = (BgpAst) p0;
        assertEquals(1, bgp.triples().size(), "BGP should contain 1 triple");
        TriplePatternAst t0 = bgp.triples().getFirst();
        assertInstanceOf(IriAst.class, t0.subject());
        assertInstanceOf(IriAst.class, t0.predicate());
        assertInstanceOf(VarAst.class, t0.object());
        assertEquals("wd:Q458", ((IriAst) t0.subject()).raw());
        assertEquals("wdt:P150", ((IriAst) t0.predicate()).raw());
        assertEquals("country", ((VarAst) t0.object()).name());

        // Second pattern: UNION of two branches
        PatternAst p1 = where.patterns().get(1);
        assertInstanceOf(UnionAst.class, p1, "Second pattern should be a UNION");
        UnionAst union = (UnionAst) p1;
        assertNotNull(union.left());
        assertNotNull(union.right());
        assertInstanceOf(GroupGraphPatternAst.class, union.left());
        assertInstanceOf(GroupGraphPatternAst.class, union.right());

        // Left branch: { ?country wdt:P36 ?city. }
        GroupGraphPatternAst leftBranch = union.left();
        assertEquals(1, leftBranch.patterns().size());
        assertInstanceOf(BgpAst.class, leftBranch.patterns().getFirst());
        BgpAst leftBgp = (BgpAst) leftBranch.patterns().getFirst();
        assertEquals(1, leftBgp.triples().size());
        TriplePatternAst leftTriple = leftBgp.triples().getFirst();
        assertEquals("country", ((VarAst) leftTriple.subject()).name());
        assertEquals("wdt:P36", ((IriAst) leftTriple.predicate()).raw());
        assertEquals("city", ((VarAst) leftTriple.object()).name());

        // Right branch: { ?city wdt:P17 ?country. ?city wdt:P31 wd:Q1637706. } → two TriplesBlocks → two BGPs
        GroupGraphPatternAst rightBranch = union.right();
        assertEquals(2, rightBranch.patterns().size());
        assertInstanceOf(BgpAst.class, rightBranch.patterns().get(0));
        assertInstanceOf(BgpAst.class, rightBranch.patterns().get(1));
        BgpAst rightBgp1 = (BgpAst) rightBranch.patterns().get(0);
        BgpAst rightBgp2 = (BgpAst) rightBranch.patterns().get(1);
        assertEquals(1, rightBgp1.triples().size());
        assertEquals(1, rightBgp2.triples().size());
        List<TriplePatternAst> rightTriples = List.of(
                rightBgp1.triples().getFirst(),
                rightBgp2.triples().getFirst()
        );
        assertTrue(rightTriples.stream().anyMatch(t ->
                "city".equals(((VarAst) t.subject()).name())
                        && "wdt:P17".equals(((IriAst) t.predicate()).raw())
                        && "country".equals(((VarAst) t.object()).name())),
                "Right branch should contain triple ?city wdt:P17 ?country");
        assertTrue(rightTriples.stream().anyMatch(t ->
                "city".equals(((VarAst) t.subject()).name())
                        && "wdt:P31".equals(((IriAst) t.predicate()).raw())
                        && "wd:Q1637706".equals(((IriAst) t.object()).raw())),
                "Right branch should contain triple ?city wdt:P31 wd:Q1637706");

        // --- PROJECTION ---
        ProjectionAst projection = selectQueryAst.projection();
        assertFalse(projection.selectAll(), "Projection should not be SELECT *");
        assertEquals(2, projection.variables().size());
        assertEquals("city", projection.variables().get(0).name());
        assertEquals("cityLabel", projection.variables().get(1).name());
    }
}