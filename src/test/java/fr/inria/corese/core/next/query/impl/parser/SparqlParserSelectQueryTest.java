package fr.inria.corese.core.next.query.impl.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.ASTConstants;
import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UnionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BoundAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IfAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IsIriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrAst;


@SuppressWarnings("java:S5976")
class SparqlParserSelectQueryTest extends AbstractSparqlParserFeatureTest {
    @Test
    @DisplayName("Should parse a basic SELECT ?s ?o ?p WHERE {?s ?p ?o} query")
    void shouldParseBasicSelectQueryTest() {
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
    void shouldParseBasicSelectAllQueryTest() {

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
    void shouldParseBasicSelectShortQueryTest() {

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
    @DisplayName("Should parse GROUP BY ?g on SELECT")
    void shouldParseGroupBy() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
            SELECT ?s WHERE {
                ?s ?p ?o
            }
            GROUP BY ?s
            """);

        assertInstanceOf(SelectQueryAst.class, ast);
        SelectQueryAst select = (SelectQueryAst) ast;
        SolutionModifierAst mod = select.solutionModifier();
        assertEquals(1, mod.groupBy().expressions().size());
        assertEquals("s", assertInstanceOf(VarAst.class, mod.groupBy().expressions().getFirst()).name());
        assertFalse(mod.hasOrderBy());
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
    @DisplayName("Should parse LIMIT")
    void shouldParseLimit() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
        SELECT ?s WHERE {
            ?s ?p ?o
        }
        LIMIT 10
    """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(10L, select.solutionModifier().limit());
        assertNull(select.solutionModifier().offset());
    }

    @Test
    @DisplayName("Should parse OFFSET")
    void shouldParseOffset() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
        SELECT ?s WHERE {
            ?s ?p ?o
        }
        OFFSET 5
    """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(5L, select.solutionModifier().offset());
    }

    @Test
    @DisplayName("Should parse LIMIT OFFSET")
    void shouldParseLimitOffset() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    LIMIT 10
                    OFFSET 20
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(10L, select.solutionModifier().limit());
        assertEquals(20L, select.solutionModifier().offset());

    }

    @Test
    @DisplayName("Should parse ORDER BY ASC(VAR)")
    void shouldParseAscOrder() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY ASC(?s)
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(1, select.solutionModifier().orderBy().size());
        assertEquals(ASTConstants.OrderDirection.ASC, select.solutionModifier().orderBy().getFirst().orderDirection());
        assertInstanceOf(VarAst.class,  select.solutionModifier().orderBy().getFirst().expression());

    }

    @Test
    @DisplayName("Should parse ORDER BY VAR")
    void shouldParseOrder() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY ?s
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(1, select.solutionModifier().orderBy().size());
        assertEquals(ASTConstants.OrderDirection.ASC, select.solutionModifier().orderBy().getFirst().orderDirection());
        assertInstanceOf(VarAst.class,  select.solutionModifier().orderBy().getFirst().expression());

    }

    @Test
    @DisplayName("Should parse ORDER BY DESC(VAR)")
    void shouldParseDescOrder() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY DESC(?s)
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(1, select.solutionModifier().orderBy().size());
        assertEquals(ASTConstants.OrderDirection.DESC, select.solutionModifier().orderBy().getFirst().orderDirection());
        assertInstanceOf(VarAst.class,  select.solutionModifier().orderBy().getFirst().expression());

    }

    @Test
    @DisplayName("Should parse multiple ORDER BY clauses")
    void shouldParseMultipleOrder() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s ?p ?o WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY DESC(?s) ?p DESC(?o)
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(3, select.solutionModifier().orderBy().size());
        assertEquals(ASTConstants.OrderDirection.DESC, select.solutionModifier().orderBy().get(0).orderDirection());
        assertInstanceOf(VarAst.class,  select.solutionModifier().orderBy().get(0).expression());
        assertEquals(ASTConstants.OrderDirection.ASC, select.solutionModifier().orderBy().get(1).orderDirection());
        assertInstanceOf(VarAst.class,  select.solutionModifier().orderBy().get(1).expression());
        assertEquals(ASTConstants.OrderDirection.DESC, select.solutionModifier().orderBy().get(2).orderDirection());
        assertInstanceOf(VarAst.class,  select.solutionModifier().orderBy().get(2).expression());
    }

    @Test
    @DisplayName("Should parse ORDER BY with a function call")
    void shouldParseDescConstraintOrder() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY DESC(STR(?s))
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(1, select.solutionModifier().orderBy().size());
        assertEquals(ASTConstants.OrderDirection.DESC, select.solutionModifier().orderBy().getFirst().orderDirection());
        assertInstanceOf(StrAst.class,  select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("Should parse multiple ORDER BY clauses combining vars and function calls")
    void shouldParseCombinedMultipleOrder() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s ?p ?o WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY DESC(?s) ?p IsIRI(?o)
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(3, select.solutionModifier().orderBy().size());
        assertEquals(ASTConstants.OrderDirection.DESC, select.solutionModifier().orderBy().get(0).orderDirection());
        assertInstanceOf(VarAst.class,  select.solutionModifier().orderBy().get(0).expression());
        assertEquals(ASTConstants.OrderDirection.ASC, select.solutionModifier().orderBy().get(1).orderDirection());
        assertInstanceOf(VarAst.class,  select.solutionModifier().orderBy().get(1).expression());
        assertEquals(ASTConstants.OrderDirection.ASC, select.solutionModifier().orderBy().get(2).orderDirection());
        assertInstanceOf(IsIriAst.class,  select.solutionModifier().orderBy().get(2).expression());
    }

    @Test
    @DisplayName("Should not parse ORDER BY with a constant")
    void shouldNotParseOrderByConstant() {
        SparqlParser parser = newParserDefault();

        assertThrows(QuerySyntaxException.class, () -> {
            parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY <http://ns.inria.fr/test>
                """);
        });
    }

    @Test
    @DisplayName("Should accept ORDER BY variable visible in WHERE but not projected")
    void shouldAcceptOrderByVariableVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY ?o
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(1, select.solutionModifier().orderBy().size());
        assertEquals(ASTConstants.OrderDirection.ASC, select.solutionModifier().orderBy().getFirst().orderDirection());
        assertInstanceOf(VarAst.class, select.solutionModifier().orderBy().getFirst().expression());
        assertEquals("o", ((VarAst) select.solutionModifier().orderBy().getFirst().expression()).name());
    }

    @Test
    @DisplayName("Should accept SELECT * with ORDER BY variable visible in WHERE")
    void shouldAcceptSelectAllWithOrderByVariableVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT * WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY ?o
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertTrue(select.projection().selectAll());
        assertEquals(1, select.solutionModifier().orderBy().size());
        assertInstanceOf(VarAst.class, select.solutionModifier().orderBy().getFirst().expression());
        assertEquals("o", ((VarAst) select.solutionModifier().orderBy().getFirst().expression()).name());
    }

    @Test
    @DisplayName("Should accept ORDER BY expression using a variable visible in WHERE but not projected")
    void shouldAcceptOrderByExpressionVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY STR(?o)
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(1, select.solutionModifier().orderBy().size());
        assertEquals(ASTConstants.OrderDirection.ASC, select.solutionModifier().orderBy().getFirst().orderDirection());
        assertInstanceOf(StrAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("Should accept ORDER BY IF expression using only variables visible in WHERE")
    void shouldAcceptOrderByIfExpressionVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s WHERE {
                        ?s ?p ?o
                    }
                    ORDER BY IF(BOUND(?o), STR(?o), ?s)
                """);

        SelectQueryAst select = (SelectQueryAst) ast;
        IfAst ifAst = assertInstanceOf(IfAst.class, select.solutionModifier().orderBy().getFirst().expression());

        assertEquals(1, select.solutionModifier().orderBy().size());
        assertEquals(ASTConstants.OrderDirection.ASC, select.solutionModifier().orderBy().getFirst().orderDirection());
        assertInstanceOf(BoundAst.class, ifAst.condition());
        assertInstanceOf(StrAst.class, ifAst.thenExpr());
        assertInstanceOf(VarAst.class, ifAst.elseExpr());
        assertEquals("s", ((VarAst) ifAst.elseExpr()).name());
    }

    @Test
    @DisplayName("Should accept ORDER BY variable visible through OPTIONAL and UNION")
    void shouldAcceptOrderByVariableVisibleThroughOptionalAndUnion() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                    SELECT ?s WHERE {
                        OPTIONAL {
                            { ?s ?p ?o }
                            UNION
                            { ?s ?q ?o }
                        }
                    }
                    ORDER BY ?o
                """);

        SelectQueryAst select = (SelectQueryAst) ast;

        assertEquals(1, select.solutionModifier().orderBy().size());
        assertInstanceOf(VarAst.class, select.solutionModifier().orderBy().getFirst().expression());
        assertEquals("o", ((VarAst) select.solutionModifier().orderBy().getFirst().expression()).name());
    }

    @Test
    @DisplayName("Should accept projection variables visible through OPTIONAL")
    void shouldAcceptProjectionVisibleThroughOptional() {
        SparqlParser parser = newParserDefault();

        assertDoesNotThrow(() -> parser.parse("""
                    SELECT ?o WHERE {
                        OPTIONAL { ?s ?p ?o }
                    }
                """));
    }

    @Test
    @DisplayName("Should parse SELECT DISTINCT ?city ?country WHERE with BGP and UNION")
    void shouldParseSelectDistinctWithUnionQueryTest() {
        SparqlParser parser = newParserDefault();

        //https://en.wikibooks.org/wiki/SPARQL/UNION
        QueryAst ast = parser.parse("""
                SELECT DISTINCT ?city ?country
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
        assertEquals("country", projection.variables().get(1).name());
    }

    @Test
    @DisplayName("Insertion of a comment before the query should not change the treatment of the query")
    void shouldIgnoreCommentBeforeQuery() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                # This is a test comment
                SELECT DISTINCT ?c ?p
                WHERE {
                    ?s a ?c ;
                        ?p ?o .
                } LIMIT 10
                """;
        assertDoesNotThrow(() -> {
                    parser.parse(commentedQuery);
                });
        // Test if the query is still exactly as expected
        QueryAst ast = parser.parse(commentedQuery);
        assertNotNull(ast);
        assertInstanceOf(SelectQueryAst.class, ast);
        SelectQueryAst selectQueryAst = (SelectQueryAst) ast;
        assertNotNull(selectQueryAst.projection());
        assertInstanceOf(ProjectionAst.class, selectQueryAst.projection());
        assertEquals(2, selectQueryAst.projection().variables().size());
        assertInstanceOf(VarAst.class, selectQueryAst.projection().variables().getFirst());
        assertEquals("c", selectQueryAst.projection().variables().getFirst().name());
        assertInstanceOf(VarAst.class, selectQueryAst.projection().variables().getLast());
        assertEquals("p", selectQueryAst.projection().variables().getLast().name());
        assertNotNull(selectQueryAst.whereClause());
        assertEquals(1, selectQueryAst.whereClause().patterns().size());
        assertNotNull(selectQueryAst.whereClause().patterns().getFirst());
        assertInstanceOf(BgpAst.class, selectQueryAst.whereClause().patterns().getFirst());
        BgpAst bgpAst = (BgpAst) selectQueryAst.whereClause().patterns().getFirst();
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
        assertNotNull(selectQueryAst.solutionModifier());
        assertEquals(10, selectQueryAst.solutionModifier().limit());
    }

    @Test
    @DisplayName("Insertion of a comment after the query should not change the treatment of the query")
    void shouldIgnoreCommentAfterQuery() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                SELECT DISTINCT ?c ?p
                WHERE {
                    ?s a ?c ;
                        ?p ?o .
                } LIMIT 10
               # This is a test comment
               """;
        assertDoesNotThrow(() -> {
            parser.parse(commentedQuery);
        });
        // Test if the query is still exactly as expected
        QueryAst ast = parser.parse(commentedQuery);
        assertNotNull(ast);
        assertInstanceOf(SelectQueryAst.class, ast);
        SelectQueryAst selectQueryAst = (SelectQueryAst) ast;
        assertNotNull(selectQueryAst.projection());
        assertInstanceOf(ProjectionAst.class, selectQueryAst.projection());
        assertEquals(2, selectQueryAst.projection().variables().size());
        assertInstanceOf(VarAst.class, selectQueryAst.projection().variables().getFirst());
        assertEquals("c", selectQueryAst.projection().variables().getFirst().name());
        assertInstanceOf(VarAst.class, selectQueryAst.projection().variables().getLast());
        assertEquals("p", selectQueryAst.projection().variables().getLast().name());
        assertNotNull(selectQueryAst.whereClause());
        assertEquals(1, selectQueryAst.whereClause().patterns().size());
        assertNotNull(selectQueryAst.whereClause().patterns().getFirst());
        assertInstanceOf(BgpAst.class, selectQueryAst.whereClause().patterns().getFirst());
        BgpAst bgpAst = (BgpAst) selectQueryAst.whereClause().patterns().getFirst();
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
        assertNotNull(selectQueryAst.solutionModifier());
        assertEquals(10, selectQueryAst.solutionModifier().limit());
    }

    @Test
    @DisplayName("Insertion of a comment in the middle of the query should not change the treatment of the query")
    void shouldIgnoreCommentInTheMiddleOfQuery() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                SELECT DISTINCT ?c ?p
                WHERE {
                    ?s a ?c ;
               # This is a test comment
                        ?p ?o .
                } LIMIT 10
               """;
        assertDoesNotThrow(() -> {
            parser.parse(commentedQuery);
        });
        // Test if the query is still exactly as expected
        QueryAst ast = parser.parse(commentedQuery);
        assertNotNull(ast);
        assertInstanceOf(SelectQueryAst.class, ast);
        SelectQueryAst selectQueryAst = (SelectQueryAst) ast;
        assertNotNull(selectQueryAst.projection());
        assertInstanceOf(ProjectionAst.class, selectQueryAst.projection());
        assertEquals(2, selectQueryAst.projection().variables().size());
        assertInstanceOf(VarAst.class, selectQueryAst.projection().variables().getFirst());
        assertEquals("c", selectQueryAst.projection().variables().getFirst().name());
        assertInstanceOf(VarAst.class, selectQueryAst.projection().variables().getLast());
        assertEquals("p", selectQueryAst.projection().variables().getLast().name());
        assertNotNull(selectQueryAst.whereClause());
        assertEquals(1, selectQueryAst.whereClause().patterns().size());
        assertNotNull(selectQueryAst.whereClause().patterns().getFirst());
        assertInstanceOf(BgpAst.class, selectQueryAst.whereClause().patterns().getFirst());
        BgpAst bgpAst = (BgpAst) selectQueryAst.whereClause().patterns().getFirst();
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
        assertNotNull(selectQueryAst.solutionModifier());
        assertEquals(10, selectQueryAst.solutionModifier().limit());
    }

    @Test
    @DisplayName("Insertion of a comment in the middle of the query should not change the treatment of the query, even with a query containing #")
    void shouldIgnoreCommentInTheMiddleOfQueryWithIRI() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                SELECT DISTINCT ?c ?o
                WHERE {
                    ?s a ?c ;
               # This is a test comment
                        <http://ns.inria.fr/test#property> ?o .
                } LIMIT 10
               """;
        assertDoesNotThrow(() -> {
            parser.parse(commentedQuery);
        });
        // Test if the query is still exactly as expected
        QueryAst ast = parser.parse(commentedQuery);
        assertNotNull(ast);
        assertInstanceOf(SelectQueryAst.class, ast);
        SelectQueryAst selectQueryAst = (SelectQueryAst) ast;
        assertNotNull(selectQueryAst.projection());
        assertInstanceOf(ProjectionAst.class, selectQueryAst.projection());
        assertEquals(2, selectQueryAst.projection().variables().size());
        assertInstanceOf(VarAst.class, selectQueryAst.projection().variables().getFirst());
        assertEquals("c", selectQueryAst.projection().variables().getFirst().name());
        assertInstanceOf(VarAst.class, selectQueryAst.projection().variables().getLast());
        assertEquals("o", selectQueryAst.projection().variables().getLast().name());
        assertNotNull(selectQueryAst.whereClause());
        assertEquals(1, selectQueryAst.whereClause().patterns().size());
        assertNotNull(selectQueryAst.whereClause().patterns().getFirst());
        assertInstanceOf(BgpAst.class, selectQueryAst.whereClause().patterns().getFirst());
        BgpAst bgpAst = (BgpAst) selectQueryAst.whereClause().patterns().getFirst();
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
        assertInstanceOf(IriAst.class, bgpAst.triples().getLast().predicate());
        assertEquals("<http://ns.inria.fr/test#property>", ((IriAst)bgpAst.triples().getLast().predicate()).raw());
        assertInstanceOf(VarAst.class, bgpAst.triples().getLast().object());
        assertEquals("o", ((VarAst)bgpAst.triples().getLast().object()).name());
        assertNotNull(selectQueryAst.solutionModifier());
        assertEquals(10, selectQueryAst.solutionModifier().limit());
    }
    @Test
    @DisplayName("a list of From graphs can be inserted")
    public void fromGraphs() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                SELECT DISTINCT ?c ?o
                FROM <http://ns.inria.fr/graph>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                } LIMIT 10
               """;
        QueryAst queryAst = parser.parse(commentedQuery);
        assertNotNull(queryAst);
        assertNotNull(queryAst.datasetClause());
        assertNotNull(queryAst.datasetClause().graphs());
        assertEquals(1, queryAst.datasetClause().graphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[0]);
        assertNotNull(queryAst.datasetClause().namedGraphs());
        assertEquals(0, queryAst.datasetClause().namedGraphs().size());
    }

    @Test
    @DisplayName("a list of From graphs can be inserted")
    public void fromMultipleGraphs() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                SELECT DISTINCT ?c ?o
                FROM <http://ns.inria.fr/graph1>
                FROM <http://ns.inria.fr/graph2>
                FROM <http://ns.inria.fr/graph3>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                } LIMIT 10
               """;
        QueryAst queryAst = parser.parse(commentedQuery);
        assertNotNull(queryAst);
        assertNotNull(queryAst.datasetClause());
        assertNotNull(queryAst.datasetClause().graphs());
        assertEquals(3, queryAst.datasetClause().graphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[0]);
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[1]);
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[2]);
        assertNotNull(queryAst.datasetClause().namedGraphs());
        assertEquals(0, queryAst.datasetClause().namedGraphs().size());
    }

    @Test
    @DisplayName("a list of From graphs can be inserted")
    public void fromNamedGraphs() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                SELECT DISTINCT ?c ?o
                FROM NAMED <http://ns.inria.fr/graph>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                } LIMIT 10
               """;
        QueryAst queryAst = parser.parse(commentedQuery);
        assertNotNull(queryAst);
        assertNotNull(queryAst.datasetClause());
        assertNotNull(queryAst.datasetClause().namedGraphs());
        assertEquals(1, queryAst.datasetClause().namedGraphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().namedGraphs().toArray()[0]);
        assertNotNull(queryAst.datasetClause().graphs());
        assertEquals(0, queryAst.datasetClause().graphs().size());
    }

    @Test
    @DisplayName("a list of From graphs can be inserted")
    public void fromMultipleNamedGraphs() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                SELECT DISTINCT ?c ?o
                FROM NAMED <http://ns.inria.fr/graph1>
                FROM NAMED <http://ns.inria.fr/graph2>
                FROM NAMED <http://ns.inria.fr/graph3>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                } LIMIT 10
               """;
        QueryAst queryAst = parser.parse(commentedQuery);
        assertNotNull(queryAst);
        assertNotNull(queryAst.datasetClause());
        assertNotNull(queryAst.datasetClause().graphs());
        assertNotNull(queryAst.datasetClause().namedGraphs());
        assertEquals(3, queryAst.datasetClause().namedGraphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().namedGraphs().toArray()[0]);
        assertInstanceOf(IriAst.class, queryAst.datasetClause().namedGraphs().toArray()[1]);
        assertInstanceOf(IriAst.class, queryAst.datasetClause().namedGraphs().toArray()[2]);
        assertEquals(0, queryAst.datasetClause().graphs().size());
    }

    @Test
    @DisplayName("a list of From graphs can be inserted")
    public void fromMultipleMixedGraphs() {
        SparqlParser parser = newParserDefault();
        String commentedQuery = """
                SELECT DISTINCT ?c ?o
                FROM <http://ns.inria.fr/graph1>
                FROM NAMED <http://ns.inria.fr/graph2>
                FROM <http://ns.inria.fr/graph3>
                WHERE {
                    ?s a ?c ;
                        <http://ns.inria.fr/test#property> ?o .
                } LIMIT 10
               """;
        QueryAst queryAst = parser.parse(commentedQuery);
        assertNotNull(queryAst);
        assertNotNull(queryAst.datasetClause());
        assertNotNull(queryAst.datasetClause().graphs());
        assertEquals(2, queryAst.datasetClause().graphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[0]);
        assertInstanceOf(IriAst.class, queryAst.datasetClause().graphs().toArray()[1]);
        assertNotNull(queryAst.datasetClause().namedGraphs());
        assertEquals(1, queryAst.datasetClause().namedGraphs().size());
        assertInstanceOf(IriAst.class, queryAst.datasetClause().namedGraphs().toArray()[0]);
    }

    @Test
    @DisplayName("Should parse a SELECT Subquery inside WHERE")
    public void shouldParseASelectWithSubquery() {
        SparqlParser parser = newParserDefault();
        QueryAst ast = parser.parse("""
            PREFIX foaf: <http://xmlns.com/foaf/0.1/>
            SELECT ?s WHERE {
              {
                SELECT ?s WHERE {
                  ?s a foaf:Person
                }
              }
            }
            """);

        assertInstanceOf(SelectQueryAst.class, ast);

        SelectQueryAst outer = (SelectQueryAst) ast;
        GroupGraphPatternAst outerWhere = outer.whereClause();

        assertEquals(1, outerWhere.patterns().size());
        assertInstanceOf(GroupGraphPatternAst.class, outerWhere.patterns().getFirst());

        GroupGraphPatternAst wrapperGroup = (GroupGraphPatternAst) outerWhere.patterns().getFirst();
        assertEquals(1, wrapperGroup.patterns().size());
        assertInstanceOf(SubQueryAst.class, wrapperGroup.patterns().getFirst());

        SubQueryAst subQuery = (SubQueryAst) wrapperGroup.patterns().getFirst();
        assertInstanceOf(SelectQueryAst.class, subQuery.query());

        SelectQueryAst inner = (SelectQueryAst) subQuery.query();
        assertNotNull(inner.whereClause());
        assertEquals(1, inner.whereClause().patterns().size());
        assertInstanceOf(BgpAst.class, inner.whereClause().patterns().getFirst());
    }
}
