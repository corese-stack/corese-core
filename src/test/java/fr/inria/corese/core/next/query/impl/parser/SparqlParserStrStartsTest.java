package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrStartsAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : STRSTARTS")
class SparqlParserStrStartsTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("FILTER(STRSTARTS(?label, \"core\"))")
    void shouldParseStrStartsInFilter() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(STRSTARTS(?label, "core"))
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        StrStartsAst strStarts = assertInstanceOf(StrStartsAst.class, filter.operator());
        assertEquals("label", assertInstanceOf(VarAst.class, strStarts.getLeftArgument()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, strStarts.getRightArgument()).lexical());
    }

    @Test
    @DisplayName("BIND(STRSTARTS(?label, \"core\") AS ?startsWithCore)")
    void shouldParseStrStartsInBind() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRSTARTS(?label, "core") AS ?startsWithCore)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrStartsAst strStarts = assertInstanceOf(StrStartsAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, strStarts.getLeftArgument()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, strStarts.getRightArgument()).lexical());
        assertEquals("startsWithCore", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(STRSTARTS(\"foobar\", \"foo\") AS ?startsWithFoo)")
    void shouldParseStrStartsWithLiteralArguments() {
        SparqlParser parser = newParserDefault();

        SparqlQueryAst ast = (SparqlQueryAst) parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRSTARTS("foobar", "foo") AS ?startsWithFoo)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrStartsAst strStarts = assertInstanceOf(StrStartsAst.class, bind.expression());
        assertEquals("\"foobar\"", assertInstanceOf(LiteralAst.class, strStarts.getLeftArgument()).lexical());
        assertEquals("\"foo\"", assertInstanceOf(LiteralAst.class, strStarts.getRightArgument()).lexical());
        assertEquals("startsWithFoo", bind.variable().name());
    }

    @Test
    @DisplayName("ORDER BY STRSTARTS(?label, ?prefix)")
    void shouldParseOrderByStrStartsAndValidateVariableScope() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                  ?s <http://example.com/prefix> ?prefix .
                } ORDER BY STRSTARTS(?label, ?prefix)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        StrStartsAst strStarts = assertInstanceOf(StrStartsAst.class, select.solutionModifier().orderBy().getFirst().expression());
        assertEquals("label", assertInstanceOf(VarAst.class, strStarts.getLeftArgument()).name());
        assertEquals("prefix", assertInstanceOf(VarAst.class, strStarts.getRightArgument()).name());
    }

    @Test
    @DisplayName("ORDER BY STRSTARTS(?label, ?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByStrStartsWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY STRSTARTS(?label, ?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
