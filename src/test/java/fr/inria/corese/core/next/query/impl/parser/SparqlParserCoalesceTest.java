package fr.inria.corese.core.next.query.impl.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.CoalesceAst;

@DisplayName("SPARQL 1.1 - Parser and AST : COALESCE")
class SparqlParserCoalesceTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("COALESCE(?a, ?b, ?c)")
    void shouldParseCoalesceWithThreeArgs() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(COALESCE(?s, ?p, ?o) = "")
                }
                """);

        assertNotNull(ast);
        FilterAst filter = (FilterAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(EqualsAst.class, filter.operator());
        EqualsAst equalsAst = (EqualsAst) filter.operator();
        assertInstanceOf(CoalesceAst.class, equalsAst.getLeftArgument());
        CoalesceAst coalesce = (CoalesceAst) equalsAst.getLeftArgument();
        assertEquals(3, coalesce.arguments().size());
        assertInstanceOf(VarAst.class, coalesce.arguments().get(0));
        assertInstanceOf(VarAst.class, coalesce.arguments().get(1));
        assertInstanceOf(VarAst.class, coalesce.arguments().get(2));
        assertEquals("s", ((VarAst) coalesce.arguments().get(0)).name());
        assertEquals("p", ((VarAst) coalesce.arguments().get(1)).name());
        assertEquals("o", ((VarAst) coalesce.arguments().get(2)).name());
    }

    @Test
    @DisplayName("BIND(COALESCE(?a, \"default\") AS ?result)")
    void shouldParseCoalesceInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  BIND(COALESCE(?s, "default") AS ?result)
                }
                """);

        assertNotNull(ast);
        BindAst bind = (BindAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(CoalesceAst.class, bind.expression());

        CoalesceAst coalesce = (CoalesceAst) bind.expression();
        assertEquals(2, coalesce.arguments().size());
        assertInstanceOf(VarAst.class, coalesce.arguments().getFirst());
        assertInstanceOf(LiteralAst.class, coalesce.arguments().getLast());
        assertEquals("s", ((VarAst) coalesce.arguments().getFirst()).name());
    }

    @Test
    @DisplayName("COALESCE(?a) — single argument")
    void shouldParseCoalesceWithOneArg() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?o .
                  FILTER(COALESCE(?s) = 1)
                }
                """);

        assertNotNull(ast);
        FilterAst filter = (FilterAst) ast.whereClause().patterns().getLast();
        assertInstanceOf(EqualsAst.class, filter.operator());
        EqualsAst equalsAst = (EqualsAst) filter.operator();
        assertInstanceOf(CoalesceAst.class, equalsAst.getLeftArgument());
        CoalesceAst coalesce = (CoalesceAst) equalsAst.getLeftArgument();
        assertEquals(1, coalesce.arguments().size());
    }

    @Test
    @DisplayName("ORDER BY COALESCE(?s, ?o) — exercises VariableScopeAnalyzer on COALESCE via semantic validation")
    void shouldParseOrderByCoalesceAndValidateVariableScope() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?s WHERE {
                  ?s ?p ?o .
                } ORDER BY COALESCE(?s, ?o)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        var order = select.solutionModifier().orderBy().getFirst();
        assertInstanceOf(CoalesceAst.class, order.expression());
        CoalesceAst coalesce = (CoalesceAst) order.expression();
        assertEquals(2, coalesce.arguments().size());
    }

    @Test
    @DisplayName("ORDER BY COALESCE(?s, ?z) — should reject variable not visible in WHERE")
    void shouldRejectOrderByCoalesceWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?s WHERE {
                  ?s ?p ?o .
                } ORDER BY COALESCE(?s, ?z)
                """));

        assertEquals("Variable ?z used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
