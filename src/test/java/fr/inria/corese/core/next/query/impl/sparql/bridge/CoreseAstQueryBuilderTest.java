package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.parser.AbstractSparqlParserFeatureTest;
import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DatasetClauseAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.MinusAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.OrderConditionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAsts;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ASTConstants;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.ExistsAst;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.impl.kgram.api.core.ExprType;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;
import fr.inria.corese.core.next.query.impl.kgram.core.Exp;
import fr.inria.corese.core.next.query.impl.kgram.core.Query;
import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreseAstQueryBuilderTest extends AbstractSparqlParserFeatureTest {

    private final CoreseAstQueryBuilder builder = new CoreseAstQueryBuilder();

    @Test
    @DisplayName("SELECT * WHERE { ?s ?p ?o . } builds a next Query")
    void buildsMinimalSelectStarQuery() {
        SelectQueryAst select = new SelectQueryAst(group(singleTriple()));

        Query query = builder.toNextQuery(select);

        assertTrue(query.getBody().isAnd(), "the query body should be an AND group");
        assertEquals(1, query.getBody().size(), "the WHERE should contain one compiled element");

        Exp bgp = query.getBody().get(0);
        assertTrue(bgp.isBGP(), "the compiled WHERE should contain one BGP");
        assertEquals(List.of("s", "p", "o"), labels(query.getSelect()),
                "SELECT * should project the visible WHERE variables");
    }

    @Test
    @DisplayName("Parser -> SelectQueryAst -> Query keeps the SELECT * path executable")
    void buildsQueryFromParsedSelectStar() {
        QueryAst ast = newParserDefault().parse("SELECT * WHERE { ?s ?p ?o . }");
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);

        Query query = builder.toNextQuery(select);

        assertEquals(List.of("s", "p", "o"), labels(query.getSelect()));
        assertTrue(query.getBody().get(0).isBGP());
    }

    @Test
    @DisplayName("Prefixed names and default prefix are expanded to full IRIs in compiled Query")
    void expandsPrefixesInQuery() {
        QueryAst ast = newParserDefault().parse(
                "PREFIX : <http://example.org/ns#> " +
                "PREFIX ex: <http://example.org/ex#> " +
                "SELECT * WHERE { ?s :p ex:o . }");
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        Query query = builder.toNextQuery(select);

        Exp bgp = query.getBody().get(0);
        Edge edge = bgp.get(0).getEdge();
        assertEquals("http://example.org/ns#p", edge.getEdgeNode().getLabel());
        assertEquals("http://example.org/ex#o", edge.getNode(1).getLabel());
    }

    @Test
    @DisplayName("Explicit angle-bracketed IRIs are not treated as prefixed names")
    void preservesAngleBracketedIrisWithoutPrefixExpansion() {
        QueryAst ast = newParserDefault().parse(
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "SELECT * WHERE { ?s <rdf:type> ?o . }");
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        Query query = builder.toNextQuery(select);

        Exp bgp = query.getBody().get(0);
        Edge edge = bgp.get(0).getEdge();
        assertEquals("rdf:type", edge.getEdgeNode().getLabel());
    }

    @Test
    @DisplayName("Multiple identical BASE declarations are rejected with QuerySyntaxException")
    void rejectsMultipleIdenticalBaseDeclarations() {
        var parser = newParserDefault();
        String query = "BASE <http://example.org/> " +
                "BASE <http://example.org/> " +
                "SELECT * WHERE { ?s ?p ?o . }";
        assertThrows(QuerySyntaxException.class, () -> parser.parse(query));
    }

    @Test
    @DisplayName("Boolean and numeric literals retain XSD datatypes even if query defines matching prefix like http:")
    void preservesNormalizedDatatypesWithConflictingPrefix() {
        QueryAst ast = newParserDefault().parse(
                "PREFIX http: <http://wrong/> " +
                "SELECT * WHERE { ?s ?p true . }");
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        Query query = builder.toNextQuery(select);

        Exp bgp = query.getBody().get(0);
        Edge edge = bgp.get(0).getEdge();
        Node object = edge.getNode(1);
        assertEquals("http://www.w3.org/2001/XMLSchema#boolean", object.getDatatypeValue().getDatatypeURI());
    }

    @Test
    @DisplayName("urn: and file: can be declared and used as SPARQL prefixes")
    void expandsUrnAndFilePrefixes() {
        QueryAst ast = newParserDefault().parse(
                "PREFIX urn: <http://example.org/urn/> " +
                "PREFIX file: <http://example.org/file/> " +
                "SELECT * WHERE { ?s urn:p \"val\"^^file:type . }");
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        Query query = builder.toNextQuery(select);

        Exp bgp = query.getBody().get(0);
        Edge edge = bgp.get(0).getEdge();
        assertEquals("http://example.org/urn/p", edge.getEdgeNode().getLabel());
        assertEquals("http://example.org/file/type", edge.getNode(1).getDatatypeValue().getDatatypeURI());
    }

    @Test
    @DisplayName("Explicit projection reuses the compiled query nodes")
    void buildsExplicitProjection() {
        SelectQueryAst select = new SelectQueryAst(
                ProjectionAsts.of(List.of(new VarAst("o"), new VarAst("s"))),
                null,
                group(singleTriple()));

        Query query = builder.toNextQuery(select);

        assertEquals(List.of("o", "s"), labels(query.getSelect()));
    }

    @Test
    @DisplayName("SELECT * excludes variables that only occur in MINUS")
    void selectStarExcludesMinusOnlyVariables() {
        SelectQueryAst select = parseSelect("SELECT * WHERE { ?s ?p ?o . MINUS { ?s ?q ?z } }");

        Query query = builder.toNextQuery(select);

        assertEquals(List.of("s", "p", "o"), labels(query.getSelect()),
                "MINUS-only variables are stored separately and must not be visible to SELECT *");
        assertFalse(labels(query.getSelect()).contains("z"));
    }

    @Test
    @DisplayName("Explicit projection rejects a variable that only occurs in MINUS")
    void explicitProjectionRejectsMinusOnlyVariable() {
        SelectQueryAst select = new SelectQueryAst(
                ProjectionAsts.of(List.of(new VarAst("z"))),
                null,
                group(bgp("s", "p", "o"), new MinusAst(group(bgp("s", "q", "z")))));

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> builder.toNextQuery(select));

        assertEquals("Projected variable ?z is not visible in the compiled query body", error.getMessage());
    }

    @Test
    @DisplayName("SELECT * includes variables introduced by OPTIONAL")
    void selectStarIncludesOptionalVariables() {
        SelectQueryAst select = parseSelect("SELECT * WHERE { ?s ?p ?o . OPTIONAL { ?s ?q ?z } }");

        Query query = builder.toNextQuery(select);

        assertEquals(List.of("s", "p", "o", "q", "z"), labels(query.getSelect()),
                "OPTIONAL variables remain visible in the outer query scope");
    }

    @Test
    @DisplayName("DISTINCT, LIMIT and OFFSET are copied onto the next Query")
    void appliesSimpleSolutionModifiers() {
        SelectQueryAst select = new SelectQueryAst(
                ProjectionAsts.selectAll(),
                null,
                group(singleTriple()),
                SolutionModifierAst.withoutGroupBy(true, false, List.of(), 10L, 3L));

        Query query = builder.toNextQuery(select);

        assertTrue(query.isDistinct());
        assertEquals(10, query.getLimit());
        assertEquals(3, query.getOffset());
    }

    @Test
    @DisplayName("ORDER BY ?var is mapped onto runtime order expressions")
    void appliesVariableOrderBy() {
        SelectQueryAst select = new SelectQueryAst(
                ProjectionAsts.selectAll(),
                null,
                group(singleTriple()),
                SolutionModifierAst.withoutGroupBy(false, false,
                        List.of(new OrderConditionAst(ASTConstants.OrderDirection.ASC, new VarAst("s"))),
                        null,
                        null));

        Query query = builder.toNextQuery(select);

        assertEquals(1, query.getOrderBy().size());
        assertEquals("s", query.getOrderBy().getFirst().getNode().getLabel());
        assertFalse(query.getOrderBy().getFirst().status(), "ASC should keep the default ascending status");
    }

    @Test
    @DisplayName("ORDER BY DESC(?var) keeps the descending flag on the runtime expression")
    void appliesDescendingOrderBy() {
        QueryAst ast = newParserDefault().parse("SELECT * WHERE { ?s ?p ?o . } ORDER BY DESC(?o)");
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);

        Query query = builder.toNextQuery(select);

        assertEquals(1, query.getOrderBy().size());
        assertEquals("o", query.getOrderBy().getFirst().getNode().getLabel());
        assertTrue(query.getOrderBy().getFirst().status(), "DESC should be preserved on the runtime expression");
    }

    @Test
    @DisplayName("ORDER BY expressions are wrapped as runtime filters")
    void appliesExpressionOrderBy() {
        QueryAst ast = newParserDefault().parse("SELECT * WHERE { ?s ?p ?o . } ORDER BY STR(?o)");
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);

        Query query = builder.toNextQuery(select);

        assertEquals(1, query.getOrderBy().size());
        assertNotNull(query.getOrderBy().getFirst().getFilter(),
                "non-variable ORDER BY expressions should be carried by a runtime filter");
    }

    @Test
    @DisplayName("ORDER BY rejects a variable that only occurs in MINUS")
    void orderByRejectsMinusOnlyVariable() {
        SelectQueryAst select = new SelectQueryAst(
                ProjectionAsts.selectAll(),
                null,
                group(bgp("s", "p", "o"), new MinusAst(group(bgp("s", "q", "z")))),
                SolutionModifierAst.withoutGroupBy(false, false,
                        List.of(new OrderConditionAst(ASTConstants.OrderDirection.ASC, new VarAst("z"))),
                        null,
                        null));

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> builder.toNextQuery(select));

        assertEquals("ORDER BY variable ?z is not visible in the compiled query", error.getMessage());
    }

    @Test
    @DisplayName("ORDER BY accepts a variable introduced by OPTIONAL")
    void orderByAcceptsOptionalVariable() {
        SelectQueryAst select = parseSelect("SELECT * WHERE { ?s ?p ?o . OPTIONAL { ?s ?q ?z } } ORDER BY ?z");

        Query query = builder.toNextQuery(select);

        assertEquals(1, query.getOrderBy().size());
        assertEquals("z", query.getOrderBy().getFirst().getNode().getLabel());
    }

    @Test
    @DisplayName("FROM and FROM NAMED are copied onto the next Query dataset")
    void appliesDatasetClauses() {
        DatasetClauseAst dataset = new DatasetClauseAst(
                Set.of(new IriAst("http://example.org/default")),
                Set.of(new IriAst("http://example.org/named")));
        SelectQueryAst select = new SelectQueryAst(
                ProjectionAsts.selectAll(),
                dataset,
                group(singleTriple()));

        Query query = builder.toNextQuery(select);

        assertEquals(List.of("http://example.org/default"), labels(query.getFrom()));
        assertEquals(List.of("http://example.org/named"), labels(query.getNamed()));
    }

    @Test
    @DisplayName("Projecting a variable outside the WHERE stays an explicit error")
    void rejectsProjectionOutsideWhereScope() {
        SelectQueryAst select = new SelectQueryAst(
                ProjectionAsts.of(List.of(new VarAst("missing"))),
                null,
                group(singleTriple()));

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> builder.toNextQuery(select));

        assertEquals("Projected variable ?missing is not visible in the compiled query body", error.getMessage());
    }

    @Test
    @DisplayName("toNextQuery(null) throws NullPointerException for SELECT as well")
    void rejectsNullSelect() {
        assertThrows(NullPointerException.class, () -> builder.toNextQuery((SelectQueryAst) null));
    }

    @Test
    @DisplayName("ORDER BY on an unknown variable stays an explicit error")
    void rejectsUnknownOrderByVariable() {
        SelectQueryAst select = new SelectQueryAst(
                ProjectionAsts.selectAll(),
                null,
                group(singleTriple()),
                SolutionModifierAst.withoutGroupBy(false, false,
                        List.of(new OrderConditionAst(ASTConstants.OrderDirection.ASC, new VarAst("missing"))),
                        null,
                        null));

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> builder.toNextQuery(select));

        assertEquals("ORDER BY variable ?missing is not visible in the compiled query", error.getMessage());
    }

    @Test
    @DisplayName("SELECT WHERE { FILTER EXISTS { ... } } compiles the EXISTS pattern")
    void filterExistsInSelectWhere() {
        SelectQueryAst select = getSelectQueryAst();

        Query query = builder.toNextQuery(select);

        Exp filterExp = findFilter(query.getBody());
        assertNotNull(filterExp, "FILTER is in the compiled body");
        assertEquals(ExprType.EXIST, filterExp.getFilter().getExp().oper(), "operator is EXIST");
        assertInstanceOf(Exp.class, filterExp.getFilter().getExp().getPattern(), "EXISTS pattern is compiled");
    }

    private static SelectQueryAst getSelectQueryAst() {
        GroupGraphPatternAst existsPattern = new GroupGraphPatternAst(List.of(
                new BgpAst(List.of(new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))))));
        FilterAst filter = new FilterAst(new ExistsAst(existsPattern));
        GroupGraphPatternAst where = new GroupGraphPatternAst(List.of(
                new BgpAst(List.of(new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o")))),
                filter));
        return new SelectQueryAst(where);
    }

    @Test
    @DisplayName("Parser -> SELECT WHERE { FILTER NOT EXISTS } produces NOT wrapping EXIST")
    void parsedSelectWithFilterNotExists() {
        QueryAst ast = newParserDefault().parse(
                "SELECT * WHERE { ?s ?p ?o . FILTER NOT EXISTS { ?s <http://example.org/type> ?t } }");
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);

        Query query = builder.toNextQuery(select);

        Exp filterExp = findFilter(query.getBody());
        assertNotNull(filterExp, "FILTER NOT EXISTS compiled into body");
        Expr notExpr = filterExp.getFilter().getExp();
        assertEquals(ExprType.NOT, notExpr.oper(), "top operator is boolean NOT");
        assertEquals(ExprType.EXIST, notExpr.getExpList().getFirst().oper(), "inner expression is EXIST");
        assertInstanceOf(Exp.class, notExpr.getExpList().getFirst().getPattern(), "NOT EXISTS pattern is compiled");
    }

    @Test
    @DisplayName("Parser -> SELECT WHERE { FILTER(!EXISTS { ... }) } produces NOT wrapping EXIST")
    void parsedSelectWithNegatedExists() {
        QueryAst ast = newParserDefault().parse(
                "SELECT * WHERE { ?s ?p ?o . FILTER(!EXISTS { ?s <http://example.org/type> ?t }) }");
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);

        Query query = builder.toNextQuery(select);

        Exp filterExp = findFilter(query.getBody());
        assertNotNull(filterExp, "FILTER !EXISTS compiled into body");
        Expr notExpr = filterExp.getFilter().getExp();
        assertEquals(ExprType.NOT, notExpr.oper(), "top operator is boolean NOT");
        Expr exist = notExpr.getExpList().getFirst();
        assertEquals(ExprType.EXIST, exist.oper(), "inner expression is EXIST");
        assertInstanceOf(Exp.class, exist.getPattern(), "negated EXISTS pattern is compiled");
    }

    private static Exp findFilter(Exp body) {
        for (int i = 0; i < body.size(); i++) {
            if (body.get(i).isFilter()) {
                return body.get(i);
            }
        }
        return null;
    }

    private static TriplePatternAst singleTriple() {
        return new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"));
    }

    private static GroupGraphPatternAst group(TriplePatternAst triple) {
        return new GroupGraphPatternAst(List.of(new BgpAst(List.of(triple))));
    }

    private static GroupGraphPatternAst group(PatternAst... elements) {
        return new GroupGraphPatternAst(List.of(elements));
    }

    private static BgpAst bgp(String s, String p, String o) {
        return new BgpAst(List.of(
                new TriplePatternAst(new VarAst(s), new VarAst(p), new VarAst(o))));
    }

    private SelectQueryAst parseSelect(String query) {
        return assertInstanceOf(SelectQueryAst.class, newParserDefault().parse(query));
    }

    private static List<String> labels(List<Node> nodes) {
        return nodes.stream().map(Node::getLabel).toList();
    }
}
