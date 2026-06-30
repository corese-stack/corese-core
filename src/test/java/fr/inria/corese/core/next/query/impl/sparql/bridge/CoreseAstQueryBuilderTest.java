package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.parser.AbstractSparqlParserFeatureTest;
import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DatasetClauseAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.OrderConditionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAsts;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SolutionModifierAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ASTConstants;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(!query.getOrderBy().getFirst().status(), "ASC should keep the default ascending status");
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

    private static TriplePatternAst singleTriple() {
        return new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"));
    }

    private static GroupGraphPatternAst group(TriplePatternAst triple) {
        return new GroupGraphPatternAst(List.of(new BgpAst(List.of(triple))));
    }

    private static List<String> labels(List<Node> nodes) {
        return nodes.stream().map(Node::getLabel).toList();
    }
}
