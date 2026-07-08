package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CoreseAstQueryBuilderDescribeTest {

    private final CoreseAstQueryBuilder builder = new CoreseAstQueryBuilder();

    private static GroupGraphPatternAst whereBindingX() {
        return new GroupGraphPatternAst(List.of(
                new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("x"), new VarAst("p"), new VarAst("o"))))));
    }

    private static GroupGraphPatternAst whereWithMinusOnlyZ() {
        return new GroupGraphPatternAst(List.of(
                new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("x"), new VarAst("p"), new VarAst("o")))),
                new MinusAst(new GroupGraphPatternAst(List.of(
                        new BgpAst(List.of(
                                new TriplePatternAst(new VarAst("x"), new VarAst("q"), new VarAst("z")))))))));
    }

    @Test
    @DisplayName("Parser -> DescribeQueryAst -> Query lowers DESCRIBE to a construct-like query")
    void parserDescribeAstToQuery() {
        SparqlParser parser = new SparqlParser();
        DescribeQueryAst describe = (DescribeQueryAst) parser.parse(
                "DESCRIBE ?x WHERE { ?x ?p ?o }");

        Query query = builder.toNextQuery(describe);

        assertLoweredDescribeConstruct(query, 1);
        assertTrue(query.getBody().isAnd());
        assertEquals(1, describeOptionalCount(query), "one DESCRIBE optional generated");
        assertSame(query.getExtNode("x"), query.getConstruct().get(0).getEdge().getNode(0));
    }

    @Test
    @DisplayName("DESCRIBE ?x WHERE { ... }: ?x resolves to the body node")
    void describesVariableResolvedAgainstBody() {
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("x")), whereBindingX());

        Query query = builder.toNextQuery(describe);

        assertLoweredDescribeConstruct(query, 1);
        assertTrue(query.getBody().isAnd(), "WHERE compiled into the body");
        assertEquals(1, describeOptionalCount(query));
        Node described = query.getConstruct().get(0).getEdge().getNode(0);
        assertTrue(described.isVariable());
        assertSame(query.getExtNode("x"), described, "described ?x is the runtime node bound by the body");
    }

    @Test
    @DisplayName("DESCRIBE <uri> with no WHERE: IRI as a fresh constant node and lowered construct shape")
    void describesFixedResourceWithoutWhere() {
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new IriAst("<http://example.org/x>")), null);

        Query query = builder.toNextQuery(describe);

        assertLoweredDescribeConstruct(query, 1);
        assertTrue(query.getBody().isAnd(), "body remains an AND");
        assertEquals(1, describeOptionalCount(query));
        assertFalse(query.getConstruct().get(0).getEdge().getNode(0).isVariable(), "described term is a fixed IRI");
    }

    @Test
    @DisplayName("DESCRIBE * reuses the in-scope variables of the body (like SELECT *)")
    void describeAllUsesInScopeVariables() {
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(), whereBindingX());

        Query query = builder.toNextQuery(describe);

        assertLoweredDescribeConstruct(query, 3);
        assertEquals(3, describeOptionalCount(query));
        List<Node> described = query.getConstruct().getNodes().stream()
                .filter(node -> "x".equals(node.getLabel()) || "p".equals(node.getLabel()) || "o".equals(node.getLabel()))
                .toList();
        assertTrue(described.stream().allMatch(Node::isVariable));
        assertTrue(described.stream().anyMatch(n -> "x".equals(n.getLabel())));
        assertTrue(described.stream().anyMatch(n -> "p".equals(n.getLabel())), "variable predicate is in scope too");
        assertTrue(described.stream().anyMatch(n -> "o".equals(n.getLabel())));
    }

    @Test
    @DisplayName("DESCRIBE ?y where ?y is not visible in the body → IllegalArgumentException")
    void rejectsDescribeVariableNotVisible() {
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("y")), whereBindingX());

        assertThrows(IllegalArgumentException.class, () -> builder.toNextQuery(describe));
    }

    @Test
    @DisplayName("DESCRIBE rejects a variable that only occurs in MINUS")
    void rejectsDescribeMinusOnlyVariable() {
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("z")), whereWithMinusOnlyZ());

        assertThrows(IllegalArgumentException.class, () -> builder.toNextQuery(describe));
    }

    @Test
    @DisplayName("FROM / FROM NAMED are applied to the query dataset")
    void appliesDatasetClause() {
        DatasetClauseAst dataset = new DatasetClauseAst(
                Set.of(new IriAst("http://example.org/g")),
                Set.of(new IriAst("http://example.org/n")));
        DescribeQueryAst describe = new DescribeQueryAst(
                dataset, List.of(new VarAst("x")), whereBindingX());

        Query query = builder.toNextQuery(describe);

        assertEquals(1, query.getFrom().size(), "FROM applied");
        assertEquals(1, query.getNamed().size(), "FROM NAMED applied");
    }

    @Test
    @DisplayName("LIMIT / OFFSET are applied to the query")
    void appliesLimitAndOffset() {
        SolutionModifierAst mod = SolutionModifierAst.withoutGroupBy(
                false, false, List.of(), 5L, 2L);
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("x")), whereBindingX(), mod);

        Query query = builder.toNextQuery(describe);

        assertEquals(5, query.getLimit(), "LIMIT applied");
        assertEquals(2, query.getOffset(), "OFFSET applied");
    }

    @Test
    @DisplayName("ORDER BY ?x is applied to the query")
    void appliesOrderBy() {
        SolutionModifierAst mod = SolutionModifierAst.withoutGroupBy(
                false, false,
                List.of(new OrderConditionAst(ASTConstants.OrderDirection.ASC, new VarAst("x"))),
                null, null);
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("x")), whereBindingX(), mod);

        Query query = builder.toNextQuery(describe);

        assertFalse(query.getOrderBy().isEmpty(), "ORDER BY applied");
    }

    @Test
    @DisplayName("Inline VALUES is not supported yet -> UnsupportedQueryFeatureException")
    void rejectsValuesClause() {
        ValuesAst values = new ValuesAst(List.of(
                new ValueMappingAst(Map.of(new VarAst("x"), new IriAst("http://example.org/v")))));
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("x")), whereBindingX(), null, null, values);

        assertThrows(UnsupportedQueryFeatureException.class, () -> builder.toNextQuery(describe));
    }

    @Test
    @DisplayName("Unsupported solution modifier (e.g. DISTINCT) -> UnsupportedQueryFeatureException")
    void rejectsUnsupportedModifier() {
        SolutionModifierAst mod = SolutionModifierAst.withoutGroupBy(
                true, false, List.of(), null, null);
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("x")), whereBindingX(), mod);

        assertThrows(UnsupportedQueryFeatureException.class, () -> builder.toNextQuery(describe));
    }

    @Test
    @DisplayName("toNextQuery((DescribeQueryAst) null) throws NullPointerException")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> builder.toNextQuery((DescribeQueryAst) null));
    }

    private static void assertLoweredDescribeConstruct(Query query, int describedNodeCount) {
        assertTrue(query.isConstruct(), "DESCRIBE is lowered to the construct runtime path");
        assertFalse(query.isSelect(), "a lowered DESCRIBE must not report itself as SELECT");
        assertNotNull(query.getConstruct(), "construct template created");
        assertEquals(2 * describedNodeCount, query.getConstruct().size(),
                "construct template has outgoing and incoming triple patterns per described node");
        assertFalse(query.getConstructNodes().isEmpty(), "construct nodes collected");
    }

    /**
     * Counts the DESCRIBE-generated {@code OPTIONAL { outgoing UNION incoming }} patterns directly
     * under the body — independently of how the parser shaped the original WHERE.
     */
    private static long describeOptionalCount(Query query) {
        Exp body = query.getBody();
        long count = 0;
        for (int i = 0; i < body.size(); i++) {
            Exp child = body.get(i);
            if (child.isOptional() && child.size() > 1 && child.get(1).isUnion()) {
                count++;
            }
        }
        return count;
    }
}
