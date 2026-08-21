package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : Functions on Numerics")
class SparqlParserNumericFunctionsTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(ABS(?x) AS ?abs)")
    void shouldParseAbsInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(ABS(?x) AS ?abs)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        AbsAst abs = assertInstanceOf(AbsAst.class, bind.expression());
        assertEquals("x", assertInstanceOf(VarAst.class, abs.argument()).name());
        assertEquals("abs", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(ROUND(?x))")
    void shouldParseRoundInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(ROUND(?x) > 0)
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        GreaterThanAst greaterThanAst = assertInstanceOf(GreaterThanAst.class, filter.operator());
        RoundAst round = assertInstanceOf(RoundAst.class, greaterThanAst.getLeftArgument());
        assertEquals("x", assertInstanceOf(VarAst.class, round.argument()).name());
    }

    @Test
    @DisplayName("FILTER(CEIL(?x))")
    void shouldParseCeilInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(CEIL(?x) > 0)
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        GreaterThanAst greaterThanAst = assertInstanceOf(GreaterThanAst.class, filter.operator());
        CeilAst ceil = assertInstanceOf(CeilAst.class, greaterThanAst.getLeftArgument());
        assertEquals("x", assertInstanceOf(VarAst.class, ceil.argument()).name());
    }

    @Test
    @DisplayName("FILTER(FLOOR(?x))")
    void shouldParseFloorInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(FLOOR(?x) > 0)
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        GreaterThanAst greaterThanAst = assertInstanceOf(GreaterThanAst.class, filter.operator());
        FloorAst floor = assertInstanceOf(FloorAst.class, greaterThanAst.getLeftArgument());
        assertEquals("x", assertInstanceOf(VarAst.class, floor.argument()).name());
    }

    @Test
    @DisplayName("BIND(RAND() AS ?rand)")
    void shouldParseRandInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(RAND() AS ?rand)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        assertInstanceOf(RandAst.class, bind.expression());
        assertEquals("rand", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(RAND())")
    void shouldParseRandInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(RAND() > 0)
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        GreaterThanAst greaterThanAst = assertInstanceOf(GreaterThanAst.class, filter.operator());
        assertInstanceOf(RandAst.class, greaterThanAst.getLeftArgument());
    }

    @Test
    @DisplayName("ORDER BY RAND()")
    void shouldParseOrderByRand() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?s WHERE {
                  ?s ?p ?o .
                } ORDER BY RAND()
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(RandAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY ABS(?s) — should keep variable scope validation working")
    void shouldParseOrderByAbs() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?s WHERE {
                  ?s ?p ?o .
                } ORDER BY ABS(?s)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        assertInstanceOf(AbsAst.class, select.solutionModifier().orderBy().getFirst().expression());
    }

    @Test
    @DisplayName("ORDER BY FLOOR(?z) — should reject variable not visible in WHERE")
    void shouldRejectOrderByFloorWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?s WHERE {
                  ?s ?p ?o .
                } ORDER BY FLOOR(?z)
                """));

        assertEquals("Variable ?z used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
