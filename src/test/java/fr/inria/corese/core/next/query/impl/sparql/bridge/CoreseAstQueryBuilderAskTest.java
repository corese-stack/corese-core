package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.GreaterThanAst;
import fr.inria.corese.core.next.query.kgram.core.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CoreseAstQueryBuilderAskTest {

    private final CoreseAstQueryBuilder builder = new CoreseAstQueryBuilder();

    private static GroupGraphPatternAst whereOneTriple() {
        return new GroupGraphPatternAst(List.of(
                new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))))));
    }

    @Test
    @DisplayName("Query body is the compiled WHERE and the query is flagged as ASK")
    void buildsBodyAndAskFlag() {
        AskQueryAst ask = new AskQueryAst(DatasetClauseAst.none(), whereOneTriple());

        Query query = builder.toQuery(ask);

        assertNotNull(query.getBody(), "body set from WHERE");
        assertTrue(query.getBody().isAnd(), "group compiles to AND");
        assertTrue(query.getBody().get(0).isBGP(), "containing the BGP");
        assertTrue(query.isAsk(), "query flagged as ASK");
    }

    @Test
    @DisplayName("A FILTER in the WHERE no longer throws an NPE (lExp fix)")
    void buildsAskWithFilter() {
        FilterAst filter = new FilterAst(new GreaterThanAst(List.of(
                new VarAst("o"), new LiteralAst("5", null, null))));
        GroupGraphPatternAst where = new GroupGraphPatternAst(List.of(
                new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o")))),
                filter));
        AskQueryAst ask = new AskQueryAst(DatasetClauseAst.none(), where);

        Query query = builder.toQuery(ask);

        assertTrue(query.isAsk());
        assertTrue(query.getBody().get(1).isFilter(), "FILTER compiled into the body");
    }

    @Test
    @DisplayName("FROM is not supported yet → UnsupportedOperationException")
    void rejectsFromClause() {
        DatasetClauseAst dataset = new DatasetClauseAst(
                Set.of(new IriAst("http://example.org/g")), Set.of());
        AskQueryAst ask = new AskQueryAst(dataset, whereOneTriple());

        assertThrows(UnsupportedOperationException.class, () -> builder.toQuery(ask));
    }

    @Test
    @DisplayName("FROM NAMED is not supported yet → UnsupportedOperationException")
    void rejectsFromNamedClause() {
        DatasetClauseAst dataset = new DatasetClauseAst(
                Set.of(), Set.of(new IriAst("http://example.org/g")));
        AskQueryAst ask = new AskQueryAst(dataset, whereOneTriple());

        assertThrows(UnsupportedOperationException.class, () -> builder.toQuery(ask));
    }

    @Test
    @DisplayName("Inline VALUES is not supported yet → UnsupportedOperationException")
    void rejectsValuesClause() {
        ValuesAst values = new ValuesAst(List.of(
                new ValueMappingAst(Map.of(new VarAst("s"), new IriAst("http://example.org/x")))));
        AskQueryAst ask = new AskQueryAst(
                DatasetClauseAst.none(), whereOneTriple(), null, null, values);

        assertThrows(UnsupportedOperationException.class, () -> builder.toQuery(ask));
    }

    @Test
    @DisplayName("toQuery(null) throws NullPointerException")
    void rejectsNullAsk() {
        assertThrows(NullPointerException.class, () -> builder.toQuery(null));
    }

    @Test
    @DisplayName("ASK {} with empty WHERE compiles to an empty AND body")
    void buildsAskWithEmptyWhere() {
        AskQueryAst ask = new AskQueryAst(DatasetClauseAst.none(),
                new GroupGraphPatternAst(List.of()));

        Query query = builder.toQuery(ask);

        assertNotNull(query.getBody());
        assertTrue(query.getBody().isAnd());
        assertEquals(0, query.getBody().size(), "empty AND — no children");
        assertTrue(query.isAsk());
    }

    @Test
    @DisplayName("Solution modifiers (LIMIT) are not supported yet → UnsupportedOperationException")
    void rejectsSolutionModifier() {
        SolutionModifierAst mod = SolutionModifierAst.withoutGroupBy(false, false, List.of(), 10L, null);
        AskQueryAst ask = new AskQueryAst(DatasetClauseAst.none(), whereOneTriple(), mod, null, null);

        assertThrows(UnsupportedOperationException.class, () -> builder.toQuery(ask));
    }
}