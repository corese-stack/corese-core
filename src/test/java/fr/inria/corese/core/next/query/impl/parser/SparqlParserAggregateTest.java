package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.VariableScopeAnalyzer;
import fr.inria.corese.core.next.query.impl.sparql.ast.AggregateAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.AggregateFunction;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.OrderConditionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.GreaterThanAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SPARQL 1.1 — Parser and AST : aggregates")
class SparqlParserAggregateTest extends AbstractSparqlParserFeatureTest {

    private static AggregateAst lastBindAggregate(QueryAst ast) {
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        return assertInstanceOf(AggregateAst.class, bind.expression());
    }

    @Test
    @DisplayName("SELECT (COUNT(?s) AS ?count) : projection and variable linked with an expression")
    void shouldParseCountInSelectProjection() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT (COUNT(?s) AS ?count)
                WHERE {
                    ?s ?p ?o
                }
                """);

        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.projection().selectAll());
        assertEquals(1, select.projection().variables().size());
        assertEquals("count", select.projection().variables().getFirst().name());
        assertTrue(select.projection().expressionBoundVariables().contains("count"));
    }

    @Test
    @DisplayName("BIND(COUNT(?s) AS ?c)")
    void shouldParseCountWithVariableArgument() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?o .
                    BIND(COUNT(?s) AS ?c)
                }
                """);

        AggregateAst agg = lastBindAggregate(ast);
        assertEquals(AggregateFunction.COUNT, agg.function());
        assertFalse(agg.distinct());
        assertNotNull(agg.expression());
        assertEquals("s", assertInstanceOf(VarAst.class, agg.expression()).name());
        assertNull(agg.groupConcatSeparator());
    }

    @Test
    @DisplayName("BIND(COUNT(*) AS ?c)")
    void shouldParseCountStar() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?o .
                    BIND(COUNT(*) AS ?c)
                }
                """);

        AggregateAst agg = lastBindAggregate(ast);
        assertEquals(AggregateFunction.COUNT, agg.function());
        assertFalse(agg.distinct());
        assertNull(agg.expression());
        assertNull(agg.groupConcatSeparator());
    }

    @Test
    @DisplayName("BIND(COUNT(DISTINCT ?s) AS ?c)")
    void shouldParseCountDistinct() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?o .
                    BIND(COUNT(DISTINCT ?s) AS ?c)
                }
                """);

        AggregateAst agg = lastBindAggregate(ast);
        assertEquals(AggregateFunction.COUNT, agg.function());
        assertTrue(agg.distinct());
        assertEquals("s", assertInstanceOf(VarAst.class, agg.expression()).name());
    }

    @Test
    @DisplayName("BIND(SUM(?x) AS ?sum)")
    void shouldParseSum() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?x .
                    BIND(SUM(?x) AS ?sum)
                }
                """);

        AggregateAst agg = lastBindAggregate(ast);
        assertEquals(AggregateFunction.SUM, agg.function());
        assertFalse(agg.distinct());
        assertEquals("x", assertInstanceOf(VarAst.class, agg.expression()).name());
        assertNull(agg.groupConcatSeparator());
    }

    @Test
    @DisplayName("BIND(AVG(DISTINCT ?x) AS ?avg)")
    void shouldParseAvgDistinct() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?x .
                    BIND(AVG(DISTINCT ?x) AS ?avg)
                }
                """);

        AggregateAst agg = lastBindAggregate(ast);
        assertEquals(AggregateFunction.AVG, agg.function());
        assertTrue(agg.distinct());
        assertEquals("x", assertInstanceOf(VarAst.class, agg.expression()).name());
    }

    @Test
    @DisplayName("BIND(MIN(?x) AS ?m) / MAX(?x) / SAMPLE(?x)")
    void shouldParseMinMaxSample() {
        SparqlParser parser = newParserDefault();

        QueryAst minQ = parser.parse("""
                SELECT * WHERE { ?s ?p ?x . BIND(MIN(?x) AS ?m) }
                """);
        assertEquals(AggregateFunction.MIN, lastBindAggregate(minQ).function());

        QueryAst maxQ = parser.parse("""
                SELECT * WHERE { ?s ?p ?x . BIND(MAX(?x) AS ?m) }
                """);
        assertEquals(AggregateFunction.MAX, lastBindAggregate(maxQ).function());

        QueryAst sampleQ = parser.parse("""
                SELECT * WHERE { ?s ?p ?x . BIND(SAMPLE(?x) AS ?m) }
                """);
        assertEquals(AggregateFunction.SAMPLE, lastBindAggregate(sampleQ).function());
    }

    @Test
    @DisplayName("BIND(GROUP_CONCAT(?o) AS ?g) sans SEPARATOR")
    void shouldParseGroupConcatWithoutSeparator() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?o .
                    BIND(GROUP_CONCAT(?o) AS ?g)
                }
                """);

        AggregateAst agg = lastBindAggregate(ast);
        assertEquals(AggregateFunction.GROUP_CONCAT, agg.function());
        assertFalse(agg.distinct());
        assertEquals("o", assertInstanceOf(VarAst.class, agg.expression()).name());
        assertNull(agg.groupConcatSeparator());
    }

    @Test
    @DisplayName("BIND(GROUP_CONCAT(?o ; SEPARATOR = \"|\") AS ?g)")
    void shouldParseGroupConcatWithSeparator() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?o .
                    BIND(GROUP_CONCAT(?o ; SEPARATOR = "|") AS ?g)
                }
                """);

        AggregateAst agg = lastBindAggregate(ast);
        assertEquals(AggregateFunction.GROUP_CONCAT, agg.function());
        assertEquals("\"|\"", agg.groupConcatSeparator());
    }

    @Test
    @DisplayName("FILTER(COUNT(*) > 0)")
    void shouldParseAggregateInFilterComparison() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?o .
                    FILTER(COUNT(*) > 0)
                }
                """);

        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        GreaterThanAst gt = assertInstanceOf(GreaterThanAst.class, filter.operator());
        AggregateAst left = assertInstanceOf(AggregateAst.class, gt.getLeftArgument());
        assertEquals(AggregateFunction.COUNT, left.function());
        assertNull(left.expression());
        assertNotNull(gt.getRightArgument());
    }

    @Test
    @DisplayName("ORDER BY AVG(?val)")
    void shouldParseAggregateInOrderBy() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?s WHERE {
                    ?s ?p ?val
                }
                ORDER BY AVG(?val)
                """);

        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertEquals(1, select.solutionModifier().orderBy().size());
        OrderConditionAst cond = select.solutionModifier().orderBy().getFirst();
        AggregateAst agg = assertInstanceOf(AggregateAst.class, cond.expression());
        assertEquals(AggregateFunction.AVG, agg.function());
        assertEquals("val", assertInstanceOf(VarAst.class, agg.expression()).name());
    }

    @Test
    @DisplayName("VariableScopeAnalyzer see variables in aggregate")
    void shouldCollectVariablesReferencedInsideAggregate() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?price .
                    BIND(AVG(?price) AS ?a)
                }
                """);

        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        Set<String> refs = new VariableScopeAnalyzer().collectReferencedVariables(bind.expression());
        assertTrue(refs.contains("price"));
    }
}
