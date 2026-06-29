package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.parser.AbstractSparqlParserFeatureTest;
import fr.inria.corese.core.next.query.impl.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.GreaterThanAst;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CoreseAstQueryBuilderAskTest extends AbstractSparqlParserFeatureTest {

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

        Query query = builder.toNextQuery(ask);

        assertNotNull(query.getBody(), "body set from WHERE");
        assertTrue(query.getBody().isAnd(), "group compiles to AND");
        assertTrue(query.getBody().get(0).isBGP(), "containing the BGP");
        assertTrue(query.isAsk(), "query flagged as ASK");
        assertFalse(query.isSelect(), "ASK should not also report itself as SELECT");
    }

    @Test
    @DisplayName("Parser -> AskQueryAst -> Query keeps the ASK form")
    void buildsQueryFromParsedAsk() {
        SparqlParser parser = newParserDefault();
        AskQueryAst ask = assertInstanceOf(AskQueryAst.class, parser.parse("ASK WHERE { ?s ?p ?o . }"));

        Query query = builder.toNextQuery(ask);

        assertTrue(query.isAsk());
        assertFalse(query.isSelect());
        assertTrue(query.getBody().get(0).isBGP());
    }

    @Test
    @DisplayName("A FILTER in the WHERE no longer throws an NPE")
    void buildsAskWithFilter() {
        FilterAst filter = new FilterAst(new GreaterThanAst(List.of(
                new VarAst("o"), new LiteralAst("5", null, null))));
        GroupGraphPatternAst where = new GroupGraphPatternAst(List.of(
                new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o")))),
                filter));
        AskQueryAst ask = new AskQueryAst(DatasetClauseAst.none(), where);

        Query query = builder.toNextQuery(ask);

        assertTrue(query.isAsk());
        assertTrue(query.getBody().get(1).isFilter(), "FILTER compiled into the body");
    }

    @Test
    @DisplayName("FROM graphs are copied onto the next Query dataset")
    void appliesFromClause() {
        DatasetClauseAst dataset = new DatasetClauseAst(
                Set.of(new IriAst("http://example.org/g")), Set.of());
        AskQueryAst ask = new AskQueryAst(dataset, whereOneTriple());

        Query query = builder.toNextQuery(ask);

        assertEquals(List.of("http://example.org/g"), labels(query.getFrom()));
        assertTrue(query.getNamed().isEmpty());
    }

    @Test
    @DisplayName("FROM NAMED graphs are copied onto the next Query dataset")
    void appliesFromNamedClause() {
        DatasetClauseAst dataset = new DatasetClauseAst(
                Set.of(), Set.of(new IriAst("http://example.org/g")));
        AskQueryAst ask = new AskQueryAst(dataset, whereOneTriple());

        Query query = builder.toNextQuery(ask);

        assertTrue(query.getFrom().isEmpty());
        assertEquals(List.of("http://example.org/g"), labels(query.getNamed()));
    }

    @Test
    @DisplayName("Inline VALUES is not supported yet → UnsupportedOperationException")
    void rejectsValuesClause() {
        ValuesAst values = new ValuesAst(List.of(
                new ValueMappingAst(Map.of(new VarAst("s"), new IriAst("http://example.org/x")))));
        AskQueryAst ask = new AskQueryAst(
                DatasetClauseAst.none(), whereOneTriple(), null, null, values);

        assertThrows(UnsupportedOperationException.class, () -> builder.toNextQuery(ask));
    }

    @Test
    @DisplayName("toNextQuery(null) throws NullPointerException")
    void rejectsNullAsk() {
        assertThrows(NullPointerException.class, () -> builder.toNextQuery(null));
    }

    @Test
    @DisplayName("ASK {} with empty WHERE compiles to an empty AND body")
    void buildsAskWithEmptyWhere() {
        AskQueryAst ask = new AskQueryAst(DatasetClauseAst.none(),
                new GroupGraphPatternAst(List.of()));

        Query query = builder.toNextQuery(ask);

        assertNotNull(query.getBody());
        assertTrue(query.getBody().isAnd());
        assertEquals(0, query.getBody().size(), "empty AND — no children");
        assertTrue(query.isAsk());
    }

    @Test
    @DisplayName("LIMIT and OFFSET are copied onto the next Query")
    void appliesLimitAndOffset() {
        SolutionModifierAst mod = SolutionModifierAst.withoutGroupBy(false, false, List.of(), 10L, 3L);
        AskQueryAst ask = new AskQueryAst(DatasetClauseAst.none(), whereOneTriple(), mod, null, null);

        Query query = builder.toNextQuery(ask);

        assertEquals(10, query.getLimit());
        assertEquals(3, query.getOffset());
    }

    @Test
    @DisplayName("ORDER BY stays out of scope for this first ASK increment")
    void rejectsOrderBy() {
        AskQueryAst ask = assertInstanceOf(AskQueryAst.class,
                newParserDefault().parse("ASK WHERE { ?s ?p ?o . } ORDER BY ?s"));

        UnsupportedOperationException error =
                assertThrows(UnsupportedOperationException.class, () -> builder.toNextQuery(ask));

        assertEquals("Solution modifiers (ORDER BY, GROUP BY, HAVING, DISTINCT, REDUCED) are not supported for ASK",
                error.getMessage());
    }

    private static List<String> labels(List<Node> nodes) {
        return nodes.stream().map(Node::getLabel).toList();
    }
}
