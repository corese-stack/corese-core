package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
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
    @DisplayName("DESCRIBE ?x WHERE { ... }: body compiled, DESCRIBE flag set, ?x in the describe list")
    void describesVariableBoundByWhere() {
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("x")), whereBindingX());

        Query query = builder.toQuery(describe);

        assertTrue(query.isDescribe(), "query flagged as DESCRIBE");
        assertTrue(query.getBody().isAnd(), "WHERE compiled into the body");
        assertEquals(1, query.getDescribeList().size());
        assertTrue(query.getDescribeList().getFirst().isVariable(), "described term is the variable ?x");
    }

    @Test
    @DisplayName("DESCRIBE <uri> with no WHERE: empty body, IRI in the describe list")
    void describesFixedResourceWithoutWhere() {
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new IriAst("<http://example.org/x>")), null);

        Query query = builder.toQuery(describe);

        assertTrue(query.isDescribe());
        assertTrue(query.getBody().isAnd(), "default empty WHERE compiles to an empty AND");
        assertEquals(0, query.getBody().size(), "no pattern in the body");
        assertEquals(1, query.getDescribeList().size());
        assertFalse(query.getDescribeList().getFirst().isVariable(), "described term is a fixed IRI");
    }

    @Test
    @DisplayName("DESCRIBE * : DESCRIBE flag set and an empty describe list (= describe all in-scope)")
    void describeAllHasEmptyList() {
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(), whereBindingX());

        Query query = builder.toQuery(describe);

        assertTrue(query.isDescribe());
        assertTrue(query.getDescribeList().isEmpty(), "DESCRIBE * carries no explicit node");
    }

    @Test
    @DisplayName("FROM / FROM NAMED is not supported yet → UnsupportedOperationException")
    void rejectsDatasetClause() {
        DatasetClauseAst dataset = new DatasetClauseAst(
                Set.of(new IriAst("http://example.org/g")), Set.of());
        DescribeQueryAst describe = new DescribeQueryAst(
                dataset, List.of(new VarAst("x")), whereBindingX());

        assertThrows(UnsupportedOperationException.class, () -> builder.toQuery(describe));
    }

    @Test
    @DisplayName("FROM NAMED is not supported yet → UnsupportedOperationException")
    void rejectsFromNamedClause() {
        DatasetClauseAst dataset = new DatasetClauseAst(
                Set.of(), Set.of(new IriAst("http://example.org/g")));
        DescribeQueryAst describe = new DescribeQueryAst(
                dataset, List.of(new VarAst("x")), whereBindingX());

        assertThrows(UnsupportedOperationException.class, () -> builder.toQuery(describe));
    }

    @Test
    @DisplayName("Inline VALUES is not supported yet → UnsupportedOperationException")
    void rejectsValuesClause() {
        ValuesAst values = new ValuesAst(List.of(
                new ValueMappingAst(Map.of(new VarAst("x"), new IriAst("http://example.org/v")))));
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("x")), whereBindingX(), null, null, values);

        assertThrows(UnsupportedOperationException.class, () -> builder.toQuery(describe));
    }

    @Test
    @DisplayName("Solution modifiers (LIMIT) are not supported yet → UnsupportedOperationException")
    void rejectsSolutionModifier() {
        SolutionModifierAst mod = SolutionModifierAst.withoutGroupBy(false, false, List.of(), 10L, null);
        DescribeQueryAst describe = new DescribeQueryAst(
                DatasetClauseAst.none(), List.of(new VarAst("x")), whereBindingX(), mod);

        assertThrows(UnsupportedOperationException.class, () -> builder.toQuery(describe));
    }

    @Test
    @DisplayName("toQuery(null) throws NullPointerException")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> builder.toQuery((DescribeQueryAst) null));
    }
}