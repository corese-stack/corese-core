package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrBeforeAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : STRBEFORE")
class SparqlParserStrBeforeTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(STRBEFORE(?label, \"core\") AS ?prefix)")
    void shouldParseStrBeforeInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRBEFORE(?label, "core") AS ?prefix)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrBeforeAst strBefore = assertInstanceOf(StrBeforeAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, strBefore.getLeftArgument()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, strBefore.getRightArgument()).lexical());
        assertEquals("prefix", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(STRBEFORE(\"foobar\", \"bar\") AS ?prefix)")
    void shouldParseStrBeforeWithLiteralArguments() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRBEFORE("foobar", "bar") AS ?prefix)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrBeforeAst strBefore = assertInstanceOf(StrBeforeAst.class, bind.expression());
        assertEquals("\"foobar\"", assertInstanceOf(LiteralAst.class, strBefore.getLeftArgument()).lexical());
        assertEquals("\"bar\"", assertInstanceOf(LiteralAst.class, strBefore.getRightArgument()).lexical());
        assertEquals("prefix", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(STRBEFORE(?label, \"core\") = \"pre\")")
    void shouldParseStrBeforeInFilterComparison() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(STRBEFORE(?label, "core") = "pre")
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        StrBeforeAst strBefore = assertInstanceOf(StrBeforeAst.class, equals.getLeftArgument());
        assertEquals("label", assertInstanceOf(VarAst.class, strBefore.getLeftArgument()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, strBefore.getRightArgument()).lexical());
    }

    @Test
    @DisplayName("ORDER BY STRBEFORE(?label, ?needle)")
    void shouldParseOrderByStrBeforeAndValidateVariableScope() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                  ?s <http://example.com/needle> ?needle .
                } ORDER BY STRBEFORE(?label, ?needle)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        StrBeforeAst strBefore = assertInstanceOf(StrBeforeAst.class, select.solutionModifier().orderBy().getFirst().expression());
        assertEquals("label", assertInstanceOf(VarAst.class, strBefore.getLeftArgument()).name());
        assertEquals("needle", assertInstanceOf(VarAst.class, strBefore.getRightArgument()).name());
    }

    @Test
    @DisplayName("ORDER BY STRBEFORE(?label, ?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByStrBeforeWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY STRBEFORE(?label, ?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
