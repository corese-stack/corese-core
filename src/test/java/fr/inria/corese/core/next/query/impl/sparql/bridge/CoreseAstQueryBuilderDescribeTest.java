package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
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

    @Test
    @DisplayName("DESCRIBE ?x WHERE { ... }: ?x resolves to the body node and is flagged DESCRIBE")
    void describesVariableResolvedAgainstBody() {
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("x")), whereBindingX());

        Query query = builder.toNextQuery(describe);

        assertTrue(query.isDescribe(), "query flagged as DESCRIBE");
        assertTrue(query.getBody().isAnd(), "WHERE compiled into the body");
        assertEquals(1, query.getDescribeList().size());
        Node described = query.getDescribeList().getFirst();
        assertTrue(described.isVariable());
        assertSame(query.getExtNode("x"), described, "described ?x is the runtime node bound by the body");
    }

    @Test
    @DisplayName("DESCRIBE <uri> with no WHERE: empty body, IRI as a fresh constant node")
    void describesFixedResourceWithoutWhere() {
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new IriAst("<http://example.org/x>")), null);

        Query query = builder.toNextQuery(describe);

        assertTrue(query.isDescribe());
        assertTrue(query.getBody().isAnd(), "default empty WHERE compiles to an empty AND");
        assertEquals(0, query.getBody().size(), "no pattern in the body");
        assertEquals(1, query.getDescribeList().size());
        assertFalse(query.getDescribeList().getFirst().isVariable(), "described term is a fixed IRI");
    }

    @Test
    @DisplayName("DESCRIBE * reuses the in-scope variables of the body (like SELECT *)")
    void describeAllUsesInScopeVariables() {
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(), whereBindingX());

        Query query = builder.toNextQuery(describe);

        List<Node> described = query.getDescribeList();
        assertFalse(described.isEmpty(), "DESCRIBE * describes the in-scope variables");
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
    @DisplayName("Inline VALUES is not supported yet → UnsupportedOperationException")
    void rejectsValuesClause() {
        ValuesAst values = new ValuesAst(List.of(
                new ValueMappingAst(Map.of(new VarAst("x"), new IriAst("http://example.org/v")))));
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("x")), whereBindingX(), null, null, values);

        assertThrows(UnsupportedOperationException.class, () -> builder.toNextQuery(describe));
    }

    @Test
    @DisplayName("Unsupported solution modifier (e.g. DISTINCT) → UnsupportedOperationException")
    void rejectsUnsupportedModifier() {
        SolutionModifierAst mod = SolutionModifierAst.withoutGroupBy(
                true, false, List.of(), null, null);
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("x")), whereBindingX(), mod);

        assertThrows(UnsupportedOperationException.class, () -> builder.toNextQuery(describe));
    }

    @Test
    @DisplayName("toNextQuery((DescribeQueryAst) null) throws NullPointerException")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> builder.toNextQuery((DescribeQueryAst) null));
    }
}