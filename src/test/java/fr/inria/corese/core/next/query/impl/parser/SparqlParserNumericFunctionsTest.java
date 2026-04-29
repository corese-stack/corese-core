package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.AbsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.CeilAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.FloorAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.RandAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.RoundAst;
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

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(ABS(?x) AS ?abs)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        AbsAst abs = assertInstanceOf(AbsAst.class, bind.expression());
        assertEquals("x", assertInstanceOf(VarAst.class, abs.getArgument()).name());
        assertEquals("abs", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(ROUND(?x))")
    void shouldParseRoundInFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(ROUND(?x))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        RoundAst round = assertInstanceOf(RoundAst.class, filter.operator());
        assertEquals("x", assertInstanceOf(VarAst.class, round.getArgument()).name());
    }

    @Test
    @DisplayName("FILTER(CEIL(?x))")
    void shouldParseCeilInFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(CEIL(?x))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        CeilAst ceil = assertInstanceOf(CeilAst.class, filter.operator());
        assertEquals("x", assertInstanceOf(VarAst.class, ceil.getArgument()).name());
    }

    @Test
    @DisplayName("FILTER(FLOOR(?x))")
    void shouldParseFloorInFilter() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(FLOOR(?x))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        FloorAst floor = assertInstanceOf(FloorAst.class, filter.operator());
        assertEquals("x", assertInstanceOf(VarAst.class, floor.getArgument()).name());
    }

    @Test
    @DisplayName("BIND(RAND() AS ?rand)")
    void shouldParseRandInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
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

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(RAND())
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        assertInstanceOf(RandAst.class, filter.operator());
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
