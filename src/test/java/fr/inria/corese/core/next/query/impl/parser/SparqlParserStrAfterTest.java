package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.api.exception.QueryValidationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.EqualsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.StrAfterAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SPARQL 1.1 - Parser and AST : STRAFTER")
class SparqlParserStrAfterTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("BIND(STRAFTER(?label, \"core\") AS ?suffix)")
    void shouldParseStrAfterInBind() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRAFTER(?label, "core") AS ?suffix)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrAfterAst strAfter = assertInstanceOf(StrAfterAst.class, bind.expression());
        assertEquals("label", assertInstanceOf(VarAst.class, strAfter.getLeftArgument()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, strAfter.getRightArgument()).lexical());
        assertEquals("suffix", bind.variable().name());
    }

    @Test
    @DisplayName("BIND(STRAFTER(\"foobar\", \"foo\") AS ?suffix)")
    void shouldParseStrAfterWithLiteralArguments() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  BIND(STRAFTER("foobar", "foo") AS ?suffix)
                }
                """);

        assertNotNull(ast);
        BindAst bind = assertInstanceOf(BindAst.class, ast.whereClause().patterns().getLast());
        StrAfterAst strAfter = assertInstanceOf(StrAfterAst.class, bind.expression());
        assertEquals("\"foobar\"", assertInstanceOf(LiteralAst.class, strAfter.getLeftArgument()).lexical());
        assertEquals("\"foo\"", assertInstanceOf(LiteralAst.class, strAfter.getRightArgument()).lexical());
        assertEquals("suffix", bind.variable().name());
    }

    @Test
    @DisplayName("FILTER(STRAFTER(?label, \"core\") = \"suffix\")")
    void shouldParseStrAfterInFilterComparison() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                  ?s ?p ?label .
                  FILTER(STRAFTER(?label, "core") = "suffix")
                }
                """);

        assertNotNull(ast);
        FilterAst filter = assertInstanceOf(FilterAst.class, ast.whereClause().patterns().getLast());
        EqualsAst equals = assertInstanceOf(EqualsAst.class, filter.operator());
        StrAfterAst strAfter = assertInstanceOf(StrAfterAst.class, equals.getLeftArgument());
        assertEquals("label", assertInstanceOf(VarAst.class, strAfter.getLeftArgument()).name());
        assertEquals("\"core\"", assertInstanceOf(LiteralAst.class, strAfter.getRightArgument()).lexical());
    }

    @Test
    @DisplayName("ORDER BY STRAFTER(?label, ?needle)")
    void shouldParseOrderByStrAfterAndValidateVariableScope() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                  ?s <http://example.com/needle> ?needle .
                } ORDER BY STRAFTER(?label, ?needle)
                """);

        assertNotNull(ast);
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class, ast);
        assertFalse(select.solutionModifier().orderBy().isEmpty());
        StrAfterAst strAfter = assertInstanceOf(StrAfterAst.class, select.solutionModifier().orderBy().getFirst().expression());
        assertEquals("label", assertInstanceOf(VarAst.class, strAfter.getLeftArgument()).name());
        assertEquals("needle", assertInstanceOf(VarAst.class, strAfter.getRightArgument()).name());
    }

    @Test
    @DisplayName("ORDER BY STRAFTER(?label, ?missing) — should reject variable not visible in WHERE")
    void shouldRejectOrderByStrAfterWhenVariableNotVisibleInWhere() {
        SparqlParser parser = newParserDefault();

        QueryValidationException exception = assertThrows(QueryValidationException.class, () -> parser.parse("""
                SELECT ?label WHERE {
                  ?s ?p ?label .
                } ORDER BY STRAFTER(?label, ?missing)
                """));

        assertEquals("Variable ?missing used in ORDER BY is not visible in WHERE clause", exception.getMessage());
    }
}
